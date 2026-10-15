package com.miflix.native2.data;

/** Metadata eligibility for shuffle: exclude specials, unconfirmed dates and future episodes. */
public final class RandomEpisodePolicy {
    private RandomEpisodePolicy(){}
    public static boolean aired(String date,String today){return date!=null && date.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}") && date.compareTo(today)<=0;}
    public static boolean season(int number,int count,String date,String today){return number>0 && count>0 && aired(date,today);}
    public static int indexForDraw(int[] counts,int draw){
        if(draw<0)throw new IllegalArgumentException("Negative draw");
        for(int i=0;i<counts.length;i++){int count=Math.max(0,counts[i]);if(draw<count)return i;draw-=count;}
        throw new IllegalArgumentException("Draw exceeds episode total");
    }
}
