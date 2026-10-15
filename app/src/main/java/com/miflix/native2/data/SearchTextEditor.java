package com.miflix.native2.data;

/** Cursor editing never splits UTF-16 surrogate pairs (voice input may contain emoji). */
public final class SearchTextEditor {
    private SearchTextEditor() {}
    public static final class Edit {
        public final String text;
        public final int cursor;
        public Edit(String text,int cursor) {this.text=text;this.cursor=cursor;}
    }
    private static int position(String s,int index) {
        int p=Math.max(0,Math.min(s.length(),index));
        if(p>0 && p<s.length() && Character.isLowSurrogate(s.charAt(p)) && Character.isHighSurrogate(s.charAt(p-1))) p--;
        return p;
    }
    public static Edit replace(String value) {
        String s=value==null?"":value;
        if(s.codePointCount(0,s.length())>100) s=s.substring(0,s.offsetByCodePoints(0,100));
        return new Edit(s,s.length());
    }
    public static Edit insert(String text,int cursor,String addition) {
        int p=position(text,cursor);
        int room=100-text.codePointCount(0,text.length());
        String add=addition==null?"":addition;
        if(add.codePointCount(0,add.length())>room) add=add.substring(0,add.offsetByCodePoints(0,Math.max(0,room)));
        return new Edit(text.substring(0,p)+add+text.substring(p),p+add.length());
    }
    public static Edit backspace(String text,int cursor) {
        int p=position(text,cursor);if(p==0) return new Edit(text,p);
        int left=text.offsetByCodePoints(p,-1);
        return new Edit(text.substring(0,left)+text.substring(p),left);
    }
    public static int move(String text,int cursor,int direction) {
        int p=position(text,cursor);
        if(direction<0 && p>0) return text.offsetByCodePoints(p,-1);
        if(direction>0 && p<text.length()) return text.offsetByCodePoints(p,1);
        return p;
    }
}
