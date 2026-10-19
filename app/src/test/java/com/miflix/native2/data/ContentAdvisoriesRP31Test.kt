package com.miflix.native2.data
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
class ContentAdvisoriesRP31Test {
 @Test fun readsMajorityVote() {val r=ContentAdvisoriesRP31.parse(JSONObject("""{"parentsGuide":[{"category":"PROFANITY","severityBreakdowns":[{"severityLevel":"mild","voteCount":2},{"severityLevel":"severe","voteCount":15}]}]}"""));assertEquals(listOf(ContentAdvisoriesRP31.Warning("PROFANITY","severe")),r)}
 @Test fun noneWinnerProducesNoWarning() {assertTrue(ContentAdvisoriesRP31.parse(JSONObject("""{"parentsGuide":[{"category":"VIOLENCE","severityBreakdowns":[{"severityLevel":"none","voteCount":20},{"severityLevel":"severe","voteCount":1}]}]}""")).isEmpty())}
 @Test fun missingVotesNotInvented() {assertTrue(ContentAdvisoriesRP31.parse(JSONObject("""{"parentsGuide":[{"category":"PROFANITY","severityBreakdowns":[{"severityLevel":"severe"}]}]}""")).isEmpty())}
 @Test fun errorResponsesEmpty() {assertTrue(ContentAdvisoriesRP31.parse(JSONObject("""{"error":"unavailable"}""")).isEmpty())}
 @Test fun unknownCategoriesIgnored() {assertTrue(ContentAdvisoriesRP31.parse(JSONObject("""{"parentsGuide":[{"category":"FAKE","severityBreakdowns":[{"severityLevel":"severe","voteCount":10}]}]}""")).isEmpty())}
 @Test fun unknownSeverityIgnored() {assertTrue(ContentAdvisoriesRP31.parse(JSONObject("""{"parentsGuide":[{"category":"PROFANITY","severityBreakdowns":[{"severityLevel":"extreme","voteCount":10}]}]}""")).isEmpty())}
}
