package com.miflix.native2.data;
import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;
/** Explicit replay metadata only; a .ts extension does not imply VOD. */
public final class SportsRecordingPolicyRP72 {
 private SportsRecordingPolicyRP72(){}
 private static final Pattern RECORDED=Pattern.compile("\\b(recaps?|replays?|recordings?|highlights?|grabacion|grabaciones|repeticion|repeticiones|resumen|resumenes)\\b");
 public static boolean recording(String... fields){
  StringBuilder text=new StringBuilder();for(String field:fields)if(field!=null)text.append(' ').append(field.replaceAll("([a-z])([A-Z])","$1 $2"));
  String normalized=Normalizer.normalize(text,Normalizer.Form.NFD).replaceAll("\\p{M}","").toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+"," ");
  return RECORDED.matcher(normalized).find();
 }
}
