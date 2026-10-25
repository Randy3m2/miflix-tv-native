package com.miflix.native2.data

/** One-shot focus handoff. Recomposition cannot repeatedly reclaim focus. */
internal class HeaderFocusRP108 {
    private var bootOwner:String?=null
    private var pending:Pair<String,String>?=null
    fun hasPending()=pending!=null
    fun navigate(owner:String,destination:String) {pending=owner to destination}
    fun claim(owner:String,screen:String):Boolean {
        if(pending==(owner to screen)) {pending=null;bootOwner=owner;return true}
        if(screen=="HOME" && bootOwner!=owner) {bootOwner=owner;return true}
        return false
    }
}
