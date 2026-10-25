package com.miflix.native2.data;
import org.junit.Test;
import static org.junit.Assert.*;

public class AddonManifestRP126Test {
    private final String url="https://addon.example/private/config/manifest.json";
    private String json(String extra) {return "{\"id\":\"example\",\"name\":\"Example\",\"resources\":[\"stream\"]"+extra+"}";}
    @Test public void normalizesInstallProtocol() {assertEquals(url,AddonManifestRP126.normalize("  stremio://addon.example/private/config/manifest.json "));}
    @Test public void normalizesBaseUrlAndPreservesConfiguredPath() {assertEquals(url,AddonManifestRP126.normalize("https://addon.example/private/config/"));}
    @Test public void acceptsObjectResourceAndRelativeLogo() {
        AddonManifestRP126 m=AddonManifestRP126.parse(url,"{\"id\":\"x\",\"name\":\"Provider\",\"resources\":[{\"name\":\"stream\",\"types\":[\"movie\",\"series\"]}],\"logo\":\"./logo.png\"}");
        assertEquals("Provider",m.name);assertEquals("https://addon.example/private/config/logo.png",m.logo);
    }
    @Test public void missingLogoUsesEmptyFallback() {assertEquals("",AddonManifestRP126.parse(url,json("")).logo);}
    @Test public void unsafeLogoDoesNotDiscardValidProvider() {assertEquals("",AddonManifestRP126.parse(url,json(",\"logo\":\"javascript:bad\"")).logo);}
    @Test(expected=IllegalArgumentException.class) public void rejectsWebsiteHtml() {AddonManifestRP126.parse(url,"<html>not a manifest</html>");}
    @Test(expected=IllegalArgumentException.class) public void rejectsCatalogOnlyAddon() {AddonManifestRP126.parse(url,"{\"id\":\"x\",\"name\":\"Catalog\",\"resources\":[\"catalog\"]}");}
    @Test(expected=IllegalArgumentException.class) public void rejectsUnconfiguredAddon() {AddonManifestRP126.parse(url,json(",\"behaviorHints\":{\"configurationRequired\":true}"));}
    @Test(expected=IllegalArgumentException.class) public void rejectsCredentialsInHost() {AddonManifestRP126.normalize("https://user:password@addon.example/manifest.json");}
    @Test(expected=IllegalArgumentException.class) public void rejectsUnsupportedQueryInsteadOfInstallingWrongRoute() {AddonManifestRP126.normalize(url+"?token=secret");}
    @Test(expected=IllegalArgumentException.class) public void rejectsArbitraryJson() {AddonManifestRP126.normalize("https://example.com/list.json");}
    @Test public void boundsDisplayText() {assertEquals(80,AddonManifestRP126.parse(url,"{\"id\":\"x\",\"name\":\""+"a".repeat(200)+"\",\"resources\":[\"stream\"]}").name.length());}
}
