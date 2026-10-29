package com.miflix.native2.data

import org.junit.Assert.*
import org.junit.Test

class PartyVotingRP151Test {
    private val now=1_800_000_000_000L
    private val host="host"
    private val members=setOf("host","guest")
    private val options=listOf(PartyPickRP151(11,"movie","Star Wars"),PartyPickRP151(31910,"series","Naruto Shippuden",8,12))
    private fun event(id:Long,user:String,body:String)=PartyEvent(id,user,"chat",body,now)
    private fun initial()=PartyVotingRP151.reduce(null,event(1,host,PartyVotingRP151.start(options)),host,members,now)!!
    @Test fun proposalFitsExistingTransport() {
        assertTrue(PartyVotingRP151.start(options+PartyPickRP151(12,"movie","a".repeat(90))).length<=400)
    }
    @Test fun hostAloneCanOpenAndClose() {
        assertNull(PartyVotingRP151.reduce(null,event(1,"guest",PartyVotingRP151.start(options)),host,members,now))
        val p=initial()
        assertEquals(p,PartyVotingRP151.reduce(p,event(2,"guest",PartyVotingRP151.close(p.token)),host,members,now))
        assertTrue(PartyVotingRP151.reduce(p,event(2,host,PartyVotingRP151.close(p.token)),host,members,now)!!.closed)
    }
    @Test fun changeVoteKeepsOneVotePerPersonAndRejectsReplays() {
        val p=initial()
        val a=PartyVotingRP151.reduce(p,event(2,"guest",PartyVotingRP151.vote(p.token,0)),host,members,now)!!
        val b=PartyVotingRP151.reduce(a,event(3,"guest",PartyVotingRP151.vote(p.token,1)),host,members,now)!!
        assertEquals(mapOf("guest" to 1),b.votes)
        assertEquals(b,PartyVotingRP151.reduce(b,event(2,"guest",PartyVotingRP151.vote(p.token,0)),host,members,now))
        assertEquals(listOf(1),b.winners())
    }
    @Test fun unauthorizedInvalidExpiredAndOldBallotsCannotVote() {
        val p=initial()
        assertEquals(p,PartyVotingRP151.reduce(p,event(2,"stranger",PartyVotingRP151.vote(p.token,0)),host,members,now))
        assertEquals(p,PartyVotingRP151.reduce(p,event(2,"guest",PartyVotingRP151.vote(p.token,8)),host,members,now))
        assertEquals(p,PartyVotingRP151.reduce(p,event(2,"guest",PartyVotingRP151.vote(p.token,0)),host,members,now+600_001L))
        assertEquals(p,PartyVotingRP151.reduce(p,event(2,"guest",PartyVotingRP151.vote(java.util.UUID.randomUUID().toString(),0)),host,members,now))
    }
    @Test fun tiesAreExplicitAndZeroVotesHaveNoWinner() {
        val p=initial();assertTrue(p.winners().isEmpty())
        val a=PartyVotingRP151.reduce(p,event(2,"host",PartyVotingRP151.vote(p.token,0)),host,members,now)!!
        val b=PartyVotingRP151.reduce(a,event(3,"guest",PartyVotingRP151.vote(p.token,1)),host,members,now)!!
        assertEquals(listOf(0,1),b.winners())
    }
    @Test fun malformedAndDuplicateOptionsAreIgnored() {
        assertNull(PartyVotingRP151.reduce(null,event(1,host,PartyVotingRP151.PREFIX+"bad"),host,members,now))
        assertNull(PartyVotingRP151.reduce(null,event(1,host,PartyVotingRP151.start(listOf(options[0],options[0]))),host,members,now))
    }
}
