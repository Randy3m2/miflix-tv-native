package com.miflix.native2.ui
import org.junit.Test
import org.junit.Assert.*
class UpdateNotesRP124Test {
    @Test fun currentNotesDescribeCurrentRelease() {
        assertTrue(UpdateNotes.changes.first().first.startsWith("RP124:"))
        assertTrue(UpdateNotes.changes.first().second.startsWith("RP124:"))
        assertFalse(UpdateNotes.changes.any {it.first.startsWith("RC") || it.second.startsWith("RC")})
    }
    @Test fun recentRpHistoryPrecedesLegacyRcNotes() {
        assertTrue(UpdateNotes.previousChanges.first().first.startsWith("RP123:"))
        assertTrue(UpdateNotes.previousChanges.any {it.first.startsWith("RC")})
    }
    @Test fun currentEntriesAreNotDuplicatedInHistory() {
        assertTrue(UpdateNotes.changes.none {it in UpdateNotes.previousChanges})
    }
}
