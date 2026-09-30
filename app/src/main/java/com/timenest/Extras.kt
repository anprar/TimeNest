package com.timenest

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.datastore.preferences.core.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.Calendar

object ScreenTimeoutHelper {
    val OPTIONS = linkedMapOf("Jangan ubah" to -1, "15 dtk" to 15000, "30 dtk" to 30000,
        "1 mnt" to 60000, "2 mnt" to 120000, "5 mnt" to 300000, "10 mnt" to 600000)
    private val MODE = intPreferencesKey("st_mode") // 0=jangan,1=ubah+restore
    private val VAL = intPreferencesKey("st_val")
    private val ORIG = intPreferencesKey("st_orig")

    suspend fun getMode(c: Context) = c.ds.data.map { it[MODE] ?: 1 }.first()
    suspend fun getVal(c: Context) = c.ds.data.map { it[VAL] ?: 60000 }.first()
    suspend fun set(c: Context, mode: Int, v: Int) { c.ds.edit { it[MODE] = mode; it[VAL] = v } }
    // default = nilai sistem saat ini (jujur), bukan 1 menit sembarangan
    suspend fun current(c: Context): Int {
        val s = c.ds.data.map { it[VAL] }.first()
        if (s != null) return s
        return try { Settings.System.getInt(c.contentResolver, Settings.System.SCREEN_OFF_TIMEOUT, 60000) } catch (_: Exception) { 60000 }
    }
    fun canWrite(c: Context) = Settings.System.canWrite(c)
    fun reqWrite(c: Context) { try { c.startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply { data = android.net.Uri.parse("package:${c.packageName}") }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (_: Exception) {} }
    fun applyNow(c: Context, v: Int) {
        if (!canWrite(c) || v < 0) return
        try { Settings.System.putInt(c.contentResolver, Settings.System.SCREEN_OFF_TIMEOUT, v) } catch (_: Exception) {}
    }
    suspend fun apply(c: Context) {
        if (getMode(c) != 1) return
        val v = getVal(c); if (v < 0) return
        if (!canWrite(c)) return
        try {
            val cur = Settings.System.getInt(c.contentResolver, Settings.System.SCREEN_OFF_TIMEOUT, 60000)
            c.ds.edit { it[ORIG] = cur }
            Settings.System.putInt(c.contentResolver, Settings.System.SCREEN_OFF_TIMEOUT, v)
        } catch (_: Exception) {}
    }
    suspend fun restore(c: Context) {
        if (getMode(c) != 1) return
        if (!canWrite(c)) return
        try {
            val o = c.ds.data.map { it[ORIG] ?: -1 }.first()
            if (o > 0) Settings.System.putInt(c.contentResolver, Settings.System.SCREEN_OFF_TIMEOUT, o)
        } catch (_: Exception) {}
    }
}

object ScheduleStore {
    private val EN = booleanPreferencesKey("sch_en")
    private val H = intPreferencesKey("sch_h")
    private val M = intPreferencesKey("sch_m")
    private val DUR = intPreferencesKey("sch_dur") // menit, 0=pakai jam selesai
    private val EH = intPreferencesKey("sch_eh")
    private val EM = intPreferencesKey("sch_em")
    suspend fun get(c: Context): String {
        val p = c.ds.data.first()
        return "Aktif:${p[EN] ?: false} ${p[H] ?: 16}:${"%02d".format(p[M] ?: 0)} dur:${p[DUR] ?: 60}mnt sampai:${p[EH] ?: 18}:${"%02d".format(p[EM] ?: 0)}"
    }
    suspend fun set(c: Context, en: Boolean, h: Int, m: Int, dur: Int, eh: Int, em: Int) {
        c.ds.edit { it[EN] = en; it[H] = h; it[M] = m; it[DUR] = dur; it[EH] = eh; it[EM] = em }
        if (en) arm(c) else disarm(c)
    }
    suspend fun arm(c: Context) {
        val p = c.ds.data.first()
        if (p[EN] != true) return
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, p[H] ?: 16); set(Calendar.MINUTE, p[M] ?: 0); set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }
        val pi = PendingIntent.getBroadcast(c, 99, Intent(c, ScheduleReceiver::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        try {
            val am = c.getSystemService(AlarmManager::class.java)
            try {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
            } catch (e: SecurityException) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi) // fallback ±menit
            }
        } catch (_: Exception) {}
    }
    fun disarm(c: Context) {
        val pi = PendingIntent.getBroadcast(c, 99, Intent(c, ScheduleReceiver::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        try { c.getSystemService(AlarmManager::class.java).cancel(pi) } catch (_: Exception) {}
    }
}

class ScheduleReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        CoroutineScope(Dispatchers.IO).launch {
            val p = c.ds.data.first()
            if (SessionStore.load(c) != null) { ScheduleStore.arm(c); return@launch } // jangan dobel
            val now = System.currentTimeMillis()
            val dur = p[intPreferencesKey("sch_dur")] ?: 60
            val end = if (dur > 0) now + dur * 60_000L
            else TimeCalc.endFromClock(now, p[intPreferencesKey("sch_eh")] ?: 18, p[intPreferencesKey("sch_em")] ?: 0, false).endTime
            SessionStore.save(c, now, end, if (dur > 0) "countdown" else "clock")
            TimerService.start(c)
            ScheduleStore.arm(c) // besok lagi
        }
    }
}
