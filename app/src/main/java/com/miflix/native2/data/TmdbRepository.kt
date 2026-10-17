package com.miflix.native2.data

import com.miflix.native2.model.CastMember
import com.miflix.native2.model.EpisodeSummary
import com.miflix.native2.model.MediaDetails
import com.miflix.native2.model.MediaSummary
import com.miflix.native2.model.TrailerSummary
import com.miflix.native2.model.ReleaseNotice
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.json.JSONObject
import java.net.URLEncoder
import java.util.Locale

class TmdbRepository(var token: String) {
    var language: String = "en-US"
    private val base = "https://api.themoviedb.org/3"

    private val browseLogos=linkedMapOf<String,String?>()
    suspend fun browseLogo(item: MediaSummary): String? {
        val key="$language:${item.cloudId}"
        if(browseLogos.containsKey(key)) return browseLogos[key]
        val a=get("/${if(item.type=="series") "tv" else "movie"}/${item.id}/images",mapOf("include_image_language" to "${language.substringBefore('-')},en,null")).optJSONArray("logos")
        val rows=(0 until (a?.length() ?: 0)).mapNotNull {a?.optJSONObject(it)}
        val logo=rows.sortedBy {if(it.optString("iso_639_1")==language.substringBefore('-')) 0 else 1}.firstOrNull()?.optString("file_path")?.takeIf {it.isNotBlank()}?.let {image(it,"w500")}
        if(browseLogos.size>=60) browseLogos.remove(browseLogos.keys.first())
        browseLogos[key]=logo
        return logo
    }
    private val filterCreditsCache=linkedMapOf<String,List<JSONObject>>()
    suspend fun filterPeople(query: String): List<CatalogPerson> {
        val a=get("/search/person",mapOf("query" to query.trim(),"include_adult" to "false")).optJSONArray("results") ?: return emptyList()
        return (0 until a.length()).map { a.getJSONObject(it) }.filter { !it.optBoolean("adult") }.map { x ->
            val known=x.optJSONArray("known_for") ?: org.json.JSONArray()
            CatalogPerson(x.getInt("id"),x.getString("name"),image(x.optString("profile_path"),"w185"),
                (0 until known.length()).map { known.getJSONObject(it).let { k -> k.optString("title").ifBlank { k.optString("name") } } }.filter { it.isNotBlank() }.take(3).joinToString(" · "))
        }
    }
    suspend fun actorCredits(personId: Int): List<MediaSummary> {
        require(personId > 0)
        val a = get("/person/$personId/combined_credits").optJSONArray("cast") ?: return emptyList()
        return (0 until a.length()).map { a.getJSONObject(it) }
            .filter { !it.optBoolean("adult") && it.optString("media_type") in listOf("movie", "tv") }
            .map { mapItem(it, if(it.optString("media_type")=="tv") "series" else "movie") }
            .distinctBy { it.cloudId }.sortedByDescending { it.popularity }
    }

    suspend fun filterGenres(type: String): List<CatalogGenre> {
        val a=get("/genre/${if(type=="series") "tv" else "movie"}/list").getJSONArray("genres")
        return (0 until a.length()).map { a.getJSONObject(it).let { x -> CatalogGenre(x.getInt("id"),x.getString("name")) } }
    }
    private suspend fun filterCredits(person: CatalogPerson,type: String,director: Boolean): List<JSONObject> {
        val key="$language:${person.id}:$type:$director"
        filterCreditsCache[key]?.let { return it }
        val data=get("/person/${person.id}/${if(type=="series") "tv" else "movie"}_credits")
        val a=data.optJSONArray(if(director) "crew" else "cast") ?: org.json.JSONArray()
        val rows=(0 until a.length()).map { a.getJSONObject(it) }
            .filter { !it.optBoolean("adult") && (!director || it.optString("job")=="Director") }
            .distinctBy { it.getInt("id") }
        if(filterCreditsCache.size>=12) filterCreditsCache.remove(filterCreditsCache.keys.first())
        filterCreditsCache[key]=rows
        return rows
    }
    suspend fun customCatalog(filter: CustomCatalogFilter,page: Int): CustomCatalogPage {
        filter.validate();require(page in 1..500)
        val tv=filter.type=="series"
        val sort=when(filter.sort) { "rating" -> "vote_average.desc";"newest" -> if(tv) "first_air_date.desc" else "primary_release_date.desc";else -> "popularity.desc" }
        if(filter.actor==null && filter.director==null) {
            val params=linkedMapOf("include_adult" to "false","sort_by" to sort,"page" to page.toString(),"vote_average.gte" to filter.minimumRating.toString())
            filter.genre?.let { params["with_genres"]=it.id.toString() }
            val field=if(tv) "first_air_date" else "primary_release_date"
            if(filter.fromYear.isNotBlank()) params["$field.gte"]="${filter.fromYear}-01-01"
            if(filter.toYear.isNotBlank()) params["$field.lte"]="${filter.toYear}-12-31"
            val result=get("/discover/${if(tv) "tv" else "movie"}",params)
            return CustomCatalogPage(parseList(result,filter.type,20),page<minOf(result.optInt("total_pages"),500))
        }
        // A crew credit alone is insufficient: explicitly require job == Director.
        val cast=filter.actor?.let { filterCredits(it,filter.type,false) }
        val crew=filter.director?.let { filterCredits(it,filter.type,true) }
        val allowed=crew?.map { it.getInt("id") }?.toSet()
        val dateField=if(tv) "first_air_date" else "release_date"
        val rows=(cast ?: crew.orEmpty()).filter { x ->
            val date=x.optString(dateField);val year=date.take(4).toIntOrNull()
            val genres=x.optJSONArray("genre_ids") ?: org.json.JSONArray()
            (cast==null || allowed==null || x.getInt("id") in allowed) &&
                (filter.genre==null || (0 until genres.length()).any { genres.optInt(it)==filter.genre.id }) &&
                (filter.fromYear.isBlank() || year!=null && year>=filter.fromYear.toInt()) &&
                (filter.toYear.isBlank() || year!=null && year<=filter.toYear.toInt()) &&
                x.optDouble("vote_average",0.0)>=filter.minimumRating
        }.sortedWith(when(filter.sort) {
            "rating" -> compareByDescending<JSONObject> { it.optDouble("vote_average",0.0) }.thenByDescending { it.optDouble("popularity",0.0) }
            "newest" -> compareByDescending<JSONObject> { it.optString(dateField) }
            else -> compareByDescending<JSONObject> { it.optDouble("popularity",0.0) }
        }.thenBy { it.getInt("id") })
        return CustomCatalogPage(rows.drop((page-1)*20).take(20).map { mapItem(it,filter.type) },rows.size>page*20)
    }

    private val franchiseIds=mutableMapOf<String,Pair<List<Int>,List<Int>>>()
    private suspend fun franchiseMetadata(brand: String): Pair<List<Int>,List<Int>> = coroutineScope {
        franchiseIds[brand]?.let { return@coroutineScope it }
        require(brand in listOf("marvel","dc"))
        val names=if(brand=="marvel") listOf("Marvel Studios","Marvel Entertainment","Marvel Television","Marvel Animation") else listOf("DC Entertainment","DC Films","DC Studios")
        val companies=names.map { name -> async {
            val a=get("/search/company",mapOf("query" to name)).optJSONArray("results") ?: org.json.JSONArray()
            (0 until a.length()).map { a.getJSONObject(it) }.filter { it.optString("name").equals(name,true) }.map { it.getInt("id") }
        } }
        val word=if(brand=="marvel") "marvel comics" else "dc comics"
        val a=get("/search/keyword",mapOf("query" to word)).optJSONArray("results") ?: org.json.JSONArray()
        val keywords=(0 until a.length()).map { a.getJSONObject(it) }.filter { it.optString("name").equals(word,true) }.map { it.getInt("id") }
        val result=companies.awaitAll().flatten().distinct() to keywords.distinct()
        require(result.first.isNotEmpty() || result.second.isNotEmpty()) { "Franchise metadata unavailable" }
        franchiseIds[brand]=result;result
    }
    suspend fun franchiseCatalog(brand: String,type: String,page: Int): CustomCatalogPage = coroutineScope {
        require(page in 1..500 && type in listOf("movie","series"))
        val (companies,keywords)=franchiseMetadata(brand)
        val filters=buildList {
            if(companies.isNotEmpty()) add("with_companies" to companies.joinToString("|"))
            if(keywords.isNotEmpty()) add("with_keywords" to keywords.joinToString("|"))
        }
        val pages=filters.map { (key,value) -> async {
            val result=get("/discover/${if(type=="series") "tv" else "movie"}",mapOf(key to value,"include_adult" to "false","sort_by" to "popularity.desc","page" to page.toString()))
            CustomCatalogPage(parseList(result,type,20),page<minOf(result.optInt("total_pages"),500))
        } }.awaitAll()
        CustomCatalogPage(pages.flatMap { it.items }.distinctBy { it.cloudId }.sortedByDescending { it.popularity },pages.any { it.hasMore })
    }

    suspend fun directorPerson(name: String): DirectorPerson? {
        val a=get("/search/person",mapOf("query" to name,"include_adult" to "false")).optJSONArray("results") ?: return null
        val people=(0 until a.length()).map { a.getJSONObject(it) }
        val person=people.filter { it.optString("name").equals(name,true) && !it.optBoolean("adult") }
            .sortedWith(compareByDescending<JSONObject> { it.optString("known_for_department")=="Directing" }.thenByDescending { it.optDouble("popularity") }).firstOrNull() ?: return null
        return DirectorPerson(person.getInt("id"),person.getString("name"),image(person.optString("profile_path"),"w185"))
    }
    suspend fun directedMovies(people: List<DirectorPerson>): List<MediaSummary> = coroutineScope {
        people.map { person -> async {
            val crew=get("/person/${person.id}/movie_credits").optJSONArray("crew") ?: org.json.JSONArray()
            (0 until crew.length()).map { crew.getJSONObject(it) }.filter { it.optString("job")=="Director" && !it.optBoolean("adult") }
                .map { mapItem(it,"movie") }
        } }.awaitAll().flatten().distinctBy { it.cloudId }.filter { it.poster!=null || it.backdrop!=null }
            .sortedByDescending { it.popularity }
    }

    private suspend fun get(path: String, params: Map<String, String> = emptyMap()): JSONObject {
        val all = LinkedHashMap(params)
        if (!all.containsKey("language")) all["language"] = language
        val credential = token.trim()
        if (credential.isBlank()) throw IllegalStateException("TMDB is not configured")
        if (!credential.startsWith("eyJ")) all["api_key"] = credential
        val query = all.entries.joinToString("&") { (k, v) ->
            URLEncoder.encode(k, "UTF-8") + "=" + URLEncoder.encode(v, "UTF-8")
        }
        val headers = if (credential.startsWith("eyJ")) mapOf("Authorization" to "Bearer $credential") else emptyMap()
        return JSONObject(Http.text("$base$path?$query", headers = headers))
    }

    // Theme keywords describe the subject, unlike title searches for "Christmas".
    suspend fun seasonal(theme: String): List<MediaSummary> {
        val keyword=when(theme) { "halloween" -> "halloween"; "christmas" -> "christmas"; else -> null }
        if(keyword!=null) {
            val matches=get("/search/keyword",mapOf("query" to keyword)).optJSONArray("results")
            val ids=(0 until (matches?.length() ?: 0)).mapNotNull { i -> matches?.optJSONObject(i) }
                .filter { it.optString("name").equals(keyword,ignoreCase=true) }.map { it.optInt("id") }.filter { it>0 }
            if(ids.isNotEmpty()) {
                val rows=parseList(get("/discover/movie",mapOf("with_keywords" to ids.joinToString("|"),
                    "include_adult" to "false","sort_by" to "popularity.desc")),"movie",20)
                if(rows.isNotEmpty()) return rows
            }
        }
        if(theme=="christmas") return emptyList()
        return discoverMovieGenre(SeasonalProgramming.genre(theme),pages=1,limit=20)
    }

    private val kidsChecks=linkedMapOf<String,Pair<Long,Boolean>>()
    private val kidsPermits=Semaphore(4)
    suspend fun kidsAllowed(item: MediaSummary): Boolean {
        val key=item.cloudId
        kidsChecks[key]?.takeIf { System.currentTimeMillis()-it.first<24*60*60*1000L }?.let { return it.second }
        return kidsPermits.withPermit {
            kidsChecks[key]?.takeIf { System.currentTimeMillis()-it.first<24*60*60*1000L }?.let { return@withPermit it.second }
            try {
                val series=item.type=="series"
                if(!series && item.type!="movie") return@withPermit false
                val j=get("/${if(series) "tv" else "movie"}/${item.id}",mapOf("append_to_response" to if(series) "content_ratings" else "release_dates"))
                val g=j.optJSONArray("genres")
                val genres=(0 until (g?.length() ?: 0)).map { g!!.getJSONObject(it).optInt("id") }.toIntArray()
                val ratings=mutableListOf<String>()
                val all=j.optJSONObject(if(series) "content_ratings" else "release_dates")?.optJSONArray("results")
                for(i in 0 until (all?.length() ?: 0)) {
                    val country=all!!.getJSONObject(i)
                    if(country.optString("iso_3166_1")!="US") continue
                    if(series) country.optString("rating").takeIf {it.isNotBlank()}?.let {ratings.add(it)}
                    else {
                        val dates=country.optJSONArray("release_dates")
                        for(n in 0 until (dates?.length() ?: 0)) dates!!.getJSONObject(n).optString("certification").takeIf {it.isNotBlank()}?.let {ratings.add(it)}
                    }
                }
                val allowed=KidsContentPolicy.allows(j.optBoolean("adult"),series,genres,ratings.toTypedArray())
                if(kidsChecks.size>=200) kidsChecks.remove(kidsChecks.keys.first())
                kidsChecks[key]=System.currentTimeMillis() to allowed
                allowed
            } catch(e: CancellationException) { throw e }
            catch(_: Exception) { false } // Missing/unavailable rating never admits a title.
        }
    }
    suspend fun kidsFilter(rows: List<MediaSummary>): List<MediaSummary> = coroutineScope {
        rows.distinctBy {it.cloudId}.map { item -> async { if(kidsAllowed(item)) item else null } }.awaitAll().filterNotNull()
    }
    suspend fun kidsCatalog(series: Boolean): List<MediaSummary> {
        val params=linkedMapOf("include_adult" to "false","sort_by" to "popularity.desc","with_genres" to if(series) "10762" else "16|10751")
        if(!series) {params["certification_country"]="US";params["certification.lte"]="PG"}
        val rows=parseList(get("/discover/${if(series) "tv" else "movie"}",params),if(series) "series" else "movie",20)
        return kidsFilter(rows)
    }

    suspend fun trending(): List<MediaSummary> = parseList(get("/trending/all/week"), limit = 20)
    suspend fun popularMovies(): List<MediaSummary> = parseList(get("/movie/popular"), "movie", 20)
    suspend fun popularSeries(): List<MediaSummary> = parseList(get("/tv/popular"), "series", 20)
    suspend fun topRatedMovies(): List<MediaSummary> = parseList(get("/movie/top_rated"), "movie", 20)
    suspend fun nowPlayingMovies(): List<MediaSummary> = parseList(get("/movie/now_playing"),"movie",20)
    suspend fun topRatedSeries(): List<MediaSummary> = parseList(get("/tv/top_rated"),"series",20)
    suspend fun search(query: String): List<MediaSummary> = parseList(get("/search/multi", mapOf("query" to query)), limit = 40)

    suspend fun discoverGenre(genreId: Int): List<MediaSummary> = discoverMovieGenre(genreId, "popularity.desc", 1, 20)

    suspend fun discoverMovieGenre(genreId: Int, sortBy: String = "popularity.desc", pages: Int = 3, limit: Int = 50): List<MediaSummary> =
        discoverPaged(
            path = "/discover/movie",
            type = "movie",
            baseParams = mapOf("with_genres" to genreId.toString(), "sort_by" to sortBy, "vote_count.gte" to if (sortBy.startsWith("vote_average")) "150" else "0"),
            pages = pages,
            limit = limit
        )

    suspend fun discoverTvGenre(genreId: Int, sortBy: String = "popularity.desc", pages: Int = 3, limit: Int = 50): List<MediaSummary> =
        discoverPaged(
            path = "/discover/tv",
            type = "series",
            baseParams = mapOf("with_genres" to genreId.toString(), "sort_by" to sortBy, "vote_count.gte" to if (sortBy.startsWith("vote_average")) "100" else "0"),
            pages = pages,
            limit = limit
        )

    data class GenreSlice(val rows: List<MediaSummary>,val hasMore: Boolean)
    suspend fun genrePage(movieGenreId: Int,tvGenreId: Int,page: Int): GenreSlice = coroutineScope {
        require(page in 1..500)
        suspend fun part(series: Boolean,genre: Int): Pair<List<MediaSummary>,Boolean> {
            if(genre<=0) return emptyList<MediaSummary>() to false
            val j=get("/discover/${if(series) "tv" else "movie"}",mapOf("with_genres" to genre.toString(),
                "sort_by" to "popularity.desc","include_adult" to "false","page" to page.toString()))
            return parseList(j,if(series) "series" else "movie",20) to (page<j.optInt("total_pages",page).coerceAtMost(500))
        }
        val movies=async {part(false,movieGenreId)};val shows=async {part(true,tvGenreId)}
        val m=movies.await();val t=shows.await()
        GenreSlice((m.first+t.first).distinctBy {it.cloudId}.sortedByDescending {it.popularity},m.second || t.second)
    }

    suspend fun discoverGenreMixed(movieGenreId: Int, tvGenreId: Int, limit: Int = 50): List<MediaSummary> = coroutineScope {
        val movies = async { discoverMovieGenre(movieGenreId, pages = 2, limit = 35) }
        val tv = async { discoverTvGenre(tvGenreId, pages = 2, limit = 35) }
        (movies.await() + tv.await())
            .distinctBy { it.cloudId }
            .sortedByDescending { it.popularity }
            .take(limit)
    }

    suspend fun discoverProvider(providerId: Int, region: String = "US"): List<MediaSummary> = discoverProviders(listOf(providerId), region)

    suspend fun discoverProviders(providerIds: List<Int>, region: String = "US"): List<MediaSummary> =
        discoverProviderType(providerIds, "movie", region = region, sortBy = "popularity.desc", limit = 20)

    suspend fun discoverProviderType(
        providerIds: List<Int>,
        type: String,
        region: String = "US",
        sortBy: String = "popularity.desc",
        genreId: Int? = null,
        pages: Int = 1,
        limit: Int = 20
    ): List<MediaSummary> {
        val kind = if (type == "series") "tv" else "movie"
        val params = linkedMapOf(
            "with_watch_providers" to providerIds.joinToString("|"),
            "watch_region" to region,
            "sort_by" to sortBy
        )
        genreId?.let { params["with_genres"] = it.toString() }
        if (sortBy.startsWith("vote_average")) params["vote_count.gte"] = if (kind == "movie") "100" else "75"
        return discoverPaged("/discover/$kind", if (kind == "tv") "series" else "movie", params, pages, limit)
    }

    suspend fun providerTop10(providerIds: List<Int>, region: String = "US"): List<MediaSummary> = coroutineScope {
        val m = async { discoverProviderType(providerIds, "movie", region, "popularity.desc", limit = 12) }
        val t = async { discoverProviderType(providerIds, "series", region, "popularity.desc", limit = 12) }
        (m.await() + t.await()).distinctBy { it.cloudId }.sortedByDescending { it.popularity }.take(10)
    }

    suspend fun moviesByYear(year: Int, limit: Int = 50): List<MediaSummary> = discoverPaged(
        "/discover/movie",
        "movie",
        mapOf("primary_release_year" to year.toString(), "sort_by" to "popularity.desc"),
        pages = 3,
        limit = limit
    )

    suspend fun catalogByYear(year: Int, category: String): List<MediaSummary> {
        val tv=category=="series"
        val params=mutableMapOf((if(tv) "first_air_date_year" else "primary_release_year") to year.toString(),"sort_by" to "popularity.desc")
        if(category=="documentary") params["with_genres"]="99"
        return discoverPaged(if(tv) "/discover/tv" else "/discover/movie",if(tv) "series" else "movie",params,3,50)
    }

    suspend fun nationalGeographic(): List<MediaSummary> = coroutineScope {
        val tv=async { discoverPaged("/discover/tv","series",mapOf("with_networks" to "43|6750","sort_by" to "popularity.desc"),3,50) }
        val companies=get("/search/company",mapOf("query" to "National Geographic")).optJSONArray("results")
        val ids=if(companies==null) emptyList() else (0 until companies.length()).map { companies.getJSONObject(it) }
            .filter { it.optString("name").contains("National Geographic",ignoreCase=true) }.map { it.getInt("id") }
        val movies=if(ids.isEmpty()) emptyList() else discoverPaged("/discover/movie","movie",mapOf("with_companies" to ids.joinToString("|"),"sort_by" to "popularity.desc"),3,50)
        (tv.await()+movies).distinctBy { it.cloudId }
    }

    private suspend fun discoverPaged(
        path: String,
        type: String,
        baseParams: Map<String, String>,
        pages: Int,
        limit: Int
    ): List<MediaSummary> = coroutineScope {
        (1..pages.coerceIn(1, 5)).map { page ->
            async {
                val params = LinkedHashMap(baseParams).apply { put("page", page.toString()) }
                parseList(get(path, params), type, 20)
            }
        }.awaitAll().flatten().distinctBy { it.cloudId }.take(limit)
    }

    suspend fun details(item: MediaSummary): MediaDetails {
        val kind = if (item.type == "series") "tv" else "movie"
        val j = get("/$kind/${item.id}", mapOf("append_to_response" to "external_ids,credits,images", "include_image_language" to "${language.substringBefore('-')},en,null"))
        val ext = j.optJSONObject("external_ids")
        val imdb = if (kind == "movie") j.optString("imdb_id") else ext?.optString("imdb_id")
        val genres = j.optJSONArray("genres")?.let { a ->
            (0 until a.length()).mapNotNull { i -> a.optJSONObject(i)?.optString("name")?.takeIf { it.isNotBlank() } }
        } ?: emptyList()

        val runtime = if (kind == "movie") {
            val mins = j.optInt("runtime", 0)
            if (mins > 0) "${mins / 60}h ${mins % 60}m" else if(language.startsWith("es")) "Película" else "Movie"
        } else {
            val mins = j.optJSONArray("episode_run_time")?.optInt(0, 0) ?: 0
            if (mins > 0) "${mins}m ${if(language.startsWith("es")) "por episodio" else "episodes"}" else "${j.optInt("number_of_seasons", 0)} ${if(language.startsWith("es")) "temporadas" else "seasons"}"
        }

        val release = if (kind == "movie") j.optString("release_date") else j.optString("first_air_date")
        val origin = if (kind == "movie") {
            j.optJSONArray("production_countries")?.let { a ->
                (0 until a.length()).mapNotNull { i -> a.optJSONObject(i)?.optString("name")?.takeIf { it.isNotBlank() } }.take(2).joinToString(", ")
            }.orEmpty()
        } else {
            j.optJSONArray("origin_country")?.let { a ->
                (0 until a.length()).map { i -> a.optString(i) }.filter { it.isNotBlank() }.joinToString(", ")
            }.orEmpty()
        }
        val originalLanguage = j.optString("original_language").uppercase(Locale.US)

        val cast = j.optJSONObject("credits")?.optJSONArray("cast")?.let { a ->
            (0 until minOf(a.length(), 10)).mapNotNull { i ->
                val x = a.optJSONObject(i) ?: return@mapNotNull null
                val name = x.optString("name").takeIf { it.isNotBlank() } ?: return@mapNotNull null
                CastMember(
                    id = x.optInt("id"),
                    name = name,
                    character = x.optString("character"),
                    profile = image(x.optString("profile_path"), "w185")
                )
            }
        } ?: emptyList()

        val logos=j.optJSONObject("images")?.optJSONArray("logos")
        val logoRows=(0 until (logos?.length() ?: 0)).mapNotNull {logos?.optJSONObject(it)}
        val logo=logoRows.sortedBy {if(it.optString("iso_639_1")==language.substringBefore('-')) 0 else 1}.firstOrNull()?.optString("file_path")?.let {image(it,"w500")}

        return MediaDetails(
            summary = item,
            imdbId = imdb?.takeIf { it.isNotBlank() },
            runtimeText = runtime,
            genres = genres,
            seasonCount = j.optInt("number_of_seasons", 0),
            releaseInfo = release.take(4).ifBlank { item.year },
            releaseDate = release,
            originCountry = origin,
            originalLanguage = originalLanguage,
            cast = cast,
            logo = logo,
            originalTitle = j.optString(if(kind=="movie") "original_title" else "original_name")
        )
    }

    suspend fun season(showId: Int, season: Int): List<EpisodeSummary> {
        val j = get("/tv/$showId/season/$season")
        val a = j.optJSONArray("episodes") ?: return emptyList()
        return (0 until a.length()).mapNotNull { i ->
            val x = a.optJSONObject(i) ?: return@mapNotNull null
            EpisodeSummary(
                season = season,
                episode = x.optInt("episode_number", i + 1),
                title = x.optString("name", "Episode ${i + 1}"),
                overview = x.optString("overview"),
                still = image(x.optString("still_path"), "w500"),
                rating = x.optDouble("vote_average", 0.0),
                airDate = x.optString("air_date")
            )
        }
    }

    private val randomSeasons=linkedMapOf<Int,Pair<Long,List<Pair<Int,Int>>>>()
    suspend fun randomEpisode(item: MediaSummary,exclude: Pair<Int,Int>?=null): EpisodeSummary {
        require(item.type=="series")
        val now=System.currentTimeMillis()
        val today=java.text.SimpleDateFormat("yyyy-MM-dd",Locale.US).format(java.util.Date())
        val seasons=randomSeasons[item.id]?.takeIf {now-it.first<6*60*60*1000L}?.second ?: run {
            val all=get("/tv/${item.id}").optJSONArray("seasons") ?: org.json.JSONArray()
            val available=(0 until all.length()).mapNotNull {index ->
                val row=all.optJSONObject(index) ?: return@mapNotNull null
                val number=row.optInt("season_number");val count=row.optInt("episode_count");val date=row.optString("air_date")
                if(RandomEpisodePolicy.season(number,count,date,today)) number to count else null
            }
            if(randomSeasons.size>=12) randomSeasons.remove(randomSeasons.keys.first())
            randomSeasons[item.id]=now to available;available
        }
        val remaining=seasons.toMutableList()
        while(remaining.isNotEmpty()) {
            val total=remaining.sumOf {it.second}
            val draw=kotlin.random.Random.nextInt(total)
            val pickedSeason=remaining[RandomEpisodePolicy.indexForDraw(remaining.map {it.second}.toIntArray(),draw)].first
            val aired=season(item.id,pickedSeason).filter {RandomEpisodePolicy.aired(it.airDate,today)}
            val choices=aired.filterNot {it.season==exclude?.first && it.episode==exclude?.second}
            if(choices.isNotEmpty()) return choices.random()
            remaining.removeAll {it.first==pickedSeason}
            if(remaining.isEmpty() && aired.isNotEmpty()) return aired.random()
        }
        error(if(language.startsWith("es")) "No hay episodios estrenados disponibles" else "No aired episodes available")
    }

    suspend fun byCloudId(cloudId: String): MediaSummary? {
        val m = Regex("^tmdb:(movie|series):(\\d+)$").find(cloudId) ?: return null
        val type = m.groupValues[1]
        val id = m.groupValues[2].toIntOrNull() ?: return null
        val kind = if (type == "series") "tv" else "movie"
        val j = get("/$kind/$id")
        return mapItem(j, type)
    }

    suspend fun comingSoon(type: String): List<MediaSummary> {
        val format=java.text.SimpleDateFormat("yyyy-MM-dd",Locale.US)
        val nextDay=java.util.Calendar.getInstance().apply { add(java.util.Calendar.DAY_OF_YEAR,1) }
        val nextYear=java.util.Calendar.getInstance().apply { add(java.util.Calendar.MONTH,12) }
        val start=format.format(nextDay.time)
        val end=format.format(nextYear.time)
        val field=if(type=="series") "first_air_date" else "primary_release_date"
        return parseList(get("/discover/${if(type=="series") "tv" else "movie"}",mapOf("$field.gte" to start,"$field.lte" to end,"sort_by" to "popularity.desc","include_adult" to "false")),type,20)
    }
    private val discoveryCache=PlaybackLinkCache<String,List<MediaSummary>>(40) { System.currentTimeMillis() }
    private val spanishTitles=PlaybackLinkCache<String,String>(80) { System.currentTimeMillis() }
    suspend fun spanishTitle(item: MediaSummary): String {
        spanishTitles.get(item.cloudId)?.let { return it }
        val json=get("/${if(item.type=="series") "tv" else "movie"}/${item.id}",mapOf("language" to "es-ES"))
        val title=json.optString(if(item.type=="series") "name" else "title")
        if(title.isNotBlank()) spanishTitles.put(item.cloudId,title)
        return title
    }
    suspend fun familiarCatalog(kind: String): List<MediaSummary> {
        val locale=language
        val key="familiar:$locale:$kind"
        discoveryCache.get(key)?.let {return it}
        val type=if(kind=="kdrama") "series" else "movie"
        val params=linkedMapOf("language" to locale,"include_adult" to "false","sort_by" to "popularity.desc",
            "vote_count.gte" to "80","vote_average.gte" to "6")
        when(kind) {
            "romance" -> params["with_genres"]="10749"
            "kdrama" -> {params["with_original_language"]="ko";params["with_genres"]="18"}
            "classics" -> {params["primary_release_date.lte"]="1999-12-31";params["sort_by"]="vote_average.desc";params["vote_count.gte"]="500"}
            else -> error("Unknown catalog")
        }
        val rows=parseList(get("/discover/${if(type=="series") "tv" else "movie"}",params),type,20)
        discoveryCache.put(key,rows)
        return rows
    }
    suspend fun recommendations(item: MediaSummary): List<MediaSummary> {
        val locale=language
        val key="recommend:$locale:${item.cloudId}"
        discoveryCache.get(key)?.let {return it}
        val rows=parseList(get("/${if(item.type == "series") "tv" else "movie"}/${item.id}/recommendations",mapOf("language" to locale)), item.type,20)
        discoveryCache.put(key,rows)
        return rows
    }

    suspend fun episodeInfo(item: MediaSummary, season: Int, episode: Int): EpisodeSummary? =
        season(item.id, season).firstOrNull { it.episode == episode }

    suspend fun latestRelease(item: MediaSummary): ReleaseNotice? {
        val j = get("/${if(item.type == "series") "tv" else "movie"}/${item.id}")
        if(item.type == "series") {
            val e = j.optJSONObject("last_episode_to_air") ?: return null
            val date = e.optString("air_date")
            if(date.isBlank() || date > java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US).format(java.util.Date())) return null
            return ReleaseNotice("${item.cloudId}:${e.optInt("id")}", item,
                "${if(language.startsWith("es")) "Nuevo episodio" else "New episode"}: S${e.optInt("season_number")} E${e.optInt("episode_number")} · ${e.optString("name")}", date)
        }
        val date = j.optString("release_date")
        if(date.isBlank() || date > java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US).format(java.util.Date())) return null
        return ReleaseNotice("${item.cloudId}:$date", item, "${if(language.startsWith("es")) "Estreno de película" else "Movie release"} · $date", date)
    }

    private fun parseList(j: JSONObject, forceType: String? = null, limit: Int = 20): List<MediaSummary> {
        val a = j.optJSONArray("results") ?: return emptyList()
        return (0 until a.length()).mapNotNull { i ->
            val x = a.optJSONObject(i) ?: return@mapNotNull null
            val mediaType = forceType ?: when (x.optString("media_type")) {
                "tv" -> "series"
                "movie" -> "movie"
                else -> return@mapNotNull null
            }
            mapItem(x, mediaType)
        }.filter { it.backdrop != null || it.poster != null }.take(limit)
    }

    private fun mapItem(x: JSONObject, type: String): MediaSummary {
        val isSeries = type == "series"
        val title = if (isSeries) x.optString("name", x.optString("original_name")) else x.optString("title", x.optString("original_title"))
        val date = if (isSeries) x.optString("first_air_date") else x.optString("release_date")
        return MediaSummary(
            id = x.optInt("id"),
            type = type,
            title = title.ifBlank { "Untitled" },
            overview = x.optString("overview"),
            backdrop = image(x.optString("backdrop_path"), "w1280"),
            poster = image(x.optString("poster_path"), "w500"),
            rating = x.optDouble("vote_average", 0.0),
            year = date.take(4),
            popularity = x.optDouble("popularity", 0.0)
        )
    }

    private fun image(path: String?, size: String): String? = path
        ?.takeIf { it.isNotBlank() && it != "null" }
        ?.let { "https://image.tmdb.org/t/p/$size$it" }
}

data class DirectorPerson(val id: Int,val name: String,val photo: String?)
data class DirectorCollection(val key: String,val title: String,val names: List<String>,val people: List<DirectorPerson> = emptyList())
