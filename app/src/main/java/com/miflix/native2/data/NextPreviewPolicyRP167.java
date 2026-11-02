package com.miflix.native2.data;
public final class NextPreviewPolicyRP167 {
 private NextPreviewPolicyRP167() {}
 public static boolean visible(long position,long duration,long creditsStart,boolean ended) {
  if(ended || duration<180000 || position<0 || position>=duration)return false;
  long start=duration-90000;
  if(creditsStart>=duration/2 && creditsStart<duration)start=Math.max(duration-180000,creditsStart);
  return position>=start;
 }
 public static long secondsRemaining(long position,long duration){return Math.max(0,(duration-position+999)/1000);}
}
