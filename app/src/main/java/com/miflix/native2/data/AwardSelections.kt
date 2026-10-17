package com.miflix.native2.data

import org.json.JSONObject

/** Verified editorial data shipped with the app; ceremony year, never film release year.
 * Oscar collection covers Best Picture nominees, with its winner identified explicitly.
 * See awards_catalog.json for official sources and the exact supported years.
 */
internal object AwardSelections {
    data class Entry(val title:String,val type:String,val year:Int,val winner:Boolean)
    private var catalog=JSONObject()
    private val resolved=linkedMapOf<String,MutableList<Entry>>()
    fun configure(raw:String) {catalog=JSONObject(raw)}
    fun years(kind:String="oscar"):List<Int> {
        val group=catalog.optJSONObject(if(kind=="emmy") "emmys" else "oscars") ?: return emptyList()
        val current=java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
        return group.keys().asSequence().mapNotNull {it.toIntOrNull()}.filter {it<=current}.sortedDescending().toList()
    }
    fun latestYear(kind:String)=years(kind).firstOrNull() ?: 2024
    fun entries(kind:String,year:Int):List<Entry> {
        val rows=catalog.optJSONObject(if(kind=="emmy") "emmys" else "oscars")?.optJSONArray(year.toString()) ?: return emptyList()
        return (0 until rows.length()).map {i ->val x=rows.getJSONObject(i);Entry(x.getString("title"),x.getString("type"),year,x.getBoolean("winner"))}
    }
    fun register(id:Int,entry:Entry) {
        val rows=resolved.getOrPut("${entry.type}:$id") {mutableListOf()}
        if(rows.none {it.year==entry.year}) rows.add(entry)
    }
    fun isWinner(id:Int,type:String,year:Int)=resolved["$type:$id"]?.any {it.year==year && it.winner}==true
    fun label(id:Int,type:String,spanish:Boolean,year:Int?=null):String? {
        val entry=resolved["$type:$id"]?.filter {year==null || it.year==year}?.maxByOrNull {it.year} ?: return null
        val award=if(type=="series") "Emmy" else "Oscar"
        val state=if(entry.winner) {if(spanish) "Ganadora" else "Winner"} else {if(spanish) "Nominada" else "Nominee"}
        return "$award · $state · ${entry.year}"
    }
}
