package com.nexus.tvapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.leanback.app.RowsSupportFragment
import androidx.leanback.widget.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*

class MainFragment : RowsSupportFragment() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val rowsAdapter = ArrayObjectAdapter(ListRowPresenter())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adapter = rowsAdapter
        onItemViewClickedListener = OnItemViewClickedListener { _, item, _, _ ->
            if (item is Filme) {
                Logger.log("MainFragment", "Clicou em: ${item.title} | Categoria: ${item.categoria} | Slug: ${item.slug}")
                if (item.categoria == "series") {
                    val intent = Intent(requireContext(), SeriesActivity::class.java)
                    intent.putExtra("id", item.id)
                    intent.putExtra("slug", item.slug)
                    intent.putExtra("titulo", item.title)
                    startActivity(intent)
                } else {
                    val intent = Intent(requireContext(), PlayerActivity::class.java)
                    intent.putExtra("categoria", item.categoria)
                    intent.putExtra("slug", item.slug)
                    intent.putExtra("titulo", item.title)
                    startActivity(intent)
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.setBackgroundColor(android.graphics.Color.parseColor("#141414"))
        carregarCatalogo()
    }

    private fun carregarCatalogo() {
        Logger.log("MainFragment", "Iniciando leitura do JSON...")
        scope.launch {
            val filmes = withContext(Dispatchers.IO) {
                try {
                    requireContext().assets.open("catalogo_completo_hyper.json")
                        .bufferedReader(Charsets.UTF_8).use { reader ->
                            val tipo = object : TypeToken<List<Filme>>() {}.type
                            Gson().fromJson<List<Filme>>(reader, tipo) ?: emptyList()
                        }
                } catch (e: Exception) {
                    Logger.log("MainFragment", "ERRO ao ler JSON: ${e.message}")
                    emptyList()
                }
            }

            Logger.log("MainFragment", "JSON carregado! Total de itens: ${filmes.size}")
            rowsAdapter.clear()
            adicionarFileira("Filmes", filmes.filter { it.categoria == "filmes" })
            adicionarFileira("Séries", filmes.filter { it.categoria == "series" })
            adicionarFileira("Novelas", filmes.filter { it.categoria == "novelas" })
        }
    }

    private fun adicionarFileira(titulo: String, itens: List<Filme>) {
        if (itens.isEmpty()) return
        val listAdapter = ArrayObjectAdapter(CardPresenter())
        itens.forEach { listAdapter.add(it) }
        rowsAdapter.add(ListRow(HeaderItem(rowsAdapter.size().toLong(), titulo), listAdapter))
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
