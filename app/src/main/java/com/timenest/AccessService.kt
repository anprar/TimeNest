package com.timenest

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

// Alternatif Admin: mengunci layar tanpa menghalangi uninstal.
// Aktif/nonaktif dari Pengaturan > Aksesibilitas (mudah, tanpa jebakan uninstal).
class TimeNestAccess : AccessibilityService() {
    companion object {
        private var inst: TimeNestAccess? = null
        fun lock(): Boolean = try {
            inst?.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN) == true
        } catch (_: Exception) { false }
    }
    override fun onServiceConnected() { inst = this }
    override fun onUnbind(intent: android.content.Intent?): Boolean { inst = null; return super.onUnbind(intent) }
    override fun onAccessibilityEvent(e: AccessibilityEvent?) {}
    override fun onInterrupt() {}
}
