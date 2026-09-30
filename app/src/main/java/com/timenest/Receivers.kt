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
                if (t != null && t.second > System.currentTimeMillis()) TimerService.start(c)
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
}
