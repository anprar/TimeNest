package com.timenest

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(32, 32, 32, 32) }
        setContentView(ScrollView(this).apply { addView(root) })
        root.addView(TextView(this).apply { text = "Pengaturan"; textSize = 20f })
        val stAdmin = TextView(this); root.addView(stAdmin)
        fun ref() { stAdmin.text = if (AdminHelper.isActive(this)) "Izin kunci: AKTIF" else "Izin kunci: BELUM AKTIF" }
        ref()
        root.addView(Button(this).apply { text = "Aktifkan izin kunci"; setOnClickListener { AdminHelper.request(this@SettingsActivity); ref() } })
        root.addView(Button(this).apply { text = "Buat / ubah PIN"; setOnClickListener { askPin(null) } })
        root.addView(Button(this).apply { text = "Screen timeout sesi"; setOnClickListener { timeoutDlg() } })
        root.addView(Button(this).apply { text = "Jadwal harian"; setOnClickListener { startActivity(android.content.Intent(this@SettingsActivity, ScheduleActivity::class.java)) } })
        root.addView(TextView(this).apply { text = "Panduan OPPO/ColorOS:\n1. Pengaturan > Baterai > izinkan autostart & background.\n2. Matikan optimasi baterai untuk TimeNest.\n3. Kunci aplikasi di recent-apps." })
        root.addView(Button(this).apply { text = "Diagnosa"; setOnClickListener {
            AlertDialog.Builder(this@SettingsActivity).setMessage(
                "Admin: ${AdminHelper.isActive(this@SettingsActivity)}\nNotifikasi: pastikan diizinkan.\nBaterai: matikan pembatasan untuk TimeNest."
            ).setPositiveButton("OK", null).show()
        }})
    }
    private fun timeoutDlg() {
        if (!ScreenTimeoutHelper.canWrite(this)) { ScreenTimeoutHelper.reqWrite(this); Toast.makeText(this, "Izinkan ubah pengaturan sistem dulu", Toast.LENGTH_LONG).show(); return }
        val names = ScreenTimeoutHelper.OPTIONS.keys.toTypedArray()
        AlertDialog.Builder(this).setTitle("Timeout selama sesi (restore otomatis)")
            .setItems(names) { _, i ->
                val v = ScreenTimeoutHelper.OPTIONS[names[i]] ?: -1
                lifecycleScope.launch { ScreenTimeoutHelper.set(this@SettingsActivity, if (v < 0) 0 else 1, v); Toast.makeText(this@SettingsActivity, "Tersimpan: ${names[i]}", Toast.LENGTH_SHORT).show() }
            }.show()
    }
    private fun askPin(cur: String?) {
        val e = EditText(this).apply { hint = "PIN baru (4-6 digit)"; inputType = 129 }
        AlertDialog.Builder(this).setTitle("PIN orang tua").setView(e)
            .setNegativeButton("Batal", null)
            .setPositiveButton("Simpan") { _, _ ->
                val v = e.text.toString()
                if (v.length < 4) { Toast.makeText(this, "Min 4 digit", Toast.LENGTH_SHORT).show(); return@setPositiveButton }
                lifecycleScope.launch { SessionStore.setPin(this@SettingsActivity, PinUtil.hash(v)); Toast.makeText(this@SettingsActivity, "PIN tersimpan", Toast.LENGTH_SHORT).show() }
            }.show()
    }
}

class HistoryActivity : AppCompatActivity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(32, 32, 32, 32) }
        setContentView(ScrollView(this).apply { addView(root) })
        val t = TextView(this); root.addView(t)
        lifecycleScope.launch { t.text = SessionStore.hist(this@HistoryActivity).ifEmpty { "Belum ada riwayat." } }
        root.addView(Button(this).apply { text = "Hapus (PIN)"; setOnClickListener {
            val e = EditText(this@HistoryActivity).apply { hint = "PIN"; inputType = 129 }
            AlertDialog.Builder(this@HistoryActivity).setView(e).setNegativeButton("Batal", null)
                .setPositiveButton("Hapus") { _, _ -> lifecycleScope.launch {
                    if (SessionStore.pin(this@HistoryActivity) == PinUtil.hash(e.text.toString())) {
                        SessionStore.clearHist(this@HistoryActivity); t.text = "Dihapus."
                    } else Toast.makeText(this@HistoryActivity, "PIN salah", Toast.LENGTH_SHORT).show()
                }}.show()
        }})
    }
}
