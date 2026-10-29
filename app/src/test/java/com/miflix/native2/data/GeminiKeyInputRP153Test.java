package com.miflix.native2.data;
import org.junit.Test;
import static org.junit.Assert.*;

public class GeminiKeyInputRP153Test {
    @Test public void supportsLegacyAndNewAuthFormatsWithoutChangingThem() {
        for(String key:new String[]{"AIza"+"test_only_".repeat(4),"AQ."+"fixture_not_a_credential-".repeat(35)+"._~+/="}) {
            assertEquals(key,GeminiKeyInputRP153.normalize(key));
            assertEquals(key,GeminiKeyInputRP153.normalize(" \n"+key+"\r\n"));
        }
    }
    @Test public void malformedInputIsRejectedWithoutEchoingTheValue() {
        for(String input:new String[]{"", "short", "AQ."+"a".repeat(2046),"AQ.test_only_with space_123456", "AQ.test_only_\r\nheader_123456", "\"AQ.test_only_not_real_123456\""}) {
            try {GeminiKeyInputRP153.normalize(input);fail("Expected rejection");}
            catch(IllegalArgumentException e) {assertEquals("Invalid key format",e.getMessage());}
        }
    }
}
