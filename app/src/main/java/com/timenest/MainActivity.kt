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
        lifecycleScope.launch {
            if (SessionStore.pin(this@MainActivity) == null)
                startActivity(Intent(this@MainActivity, SetupActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        val banner = findViewById<android.widget.TextView>(R.id.bannerPerm)
        val ok = AdminHelper.isActive(this) && Perms.notifOk(this) && Perms.writeOk(this)
        banner.visibility = if (ok) View.GONE else View.VISIBLE
    }
}
