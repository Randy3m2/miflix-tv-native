package com.miflix.native2.data;

public final class KidsContentPolicy {
    public static final String PROFILE_ID="brunio-kids";
    private KidsContentPolicy() {}
    public static boolean allows(boolean adult,boolean series,int[] genres,String[] ratings) {
        if(adult || genres==null || ratings==null || ratings.length==0) return false;
        boolean family=false;
        for(int genre: genres) if(genre==16 || genre==10751 || (series && genre==10762)) family=true;
        if(!family) return false;
        for(String rating: ratings) {
            if(series) {
                if(!"TV-Y".equals(rating) && !"TV-Y7".equals(rating) && !"TV-Y7-FV".equals(rating) && !"TV-G".equals(rating)) return false;
            } else if(!"G".equals(rating) && !"PG".equals(rating)) return false;
        }
        return true;
    }
}
