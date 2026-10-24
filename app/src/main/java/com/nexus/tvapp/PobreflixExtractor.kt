package com.nexus.tvapp

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

class PobreflixExtractor(private val context: Context) {

    @SuppressLint("SetJavaScriptEnabled")
    suspend fun extrairLink(tmdbId: String): String? = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { continuation ->
            val webView = WebView(context)
            webView.settings.javaScriptEnabled = true
            webView.settings.domStorageEnabled = true
            webView.settings.userAgentString = "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

            webView.webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(
                    view: WebView?,
                    request: WebResourceRequest?
                ): android.webkit.WebResourceResponse? {
                    val url = request?.url.toString()
                    // Intercepta o link do vídeo final
                    if (url.contains(".mp4") || url.contains(".m3u8")) {
                        if (continuation.isActive) {
                            continuation.resume(url)
                        }
                    }
                    return super.shouldInterceptRequest(view, request)
                }
            }

            // Timeout de 15 segundos para não travar a tela para sempre
            val timeoutJob = GlobalScope.launch(Dispatchers.Main) {
                delay(15000)
                if (continuation.isActive) {
                    continuation.resume(null)
                }
            }

            continuation.invokeOnCancellation {
                timeoutJob.cancel()
                webView.destroy()
            }

            // Carrega a página do embed do Pobreflix
            webView.loadUrl("https://redeflixapi.store/filme/$tmdbId")
        }
    }
}
