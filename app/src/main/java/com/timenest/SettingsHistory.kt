package com.timenest

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.datastore.preferences.core.*
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

object UiPrefs {
    private val TAB = intPreferencesKey("tabAwal")
    private val LOCKMODE = stringPreferencesKey("lockMode") // mati|kunci
    suspend fun tab(c: Context) = c.ds.data.map { it[TAB] ?: 1 }.first()
    suspend fun setTab(c: Context, v: Int) { c.ds.edit { it[TAB] = v } }
    suspend fun lockMode(c: Context) = c.ds.data.map { it[LOCKMODE] ?: "kunci" }.first()
    suspend fun setLockMode(c: Context, v: String) { c.ds.edit { it[LOCKMODE] = v } }
}

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(32, 32, 32, 32) }
        setContentView(ScrollView(this).apply { addView(root) })
        fun h(t: String) { root.addView(TextView(this).apply { text = t; textSize = 18f; setPadding(0, 24, 0, 8) }) }
        fun p(t: String) { root.addView(TextView(this).apply { text = t }) }

        h("PIN Orang Tua")
        val stPin = TextView(this); root.addView(stPin)
        lifecycleScope.launch { stPin.text = if (SessionStore.pin(this@SettingsActivity) == null) "Belum ada PIN" else "PIN sudah diatur" }
        root.addView(Button(this).apply { text = "Atur PIN"; setOnClickListener { askPin() } })
        root.addView(Button(this).apply { text = "Hapus PIN"; setOnClickListener { delPin() } })

        h("Tab Awal")
        val rgTab = RadioGroup(this)
        listOf("Waktu tunggu layar", "Timer Tidur", "Penjadwal").forEachIndexed { idx, s ->
            rgTab.addView(RadioButton(this).apply { text = s; tag = idx })
        }
        root.addView(rgTab)
        lifecycleScope.launch {
            val t = UiPrefs.tab(this@SettingsActivity)
            (rgTab.getChildAt(t) as? RadioButton)?.isChecked = true
        }
        rgTab.setOnCheckedChangeListener { g, id ->
            val idx = (g.findViewById<RadioButton>(id)?.tag as? Int) ?: return@setOnCheckedChangeListener
            lifecycleScope.launch { UiPrefs.setTab(this@SettingsActivity, idx) }
        }

        h("Cara Mematikan Layar")
        p("Matikan: layar padam, bisa dibuka sidik jari/wajah.\nKunci: perangkat dikunci, perlu PIN/pola (lebih aman untuk anak).")
        val rgLock = RadioGroup(this)
        rgLock.addView(RadioButton(this).apply { text = "Matikan"; tag = "mati" })
        rgLock.addView(RadioButton(this).apply { text = "Kunci"; tag = "kunci" })
        root.addView(rgLock)
        lifecycleScope.launch {
            val m = UiPrefs.lockMode(this@SettingsActivity)
            for (k in 0 until rgLock.childCount) {
                val r = rgLock.getChildAt(k) as RadioButton
                if (r.tag == m) r.isChecked = true
            }
        }
        rgLock.setOnCheckedChangeListener { g, id ->
            val v = g.findViewById<RadioButton>(id)?.tag as? String ?: return@setOnCheckedChangeListener
            lifecycleScope.launch { UiPrefs.setLockMode(this@SettingsActivity, v) }
        }

        h("Izin")
        permRow(root, "Ubah Pengaturan Sistem", "Untuk waktu tunggu layar.", Perms.writeOk(this)) { ScreenTimeoutHelper.reqWrite(this) }
        permRow(root, "Administrator Perangkat", "Untuk mengunci saat waktu habis.", AdminHelper.isActive(this)) { AdminHelper.request(this) }
        permRow(root, "Notifikasi", "Status & kontrol timer.", Perms.notifOk(this)) { openNotif() }
        if (Build.VERSION.SDK_INT >= 31)
            permRow(root, "Alarm Persis", "Agar jadwal tepat waktu.", Perms.exactOk(this)) { Perms.reqExact(this) }
        permRow(root, "Abaikan Optimasi Baterai", "Agar tidak dimatikan OPPO/ColorOS.", ignoringBattery()) { reqIgnoreBattery() }

        h("Panduan OPPO/ColorOS")
        p("1. Pengaturan > Baterai > izinkan autostart & background.\n2. Matikan optimasi baterai untuk TimeNest.\n3. Kunci aplikasi di recent-apps.")
        root.addView(Button(this).apply { text = "Diagnosa"; setOnClickListener { diagnosa() } })
        root.addView(Button(this).apply { text = "Riwayat"; setOnClickListener { startActivity(Intent(this@SettingsActivity, HistoryActivity::class.java)) } })
    }

    override fun onResume() {
        super.onResume()
        // status disegarkan saat layar dibuka ulang; sengaja tanpa recreate() agar tidak loop
    }

    private fun permRow(root: LinearLayout, t: String, d: String, ok: Boolean, act: () -> Unit) {
        root.addView(TextView(this).apply { text = (if (ok) "✓ " else "✗ ") + t })
        root.addView(TextView(this).apply { text = d })
        root.addView(Button(this).apply { text = if (ok) "Aktif — cek ulang" else "Aktifkan"; setOnClickListener { act() } })
    }
    private fun ignoringBattery(): Boolean {
        val pm = getSystemService(PowerManager::class.java)
        return pm.isIgnoringBatteryOptimizations(packageName)
    }
    private fun reqIgnoreBattery() {
        try { startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply { data = Uri.parse("package:$packageName") }) }
        catch (_: Exception) { Toast.makeText(this, "Buka manual: Baterai > TimeNest", Toast.LENGTH_LONG).show() }
    }
    private fun openNotif() {
        try { startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply { putExtra(Settings.EXTRA_APP_PACKAGE, packageName) }) }
        catch (_: Exception) { Toast.makeText(this, "Buka manual: Notifikasi > TimeNest", Toast.LENGTH_LONG).show() }
    }
    private fun diagnosa() {
        val s = "Admin: ${AdminHelper.isActive(this)}\nNotifikasi: ${Perms.notifOk(this)}\nWriteSettings: ${Perms.writeOk(this)}\n" +
            (if (Build.VERSION.SDK_INT >= 31) "ExactAlarm: ${Perms.exactOk(this)}\n" else "") +
            "Baterai: ${ignoringBattery()}\nPIN: ${"belum dicek"}"
        AlertDialog.Builder(this).setTitle("Diagnosa").setMessage(s).setPositiveButton("OK", null).show()
    }
    private fun askPin() {
        val e = EditText(this).apply { hint = "PIN baru (min 4 digit)"; inputType = 129 }
        AlertDialog.Builder(this).setTitle("PIN orang tua").setView(e)
            .setNegativeButton("Batal", null)
            .setPositiveButton("Simpan") { _, _ ->
                val v = e.text.toString()
                if (v.length < 4) { Toast.makeText(this, "Min 4 digit", Toast.LENGTH_SHORT).show(); return@setPositiveButton }
                lifecycleScope.launch { SessionStore.setPin(this@SettingsActivity, PinUtil.hash(v)); Toast.makeText(this@SettingsActivity, "PIN tersimpan", Toast.LENGTH_SHORT).show(); recreate() }
            }.show()
    }
    private fun delPin() {
        val e = EditText(this).apply { hint = "PIN saat ini"; inputType = 129 }
        AlertDialog.Builder(this).setTitle("Hapus PIN").setView(e)
            .setNegativeButton("Batal", null)
            .setPositiveButton("Hapus") { _, _ -> lifecycleScope.launch {
                if (SessionStore.pin(this@SettingsActivity) == PinUtil.hash(e.text.toString())) {
                    SessionStore.clearPin(this@SettingsActivity); Toast.makeText(this@SettingsActivity, "PIN dihapus", Toast.LENGTH_SHORT).show(); recreate()
                } else Toast.makeText(this@SettingsActivity, "PIN salah", Toast.LENGTH_SHORT).show()
            }}.show()
    }
}

class HistoryActivity : AppCompatActivity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(32, 32, 32, 32) }
        setContentView(ScrollView(this).apply { addView(root) })
        root.addView(TextView(this).apply { text = "Riwayat"; textSize = 20f })
        val t = TextView(this); root.addView(t)
        lifecycleScope.launch { t.text = SessionStore.hist(this@HistoryActivity).ifEmpty { "Belum ada riwayat." } }
        root.addView(Button(this).apply { text = "Hapus (PIN)"; setOnClickListener {
            val e = EditText(this@HistoryActivity).apply { hint = "PIN"; inputType = 129 }
            AlertDialog.Builder(this@HistoryActivity).setView(e).setNegativeButton("Batal", null)
                .setPositiveButton("Hapus") { _, _ -> lifecycleScope.launch {
                    val h = SessionStore.pin(this@HistoryActivity)
                    if (h == null || (h.isNotEmpty() && h == PinUtil.hash(e.text.toString()))) {
                        SessionStore.clearHist(this@HistoryActivity); t.text = "Dihapus."
                    } else Toast.makeText(this@HistoryActivity, "PIN salah", Toast.LENGTH_SHORT).show()
                }}.show()
        }})
    }
}
