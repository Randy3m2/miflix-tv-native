package com.miflix.native2.data
import org.junit.Assert.*
import org.junit.Test
import org.json.JSONArray
import org.json.JSONObject
class NuvioImportRP49Test {
    private fun source(id:String,manifest:String=NuvioTestConfigRP49.MANIFEST)=JSONObject().put("manifestUrl",manifest).put("catalogId",id).put("catalogName","Today").put("type","tv")
    private fun document(sources:JSONArray)=JSONArray().put(JSONObject().put("folders",JSONArray().put(JSONObject().put("id","baseball").put("title","Baseball").put("coverImageUrl","https://example.com/baseball.jpg").put("catalogSources",sources)))).toString()
    @Test fun importsOriginalIdsAndDeduplicatesCatalogs() {
        val folders=NuvioTestConfigRP49.parse(document(JSONArray().put(source("original-id")).put(source("original-id"))))
        assertEquals(1,folders.size);assertEquals("Baseball",folders.single().title)
        assertEquals("original-id",folders.single().catalogs.single().id)
        assertEquals("tv",folders.single().catalogs.single().type)
        assertTrue(folders.single().catalogs.single().paginated)
    }
    @Test fun ignoresUnexpectedManifestsAndEmptyCatalogIds() {
        assertTrue(NuvioTestConfigRP49.parse(document(JSONArray().put(source("foreign","https://other.example/manifest.json")).put(source("")))).isEmpty())
    }
}
