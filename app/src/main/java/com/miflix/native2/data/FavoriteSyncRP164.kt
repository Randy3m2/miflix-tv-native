package com.miflix.native2.data

internal object FavoriteSyncRP164 {
 const val PAGE=30
 fun valid(id:String)=Regex("^tmdb:(movie|series):[1-9][0-9]*$").matches(id)
 fun merge(server:Collection<String>,pending:Map<String,Boolean>):List<String> {
  val adds=pending.filterValues {it}.keys.filter(::valid)
  return (adds+server).filter {valid(it) && pending[it]!=false}.distinct()
 }
 fun acknowledge(current:Map<String,Boolean>,sent:Map<String,Boolean>)=current.filter {sent[it.key]!=it.value}
}
