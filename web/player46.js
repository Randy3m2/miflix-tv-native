'use strict';
(()=>{
 const video=document.getElementById('video');if(!video)return;
 const send=brunio=>{if(window.parent!==window)window.parent.postMessage({brunio,channel:params.get('channel')},location.origin);};
 let started=false,failed=false,timer;
 function fail(){if(started||failed)return;failed=true;clearTimeout(timer);send('player-failed46');}
 timer=setTimeout(fail,25000);
 video.addEventListener('error',fail);
 video.addEventListener('playing',()=>{clearTimeout(timer);started=true;send('player-started46');});
 const statusNode=document.getElementById('status');
 const observer=new MutationObserver(()=>{const text=statusNode.textContent||'';if(/Tap.*Play|Pulsa Play.*comenzar/i.test(text)){clearTimeout(timer);return;}if(/Source failed|fuente falló|Could not start|No se pudo iniciar|could not play|no pudo reproducir/i.test(text))fail();});observer.observe(statusNode,{childList:true,subtree:true,characterData:true});if(/Tap.*Play|Pulsa Play.*comenzar/i.test(statusNode.textContent||''))clearTimeout(timer);if(!video.paused&&video.readyState>=2){clearTimeout(timer);started=true;send('player-started46');}else if(video.error)fail();
 document.querySelector('.p-source-change')?.addEventListener('click',()=>send('player-manual46'));
 if(!liveMode&&media){const button=document.createElement('a');button.className='p-icon p-download46';button.href=media;button.download=sourceName||'';button.target='_blank';button.rel='noopener noreferrer';button.referrerPolicy='no-referrer';button.title=es?'Descargar':'Download';button.setAttribute('aria-label',button.title);button.innerHTML='<svg viewBox="0 0 24 24" width="24" height="24" aria-hidden="true"><path d="M12 3v12m-5-5 5 5 5-5M4 17v4h16v-4" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/></svg>';document.querySelector('.p-right')?.prepend(button);}
 video.addEventListener('ended',()=>send('player-ended46'));
 window.addEventListener('pagehide',()=>{clearTimeout(timer);observer.disconnect();});
})();
