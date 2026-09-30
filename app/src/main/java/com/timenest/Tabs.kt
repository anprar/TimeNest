package com.timenest

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

object Perms {
    fun notifOk(c: Context): Boolean {
        if (Build.VERSION.SDK_INT < 33) return c.getSystemService(NotificationManager::class.java).areNotificationsEnabled()
        return c.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
    }
    fun writeOk(c: Context) = Settings.System.canWrite(c)
    fun exactOk(c: Context): Boolean {
        if (Build.VERSION.SDK_INT < 31) return true
        return c.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
    }
    fun reqExact(c: Context) {
        try {
            if (Build.VERSION.SDK_INT >= 31)
                c.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = android.net.Uri.parse("package:${c.packageName}")
                }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Exception) {}
    }
}

object TimerOpts {
    var lockAtEnd = true
    var soundOn = true
}

object Draft {
    private val DM = androidx.datastore.preferences.core.intPreferencesKey("dMin")
    private val DMO = androidx.datastore.preferences.core.stringPreferencesKey("dMode")
    private val DEH = androidx.datastore.preferences.core.intPreferencesKey("dEH")
    private val DEM = androidx.datastore.preferences.core.intPreferencesKey("dEM")
    suspend fun load(c: Context): Array<Any> {
        val p = c.ds.data.first()
        return arrayOf(p[DM] ?: 10, p[DMO] ?: "countdown", p[DEH] ?: 18, p[DEM] ?: 0)
    }
    suspend fun save(c: Context, m: Int, mo: String, eh: Int, em: Int) {
        c.ds.edit { it[DM] = m; it[DMO] = mo; it[DEH] = eh; it[DEM] = em }
    }
}

// ---------- TAB 1 : WAKTU TUNGGU LAYAR ----------
class TimeoutFragment : Fragment() {
    private val items = listOf("15 dtk" to 15000, "30 dtk" to 30000, "1 mnt" to 60000,
        "2 mnt" to 120000, "5 mnt" to 300000, "10 mnt" to 600000,
        "15 mnt" to 900000, "Tetap Hidup" to 1800000)
    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, b: Bundle?): View {
        val v = i.inflate(R.layout.fragment_timeout, c, false)
        val grid = v.findViewById<GridLayout>(R.id.gridTimeout)
        val info = v.findViewById<TextView>(R.id.tvTimeoutInfo)
        val cb = v.findViewById<CheckBox>(R.id.cbSessionOnly)
        lifecycleScope.launch {
            cb.isChecked = ScreenTimeoutHelper.getMode(requireContext()) == 1
            buildGrid(grid, info, ScreenTimeoutHelper.current(requireContext()))
        }
        cb.setOnCheckedChangeListener { _, on ->
            lifecycleScope.launch {
                ScreenTimeoutHelper.set(requireContext(), if (on) 1 else 0, ScreenTimeoutHelper.getVal(requireContext()))
                Toast.makeText(context, if (on) "Timeout diubah selama sesi, dikembalikan setelahnya" else "Timeout diubah permanen", Toast.LENGTH_SHORT).show()
            }
        }
        return v
    }
    private fun buildGrid(grid: GridLayout, info: TextView, cur: Int) {
        grid.removeAllViews()
        info.text = "Aktif: ${nameOf(cur)}"
        items.forEachIndexed { idx, (name, ms) ->
            grid.addView(Button(context).apply {
                text = (if (ms == cur) "✓ " else "") + name
                setOnClickListener { pick(idx, info, grid) }
            })
        }
    }
    private fun nameOf(ms: Int) = items.firstOrNull { it.second == ms }?.first ?: "sistem (${ms / 1000} dtk)"
    private fun pick(idx: Int, info: TextView, grid: GridLayout) {
        val (name, ms) = items[idx]
        val c = requireContext()
        if (!Perms.writeOk(c)) {
            AlertDialog.Builder(c).setMessage("Izin 'Ubah Pengaturan Sistem' diperlukan untuk mengatur waktu tunggu layar.")
                .setNegativeButton("Batal", null)
                .setPositiveButton("Izinkan") { _, _ -> ScreenTimeoutHelper.reqWrite(c) }.show()
            return
        }
        lifecycleScope.launch {
            val sessionOnly = ScreenTimeoutHelper.getMode(c) == 1
            ScreenTimeoutHelper.set(c, if (sessionOnly) 1 else 0, ms)
            if (!sessionOnly) ScreenTimeoutHelper.applyNow(c, ms)
            buildGrid(grid, info, ms) // gambar ulang tanda ✓
            Toast.makeText(c, "Timeout: $name" + if (sessionOnly) " (selama sesi)" else "", Toast.LENGTH_SHORT).show()
        }
    }
}

// ---------- TAB 2 : TIMER TIDUR ----------
class SleepFragment : Fragment() {
    private var totalMin = 10
    private var mode = "countdown"
    private var endH = 18; private var endM = 0
    private var dragging = false
    private lateinit var tv: TextView
    private lateinit var sum: TextView
    private lateinit var ring: ProgressBar
    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, b: Bundle?): View {
        val v = i.inflate(R.layout.fragment_sleep, c, false)
        tv = v.findViewById(R.id.tvTime); sum = v.findViewById(R.id.tvSummary); ring = v.findViewById(R.id.ring)
        val lock = v.findViewById<CheckBox>(R.id.cbLock)
        val snd = v.findViewById<CheckBox>(R.id.cbSound)
        val warn = v.findViewById<TextView>(R.id.tvLockWarn)
        lock.setOnCheckedChangeListener { _, on -> TimerOpts.lockAtEnd = on }
        snd.setOnCheckedChangeListener { _, on -> TimerOpts.soundOn = on }
        if (!AdminHelper.isActive(requireContext()) && !AccessHelper.isOn(requireContext())) warn.visibility = View.VISIBLE
        lock.setOnClickListener {
            if (!AdminHelper.isActive(requireContext()) && !AccessHelper.isOn(requireContext())) {
                warn.visibility = View.VISIBLE
                AlertDialog.Builder(requireContext()).setMessage("Aktifkan pengunci (Admin utama, aksesibilitas cadangan).")
                    .setNegativeButton("Nanti", null)
                    .setPositiveButton("Admin") { _, _ -> AdminHelper.request(requireContext()) }
                    .setNeutralButton("Aksesibilitas") { _, _ -> AccessHelper.open(requireContext()) }.show()
            }
        }
        v.findViewById<View>(R.id.btnMinus).setOnClickListener { totalMin = (totalMin - 1).coerceAtLeast(1); mode = "countdown"; draw() }
        v.findViewById<View>(R.id.btnPlus).setOnClickListener { totalMin = (totalMin + 1).coerceAtMost(120); mode = "countdown"; draw() }
        val dial = v.findViewById<View>(R.id.dialWrap)
        dial.setOnTouchListener { vv, e ->
            val cx = vv.width / 2f; val cy = vv.height / 2f
            when (e.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    // hanya geser jika sentuhan tepat di cincin (bukan tengah/luar) agar scroll aman
                    val r = kotlin.math.hypot((e.x - cx).toDouble(), (cy - e.y).toDouble())
                    dragging = r > kotlin.math.min(cx, cy) * 0.55
                    if (!dragging) return@setOnTouchListener false
                    vv.parent.requestDisallowInterceptTouchEvent(true)
                }
                android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                    dragging = false
                    return@setOnTouchListener true
                }
            }
            if (!dragging) return@setOnTouchListener false
            if (e.action == android.view.MotionEvent.ACTION_DOWN || e.action == android.view.MotionEvent.ACTION_MOVE) {
                val deg = (Math.toDegrees(kotlin.math.atan2((e.x - cx).toDouble(), (cy - e.y).toDouble())) + 360) % 360
                totalMin = (deg / 3).toInt().coerceIn(1, 120) // lingkaran penuh = 2 jam
                mode = "countdown"; draw()
            }
            true
        }
        v.findViewById<View>(R.id.tabDur).setOnClickListener { mode = "countdown"; draw() }
        v.findViewById<View>(R.id.tabClock).setOnClickListener {
            TimePickerDialog(context, { _, h, m -> endH = h; endM = m; mode = "clock"; draw() }, endH, endM, true).show()
        }
        val grid = v.findViewById<GridLayout>(R.id.gridPreset)
        listOf(15, 30, 45, 60, 90, 120).forEach { p ->
            grid.addView(Button(context).apply {
                text = "%02d:%02d".format(p / 60, p % 60)
                setOnClickListener { totalMin = p; mode = "countdown"; draw() }
            })
        }
        v.findViewById<View>(R.id.btnStart).setOnClickListener { start() }
        lifecycleScope.launch {
            try {
                val d = Draft.load(requireContext())
                totalMin = (d[0] as Int).coerceIn(1, 120); mode = d[1] as String
                endH = d[2] as Int; endM = d[3] as Int
                draw()
            } catch (_: Exception) {}
        }
        draw()
        return v
    }
    override fun onPause() {
        super.onPause()
        val c = context ?: return
        lifecycleScope.launch { try { Draft.save(c, totalMin, mode, endH, endM) } catch (_: Exception) {} }
    }
    private fun draw() {
        if (mode == "countdown") {
            tv.text = "%02d:%02d".format(totalMin / 60, totalMin % 60)
            val now = System.currentTimeMillis()
            try {
                val end = TimeCalc.endFromDuration(now, totalMin / 60, totalMin % 60, 0)
                val cal = java.util.Calendar.getInstance().apply { timeInMillis = end }
                sum.text = "Selesai %02d:%02d • %s".format(cal.get(java.util.Calendar.HOUR_OF_DAY), cal.get(java.util.Calendar.MINUTE), TimeCalc.summary(end - now))
            } catch (e: Exception) { sum.text = e.message }
            ring.progress = (totalMin * 100 / 120).coerceAtMost(100)
        } else {
            tv.text = "%02d:%02d".format(endH, endM)
            val r = TimeCalc.endFromClock(System.currentTimeMillis(), endH, endM, false)
            sum.text = "Berjalan ${TimeCalc.summary(r.durationMs)} • selesai %02d:%02d${if (r.nextDay) " besok" else ""}".format(endH, endM)
            ring.progress = 50
        }
    }
    private fun start() {
        val c = requireContext()
        val now = System.currentTimeMillis()
        try {
            val end = if (mode == "countdown") TimeCalc.endFromDuration(now, totalMin / 60, totalMin % 60, 0)
            else TimeCalc.endFromClock(now, endH, endM, false).let {
                if (it.durationMs < 60_000) { Toast.makeText(c, "Jam selesai kurang dari 1 menit dari sekarang", Toast.LENGTH_LONG).show(); return }
                it.endTime
            }
            lifecycleScope.launch {
                if (SessionStore.pin(c) == null && !SessionStore.isSkipped(c)) {
                    Toast.makeText(c, "Buat PIN dulu atau lewati", Toast.LENGTH_LONG).show()
                    startActivity(Intent(c, SetupActivity::class.java)); return@launch
                }
                if (!Perms.notifOk(c)) {
                    Toast.makeText(c, "Izinkan notifikasi agar hitung mundur terlihat", Toast.LENGTH_LONG).show()
                    try { requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1) } catch (_: Exception) {}
                }
                if (SessionStore.load(c) != null) {
                    AlertDialog.Builder(c).setMessage("Timer sudah berjalan. Ganti dengan yang baru?")
                        .setNegativeButton("Batal", null)
                        .setPositiveButton("Ganti") { _, _ -> doStart(c, now, end) }.show()
                    return@launch
                }
                doStart(c, now, end)
            }
        } catch (e: Exception) { Toast.makeText(c, e.message, Toast.LENGTH_SHORT).show() }
    }
    private fun doStart(c: Context, now: Long, end: Long) {
        lifecycleScope.launch {
            SessionStore.save(c, now, end, mode)
            TimerService.start(c)
            FinishAlarm.schedule(c, end) // pengaman: kunci tepat waktu walau HP tidur (Doze)
            Toast.makeText(c, "Timer jalan di background — lihat notifikasi", Toast.LENGTH_LONG).show()
            activity?.run { moveTaskToBack(true); finish() } // paksa ke layar utama, aplikasi tertutup
        }
    }
}

// ---------- TAB 3 : PENJADWAL ----------
class ScheduleFragment : Fragment() {
    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, b: Bundle?): View {
        val v = i.inflate(R.layout.fragment_schedule, c, false)
        val info = v.findViewById<TextView>(R.id.tvSch)
        val warn = v.findViewById<TextView>(R.id.tvSchWarn)
        if (!Perms.exactOk(requireContext())) warn.visibility = View.VISIBLE
        lifecycleScope.launch { info.text = ScheduleStore.get(requireContext()) }
        v.findViewById<View>(R.id.btnSave).setOnClickListener {
            val h = v.findViewById<EditText>(R.id.eH).text.toString().toIntOrNull() ?: 16
            val m = v.findViewById<EditText>(R.id.eM).text.toString().toIntOrNull() ?: 0
            val d = v.findViewById<EditText>(R.id.eD).text.toString().toIntOrNull() ?: 60
            val eh = v.findViewById<EditText>(R.id.eEH).text.toString().toIntOrNull() ?: 18
            val em = v.findViewById<EditText>(R.id.eEM).text.toString().toIntOrNull() ?: 0
            if (!Perms.exactOk(requireContext())) Perms.reqExact(requireContext())
            lifecycleScope.launch {
                ScheduleStore.set(requireContext(), true, h, m, d, eh, em)
                info.text = ScheduleStore.get(requireContext())
                Toast.makeText(context, "Jadwal aktif tiap hari %02d:%02d".format(h, m), Toast.LENGTH_SHORT).show()
            }
        }
        v.findViewById<View>(R.id.btnOff).setOnClickListener {
            lifecycleScope.launch {
                ScheduleStore.set(requireContext(), false, 16, 0, 60, 18, 0)
                info.text = "Nonaktif"
            }
        }
        return v
    }
}
