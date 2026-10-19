package com.miflix.native2.data

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class DetailsAndAvatarRP18Test {
    @Test fun directorCardsUseOnlyDirectorJobsAndDeduplicatePeople() {
        val credits=JSONObject("""{"crew":[{"id":1,"name":"Director One","job":"Director","profile_path":"/a.jpg"},{"id":2,"name":"Producer","job":"Producer"},{"id":1,"name":"Director One","job":"Director"},{"id":3,"name":"Director Two","job":"Director"}]}""")
        val people=DetailPeopleRP18.directors(credits) {it.takeIf {p ->p.isNotBlank()}?.let {p ->"https://images.test$p"}}
        assertEquals(listOf(1,3),people.map {it.id})
        assertEquals("https://images.test/a.jpg",people.first().profile)
        assertTrue(people.all {it.creditRole=="director" && it.character=="Director"})
    }
    @Test fun missingOrInvalidCreditsDoNotInventADirector() {
        assertTrue(DetailPeopleRP18.directors(null) {null}.isEmpty())
        assertTrue(DetailPeopleRP18.directors(JSONObject("""{"crew":[{"id":0,"name":"Bad","job":"Director"},{"id":3,"name":"Creator","job":"Creator"}]}""")) {null}.isEmpty())
    }
    @Test fun missingAvatarBucketGetsSpecificSqlInstruction() {
        val error=AvatarUploadFailureRP18.message(400,"""{"message":"Bucket not found"}""")
        assertTrue(error.contains("bucket is missing"));assertTrue(error.contains("supabase_rc18_upgrade.sql"))
    }
    @Test fun avatarPermissionsAndSessionHaveDifferentMessages() {
        assertTrue(AvatarUploadFailureRP18.message(400,"""{"message":"new row violates row-level security policy"}""").contains("permissions"))
        assertTrue(AvatarUploadFailureRP18.message(401,"{}").contains("Sign in again"))
    }
    @Test fun avatarSizeAndMimeRejectionsAreDistinct() {
        assertTrue(AvatarUploadFailureRP18.message(413,"{}").contains("512000"))
        assertTrue(AvatarUploadFailureRP18.message(400,"""{"message":"mime type not supported"}""").contains("image/jpeg"))
    }
    @Test fun genericErrorDoesNotExposePrivateResponseOrClaimMissingSql() {
        val error=AvatarUploadFailureRP18.message(400,"""{"message":"private https://server.test/path?token=secret"}""")
        assertFalse(error.contains("secret"));assertFalse(error.contains("https://"));assertFalse(error.contains("Run supabase"))
    }
}
