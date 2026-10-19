package com.miflix.native2.data
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
class ContentGuideRP33Test {
 @Test fun lowercaseCategoryAccepted() {val data=JSONObject("""{"parentsGuide":[{"category":"profanity","severityBreakdowns":[{"severityLevel":"SEVERE","voteCount":12}]}]}""");assertEquals("PROFANITY",ContentAdvisoriesRP31.parse(data).single().category)}
 @Test fun transportErrorNotNegativeGuide() {assertFalse(ContentGuideClientRP33.validPayload(JSONObject("""{"error":"unavailable"}""")))}
 @Test fun validEmptyGuideCanBeCached() {assertTrue(ContentGuideClientRP33.validPayload(JSONObject("""{"parentsGuide":[]}""")))}
 @Test fun invalidEnvelopeRejected() {assertFalse(ContentGuideClientRP33.validPayload(JSONObject("""{"parentsGuide":"bad"}""")))}
}
