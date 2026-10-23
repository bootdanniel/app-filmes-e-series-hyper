package com.nexus.tvapp
import com.google.gson.annotations.SerializedName
import java.io.Serializable
data class Filme(
    @SerializedName("id") val id: Int,
    @SerializedName("kind") val kind: String = "movie",
    @SerializedName("slug") val slug: String = "",
    @SerializedName("title") val title: String = "",
    @SerializedName("poster") val poster: String? = null,
    @SerializedName("backdrop") val backdrop: String? = null,
    @SerializedName("year") val year: Int? = null,
    @SerializedName("rating") val rating: Double? = null,
    @SerializedName("categoria") val categoria: String = "",
    @SerializedName("sinopse") val sinopse: String? = null,
    @SerializedName("temporadas_episodios") val temporadasEpisodios: List<Any> = emptyList()
) : Serializable
