package com.timenest

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.util.Calendar

class ActiveTimerActivity : AppCompatActivity() {
    private lateinit var txt: TextView
    private lateinit var sub: TextView
    private var pendingPin: (() -> Unit)? = null
    private val pinLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) pendingPin?.invoke()
        pendingPin = null
    }
    private val hd = Handler(Looper.getMainLooper())
    private var end = 0L; private var start = 0L; private var mode = ""
    private val tick = object : Runnable {
        override fun run() {
            val now = System.currentTimeMillis()
            val r = TimeCalc.remain(end, now)
            txt.text = TimeCalc.format(r)
            if (r <= 0) { finishTimer(); return }
            hd.postDelayed(this, 1000)
        }
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(32, 48, 32, 32) }
        setContentView(root)
        txt = TextView(this).apply { textSize = 56f }; root.addView(txt)
        sub = TextView(this).apply { textSize = 16f }; root.addView(sub)
        val bP = Button(this).apply { text = "Pause / Resume"; setOnClickListener { askPin { togglePause() } } }
        val bX = Button(this).apply { text = "Batalkan (PIN)"; setOnClickListener { askPin { cancel() } } }
        root.addView(bP); root.addView(bX)
        lifecycleScope.launch {
            val t = SessionStore.load(this@ActiveTimerActivity)
            if (t == null) { finish(); return@launch }
            start = t.first; end = t.second; mode = t.third
            val cal = Calendar.getInstance().apply { timeInMillis = end }
            sub.text = "Mode: $mode\nSelesai: %02d:%02d\nDurasi awal: %s".format(
                cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), TimeCalc.summary(end - start))
            // cek status izin lock
            if (!isAdmin() && !AccessHelper.isOn(this@ActiveTimerActivity)) sub.append("\n⚠ Metode kunci BELUM aktif — perangkat tidak akan terkunci otomatis.")
            hd.post(tick)
        }
    }

    private fun isAdmin(): Boolean {
        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        return dpm.isAdminActive(ComponentName(this, TimeNestDeviceAdmin::class.java))
    }

    private fun togglePause() {
        lifecycleScope.launch {
            val p = SessionStore.paused(this@ActiveTimerActivity)
            if (p < 0) {
                val r = TimeCalc.remain(end, System.currentTimeMillis())
                SessionStore.pause(this@ActiveTimerActivity, r)
                TimerService.stop(this@ActiveTimerActivity)
                FinishAlarm.cancel(this@ActiveTimerActivity)
                hd.removeCallbacks(tick)
                txt.text = TimeCalc.format(r) + " (jeda)"
                Toast.makeText(this@ActiveTimerActivity, "Dijeda", Toast.LENGTH_SHORT).show()
            } else {
                end = SessionStore.resume(this@ActiveTimerActivity)
                TimerService.start(this@ActiveTimerActivity)
                FinishAlarm.schedule(this@ActiveTimerActivity, end)
                hd.post(tick)
            }
        }
    }

    private fun cancel() {
        lifecycleScope.launch {
            SessionStore.addHist(this@ActiveTimerActivity, "${java.util.Date()} | $mode | DIBATALKAN")
            SessionStore.clear(this@ActiveTimerActivity)
            TimerService.stop(this@ActiveTimerActivity)
            FinishAlarm.cancel(this@ActiveTimerActivity)
            finish()
        }
    }

    private fun finishTimer() {
        lifecycleScope.launch {
            SessionStore.addHist(this@ActiveTimerActivity, "${java.util.Date()} | $mode | SELESAI")
            SessionStore.clear(this@ActiveTimerActivity)
            finish()
        }
    }

    private fun askPin(ok: () -> Unit) {
        lifecycleScope.launch {
            if (SessionStore.pin(this@ActiveTimerActivity) == null || SessionStore.isSkipped(this@ActiveTimerActivity)) {
                ok(); return@launch // mode bebas PIN
            }
            pendingPin = ok
            pinLauncher.launch(PinActivity.createIntent(this@ActiveTimerActivity, "verify"))
        }
    }

    override fun onDestroy() { hd.removeCallbacks(tick); super.onDestroy() }
    companion object {
        fun open(c: Context) { c.startActivity(Intent(c, ActiveTimerActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }
}
