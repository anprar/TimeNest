package com.timenest

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class SetupActivity : AppCompatActivity() {
    private lateinit var stepPin: LinearLayout
    private lateinit var stepPerm: LinearLayout
    private lateinit var permBox: LinearLayout

    override fun onResume() {
        super.onResume()
        if (::permBox.isInitialized && stepPerm.visibility == View.VISIBLE) refreshPerms()
    }

    private fun refreshPerms() {
        permBox.removeAllViews()
        permRow(permBox, "Ubah Pengaturan Sistem", "Waktu tunggu layar.", Perms.writeOk(this)) { ScreenTimeoutHelper.reqWrite(this) }
        permRow(permBox, "Administrator Perangkat", "Kunci layar saat waktu habis.", AdminHelper.isActive(this)) { AdminHelper.request(this) }
        permRow(permBox, "Notifikasi", "Tampilkan hitung mundur.", Perms.notifOk(this)) { reqNotif() }
        if (Build.VERSION.SDK_INT >= 31)
            permRow(permBox, "Alarm Persis", "Jadwal tepat waktu.", Perms.exactOk(this)) { Perms.reqExact(this) }
        permRow(permBox, "Abaikan Optimasi Baterai", "Tidak dimatikan OPPO.", ignoringBattery()) { reqIgnoreBattery() }
    }

    private val pinLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) {
            lifecycleScope.launch { SessionStore.setSkipped(this@SetupActivity, false) }
            showPerm(); Toast.makeText(this, "PIN tersimpan", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        lifecycleScope.launch {
            if (SessionStore.pin(this@SetupActivity) != null) {
                startActivity(Intent(this@SetupActivity, MainActivity::class.java)); finish(); return@launch
            }
            build()
        }
    }

    private fun build() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(48, 64, 48, 32)
            setBackgroundColor(Color.parseColor("#121212"))
        }
        setContentView(ScrollView(this).apply { addView(root) })
        root.addView(TextView(this).apply { text = "TimeNest"; textSize = 26f; setTextColor(Color.WHITE) })
        root.addView(TextView(this).apply { text = "Batasi waktu HP anak, tanpa iklan."; setTextColor(Color.GRAY) })

        stepPin = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        stepPin.addView(TextView(this).apply { text = "\nLangkah 1 — Buat PIN orang tua"; textSize = 18f; setTextColor(Color.WHITE) })
        stepPin.addView(Button(this).apply {
            text = "BUAT PIN (tombol angka)"; setOnClickListener { pinLauncher.launch(PinActivity.createIntent(this@SetupActivity, "create")) }
        })
        stepPin.addView(Button(this).apply {
            text = "LEWATI (tanpa PIN — semua tindakan bebas)"; setOnClickListener {
                lifecycleScope.launch { SessionStore.setSkipped(this@SetupActivity, true); showPerm() }
            }
        })
        root.addView(stepPin)

        stepPerm = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; visibility = View.GONE }
        stepPerm.addView(TextView(this).apply { text = "\nLangkah 2 — Izin yang dibutuhkan (hijau = sudah, abu = belum)"; textSize = 18f; setTextColor(Color.WHITE) })
        permBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        stepPerm.addView(permBox)
        refreshPerms()
        stepPerm.addView(Button(this).apply {
            text = "MASUK APLIKASI"; setOnClickListener {
                lifecycleScope.launch {
                    if (SessionStore.pin(this@SetupActivity) == null && !SessionStore.isSkipped(this@SetupActivity)) {
                        Toast.makeText(this@SetupActivity, "Buat PIN dulu atau LEWATI", Toast.LENGTH_SHORT).show(); return@launch
                    }
                    startActivity(Intent(this@SetupActivity, MainActivity::class.java)); finish()
                }
            }
        })
        root.addView(stepPerm)
    }

    private fun showPerm() {
        stepPin.visibility = View.GONE
        stepPerm.visibility = View.VISIBLE
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
    }

    private fun permRow(root: LinearLayout, t: String, d: String, ok: Boolean, act: () -> Unit) {
        root.addView(TextView(this).apply {
            text = (if (ok) "✓ SUDAH ✓  " else "✗ BELUM  ") + t
            setTextColor(if (ok) Color.parseColor("#4CAF50") else Color.parseColor("#9E9E9E"))
            textSize = 16f
        })
        root.addView(TextView(this).apply { text = d; setTextColor(Color.GRAY) })
        root.addView(Button(this).apply {
            text = if (ok) "Aktif ✓" else "Aktifkan"; isEnabled = !ok
            setOnClickListener { act() }
        })
    }

    private fun reqNotif() {
        try { startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply { putExtra(Settings.EXTRA_APP_PACKAGE, packageName) }) }
        catch (_: Exception) {}
    }
    private fun ignoringBattery() =
        getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(packageName)
    private fun reqIgnoreBattery() {
        try { startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply { data = Uri.parse("package:$packageName") }) }
        catch (_: Exception) {}
    }

    override fun onBackPressed() {
        lifecycleScope.launch {
            if (SessionStore.pin(this@SetupActivity) == null) finish() // belum ada PIN = keluar
            else super.onBackPressed()
        }
    }
}
