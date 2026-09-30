package com.timenest

import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TimeNestDeviceAdmin : DeviceAdminReceiver() {
    override fun onEnabled(c: Context, i: Intent) { Toast.makeText(c, "Izin kunci aktif", Toast.LENGTH_SHORT).show() }
}

class BootReceiver : android.content.BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        if (i.action == Intent.ACTION_BOOT_COMPLETED) {
            CoroutineScope(Dispatchers.IO).launch {
                val t = SessionStore.load(c)
                if (t != null && t.second > System.currentTimeMillis()) {
                    TimerService.start(c)
                    FinishAlarm.schedule(c, t.second)
                }
                try { ScheduleStore.arm(c) } catch (_: Exception) {}
            }
        }
    }
}

object AdminHelper {
    fun isActive(c: Context): Boolean {
        val dpm = c.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        return dpm.isAdminActive(ComponentName(c, TimeNestDeviceAdmin::class.java))
    }
    fun request(c: Context) {
        val i = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, ComponentName(c, TimeNestDeviceAdmin::class.java))
            putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Diperlukan agar TimeNest dapat mengunci layar saat waktu habis.")
        }
        c.startActivity(i)
    }
    // Untuk uninstal normal: nonaktifkan dulu dari sini, tak perlu cari di pengaturan HP.
    fun remove(c: Context) {
        try {
            val dpm = c.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            dpm.removeActiveAdmin(ComponentName(c, TimeNestDeviceAdmin::class.java))
            Toast.makeText(c, "Admin nonaktif — aplikasi bisa diuninstal", Toast.LENGTH_LONG).show()
        } catch (e: Exception) { Toast.makeText(c, "Gagal: ${e.message}", Toast.LENGTH_SHORT).show() }
    }
}

object AccessHelper {
    fun isOn(c: Context): Boolean {
        val s = android.provider.Settings.Secure.getString(c.contentResolver, "enabled_accessibility_services") ?: return false
        return s.contains(c.packageName, true) && s.contains("TimeNestAccess", true)
    }
    fun open(c: Context) {
        try { c.startActivity(Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        catch (_: Exception) {}
    }
}

// Admin UTAMA, aksesibilitas cadangan. Urutan: admin > aksesibilitas > jujur gagal.
object LockHelper {
    suspend fun lock(c: Context): String {
        try { UiPrefs.lockMode(c) } catch (_: Exception) {}
        val adminOk = try {
            val dpm = c.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val cn = ComponentName(c, TimeNestDeviceAdmin::class.java)
            if (dpm.isAdminActive(cn)) { dpm.lockNow(); true } else false
        } catch (_: Exception) { false }
        if (adminOk) return "SELESAI dikunci via Admin"
        val accessOk = try { TimeNestAccess.lock() } catch (_: Exception) { false }
        if (accessOk) return "SELESAI, layar dimatikan via aksesibilitas"
        return "SELESAI tapi GAGAL mengunci — aktifkan Admin Perangkat atau Aksesibilitas di Pengaturan"
    }
}
