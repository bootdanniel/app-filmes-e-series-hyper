package com.nexus.tvapp

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

class PlayerActivity : FragmentActivity() {

    private lateinit var playerView: PlayerView
    private lateinit var progressBar: ProgressBar
    private lateinit var txtError: TextView
    private lateinit var hiddenWebView: WebView
    private var player: ExoPlayer? = null
    private var linkExtraido = false

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        playerView = findViewById(R.id.player_view)
        progressBar = findViewById(R.id.progressBar)
        txtError = findViewById(R.id.txtError)
        hiddenWebView = findViewById(R.id.hiddenWebView)

        val categoria = intent.getStringExtra("categoria") ?: ""
        val slug = intent.getStringExtra("slug") ?: ""
        val titulo = intent.getStringExtra("titulo") ?: ""

        buscarLinkDireto(categoria, slug, titulo)
    }

    private fun buscarLinkDireto(categoria: String, slug: String, titulo: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val urlPagina = "https://hyper.hyperapps.site/$categoria/$slug"
            try {
                val client = OkHttpClient()
                val request = Request.Builder().url(urlPagina).header("User-Agent", "Mozilla/5.0").build()
                val response = client.newCall(request).execute()
                val html = response.body?.string() ?: ""

                val regex = Regex("https://s3\\.us-central-1\\.wasabisys\\.com[^\"\\\\]+?\\.mp4[^\"\\\\]*")
                val match = regex.find(html)

                withContext(Dispatchers.Main) {
                    if (match != null) {
                        val linkLimpo = match.value.replace("\\u0026", "&").replace("\\", "")
                        tocarVideo(linkLimpo)
                    } else {
                        interceptarComWebView(urlPagina)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    interceptarComWebView(urlPagina)
                }
            }
        }
    }

    private fun interceptarComWebView(url: String) {
        hiddenWebView.settings.javaScriptEnabled = true
        hiddenWebView.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): android.webkit.WebResourceResponse? {
                val reqUrl = request?.url.toString()
                if (reqUrl.contains(".mp4") && !linkExtraido) {
                    linkExtraido = true
                    runOnUiThread { tocarVideo(reqUrl) }
                }
                return super.shouldInterceptRequest(view, request)
            }
        }
        hiddenWebView.loadUrl(url)
    }

    private fun tocarVideo(urlVideo: String) {
        progressBar.visibility = View.GONE

        val dataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("ExoPlayerLib/2.18.1")
            .setAllowCrossProtocolRedirects(true)
            .setDefaultRequestProperties(mapOf("Range" to "bytes=0-1"))

        player = ExoPlayer.Builder(this).build().also { exoPlayer ->
            playerView.player = exoPlayer
            val mediaItem = MediaItem.fromUri(urlVideo)
            val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(mediaItem)

            exoPlayer.setMediaSource(mediaSource)
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true

            exoPlayer.addListener(object : Player.Listener {
                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    txtError.visibility = View.VISIBLE
                    txtError.text = "Erro ao tocar: ${error.message}"
                    progressBar.visibility = View.GONE
                }
            })
        }
    }

    override fun onStop() {
        super.onStop()
        player?.release()
        player = null
    }
}
