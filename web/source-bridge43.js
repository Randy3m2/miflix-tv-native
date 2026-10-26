'use strict';
// Keep the in-player picker independent of features46.js and its cache version.
const playerSourcePolicy51='torrentio-comet-v53';
function playerFactorySources51(result){
 if(result?.sourcePolicy!==playerSourcePolicy51)throw Error(language==='en'?'Update the brunio-downloads function in Supabase. The server has not confirmed Torrentio and Comet yet.':'Actualiza la función brunio-downloads en Supabase. El servidor aún no confirmó Torrentio y Comet.');
 return (result.sources||[]).filter(s=>['torrentio','comet'].includes(s.providerId)).sort((a,b)=>Number(a.providerId==='torrentio')-Number(b.providerId==='torrentio'));
}
function sourceCurrent43(current,picker){return activeWebPlayer===current&&current.picker43===picker&&session?.user.id===current.owner&&selected()===current.profile;}
function sourceReply43(current,picker,body){if(sourceCurrent43(current,picker))current.frame.contentWindow.postMessage({brunio:'player-source-list43',channel:current.channel,requestId:picker.id,...body},location.origin);}
async function openPlayerSources43(current,data){
 if(current.auto46){current.auto46=null;++sourceRun46;}const picker={id:String(data.requestId||'').slice(0,100),sources:[],maxGb:downloadMaxGb,busy:false};current.picker43=picker;
 try{
  if(current.target?.live){const channel=current.target.liveChannel;if(!channel)throw Error(language==='en'?'No channel sources available.':'No hay fuentes disponibles para este canal.');
   let streams=channel.streams;if(!streams){const base=liveManifest().replace(/\/manifest\.json(?:\?.*)?$/,'');streams=(await liveFetch(base+'/stream/'+encodeURIComponent(channel.type||'tv')+'/'+encodeURIComponent(channel.id)+'.json',true,30000)).streams||[];}
   picker.sources=streams.slice(0,100).flatMap((s,i)=>{try{return [{id:String(i),name:String(s.name||s.title||'Source '+(i+1)).slice(0,180),url:publicLiveURL(liveCastSource(s)).href}];}catch{return [];}});
  }else{
   const target=current.target;if(!target?.cloudId)throw Error(language==='en'?'No title selected.':'No hay un título seleccionado.');
   const result=await api('/functions/v1/brunio-downloads','POST',{action:'sources',sourcePolicy:playerSourcePolicy51,maxGb:picker.maxGb,refresh:false,cloud:target.cloudId,profile:current.profile,season:target.season||1,episode:target.episode||1,language});
   picker.sources=playerFactorySources51(result).slice(0,100).map((s,i)=>({...s,id:String(i)}));
  }
  sourceReply43(current,picker,{sources:picker.sources.map(s=>({id:s.id,name:String(s.name||'Source').slice(0,180),description:[String(s.provider||''),s.size>0?(s.size/1e9).toFixed(2)+' GB':'',''].filter(Boolean).join(' · ')}))});
 }catch(e){sourceReply43(current,picker,{error:e.message});}
}
async function selectPlayerSource43(current,data){
 const picker=current.picker43;if(!picker||picker.id!==data.requestId||!sourceCurrent43(current,picker)||picker.busy)return;
 const source=picker.sources.find(s=>s.id===String(data.id));if(!source)return;picker.busy=true;
 try{
  const result=current.target.live?{url:source.url,filename:''}:await sourceDeadline44(api('/functions/v1/brunio-downloads','POST',{action:'link',maxGb:picker.maxGb,ticket:source.ticket,profile:current.profile,language}));
  if(!sourceCurrent43(current,picker))return;
  // Reuse exactly the existing media URL validation; never navigate to provider pages.
  const target={...current.target,sourceFilename:result.filename||source.filename||''};
  const address=webPlayerAddress(result.url,target.title||target.liveChannel?.name,target.backdrop||target.poster,{channel:current.channel,target});
  current.target=target;current.frame.contentWindow.postMessage({brunio:'player-source-ready43',channel:current.channel,requestId:picker.id,address,live:!!target.live},location.origin);
 }catch(e){sourceReply43(current,picker,{error:e.message});}finally{picker.busy=false;}
}

async function sourceDeadline44(promise){let timer;try{return await Promise.race([promise,new Promise((_,reject)=>{timer=setTimeout(()=>reject(Error(language==='en'?'This source took too long. Try another link.':'Este enlace tardó demasiado. Prueba otro.')),45000);})]);}finally{clearTimeout(timer);}}
