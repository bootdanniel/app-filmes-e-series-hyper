package com.nexus.tvapp

object Logger {
    private val logs = mutableListOf<String>()

    fun log(tag: String, message: String) {
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        val logLine = "[$timestamp] [$tag] $message"
        android.util.Log.d(tag, message)
        logs.add(logLine)
        if (logs.size > 200) logs.removeAt(0) // Mantém só os últimos 200 logs
    }

    fun getLogs(): String = logs.joinToString("\n")
    
    fun clear() = logs.clear()
}
