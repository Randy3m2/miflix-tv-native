package com.miflix.native2.ui
import org.junit.Test
import org.junit.Assert.*
class UpdateNotesRP124Test {
    @Test fun currentNotesDescribeCurrentRelease() {
        val release=com.miflix.native2.BuildConfig.VERSION_NAME.substringAfterLast("-").uppercase()+":"
        assertTrue(UpdateNotes.changes.first().first.startsWith(release))
        assertTrue(UpdateNotes.changes.first().second.startsWith(release))
        assertFalse(UpdateNotes.changes.any {it.first.startsWith("RC") || it.second.startsWith("RC")})
    }
    @Test fun recentRpHistoryPrecedesLegacyRcNotes() {
        assertTrue(UpdateNotes.previousChanges.first().first.startsWith("RP"))
        assertTrue(UpdateNotes.previousChanges.any {it.first.startsWith("RC")})
    }
    @Test fun currentEntriesAreNotDuplicatedInHistory() {
        assertTrue(UpdateNotes.changes.none {it in UpdateNotes.previousChanges})
    }
}
