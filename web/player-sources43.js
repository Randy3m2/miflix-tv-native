'use strict';
(function(){
 const trigger=document.querySelector('.p-source-change'),root=document.querySelector('main'),video=document.getElementById('video');if(!trigger||!root||!video||window.parent===window)return;
 const word=(esText,enText)=>es?esText:enText;
 const panel=document.createElement('aside');panel.className='p-source-panel43';panel.hidden=true;panel.setAttribute('aria-label',word('Cambiar enlace','Change Source'));panel.setAttribute('role','region');
 const header=document.createElement('header'),heading=document.createElement('h2'),close=document.createElement('button'),status=document.createElement('p'),list=document.createElement('div');
 heading.textContent=word('Cambiar enlace','Change Source');close.textContent='×';close.type='button';close.setAttribute('aria-label',word('Cerrar enlaces','Close sources'));status.setAttribute('role','status');list.className='p-source-list43';header.append(heading,close);panel.append(header,status,list);root.append(panel);
 let requestId='',choosing=false;
 const send=body=>window.parent.postMessage({channel:params.get('channel'),requestId,...body},location.origin);
 function hide(){panel.hidden=true;trigger.setAttribute('aria-expanded','false');trigger.focus();requestId='';}
 close.onclick=hide;trigger.setAttribute('aria-expanded','false');trigger.setAttribute('aria-controls','sourcePanel43');panel.id='sourcePanel43';
 trigger.onclick=()=>{if(!panel.hidden){hide();return;}requestId=Date.now().toString(36)+'-'+Math.random().toString(36).slice(2);choosing=false;list.replaceChildren();status.textContent=word('Cargando enlaces… Puedes seguir viendo.','Loading sources… Playback continues.');panel.hidden=false;trigger.setAttribute('aria-expanded','true');close.focus();send({brunio:'player-sources'});};
 panel.onkeydown=event=>{if(event.key==='Escape'){event.preventDefault();event.stopPropagation();hide();}};
 window.addEventListener('message',event=>{const data=event.data;if(event.origin!==location.origin||event.source!==window.parent||data?.channel!==params.get('channel')||data.requestId!==requestId||panel.hidden)return;
  if(data.brunio==='player-source-ready43'){
   let address;try{address=new URL(data.address,location.href);if(address.origin!==location.origin||address.pathname!==location.pathname)return;}catch{return;}
   const fragment=new URLSearchParams(address.hash.slice(1));if(!data.live&&Number.isFinite(video.currentTime))fragment.set('resume',String(video.currentTime));address.hash=fragment.toString();address.searchParams.set('source',requestId);location.replace(address.href);return;
  }
  if(data.brunio!=='player-source-list43')return;choosing=false;
  if(data.error){status.textContent=data.error;list.querySelectorAll('button').forEach(b=>b.disabled=false);return;}
  list.replaceChildren();const sources=Array.isArray(data.sources)?data.sources:[];status.textContent=sources.length?word('Elige otro enlace para cambiar.','Choose another source to switch.'):word('No hay enlaces con el límite actual.','No sources match your current limit.');
  for(const source of sources){const b=document.createElement('button'),name=document.createElement('strong'),meta=document.createElement('small');b.type='button';name.textContent=String(source.name||'Source');meta.textContent=String(source.description||'');b.append(name,meta);b.onclick=()=>{if(choosing)return;choosing=true;list.querySelectorAll('button').forEach(x=>x.disabled=true);status.textContent=word('Preparando el enlace… La reproducción continúa.','Preparing source… Playback continues.');send({brunio:'player-source-select43',id:source.id});};list.append(b);}
 });
})();
