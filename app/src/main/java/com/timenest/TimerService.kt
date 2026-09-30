package com.timenest

import android.app.*
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*

class TimerService : Service() {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var warned = mutableSetOf<Long>()
    companion object {
        const val CH = "timenest_timer"
        fun start(c: Context) {
            val i = Intent(c, TimerService::class.java)
            if (Build.VERSION.SDK_INT >= 26) c.startForegroundService(i) else c.startService(i)
        }
        fun stop(c: Context) { c.stopService(Intent(c, TimerService::class.java)) }
    }
    override fun onBind(i: Intent?): IBinder? = null
    override fun onCreate() {
        super.onCreate()
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CH, "Screen Timer", NotificationManager.IMPORTANCE_LOW))
    }
    override fun onStartCommand(i: Intent?, f: Int, sid: Int): Int {
        startForeground(1, notif("Memulai..."))
        scope.launch {
            ScreenTimeoutHelper.apply(this@TimerService)
            while (isActive) {
                val t = SessionStore.load(this@TimerService)
                if (t == null) { stopSelf(); break }
                val now = System.currentTimeMillis()
                val remain = t.second - now
                if (remain <= 0) { onFinish(t.first, t.second, t.third); break }
                if (TimerOpts.soundOn) checkWarn(remain)
                val nm = getSystemService(NotificationManager::class.java)
                nm.notify(1, notif("Sisa ${TimeCalc.format(remain)} • selesai ${hm(t.second)}"))
                delay(5000)
            }
        }
        return START_STICKY
    }
    private fun notif(txt: String): Notification {
        val pi = PendingIntent.getActivity(this, 0, Intent(this, ActiveTimerActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        return NotificationCompat.Builder(this, CH)
            .setContentTitle("Screen Timer aktif").setContentText(txt)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentIntent(pi).setOngoing(true).build()
    }
    private fun hm(ts: Long): String {
        val c = java.util.Calendar.getInstance().apply { timeInMillis = ts }
        return "%02d:%02d".format(c.get(java.util.Calendar.HOUR_OF_DAY), c.get(java.util.Calendar.MINUTE))
    }
    private suspend fun checkWarn(remain: Long) {
        listOf(10 * 60_000L, 5 * 60_000L, 60_000L).forEach { w ->
            if (remain <= w && !warned.contains(w)) {
                warned.add(w); warn("Sisa ${TimeCalc.summary(remain)}")
            }
        }
    }
    private fun warn(t: String) {
        try {
            RingtoneManager.getRingtone(this, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))?.play()
            val v = if (Build.VERSION.SDK_INT >= 31) getSystemService(VibratorManager::class.java)?.defaultVibrator
            else @Suppress("DEPRECATION") getSystemService(VIBRATOR_SERVICE) as android.os.Vibrator
            v?.vibrate(android.os.VibrationEffect.createOneShot(400, 100))
        } catch (_: Exception) {}
    }
    private suspend fun onFinish(start: Long, end: Long, mode: String) {
        if (TimerOpts.soundOn) warn("Waktu habis")
        val locked = if (TimerOpts.lockAtEnd) lockNow() else false
        val status = when {
            !TimerOpts.lockAtEnd -> "SELESAI (tanpa kunci, sesuai opsi)"
            locked -> "SELESAI dikunci"
            else -> "SELESAI gagal kunci (izin Admin belum aktif)"
        }
        ScreenTimeoutHelper.restore(this)
        SessionStore.addHist(this, "${java.util.Date()} | $mode | $status")
        SessionStore.clear(this)
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(2, NotificationCompat.Builder(this, CH)
            .setContentTitle("Waktu habis").setContentText(status)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm).build())
        stopSelf()
    }
    private fun lockNow(): Boolean {
        return try {
            val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val c = ComponentName(this, TimeNestDeviceAdmin::class.java)
            if (dpm.isAdminActive(c)) { dpm.lockNow(); true } else false
        } catch (_: Exception) { false }
    }
    override fun onDestroy() { CoroutineScope(Dispatchers.IO).launch { ScreenTimeoutHelper.restore(this@TimerService) }; scope.cancel(); super.onDestroy() }
}
