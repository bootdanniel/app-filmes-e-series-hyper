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
import java.net.URLEncoder

class SeriesActivity : FragmentActivity() {
    private val apiKey = "5a4258a8c65fdc557abc5818a359ba5f"
    private lateinit var spinner: Spinner
    private lateinit var lista: ListView
    private lateinit var progressBar: ProgressBar
    private lateinit var txtSinopse: TextView
    
    private var tmdbId = 0
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

        slug = intent.getStringExtra("slug") ?: ""
        titulo = intent.getStringExtra("titulo") ?: ""

        if (titulo.isEmpty()) {
            Logger.log("Series", "Erro: Titulo nao recebido")
            finish()
            return
        }
        
        Logger.log("Series", "Buscando ID no TMDB para o titulo: $titulo")
        buscarIdTmdb()
    }

    private fun buscarIdTmdb() {
        progressBar.visibility = View.VISIBLE
        txtSinopse.text = "Buscando dados da série..."
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val query = URLEncoder.encode(titulo, "UTF-8")
                val urlBusca = "https://api.themoviedb.org/3/search/tv?api_key=$apiKey&query=$query&language=pt-BR"
                
                val respBusca = OkHttpClient().newCall(Request.Builder().url(urlBusca).build()).execute().body?.string()
                val jsonBusca = JsonParser().parse(respBusca).asJsonObject
                val results = jsonBusca.getAsJsonArray("results")
                
                if (results != null && results.size() > 0) {
                    tmdbId = results[0].asJsonObject.get("id").asInt
                    Logger.log("Series", "ID do TMDB encontrado: $tmdbId")
                    carregarTemporadas()
                } else {
                    withContext(Dispatchers.Main) {
                        progressBar.visibility = View.GONE
                        txtSinopse.text = "Série não encontrada no TMDB."
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    txtSinopse.text = "Erro ao buscar no TMDB: ${e.message}"
                }
            }
        }
    }

    private fun carregarTemporadas() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = "https://api.themoviedb.org/3/tv/$tmdbId?api_key=$apiKey&language=pt-BR"
                val resp = OkHttpClient().newCall(Request.Builder().url(url).build()).execute().body?.string()
                val json = JsonParser().parse(resp).asJsonObject
                
                val overview = if (json.has("overview") && !json.get("overview").isJsonNull) json.get("overview").asString else "Sinopse não disponível."
                val seasonsArray = json.getAsJsonArray("seasons")
                
                temporadas.clear()
                for (i in 0 until seasonsArray.size()) {
                    val num = seasonsArray[i].asJsonObject.get("season_number").asInt
                    if (num > 0) temporadas.add(num)
                }
                
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    txtSinopse.text = if (overview.isEmpty()) "Sinopse não disponível." else overview
                    configurarSpinner()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { 
                    progressBar.visibility = View.GONE
                    txtSinopse.text = "Erro ao carregar temporadas: ${e.message}"
                }
            }
        }
    }

    private fun configurarSpinner() {
        if (temporadas.isEmpty()) {
            txtSinopse.text = "Nenhuma temporada encontrada."
            return
        }
        val adapter = ArrayAdapter(this, R.layout.item_episodio, temporadas.map { "Temporada $it" })
        spinner.adapter = adapter
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                carregarEpisodios(temporadas[position])
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        carregarEpisodios(temporadas[0])
    }

    private fun carregarEpisodios(temp: Int) {
        progressBar.visibility = View.VISIBLE
        lista.adapter = null
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = "https://api.themoviedb.org/3/tv/$tmdbId/season/$temp?api_key=$apiKey&language=pt-BR"
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
                withContext(Dispatchers.Main) { 
                    progressBar.visibility = View.GONE 
                }
            }
        }
    }
}