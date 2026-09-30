package com.timenest

import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.util.Calendar

class MainActivity : AppCompatActivity() {
    private var mode = "countdown"
    private var h = 0; private var m = 45; private var s = 0
    private var endH = 18; private var endM = 0
    private lateinit var info: TextView
    private lateinit var btnModeC: Button
    private lateinit var btnModeJ: Button

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        if (android.os.Build.VERSION.SDK_INT >= 33) requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(32, 32, 32, 32) }
        val scroll = ScrollView(this).apply { addView(root) }
        setContentView(scroll)

        root.addView(TextView(this).apply { text = "TimeNest — Screen Timer Global"; textSize = 20f })
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        btnModeC = Button(this).apply { text = "Hitung Mundur"; setOnClickListener { setMode("countdown") } }
        btnModeJ = Button(this).apply { text = "Selesai Pada Jam"; setOnClickListener { setMode("clock") } }
        row.addView(btnModeC); row.addView(btnModeJ); root.addView(row)

        // preset
        root.addView(TextView(this).apply { text = "Preset (menit):" })
        val prow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        listOf(5, 15, 30, 45, 60, 90, 120).forEach { p ->
            prow.addView(Button(this).apply { text = "$p"; setOnClickListener { h = p / 60; m = p % 60; s = 0; refresh() } })
        }
        root.addView(prow)

        val durRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val eH = EditText(this).apply { hint = "jam"; inputType = 2 }
        val eM = EditText(this).apply { hint = "mnt"; inputType = 2 }
        val eS = EditText(this).apply { hint = "dtk"; inputType = 2 }
        durRow.addView(eH); durRow.addView(eM); durRow.addView(eS)
        root.addView(durRow)
        root.addView(Button(this).apply { text = "Terapkan durasi manual"; setOnClickListener {
            h = eH.text.toString().toIntOrNull() ?: 0
            m = eM.text.toString().toIntOrNull() ?: 0
            s = eS.text.toString().toIntOrNull() ?: 0
            refresh()
        }})

        root.addView(Button(this).apply { text = "Pilih jam selesai"; setOnClickListener { pickClock() } })
        info = TextView(this).apply { textSize = 16f }; root.addView(info)
        root.addView(Button(this).apply { text = "MULAI"; setOnClickListener { start() } })
        root.addView(Button(this).apply { text = "Riwayat"; setOnClickListener { startActivity(Intent(this@MainActivity, HistoryActivity::class.java)) } })
        root.addView(Button(this).apply { text = "Pengaturan"; setOnClickListener { startActivity(Intent(this@MainActivity, SettingsActivity::class.java)) } })

        lifecycleScope.launch { mode = SessionStore.lastMode(this@MainActivity); setMode(mode) }
    }

    private fun setMode(mm: String) {
        mode = mm
        lifecycleScope.launch { SessionStore.setLastMode(this@MainActivity, mm) }
        btnModeC.isEnabled = mm != "countdown"; btnModeJ.isEnabled = mm != "clock"
        refresh()
    }

    private fun refresh() {
        val now = System.currentTimeMillis()
        info.text = try {
            if (mode == "countdown") {
                val end = TimeCalc.endFromDuration(now, h, m, s)
                val cal = Calendar.getInstance().apply { timeInMillis = end }
                "Mode: Hitung Mundur\nDurasi: ${TimeCalc.format(end - now)}\nSelesai: %02d:%02d".format(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
            } else {
                val r = TimeCalc.endFromClock(now, endH, endM, false)
                val tag = if (r.nextDay) " (besok)" else ""
                "Mode: Selesai Pada Jam\nSelesai: %02d:%02d%s\nDurasi: %s".format(endH, endM, tag, TimeCalc.summary(r.durationMs))
            }
        } catch (e: Exception) { "Durasi tidak valid (min 1 mnt, maks 24 jam)" }
    }

    private fun pickClock() {
        TimePickerDialog(this, { _, hh, mm -> endH = hh; endM = mm; setMode("clock") }, endH, endM, true).show()
    }

    private fun start() {
        val now = System.currentTimeMillis()
        try {
            val end = if (mode == "countdown") TimeCalc.endFromDuration(now, h, m, s)
            else TimeCalc.endFromClock(now, endH, endM, false).endTime
            val r = if (mode == "clock") TimeCalc.endFromClock(now, endH, endM, false) else null
            val msg = if (mode == "countdown") "Timer ${TimeCalc.summary(end - now)}, selesai %02d:%02d?".format(
                Calendar.getInstance().apply { timeInMillis = end }.get(Calendar.HOUR_OF_DAY),
                Calendar.getInstance().apply { timeInMillis = end }.get(Calendar.MINUTE))
            else "Timer ${TimeCalc.summary(r!!.durationMs)} sampai %02d:%02d${if (r.nextDay) " besok" else ""}?".format(endH, endM)
            AlertDialog.Builder(this).setMessage(msg)
                .setNegativeButton("Batal", null)
                .setPositiveButton("Mulai Timer") { _, _ ->
                    lifecycleScope.launch {
                        // cek PIN pertama kali
                        if (SessionStore.pin(this@MainActivity) == null) {
                            startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
                            Toast.makeText(this@MainActivity, "Buat PIN dulu", Toast.LENGTH_SHORT).show()
                            return@launch
                        }
                        SessionStore.save(this@MainActivity, now, end, mode)
                        TimerService.start(this@MainActivity)
                        startActivity(Intent(this@MainActivity, ActiveTimerActivity::class.java))
                    }
                }.show()
        } catch (e: Exception) { Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show() }
    }
}
