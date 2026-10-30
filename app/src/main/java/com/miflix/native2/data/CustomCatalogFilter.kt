package com.miflix.native2.data

import com.miflix.native2.model.MediaSummary

data class CatalogPerson(val id: Int,val name: String,val photo: String?,val knownFor: String,val department:String="")
data class CatalogGenre(val id: Int,val name: String)
data class CustomCatalogFilter(
    val type: String="movie",
    val actor: CatalogPerson?=null,
    val director: CatalogPerson?=null,
    val genre: CatalogGenre?=null,
    val fromYear: String="",
    val toYear: String="",
    val minimumRating: Int=0,
    val sort: String="popular"
) {
    fun validate() {
        require(type in listOf("movie","series")) { "Invalid content type" }
        val end=java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)+5
        listOf(fromYear,toYear).forEach { year ->
            require(year.isBlank() || year.length==4 && (year.toIntOrNull() ?: 0) in 1800..end) { "${if(com.miflix.native2.ui.UiLanguage.code=="es") "Año inválido" else "Invalid year"}: 1800–$end" }
        }
        require(fromYear.isBlank() || toYear.isBlank() || fromYear.toInt()<=toYear.toInt()) { com.miflix.native2.ui.tr("El año inicial no puede superar al final","From year must not exceed To year") }
        require(minimumRating in 0..10)
        require(sort in listOf("popular","rating","newest"))
    }
}
data class CustomCatalogPage(val items: List<MediaSummary>,val hasMore: Boolean)
