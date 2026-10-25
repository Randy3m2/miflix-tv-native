package com.miflix.native2.data

import org.json.*

internal data class SportsPersonRP121(val id:String,val name:String,val league:String,val photo:String="",val teamId:String="",
    val team:String="",val logo:String="",val position:String="") {
    fun json()=JSONObject().put("id",id).put("name",name).put("league",league).put("photo",photo).put("teamId",teamId).put("team",team).put("logo",logo).put("position",position)
    companion object {fun parse(j:JSONObject)=SportsPersonRP121(j.getString("id"),j.getString("name"),j.getString("league"),j.optString("photo"),j.optString("teamId"),j.optString("team"),j.optString("logo"),j.optString("position"))}
}
internal data class SportsLeaderRP121(val rank:String,val person:SportsPersonRP121,val value:String)
internal data class SportsLeadersRP121(val key:String,val title:String,val unit:String,val rows:List<SportsLeaderRP121>) {
    fun json()=JSONObject().put("key",key).put("title",title).put("unit",unit).put("rows",JSONArray(rows.map {JSONObject().put("rank",it.rank).put("person",it.person.json()).put("value",it.value)}))
    companion object {fun parse(j:JSONObject):SportsLeadersRP121 {val a=j.getJSONArray("rows");return SportsLeadersRP121(j.getString("key"),j.getString("title"),j.getString("unit"),(0 until a.length()).map {val r=a.getJSONObject(it);SportsLeaderRP121(r.optString("rank"),SportsPersonRP121.parse(r.getJSONObject("person")),r.optString("value"))})}}
}

internal object SportsParsersRP121 {
    private fun objects(a:JSONArray?):List<JSONObject> = if(a==null) emptyList() else (0 until a.length()).mapNotNull {a.optJSONObject(it)}
    fun nbaLeaders(j:JSONObject,expectedSeason:Int?=null):List<SportsLeadersRP121> {
        if(expectedSeason!=null && j.optJSONObject("requestedSeason")?.optInt("year")!=expectedSeason) return emptyList()
        val wanted=listOf("pointsPerGame","reboundsPerGame","assistsPerGame","fieldGoalPercentage","3PointsMadePerGame","3PointPct","blocksPerGame","stealsPerGame")
        return objects(j.optJSONObject("leaders")?.optJSONArray("categories")).filter {it.optString("name") in wanted}.sortedBy {wanted.indexOf(it.optString("name"))}.map {category ->
            val rows=objects(category.optJSONArray("leaders")).mapIndexedNotNull {index,row ->
                val athlete=row.optJSONObject("athlete") ?: return@mapIndexedNotNull null
                val id=athlete.optString("id");if(id.isBlank()) return@mapIndexedNotNull null
                val team=row.optJSONObject("team") ?: JSONObject()
                SportsLeaderRP121((index+1).toString(),SportsPersonRP121(id,athlete.optString("displayName",athlete.optString("fullName")),"NBA",
                    athlete.optJSONObject("headshot")?.optString("href").orEmpty().ifBlank {"https://a.espncdn.com/i/headshots/nba/players/full/$id.png"},team.optString("id"),team.optString("displayName"),team.optJSONArray("logos")?.optJSONObject(0)?.optString("href").orEmpty()),row.optString("displayValue","—"))
            }.distinctBy {it.person.id}.take(5)
            SportsLeadersRP121(category.optString("name"),category.optString("displayName"),category.optString("abbreviation"),rankTies(rows))
        }.filter {it.rows.isNotEmpty()}
    }
    fun baseballLeaders(j:JSONObject,league:String,group:String,teams:List<CalendarTeamRP120>,expectedSeason:Int?=null):List<SportsLeadersRP121> = objects(j.optJSONArray("leagueLeaders"))
        .filter {it.optString("statGroup")==group && (expectedSeason==null || it.optInt("season")==expectedSeason)}.groupBy {it.optString("leaderCategory")}.map {(category,groups) ->
            val rows=groups.flatMap {objects(it.optJSONArray("leaders"))}.mapNotNull {row ->
                val person=row.optJSONObject("person") ?: return@mapNotNull null
                val id=person.optString("id");if(id.isBlank()) return@mapNotNull null
                val team=row.optJSONObject("team") ?: JSONObject();val teamId=team.optString("id")
                SportsLeaderRP121(row.optString("rank"),SportsPersonRP121(id,person.optString("fullName"),league,mlbPhoto(id),teamId,
                    teams.firstOrNull {it.league==league && it.id==teamId}?.name ?: team.optString("name"),teams.firstOrNull {it.league==league && it.id==teamId}?.logo.orEmpty()),row.optString("value","—"))
            }.distinctBy {it.person.id}.sortedBy {it.rank.toIntOrNull() ?: 999}.take(5)
            SportsLeadersRP121("$group:$category",category,when(category) {"battingAverage"->"AVG";"earnedRunAverage"->"ERA";"runsBattedIn"->"RBI";"homeRuns"->"HR";"stolenBases"->"SB";"strikeouts"->"K";"whip"->"WHIP";else->""},rows)
        }.filter {it.rows.isNotEmpty()}
    fun nbaActive(j:JSONObject):List<SportsPersonRP121> = objects(j.optJSONArray("items")).filter {it.optBoolean("active",false)}.mapNotNull {p ->
        val id=p.optString("id");val name=p.optString("fullName",p.optString("displayName"));if(id.isBlank() || name.isBlank()) null
        else SportsPersonRP121(id,name,"NBA","https://a.espncdn.com/i/headshots/nba/players/full/$id.png",position=p.optJSONObject("position")?.optString("abbreviation").orEmpty())
    }.distinctBy {it.id}
    fun nbaRoster(j:JSONObject,team:CalendarTeamRP120):List<SportsPersonRP121> = objects(j.optJSONArray("athletes")).filter {
        val type=it.optJSONObject("status")?.optString("type");type=="active"
    }.mapNotNull {p ->val id=p.optString("id");if(id.isBlank()) null else SportsPersonRP121(id,p.optString("displayName"),"NBA",p.optJSONObject("headshot")?.optString("href").orEmpty().ifBlank {"https://a.espncdn.com/i/headshots/nba/players/full/$id.png"},team.id,team.name,team.logo,p.optJSONObject("position")?.optString("abbreviation").orEmpty())}.distinctBy {it.id}
    fun mlbActive(j:JSONObject,teams:List<CalendarTeamRP120>):List<SportsPersonRP121> = objects(j.optJSONArray("people")).filter {it.optBoolean("active") && it.optBoolean("isPlayer",true)}.mapNotNull {p ->
        val team=teams.firstOrNull {it.league=="MLB" && it.id==p.optJSONObject("currentTeam")?.optString("id")} ?: return@mapNotNull null
        val id=p.optString("id");if(id.isBlank()) null else SportsPersonRP121(id,p.optString("fullName"),"MLB",mlbPhoto(id),team.id,team.name,team.logo,p.optJSONObject("primaryPosition")?.optString("abbreviation").orEmpty())
    }.distinctBy {it.id}
    fun baseballRoster(j:JSONObject,team:CalendarTeamRP120):List<SportsPersonRP121> = objects(j.optJSONArray("roster")).filter {it.optJSONObject("status")?.optString("code")=="A"}.mapNotNull {entry ->
        val p=entry.optJSONObject("person") ?: return@mapNotNull null;val id=p.optString("id");if(id.isBlank()) null
        else SportsPersonRP121(id,p.optString("fullName"),team.league,mlbPhoto(id),team.id,team.name,team.logo,entry.optJSONObject("position")?.optString("abbreviation").orEmpty())
    }.distinctBy {it.id}
    fun f1People(j:JSONObject,heads:JSONObject,expectedSeason:Int?=null):List<SportsPersonRP121> {
        if(expectedSeason!=null && j.optJSONObject("MRData")?.optJSONObject("RaceTable")?.optInt("season")!=expectedSeason) return emptyList()
        val race=j.optJSONObject("MRData")?.optJSONObject("RaceTable")?.optJSONArray("Races")?.optJSONObject(0)
        val photos=objects(heads.optJSONArray("items"))
        return objects(race?.optJSONArray("Results")).mapNotNull {r ->
            val d=r.optJSONObject("Driver") ?: return@mapNotNull null;val c=r.optJSONObject("Constructor") ?: JSONObject()
            val name=d.optString("givenName")+" "+d.optString("familyName");val id=d.optString("driverId")
            val head=photos.firstOrNull {SportsBrowsePolicyRP121.key(it.optString("fullName")).replace("alexander","alex")==SportsBrowsePolicyRP121.key(name).replace("alexander","alex")}
            SportsPersonRP121(id,name,"F1",head?.optString("id")?.takeIf {it.isNotBlank()}?.let {"https://a.espncdn.com/i/headshots/rpm/players/full/$it.png"}.orEmpty(),c.optString("constructorId"),c.optString("name"),StandingsRP120.f1Logo(c.optString("constructorId")),d.optString("permanentNumber"))
        }.distinctBy {it.id}
    }
    fun f1Leaders(j:JSONObject,people:List<SportsPersonRP121>,expectedSeason:Int?=null):List<SportsLeadersRP121> {
        if(expectedSeason!=null && j.optJSONObject("MRData")?.optJSONObject("StandingsTable")?.optInt("season")!=expectedSeason) return emptyList()
        val list=j.optJSONObject("MRData")?.optJSONObject("StandingsTable")?.optJSONArray("StandingsLists")?.optJSONObject(0)
        val rows=objects(list?.optJSONArray("DriverStandings"))
        fun group(key:String,title:String,unit:String,field:String)=SportsLeadersRP121(key,title,unit,rows.sortedByDescending {it.optDouble(field)}.mapNotNull {r ->
            val d=r.optJSONObject("Driver") ?: return@mapNotNull null;val c=r.optJSONArray("Constructors")?.optJSONObject(0) ?: JSONObject();val id=d.optString("driverId")
            val person=people.firstOrNull {it.id==id} ?: SportsPersonRP121(id,d.optString("givenName")+" "+d.optString("familyName"),"F1",teamId=c.optString("constructorId"),team=c.optString("name"),logo=StandingsRP120.f1Logo(c.optString("constructorId")))
            SportsLeaderRP121("",person,r.optString(field,"—"))
        }.take(5).let {rankTies(it)})
        return listOf(group("f1:points","Driver points","PTS","points"),group("f1:wins","Race wins","W","wins")).filter {it.rows.isNotEmpty()}
    }
    private fun rankTies(rows:List<SportsLeaderRP121>)=rows.mapIndexed {index,row ->
        val value=row.value.removeSuffix("%").toDoubleOrNull()
        val rank=if(value==null) index else rows.indexOfFirst {it.value.removeSuffix("%").toDoubleOrNull()==value}
        row.copy(rank=(rank+1).toString())
    }
    fun mlbPhoto(id:String)="https://img.mlbstatic.com/mlb-photos/image/upload/w_320,q_auto:best/v1/people/$id/headshot/67/current"
    fun sorted(people:List<SportsPersonRP121>):List<SportsPersonRP121> = people.distinctBy {it.id}.sortedWith(compareBy<SportsPersonRP121> {SportsBrowsePolicyRP121.featured(it.league,it.id,it.name).let {rank ->if(rank<0) Int.MAX_VALUE else rank}}.thenBy {SportsBrowsePolicyRP121.key(it.name)})
}
