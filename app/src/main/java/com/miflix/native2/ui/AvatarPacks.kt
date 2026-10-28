package com.miflix.native2.ui

import android.content.res.Resources
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.miflix.native2.R
import kotlin.math.roundToInt

data class CharacterAvatarPack(val id: String,val title: String,val image: Int,val names: List<String>,val portraits:List<Int> = emptyList())
data class CharacterAvatar(val key: String,val label: String,val pack: CharacterAvatarPack,val index: Int)

// Existing atlas keys stay intact; new packs use small bundled character portraits.
val CharacterAvatarPacks=listOf(
    CharacterAvatarPack("friends","Friends",R.drawable.brunio_friends_0_rp138,listOf("Rachel Green","Monica Geller","Phoebe Buffay","Joey Tribbiani","Chandler Bing","Ross Geller"),portraits=listOf(R.drawable.brunio_friends_0_rp138,R.drawable.brunio_friends_1_rp138,R.drawable.brunio_friends_2_rp138,R.drawable.brunio_friends_3_rp138,R.drawable.brunio_friends_4_rp138,R.drawable.brunio_friends_5_rp138)),
    CharacterAvatarPack("visionquest","VisionQuest",R.drawable.brunio_visionquest_0_rp138,listOf("Vision","Ultron","Thomas","F.R.I.D.A.Y.","E.D.I.T.H.","J.A.R.V.I.S."),portraits=listOf(R.drawable.brunio_visionquest_0_rp138,R.drawable.brunio_visionquest_1_rp138,R.drawable.brunio_visionquest_2_rp138,R.drawable.brunio_visionquest_3_rp138,R.drawable.brunio_visionquest_4_rp138,R.drawable.brunio_visionquest_5_rp138)),
    CharacterAvatarPack("loki","Loki",R.drawable.brunio_loki_0_rp138,listOf("Loki","Mobius M. Mobius","Sylvie","Hunter B-15","Ravonna Lexus Renslayer","Miss Minutes"),portraits=listOf(R.drawable.brunio_loki_0_rp138,R.drawable.brunio_loki_1_rp138,R.drawable.brunio_loki_2_rp138,R.drawable.brunio_loki_3_rp138,R.drawable.brunio_loki_4_rp138,R.drawable.brunio_loki_5_rp138)),
    CharacterAvatarPack("f1","Formula 1",R.drawable.brunio_pack_f1,listOf("Lewis Hamilton","Max Verstappen","Fernando Alonso","Charles Leclerc","Lando Norris")),
    CharacterAvatarPack("got","Game of Thrones",R.drawable.brunio_pack_got,listOf("Jon Snow","Daenerys","Tyrion","Arya","Cersei")),
    CharacterAvatarPack("lanterns","Lanterns",R.drawable.brunio_pack_lanterns,listOf("Hal Jordan","John Stewart","Sinestro","Sheriff Kerry","William Macon")),
    CharacterAvatarPack("snyder","Justice League · Snyder Cut",R.drawable.brunio_pack_snyder,listOf("Superman","Batman","Wonder Woman","Aquaman","Cyborg")),
    CharacterAvatarPack("avengers","Avengers",R.drawable.brunio_pack_avengers,listOf("Iron Man","Captain America","Thor","Black Widow","Hulk")),
    CharacterAvatarPack("tlou","The Last of Us",R.drawable.brunio_pack_tlou,listOf("Joel","Ellie","Tommy","Abby","Dina")),
    CharacterAvatarPack("gt","Gran Turismo",R.drawable.brunio_pack_gt,listOf("Jann","Jack Salter","Danny Moore","Matty Davis","Audrey")),
    CharacterAvatarPack("wwe","WWE",R.drawable.brunio_pack_wwe,listOf("Roman Reigns","Cody Rhodes","John Cena","Rhea Ripley","Undertaker")),
    CharacterAvatarPack("gow","God of War",R.drawable.brunio_pack_gow,listOf("Kratos","Atreus","Freya","Mimir","Thor")),
    CharacterAvatarPack("ghost","Ghost of Tsushima",R.drawable.brunio_pack_ghost,listOf("Jin Sakai","Yuna","Lord Shimura","Ryuzo","Khotun Khan")),
    CharacterAvatarPack("rdr2","Red Dead Redemption 2",R.drawable.brunio_pack_rdr2,listOf("Arthur Morgan","John Marston","Dutch","Sadie Adler","Charles Smith")),
    CharacterAvatarPack("dcu","DCU",R.drawable.brunio_pack_dcu,listOf("Superman","Krypto","Mister Terrific","Hawkgirl","Guy Gardner")),
    CharacterAvatarPack("power","Power",R.drawable.brunio_pack_power,listOf("Ghost","Tommy","Tasha","Tariq","Kanan")),
    CharacterAvatarPack("ds","Death Stranding",R.drawable.brunio_pack_ds,listOf("Sam","Fragile","Higgs","Cliff","Die-Hardman")),
 )
val CharacterAvatars=CharacterAvatarPacks.flatMap { pack ->
    pack.names.mapIndexed { index,label -> CharacterAvatar("pack:${pack.id}:$index",label,pack,index) }
}
fun characterAvatar(key: String?): CharacterAvatar? = CharacterAvatars.firstOrNull { it.key==key }

private object AvatarAtlasCache {
    private val images=LruCache<Int,ImageBitmap>(4)
    @Synchronized fun image(resources: Resources,id: Int): ImageBitmap {
        images.get(id)?.let { return it }
        return BitmapFactory.decodeResource(resources,id).asImageBitmap().also { images.put(id,it) }
    }
}

@Composable
internal fun CharacterAvatarImage(avatar: CharacterAvatar,modifier: Modifier=Modifier) {
    if(avatar.pack.portraits.isNotEmpty()) {
        androidx.compose.foundation.Image(androidx.compose.ui.res.painterResource(avatar.pack.portraits[avatar.index]),avatar.label,modifier,alignment=androidx.compose.ui.Alignment.TopCenter,contentScale=androidx.compose.ui.layout.ContentScale.Crop)
        return
    }
    val resources=LocalContext.current.resources
    val atlas=remember(avatar.pack.image) { AvatarAtlasCache.image(resources,avatar.pack.image) }
    Canvas(modifier) {
        drawImage(atlas,srcOffset=IntOffset(avatar.index*256,0),srcSize=IntSize(256,256),
            dstSize=IntSize(size.width.roundToInt().coerceAtLeast(1),size.height.roundToInt().coerceAtLeast(1)))
    }
}
