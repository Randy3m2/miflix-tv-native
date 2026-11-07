package com.miflix.native2.data

import java.util.Locale

/** Catalog IDs and ordinal episodes remain stable for the local broadcast date. */
internal object TirsoDayRP181 {
    private data class Title(val id:Int,val name:String,val episodes:Int=0)
    private val films=listOf(
        Title(157336,"Interstellar"),Title(603,"The Matrix"),Title(27205,"Inception"),
        Title(329,"Jurassic Park"),Title(155,"The Dark Knight"),Title(1726,"Iron Man"),
        Title(118340,"Guardians of the Galaxy"),Title(105,"Back to the Future"),
        Title(37165,"The Truman Show"),Title(808,"Shrek"),Title(12,"Finding Nemo"),
        Title(120,"The Lord of the Rings: The Fellowship of the Ring"),Title(564,"The Mummy"),
        Title(245891,"John Wick"),Title(138843,"The Conjuring"),Title(4232,"Scream"),
        Title(329865,"Arrival"),Title(546554,"Knives Out"),Title(120467,"The Grand Budapest Hotel"),
        Title(76341,"Mad Max: Fury Road"),Title(354912,"Coco"),Title(2062,"Ratatouille"))
    private val shows=listOf(Title(1668,"Friends",236),Title(1396,"Breaking Bad",62),
        Title(2316,"The Office",201),Title(246,"Avatar: The Last Airbender",61),
        Title(456,"The Simpsons",300),Title(66732,"Stranger Things",34),
        Title(2098,"Batman: The Animated Series",85),Title(1618,"Justice League",52),
        Title(37854,"One Piece",500),Title(1429,"Attack on Titan",75))
    private val dragon=listOf(Title(12971,"Dragon Ball Z",291),Title(12697,"Dragon Ball GT",64),Title(62715,"Dragon Ball Super",131))
    private val naruto=Title(46260,"Naruto",220)
    private val shippuden=Title(31910,"Naruto Shippuden",500)
    private val family=Title(1434,"Family Guy",300)
    private val heroes=listOf(Title(155,"The Dark Knight"),Title(49521,"Man of Steel"),Title(297762,"Wonder Woman"),Title(1726,"Iron Man"),Title(24428,"The Avengers"),Title(118340,"Guardians of the Galaxy"))
    fun build(year:Int,dayOfYear:Int):List<TirsoScheduleItemRP174> {
        val day=String.format(Locale.US,"%04d-%03d",year,dayOfYear)
        val seed=year.toLong()*366+dayOfYear
        val out=mutableListOf<TirsoScheduleItemRP174>()
        fun add(title:Title,start:Int,duration:Int,serial:Int) {
            val episode=if(title.episodes>0) ((seed*3+serial)%title.episodes).toInt()+1 else 0
            out+=TirsoScheduleItemRP174("$day-$start",if(episode>0) "${title.name} · Episode $episode" else title.name,
                title.name,start,start+duration,day,title.name,episode==0,title.id,episode)
        }
        repeat(4) {i->add(films[((seed+i*5)%films.size).toInt()],i*120,120,i)}
        repeat(4) {i->add(listOf(naruto,shippuden,family,shows[(seed%shows.size).toInt()])[i],480+i*30,30,i)}
        repeat(4) {i->add(shows[((seed+i)%shows.size).toInt()],600+i*30,30,i)}
        repeat(2) {i->add(films[((seed+i*7+9)%films.size).toInt()],720+i*120,120,i)}
        repeat(4) {i->add(listOf(naruto,family,shippuden,family)[i],960+i*30,30,i+4)}
        repeat(3) {i->add(dragon[(seed%dragon.size).toInt()],1080+i*40,40,i)}
        repeat(4) {i->add(if(i%2==0) shippuden else family,1200+i*30,30,i+8)}
        add(heroes[(seed%heroes.size).toInt()],1320,120,0)
        return out.sortedBy {it.startMinute}
    }
}
