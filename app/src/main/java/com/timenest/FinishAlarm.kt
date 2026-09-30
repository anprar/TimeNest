package com.timenest

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.admin.DevicePolicyManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object FinishGuard { val done = mutableSetOf<Long>() }

object FinishAlarm {
    fun schedule(c: Context, end: Long) {
        try {
            val am = c.getSystemService(AlarmManager::class.java)
            try { am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, end, pending(c)) }
            catch (e: SecurityException) { am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, end, pending(c)) }
        } catch (_: Exception) {}
    }
    fun cancel(c: Context) {
        try { c.getSystemService(AlarmManager::class.java).cancel(pending(c)) } catch (_: Exception) {}
    }
    private fun pending(c: Context) =
        PendingIntent.getBroadcast(c, 77, Intent(c, FinishReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
}

// Bunyi tepat di endTime walau HP tidur (Doze). goAsync menahan wake-lock.
class FinishReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        val pend = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val t = SessionStore.load(c) ?: return@launch
                if (!FinishGuard.done.add(t.second)) return@launch
                if (TimerOpts.soundOn) {
                    try {
                        RingtoneManager.getRingtone(c, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))?.play()
                        val v = if (Build.VERSION.SDK_INT >= 31) c.getSystemService(VibratorManager::class.java)?.defaultVibrator
                        else @Suppress("DEPRECATION") c.getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator
                        v?.vibrate(android.os.VibrationEffect.createWaveform(longArrayOf(0, 500, 300, 500), -1))
                    } catch (_: Exception) {}
                }
                val status = if (!TimerOpts.lockAtEnd) "SELESAI (tanpa kunci, sesuai opsi)"
                else LockHelper.lock(c)
                ScreenTimeoutHelper.restore(c)
                SessionStore.addHist(c, "${java.util.Date()} | ${t.third} | $status")
                SessionStore.clear(c)
                TimerService.stop(c)
                c.getSystemService(NotificationManager::class.java).notify(2,
                    NotificationCompat.Builder(c, TimerService.CH)
                        .setContentTitle("Waktu habis").setContentText(status)
                        .setSmallIcon(android.R.drawable.ic_lock_idle_alarm).build())
            } finally { pend.finish() }
        }
    }
}
