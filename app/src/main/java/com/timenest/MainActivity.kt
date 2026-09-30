package com.timenest

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private val hd = android.os.Handler(android.os.Looper.getMainLooper())
    private var cachedEnd = 0L
    private var cachedMode = ""
    private var cachedPaused = -1L
    private val tick = object : Runnable {
        override fun run() {
            try { drawStatus() } catch (_: Exception) {}
            hd.postDelayed(this, 1000)
        }
    }
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        setContentView(R.layout.activity_main)
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)

        val pager = findViewById<androidx.viewpager2.widget.ViewPager2>(R.id.pager)
        pager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount() = 3
            override fun createFragment(p: Int) = when (p) {
                0 -> TimeoutFragment()
                1 -> SleepFragment()
                else -> ScheduleFragment()
            }
        }
        val tabs = findViewById<com.google.android.material.tabs.TabLayout>(R.id.tabs)
        TabLayoutMediator(tabs, pager) { t, p ->
            t.text = when (p) {
                0 -> "WAKTU TUNGGU\nLAYAR"
                1 -> "TIMER TIDUR"
                else -> "PENJADWAL"
            }
        }.attach()

        lifecycleScope.launch {
            try { pager.setCurrentItem(UiPrefs.tab(this@MainActivity), false) } catch (_: Exception) {}
        }
        findViewById<View>(R.id.btnSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        val banner = findViewById<View>(R.id.bannerPerm)
        banner.setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }
        findViewById<View>(R.id.statusCard).setOnClickListener {
            if (cachedEnd > 0) startActivity(Intent(this, ActiveTimerActivity::class.java))
        }
        lifecycleScope.launch {
            // hanya ke setup bila PIN belum ada DAN belum memilih LEWATI;
            // tanpa cek skipped -> loop Setup↔Main (blink-blink)
            if (SessionStore.pin(this@MainActivity) == null && !SessionStore.isSkipped(this@MainActivity))
                startActivity(Intent(this@MainActivity, SetupActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        val banner = findViewById<android.widget.TextView>(R.id.bannerPerm)
        val ok = (AdminHelper.isActive(this) || AccessHelper.isOn(this)) && Perms.notifOk(this) && Perms.writeOk(this)
        banner.visibility = if (ok) View.GONE else View.VISIBLE
        lifecycleScope.launch {
            val t = SessionStore.load(this@MainActivity)
            cachedEnd = t?.second ?: 0L
            cachedMode = t?.third ?: ""
            cachedPaused = try { SessionStore.paused(this@MainActivity) } catch (_: Exception) { -1L }
            try { drawStatus() } catch (_: Exception) {}
        }
        hd.removeCallbacks(tick); hd.post(tick)
    }

    override fun onPause() { hd.removeCallbacks(tick); super.onPause() }

    // Kartu status: hijau berjalan / oranye jeda / abu diam — ketuk untuk buka timer
    private fun drawStatus() {
        val card = findViewById<View>(R.id.statusCard) ?: return
        val t = findViewById<android.widget.TextView>(R.id.statusText) ?: return
        val s = findViewById<android.widget.TextView>(R.id.statusSub) ?: return
        if (cachedEnd <= 0) {
            card.setBackgroundColor(0xFF2A2A2A.toInt())
            t.text = "○ Tidak ada timer berjalan"
            s.text = "Atur waktu di bawah lalu tekan MULAI"
            return
        }
        if (cachedPaused >= 0) {
            card.setBackgroundColor(0xFF4A3200.toInt())
            t.text = "⏸ DIJEDA — ${TimeCalc.format(cachedPaused)}"
            s.text = "Ketuk untuk lanjutkan • $cachedMode"
            return
        }
        val r = TimeCalc.remain(cachedEnd, System.currentTimeMillis())
        if (r <= 0) {
            card.setBackgroundColor(0xFF2A2A2A.toInt())
            t.text = "○ Timer selesai"
            s.text = "Menutup sesi…"
            return
        }
        card.setBackgroundColor(0xFF0E3B1E.toInt())
        t.text = "● BERJALAN — ${TimeCalc.format(r)}"
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = cachedEnd }
        s.text = "Ketuk untuk lihat • selesai %02d:%02d • %s".format(
            cal.get(java.util.Calendar.HOUR_OF_DAY), cal.get(java.util.Calendar.MINUTE), cachedMode)
    }
}
