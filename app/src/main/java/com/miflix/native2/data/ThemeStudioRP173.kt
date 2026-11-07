package com.miflix.native2.data

/** Curated, static ambient themes shared by TV and mobile. */
data class ThemeStudioPresetRP173(
    val id: String, val title: String,
    val primary: Long, val secondary: Long, val background: Long,
    val surface: Long, val focus: Long, val atmosphere: String,
    val motion: String? = null,
)

object ThemeStudioRP173 {
    val presets: List<ThemeStudioPresetRP173> = listOf(
        ThemeStudioPresetRP173("default","Default · BruniO",0xFFB792FF,0xFFAAAAAF,0xFF050505,0xFF141414,0xFFB792FF,"Original BruniO appearance"),
        ThemeStudioPresetRP173("fogata","Fogata",0xFFAD653F,0xFFDEC8B8,0xFF100C0B,0xFF251C18,0xFFFFB679,"Cobre cálido y carbón · Warm copper and charcoal"),
        ThemeStudioPresetRP173("sand","Sand",0xFF9B815E,0xFFD9CDBA,0xFF15120F,0xFF29241D,0xFFE6C99A,"Arena, ámbar y tonos tierra · Sand, amber and earth"),
        ThemeStudioPresetRP173("christmas","Christmas",0xFF65866D,0xFFD5DCCF,0xFF080F0E,0xFF192622,0xFFFFCE82,"Verde pino y dorado champán · Pine green and champagne gold"),
        ThemeStudioPresetRP173("halloween","Halloween",0xFFAD7049,0xFFD8C7DE,0xFF100D16,0xFF251E2C,0xFFFFAC47,"Ciruela oscura y naranja ámbar · Deep plum and amber orange"),
        ThemeStudioPresetRP173("summer","Summer",0xFF4A9699,0xFFC6DEDA,0xFF08171B,0xFF192D32,0xFFFFCF76,"Turquesa y luz dorada · Turquoise and golden light"),
        ThemeStudioPresetRP173("storm","Storm",0xFF667E9C,0xFFCCD5E3,0xFF0D121C,0xFF1D2838,0xFF9EBFF0,"Azul pizarra y gris plata · Slate blue and silver grey"),
        ThemeStudioPresetRP173("nature","Nature",0xFF688B72,0xFFCBDCCB,0xFF0C1410,0xFF1D2C24,0xFFABD5AD,"Verde bosque y salvia · Forest green and sage"),
        ThemeStudioPresetRP173("sea","Sea",0xFF508EAA,0xFFC1DCE6,0xFF08141E,0xFF162A39,0xFF91D4E5,"Azul profundo y aguamarina · Deep blue and aquamarine"),
        ThemeStudioPresetRP173("nba_gsw","NBA · GSW",0xFF3368B5,0xFFD3DDED,0xFF091326,0xFF162C4D,0xFFFFD166,"Azul Warriors y dorado · Warriors blue and gold"),
    )
    // Retired franchise themes return to Default rather than leaving an old skin active.
    private val retiredFocus = setOf<Long>(0xFFF2B5D4,0xFF73BBFF,0xFFF18A8F,0xFFFF6676,0xFFFF3344,0xFFCF3D49,0xFF47E2E5,0xFFC0A45E,0xFFD5AA55,0xFFE1B95C,0xFFB986E5,0xFF86B6E8,0xFFD0B878,0xFF5C9BFF,0xFFFF5252,0xFF58A0FF,0xFFE1C26B,0xFFFF3B81,0xFFE2C777,0xFFD08A51,0xFFBBD9E8,0xFFDABF8A,0xFFFF8EDB,0xFFB1F2D2,0xFF88FFB0,0xFFC0C9C7,0xFFE2EDF0,0xFFFF5858,0xFFD1BE99,0xFF6CE6FF,0xFFD2E06D,0xFFC9D5DD,0xFFEBC58A,0xFFFF765C,0xFF45F27C,0xFFFFC34D,0xFFFF9A4D)
    fun normalizeFocus(value:Int):Int = if((value.toLong() and 0xFFFFFFFFL) in retiredFocus) presets.first().focus.toInt() else value
    fun cornerRadius(id:String):Int = when(id) {"storm","nba_gsw"->10;"fogata","nature"->16;"christmas"->18;"sand"->20;"halloween"->22;else->24}
    fun byId(id:String):ThemeStudioPresetRP173 = presets.firstOrNull {it.id==id} ?: presets.first()
}
