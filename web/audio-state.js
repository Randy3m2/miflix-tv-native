'use strict';
/* Evidence-based audio state. Absence of browser APIs means unknown, never "no audio". */
(function(root){
 function audioState(input){
  const present=input.hlsAudio===true||input.nativeCount>0||input.mozAudio===true||input.decodedBytes>0;
  const absent=!present&&input.metadata===true&&(input.hlsAudio===false||input.nativeCount===0||input.mozAudio===false);
  if(absent)return 'absent';
  if(input.muted||input.volume===0)return 'muted';
  return present?'available':'unknown';
 }
 root.BrunioAudioState=audioState;
})(typeof window==='object'?window:globalThis);
