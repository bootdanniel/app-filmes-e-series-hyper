package com.nexus.tvapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.fragment.app.FragmentActivity
import com.google.gson.JsonParser
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request

class SeriesActivity : FragmentActivity() {
    private val apiKey = "5a4258a8c65fdc557abc5818a359ba5f"
    private lateinit var spinner: Spinner
    private lateinit var lista: ListView
    private lateinit var progressBar: ProgressBar
    private lateinit var txtSinopse: TextView
    private var seriesId = 0
    private var slug = ""
    private var titulo = ""
    
    private val temporadas = mutableListOf<Int>()
    private val episodios = mutableListOf<Triple<Int, Int, String>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_series)

        spinner = findViewById(R.id.spinnerTemporadas)
        lista = findViewById(R.id.listaEpisodios)
        progressBar = findViewById(R.id.progressSeries)
        txtSinopse = findViewById(R.id.txtSinopse)

        seriesId = intent.getIntExtra("id", 0)
        slug = intent.getStringExtra("slug") ?: ""
        titulo = intent.getStringExtra("titulo") ?: ""

        if (seriesId == 0) {
            Logger.log("Series", "Erro: ID do TMDB não recebido")
            finish()
            return
        }
        carregarTemporadas()
    }

    private fun carregarTemporadas() {
        progressBar.visibility = View.VISIBLE
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = "https://api.themoviedb.org/3/tv/$seriesId?api_key=$apiKey&language=pt-BR"
                val resp = OkHttpClient().newCall(Request.Builder().url(url).build()).execute().body?.string()
                val json = JsonParser().parse(resp).asJsonObject
                val overview = if (json.has("overview") && !json.get("overview").isJsonNull) json.get("overview").asString else "Sinopse não disponível."
                val seasonsArray = json.getAsJsonArray("seasons")
                
                temporadas.clear()
                for (i in 0 until seasonsArray.size()) {
                    val s = seasonsArray[i].asJsonObject
                    val num = s.get("season_number").asInt
                    if (num > 0) temporadas.add(num) // Ignora temporada 0 (especiais)
                }
                
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    txtSinopse.text = overview
                    configurarSpinner()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { 
                    progressBar.visibility = View.GONE
                    Logger.log("Series", "Erro TMDB Temporadas: ${e.message}")
                }
            }
        }
    }

    private fun configurarSpinner() {
        val adapter = ArrayAdapter(this, R.layout.item_episodio, temporadas.map { "Temporada $it" })
        spinner.adapter = adapter
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                carregarEpisodios(temporadas[position])
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        if (temporadas.isNotEmpty()) carregarEpisodios(temporadas[0])
    }

    private fun carregarEpisodios(temp: Int) {
        progressBar.visibility = View.VISIBLE
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = "https://api.themoviedb.org/3/tv/$seriesId/season/$temp?api_key=$apiKey&language=pt-BR"
                val resp = OkHttpClient().newCall(Request.Builder().url(url).build()).execute().body?.string()
                val json = JsonParser().parse(resp).asJsonObject
                val epsArray = json.getAsJsonArray("episodes")
                
                episodios.clear()
                for (i in 0 until epsArray.size()) {
                    val ep = epsArray[i].asJsonObject
                    val epNum = ep.get("episode_number").asInt
                    val epTitle = if (ep.has("name") && !ep.get("name").isJsonNull) ep.get("name").asString else "Episódio $epNum"
                    episodios.add(Triple(temp, epNum, epTitle))
                }
                
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    val titles = episodios.map { "T${it.first} E${it.second} - ${it.third}" }
                    lista.adapter = ArrayAdapter(this@SeriesActivity, R.layout.item_episodio, titles)
                    lista.setOnItemClickListener { _, _, pos, _ ->
                        val (t, e, _) = episodios[pos]
                        val intent = Intent(this@SeriesActivity, PlayerActivity::class.java)
                        intent.putExtra("categoria", "series")
                        intent.putExtra("slug", slug)
                        intent.putExtra("titulo", "$titulo - T${t}E${e}")
                        intent.putExtra("temporada", t)
                        intent.putExtra("episodio", e)
                        startActivity(intent)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { progressBar.visibility = View.GONE }
            }
        }
    }
}