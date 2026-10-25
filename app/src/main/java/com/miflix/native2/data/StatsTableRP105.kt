package com.miflix.native2.data

internal data class StatsTableRowRP105(val key:String,val name:String,val values:List<String>,val photo:String="",val number:String="")
internal data class StatsTableRP105(val columns:List<String>,val rows:List<StatsTableRowRP105>,val pages:Int)
/** Stable row identities and four aligned numeric columns, independent of refresh values. */
internal object StatsTablePolicyRP105 {
    fun build(groups:List<StatsGroupRP95>,league:String,page:Int):StatsTableRP105 {
        val all=groups.flatMap {it.columns}.distinct()
        val preferred=if(league=="NBA") listOf("PTS","REB","AST","BLK") else if("AB" in all) listOf("AB","H","R","RBI") else listOf("IP","H","ER","K")
        val ordered=preferred.filter {it in all}+all.filter {it !in preferred}
        val pages=((ordered.size+3)/4).coerceAtLeast(1)
        val columns=ordered.drop(page.coerceIn(0,pages-1)*4).take(4)
        val rows=groups.flatMap {group ->group.rows.map {row ->
            StatsTableRowRP105("${group.teamId}:${row.id}",row.name,columns.map {column ->row.values.getOrNull(group.columns.indexOf(column)) ?: "—"},row.photo,row.number)
        }}.distinctBy {it.key}
        return StatsTableRP105(columns,rows,pages)
    }
    /** -1 is the fixed header, never a dead end above/below a lazy-list viewport. */
    fun target(index:Int,direction:Int,count:Int):Int=if(count<=0) -1 else (index+direction).coerceIn(-1,count-1)
    fun pollDelayMs(live:Boolean,failures:Int,elapsedMs:Long):Long {
        val period=if(failures>0) 15_000L*failures.coerceAtMost(4) else if(live) 10_000L else 30_000L
        return (period-elapsedMs.coerceAtLeast(0L)).coerceAtLeast(1_000L)
    }
    fun preferredLeague(title:String)=if(Regex("(?i)\\b(mlb|baseball|béisbol|beisbol)\\b").containsMatchIn(title)) "MLB" else "NBA"
}
