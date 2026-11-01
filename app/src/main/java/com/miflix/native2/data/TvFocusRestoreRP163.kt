package com.miflix.native2.data

/** Native default focus must not overwrite a pending return; real user input always wins. */
internal class TvFocusRestoreRP163(initial:String?) {
    var last=initial;private set
    var pending=initial;private set
    var intent=0L;private set
    fun focused(key:String) {last=key}
    fun windowLost() {pending=last}
    fun interacted() {intent++;pending=null}
    fun restored() {pending=null}
}
internal class TvPressRP163 {
    private var held=false
    fun down(repeat:Int) {if(repeat==0) held=true}
    fun up(cancelled:Boolean):Boolean {val fire=held && !cancelled;held=false;return fire}
    fun reset() {held=false}
}
