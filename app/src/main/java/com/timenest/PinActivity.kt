package com.timenest

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

// mode = "create" (buat + konfirmasi, simpan hash) atau "verify" (cocokkan, RESULT_OK)
class PinActivity : AppCompatActivity() {
    private var entered = ""
    private var first = ""
    private var creating = true
    private lateinit var dots: TextView
    private lateinit var title: TextView
    private lateinit var btnOk: Button

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        creating = intent.getStringExtra("mode") != "verify"
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL
            setPadding(48, 64, 48, 32); setBackgroundColor(Color.parseColor("#121212"))
        }
        setContentView(ScrollView(this).apply { addView(root) })
        title = TextView(this).apply { textSize = 20f; setTextColor(Color.WHITE) }
        root.addView(title)
        dots = TextView(this).apply { textSize = 40f; gravity = Gravity.CENTER; setTextColor(Color.WHITE) }
        root.addView(dots)
        val grid = GridLayout(this).apply { columnCount = 3; alignmentMode = GridLayout.ALIGN_BOUNDS }
        root.addView(grid)
        listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "", "0", "⌫").forEach { s ->
            if (s.isEmpty()) { grid.addView(Space(this).apply { layoutParams = GridLayout.LayoutParams().apply { width = 220; height = 160 } }); return@forEach }
            grid.addView(Button(this).apply {
                text = s; textSize = 26f; layoutParams = GridLayout.LayoutParams().apply { width = 220; height = 160 }
                setOnClickListener { if (s == "⌫") entered = entered.dropLast(1) else if (entered.length < 6) entered += s; draw() }
            })
        }
        btnOk = Button(this).apply { text = "OK"; setOnClickListener { ok() } }
        root.addView(btnOk)
        draw()
    }

    private fun draw() {
        dots.text = "●".repeat(entered.length) + "○".repeat((6 - entered.length).coerceAtLeast(0))
        title.text = if (creating) (if (first.isEmpty()) "Buat PIN (4-6 angka)" else "Ulangi PIN") else "PIN Orang Tua"
    }

    private fun ok() {
        if (creating) {
            if (entered.length < 4) { Toast.makeText(this, "Min 4 angka", Toast.LENGTH_SHORT).show(); return }
            if (first.isEmpty()) { first = entered; entered = ""; draw(); return }
            if (entered != first) { Toast.makeText(this, "Tidak sama, ulangi", Toast.LENGTH_SHORT).show(); first = ""; entered = ""; draw(); return }
            lifecycleScope.launch {
                SessionStore.setPin(this@PinActivity, PinUtil.hash(entered))
                setResult(RESULT_OK); finish()
            }
        } else {
            if (entered.length < 4) { Toast.makeText(this, "Min 4 angka", Toast.LENGTH_SHORT).show(); return }
            lifecycleScope.launch {
                val h = SessionStore.pin(this@PinActivity)
                if (h != null && h == PinUtil.hash(entered)) { setResult(RESULT_OK); finish() }
                else { Toast.makeText(this@PinActivity, "PIN salah", Toast.LENGTH_SHORT).show(); entered = ""; draw() }
            }
        }
    }

    companion object {
        fun createIntent(c: Context, mode: String) = Intent(c, PinActivity::class.java).putExtra("mode", mode)
    }
}
