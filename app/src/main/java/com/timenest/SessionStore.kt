package com.timenest

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.ds by preferencesDataStore("timenest")

object SessionStore {
    private val END = longPreferencesKey("endTime")
    private val START = longPreferencesKey("startTime")
    private val MODE = stringPreferencesKey("mode")
    private val DUR = longPreferencesKey("durMs")
    private val PAUSED = longPreferencesKey("pausedRemain")
    private val PINH = stringPreferencesKey("pinHash")
    private val LASTMODE = stringPreferencesKey("lastMode")
    private val HIST = stringPreferencesKey("history")

    suspend fun save(c: Context, s: Long, e: Long, mode: String) {
        c.ds.edit { it[START] = s; it[END] = e; it[MODE] = mode; it[DUR] = e - s; it[PAUSED] = -1L }
    }
    suspend fun clear(c: Context) { c.ds.edit { it.remove(START); it.remove(END); it.remove(MODE); it.remove(PAUSED) } }
    suspend fun load(c: Context): Triple<Long, Long, String>? {
        val p = c.ds.data.first()
        val e = p[END] ?: return null
        return Triple(p[START] ?: 0L, e, p[MODE] ?: "countdown")
    }
    suspend fun pause(c: Context, remain: Long) { c.ds.edit { it[PAUSED] = remain } }
    suspend fun paused(c: Context): Long = c.ds.data.map { it[PAUSED] ?: -1L }.first()
    suspend fun resume(c: Context): Long {
        val r = paused(c); val now = System.currentTimeMillis()
        c.ds.edit { it[END] = now + r; it[PAUSED] = -1L }
        return now + r
    }
    suspend fun setPin(c: Context, h: String) { c.ds.edit { it[PINH] = h } }
    suspend fun pin(c: Context): String? = c.ds.data.map { it[PINH] }.first()
    suspend fun lastMode(c: Context) = c.ds.data.map { it[LASTMODE] ?: "countdown" }.first()
    suspend fun setLastMode(c: Context, m: String) { c.ds.edit { it[LASTMODE] = m } }
    suspend fun addHist(c: Context, line: String) {
        val old = c.ds.data.map { it[HIST] ?: "" }.first()
        c.ds.edit { it[HIST] = (line + "\n" + old).take(20000) }
    }
    suspend fun hist(c: Context) = c.ds.data.map { it[HIST] ?: "" }.first()
    suspend fun clearHist(c: Context) { c.ds.edit { it.remove(HIST) } }
}

object PinUtil {
    fun hash(pin: String): String {
        val md = java.security.MessageDigest.getInstance("SHA-256")
        return md.digest(pin.toByteArray()).joinToString("") { "%02x".format(it) }
    }
}
