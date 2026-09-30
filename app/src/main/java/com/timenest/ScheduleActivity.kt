package com.timenest

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class ScheduleActivity : AppCompatActivity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(32, 32, 32, 32) }
        setContentView(ScrollView(this).apply { addView(root) })
        root.addView(TextView(this).apply { text = "Jadwal harian\nCth: tiap hari 16:00 mulai 60 mnt, atau sampai 18:00"; textSize = 16f })
        val eH = EditText(this).apply { hint = "jam mulai (0-23)"; inputType = 2 }
        val eM = EditText(this).apply { hint = "menit mulai"; inputType = 2 }
        val eD = EditText(this).apply { hint = "durasi menit (0=pakai jam selesai)"; inputType = 2 }
        val eEH = EditText(this).apply { hint = "jam selesai (jika dur=0)"; inputType = 2 }
        val eEM = EditText(this).apply { hint = "menit selesai"; inputType = 2 }
        val info = TextView(this)
        listOf(eH, eM, eD, eEH, eEM).forEach { root.addView(it) }
        root.addView(info)
        lifecycleScope.launch { info.text = ScheduleStore.get(this@ScheduleActivity) }
        root.addView(Button(this).apply { text = "Simpan & aktifkan"; setOnClickListener {
            lifecycleScope.launch {
                ScheduleStore.set(this@ScheduleActivity, true,
                    eH.text.toString().toIntOrNull() ?: 16, eM.text.toString().toIntOrNull() ?: 0,
                    eD.text.toString().toIntOrNull() ?: 60,
                    eEH.text.toString().toIntOrNull() ?: 18, eEM.text.toString().toIntOrNull() ?: 0)
                info.text = ScheduleStore.get(this@ScheduleActivity)
                Toast.makeText(this@ScheduleActivity, "Jadwal aktif", Toast.LENGTH_SHORT).show()
            }
        }})
        root.addView(Button(this).apply { text = "Nonaktifkan"; setOnClickListener {
            lifecycleScope.launch { ScheduleStore.set(this@ScheduleActivity, false, 16, 0, 60, 18, 0); info.text = "Nonaktif" }
        }})
    }
}
