package com.miflix.native2.data;
public final class TrailerRemotePolicy {
    private TrailerRemotePolicy() {}
    public static boolean handles(int key) {return key==19||key==20||key==21||key==22||key==23||key==66||key==85;}
    public static String action(int key,boolean down,int repeats) {
        if(!down||repeats!=0) return "";
        switch(key) {case 19:return "details";case 20:return "watch";case 21:return "previous";case 22:return "next";case 23:case 66:case 85:return "pause";default:return "";}
    }
}
