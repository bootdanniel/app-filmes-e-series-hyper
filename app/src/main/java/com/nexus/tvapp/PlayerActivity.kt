package com.nexus.tvapp

import android.os.Bundle
import android.view.View
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
    private var player: ExoPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        playerView = findViewById(R.id.player_view)
        progressBar = findViewById(R.id.progressBar)
        txtError = findViewById(R.id.txtError)

        val categoria = intent.getStringExtra("categoria") ?: ""
        val slug = intent.getStringExtra("slug") ?: ""
        val titulo = intent.getStringExtra("titulo") ?: ""

        Logger.log("Player", "Abrindo Player para: $titulo ($categoria/$slug)")

        // ✅ ROTA CORRETA: /assistir
        val urlPagina = "https://hyper.hyperapps.site/$categoria/$slug/assistir"
        buscarLinkDireto(urlPagina)
    }

    private fun buscarLinkDireto(urlPagina: String) {
        CoroutineScope(Dispatchers.IO).launch {
            Logger.log("Player", "Buscando HTML em: $urlPagina")
            try {
                val client = OkHttpClient()
                val request = Request.Builder()
                    .url(urlPagina)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .build()

                val response = client.newCall(request).execute()
                val html = response.body?.string() ?: ""

                // Regex que captura o link do Wasabi (mp4)
                val regex = Regex("""https://s3\.us-central-1\.wasabisys\.com[^"\\]+?\.mp4[^"\\]*""")
                val match = regex.find(html)

                withContext(Dispatchers.Main) {
                    if (match != null) {
                        val linkLimpo = match.value
                            .replace("\\u0026", "&")
                            .replace("\\", "")
                        Logger.log("Player", "Link extraído com sucesso!")
                        tocarVideo(linkLimpo)
                    } else {
                        Logger.log("Player", "Link não encontrado no HTML.")
                        progressBar.visibility = View.GONE
                        txtError.visibility = View.VISIBLE
                        txtError.text = "Link do vídeo não encontrado nesta página."
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Logger.log("Player", "Erro de rede: ${e.message}")
                    progressBar.visibility = View.GONE
                    txtError.visibility = View.VISIBLE
                    txtError.text = "Erro de rede: ${e.message}"
                }
            }
        }
    }

    private fun tocarVideo(urlVideo: String) {
        progressBar.visibility = View.GONE
        Logger.log("Player", "Iniciando ExoPlayer...")

        // Configuração para evitar o 403 do Wasabi
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
                    Logger.log("Player", "ERRO no ExoPlayer: ${error.message}")
                    txtError.visibility = View.VISIBLE
                    txtError.text = "Erro ao tocar: ${error.message}"
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
