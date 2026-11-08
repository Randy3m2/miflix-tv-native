package com.miflix.native2.data

/** Static background-only silhouettes; no interactive or button decorations. */
internal object ThemeMotifsRP186 {
    const val silhouetteAlpha=.055f
    const val logoAlpha=.085f
    val paths=mapOf(
        "christmas" to "M50 5L28 33H36L17 57H29L8 83H44V96H56V83H92L71 57H83L64 33H72Z",
        "halloween" to "M49 25V10L61 6M50 27C17 3 0 39 10 69C18 96 36 92 50 88C64 92 82 96 90 69C100 39 83 3 50 27ZM25 51L39 42L42 59ZM58 59L61 42L75 51ZM28 73L40 79L50 72L60 79L72 73",
        "summer" to "M69 34A19 19 0 1 1 31 34A19 19 0 1 1 69 34M50 2V9M50 59V66M18 34H10M90 34H82M21 7L27 13M73 13L79 7M8 80Q22 69 36 80T64 80T92 80M8 94Q22 83 36 94T64 94T92 94",
        "fogata" to "M49 7C70 31 85 41 78 65C69 90 31 89 23 66C14 45 35 37 35 23C50 35 44 47 49 55C62 42 53 28 49 7ZM18 94L82 84M18 84L82 94",
        "sand" to "M78 21A12 12 0 1 1 54 21A12 12 0 1 1 78 21M2 77Q31 36 61 64Q81 44 98 63M2 98Q42 62 98 86",
        "storm" to "M18 53C-3 27 24 6 44 25C65 0 103 24 86 53ZM28 66L20 85M49 65L36 97M77 65L66 89",
        "nature" to "M3 91L30 44L43 67L66 19L97 91ZM30 44L30 25M12 96H38M54 96H91",
        "sea" to "M2 27Q26 5 50 27T98 27M2 55Q26 33 50 55T98 55M2 83Q26 61 50 83T98 83"
    )
    data class Placement(val x:Float,val y:Float,val side:Float)
    fun placement(width:Float,height:Float):Placement {
        val w=width.coerceAtLeast(0f);val h=height.coerceAtLeast(0f)
        val side=minOf(w*.72f,h*.58f)
        return Placement((w-side)*.92f,(h-side)*.54f,side)
    }
}
