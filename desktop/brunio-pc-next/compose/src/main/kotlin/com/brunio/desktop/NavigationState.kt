package com.brunio.desktop

/** Monotonic hover timing prevents repeated expansion when moving between child rows. */
class NavigationState(
    private val openDelayMs: Long = 140,
    private val closeDelayMs: Long = 260,
) {
    init { require(openDelayMs >= 0 && closeDelayMs >= 0) }
    var expanded = false
        private set
    private var hovered = false
    private var focused = false
    private var target: Boolean? = null
    private var due = 0L

    fun pointer(inside: Boolean, now: Long) { hovered = inside; schedule(now) }
    fun focus(inside: Boolean, now: Long) { focused = inside; schedule(now) }
    private fun schedule(now: Long) {
        val desired = hovered || focused
        if (desired == expanded) { target = null; return }
        if (target != desired) {
            target = desired
            due = now + if (desired) openDelayMs else closeDelayMs
        }
    }
    fun tick(now: Long): Boolean {
        val next = target ?: return false
        if (now < due) return false
        expanded = next
        target = null
        return true
    }
    fun hide() { hovered = false; focused = false; target = null; expanded = false }
}
