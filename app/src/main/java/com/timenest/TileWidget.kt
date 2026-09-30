package com.timenest

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.app.PendingIntent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.RemoteViews
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class QSTile : TileService() {
    override fun onStartListening() {
        CoroutineScope(Dispatchers.IO).launch {
            val t = SessionStore.load(this@QSTile)
            qsTile?.apply {
                state = if (t == null) Tile.STATE_INACTIVE else Tile.STATE_ACTIVE
                label = if (t == null) "TimeNest" else "Sisa ${TimeCalc.format(t.second - System.currentTimeMillis())}"
                contentDescription = "Buka TimeNest"
                updateTile()
            }
        }
    }
    override fun onClick() {
        // tidak boleh batal tanpa PIN -> selalu buka aplikasi
        startActivityAndCollapse(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

class TimerWidget : AppWidgetProvider() {
    override fun onUpdate(c: Context, m: AppWidgetManager, ids: IntArray) {
        CoroutineScope(Dispatchers.IO).launch {
            val t = SessionStore.load(c)
            val txt = if (t == null) "TimeNest\nTidak ada timer"
            else "Sisa ${TimeCalc.format(t.second - System.currentTimeMillis())}\nSelesai ${hm(t.second)} • ${t.third}"
            ids.forEach { id ->
                val v = RemoteViews(c.packageName, R.layout.widget_timer)
                v.setTextViewText(R.id.wtxt, txt)
                val pi = PendingIntent.getActivity(c, 0, Intent(c, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
                v.setOnClickPendingIntent(R.id.wbtn, pi)
                m.updateAppWidget(id, v)
            }
        }
    }
    private fun hm(ts: Long): String {
        val c = java.util.Calendar.getInstance().apply { timeInMillis = ts }
        return "%02d:%02d".format(c.get(java.util.Calendar.HOUR_OF_DAY), c.get(java.util.Calendar.MINUTE))
    }
}
