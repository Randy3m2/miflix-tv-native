package com.miflix.native2.data;

/** Local format bounds only; Google validates whether the credential actually works. */
public final class GeminiKeyInputRP153 {
    public static final int MAX_LENGTH=2048;
    private GeminiKeyInputRP153() {}
    public static String normalize(String value) {
        String key=value==null?"":value.trim();
        if(key.length()<20 || key.length()>MAX_LENGTH || !key.matches("[A-Za-z0-9._~+/=\\-]+"))
            throw new IllegalArgumentException("Invalid key format");
        return key;
    }
}
