package com.miflix.native2.data

import org.junit.Assert.*
import org.junit.Test

class StatsTableRP105Test {
    private fun nba(count:Int=15)=StatsGroupRP95("Bulls",listOf("MIN","PTS","REB","AST","BLK","FG","3PT"),(1..count).map {StatsRowRP95("$it","Player $it",listOf("30","20","10","5","2","8-12","4-6"),"image$it","$it")},"4")
    @Test fun nbaColumnsHaveSharedOrderAndMatchTheirValues() {
        val table=StatsTablePolicyRP105.build(listOf(nba()),"NBA",0)
        assertEquals(listOf("PTS","REB","AST","BLK"),table.columns)
        assertEquals(listOf("20","10","5","2"),table.rows[0].values)
        assertEquals(2,table.pages)
    }
    @Test fun otherColumnsRemainAccessibleWithoutExpandingRows() {
        val table=StatsTablePolicyRP105.build(listOf(nba()),"NBA",1)
        assertEquals(listOf("MIN","FG","3PT"),table.columns)
        assertEquals(listOf("30","8-12","4-6"),table.rows[0].values)
        assertEquals(15,table.rows.size)
    }
    @Test fun refreshDoesNotChangeFocusKeysOrOrder() {
        val group=nba();val updated=group.copy(rows=group.rows.map {it.copy(values=it.values.map {"99"})})
        assertEquals(StatsTablePolicyRP105.build(listOf(group),"NBA",0).rows.map {it.key},StatsTablePolicyRP105.build(listOf(updated),"NBA",0).rows.map {it.key})
    }
    @Test fun traversalWorksPastFifthRowAndReturnsToFixedHeader() {
        var row=0
        repeat(99) {row=StatsTablePolicyRP105.target(row,1,100)}
        assertEquals(99,row);assertEquals(99,StatsTablePolicyRP105.target(row,1,100))
        repeat(100) {row=StatsTablePolicyRP105.target(row,-1,100)}
        assertEquals(-1,row);assertEquals(-1,StatsTablePolicyRP105.target(-1,-1,100))
    }
    @Test fun emptyTablesAndShortValuesHaveSafeFallbacks() {
        assertEquals(-1,StatsTablePolicyRP105.target(0,1,0))
        assertEquals(1,StatsTablePolicyRP105.build(emptyList(),"NBA",9).pages)
        val group=nba(1).copy(rows=listOf(StatsRowRP95("1","Player",emptyList())))
        assertEquals(listOf("—","—","—","—"),StatsTablePolicyRP105.build(listOf(group),"NBA",0).rows[0].values)
    }
    @Test fun mlbBattingAndPitchingUseCorrectColumns() {
        val batting=StatsGroupRP95("Team",listOf("AB","H","R","RBI","HR"),listOf(StatsRowRP95("1","Batter",listOf("4","2","1","3","1"))),"1")
        assertEquals(listOf("AB","H","R","RBI"),StatsTablePolicyRP105.build(listOf(batting),"MLB",0).columns)
        val pitching=StatsGroupRP95("Team",listOf("IP","H","ER","K","BB"),emptyList(),"1")
        assertEquals(listOf("IP","H","ER","K"),StatsTablePolicyRP105.build(listOf(pitching),"MLB",0).columns)
    }
    @Test fun playerIdentityAndPhotoStayAttached() {
        val row=StatsTablePolicyRP105.build(listOf(nba(1)),"NBA",0).rows.single()
        assertEquals("4:1",row.key);assertEquals("image1",row.photo);assertEquals("1",row.number)
    }
    @Test fun leagueHintDoesNotChangeTheGameIdentity() {
        assertEquals("MLB",StatsTablePolicyRP105.preferredLeague("MLB live"))
        assertEquals("MLB",StatsTablePolicyRP105.preferredLeague("Béisbol"))
        assertEquals("NBA",StatsTablePolicyRP105.preferredLeague("NBA live"))
    }
    @Test fun livePollsRunEveryTenSecondsWithBoundedFailureBackoff() {
        assertEquals(10_000L,StatsTablePolicyRP105.pollDelayMs(true,0,0))
        assertEquals(7_000L,StatsTablePolicyRP105.pollDelayMs(true,0,3_000))
        assertEquals(30_000L,StatsTablePolicyRP105.pollDelayMs(false,0,0))
        assertEquals(15_000L,StatsTablePolicyRP105.pollDelayMs(true,1,0))
        assertEquals(60_000L,StatsTablePolicyRP105.pollDelayMs(true,10,0))
        assertEquals(1_000L,StatsTablePolicyRP105.pollDelayMs(true,0,20_000))
    }
}
