package com.miflix.native2.data

/** Editorial selections, not a live awards feed or a popularity ranking.
 * Ceremony year is explicit; any winner label means at least one award for the production.
 * https://www.oscars.org/oscars/ceremonies/2024
 * https://www.televisionacademy.com/features/news/awards-news/76th-emmy-winners
 */
internal object AwardSelections {
    val oscars=listOf(872585,792307,840430,915935,467244,1056360,346698,466420,523607,666277)
    val emmys=listOf(126308,136315,124101,241259,65494,94028,90282,95480,60622,46648)
    private val oscarWinners=setOf(872585,792307,840430,915935,467244,1056360,346698)
    fun label(id:Int,type:String,spanish:Boolean):String?=when {
        type=="movie" && id in oscars -> if(id in oscarWinners) {
            if(spanish) "Oscar · Ganadora · 2024" else "Oscar · Winner · 2024"
        } else if(spanish) "Oscar · Nominada · 2024" else "Oscar · Nominee · 2024"
        type=="series" && id in emmys -> if(spanish) "Emmy · Ganadora · 2024" else "Emmy · Winner · 2024"
        else -> null
    }
}
