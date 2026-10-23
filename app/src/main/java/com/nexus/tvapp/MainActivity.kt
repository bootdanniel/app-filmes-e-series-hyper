package com.nexus.tvapp

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentActivity

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.main_frame, MainFragment())
                .commit()
        }

        val btnShowLog = findViewById<Button>(R.id.btnShowLog)
        btnShowLog.setOnClickListener {
            val logText = Logger.getLogs()
            AlertDialog.Builder(this)
                .setTitle("Logs do Aplicativo")
                .setMessage(if (logText.isEmpty()) "Nenhum log gerado ainda." else logText)
                .setPositiveButton("Copiar") { _, _ ->
                    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("Logs NexusTV", logText)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(this, "Logs copiados!", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Fechar", null)
                .show()
        }
    }
}
