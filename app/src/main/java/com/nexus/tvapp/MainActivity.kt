package com.nexus.tvapp
import android.os.Bundle
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
class MainActivity : FragmentActivity() {
    private lateinit var txtStatus: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        txtStatus = findViewById(R.id.txtStatus)
        txtStatus.text = "Carregando..."
        txtStatus.postDelayed({ txtStatus.text = "App Pronto!" }, 1500)
    }
}
