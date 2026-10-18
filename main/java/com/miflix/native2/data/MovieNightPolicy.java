package com.miflix.native2.data;
public final class MovieNightPolicy {
    private MovieNightPolicy() {}
    public static boolean allowed(int minutes,int cap,int[] genres,int[] vetoes,boolean watched,boolean excludeWatched) {
        if(minutes<=0 || minutes>cap || watched&&excludeWatched) return false;
        for(int genre:genres) for(int veto:vetoes) if(genre==veto) return false;
        return true;
    }
    public static int[] tieIndices(int[] votes,boolean[] excluded) {
        if(votes.length!=excluded.length) throw new IllegalArgumentException();
        int max=-1,count=0;
        for(int i=0;i<votes.length;i++) if(!excluded[i]) max=Math.max(max,votes[i]);
        for(int i=0;i<votes.length;i++) if(!excluded[i]&&votes[i]==max) count++;
        int[] rows=new int[count];int n=0;
        for(int i=0;i<votes.length;i++) if(!excluded[i]&&votes[i]==max) rows[n++]=i;
        return rows;
    }
}
