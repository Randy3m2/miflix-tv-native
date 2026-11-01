package com.miflix.native2.data
internal object ReleasePolicyRP164 {
 fun changed(previous:String?,current:String?)=previous!=null && current!=null && previous!=current
 fun batch(ids:List<String>,cursor:Int,size:Int=30):List<String> {
  val valid=ids.filter(FavoriteSyncRP164::valid).distinct();if(valid.isEmpty()) return emptyList()
  val start=cursor.mod(valid.size)
  return (0 until minOf(size,valid.size)).map {valid[(start+it)%valid.size]}
 }
}
