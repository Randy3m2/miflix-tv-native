import {homeShelves} from './home.ts';
const cors={'Access-Control-Allow-Origin':'*','Access-Control-Allow-Headers':'authorization,apikey,content-type','Access-Control-Allow-Methods':'POST,OPTIONS'};
const reply=(value:unknown,status=200)=>new Response(JSON.stringify(value),{status,headers:{...cors,'Content-Type':'application/json'}});
const cache=new Map<string,{at:number;data:any}>();
async function catalog(path:string,params:Record<string,string>={}) {
 const token=Deno.env.get('TMDB_TOKEN');if(!token)throw Error('Configure TMDB_TOKEN');
 const u=new URL(`https://api.themoviedb.org/3${path}`);for(const [k,v] of Object.entries(params))u.searchParams.set(k,v);
 if(!token.startsWith('eyJ'))u.searchParams.set('api_key',token);
 const key=path+JSON.stringify(params),old=cache.get(key);if(old&&Date.now()-old.at<300000)return old.data;
 const r=await fetch(u,{headers:token.startsWith('eyJ')?{Authorization:`Bearer ${token}`}:{},signal:AbortSignal.timeout(15000)});
 if(!r.ok)throw Error(`Catalog HTTP ${r.status}`);const data=await r.json();
 if(cache.size>=500)cache.delete(cache.keys().next().value!);cache.set(key,{at:Date.now(),data});return data;
}
const platforms:Record<string,{name:string;ids:number[]}>= {
 netflix:{name:'Netflix',ids:[8]},apple:{name:'Apple TV',ids:[350]},prime:{name:'Prime Video',ids:[9,119]},max:{name:'HBO Max',ids:[1899,384]},disney:{name:'Disney+',ids:[337]},crunchyroll:{name:'Crunchyroll',ids:[283]}
};
function media(x:any,type?:string,language='es') {
 const kind=type||(x.media_type==='tv'?'series':x.media_type==='movie'?'movie':null);if(!kind||x.adult)return null;
 return {id:x.id,type:kind,cloudId:`tmdb:${kind}:${x.id}`,title:x.title||x.name||(language==='en'?'Untitled':'Sin título'),overview:x.overview||'',
 backdrop:x.backdrop_path?`https://image.tmdb.org/t/p/w1280${x.backdrop_path}`:null,runtime:x.runtime||x.episode_run_time?.[0]||null,
 poster:x.poster_path?`https://image.tmdb.org/t/p/w500${x.poster_path}`:null,year:(x.release_date||x.first_air_date||'').slice(0,4),date:x.release_date||x.first_air_date||'',rating:x.vote_average||0,genres:x.genre_ids||(x.genres||[]).map((g:any)=>g.id)};
}
// Subtitle lookup/files use fixed public providers, authenticated access and bounded memory caches.
const subtitleCache=new Map<string,{at:number;data:any}>();
const subtitlePending=new Map<string,Promise<any>>();
function subtitleURL(raw:string) {
 const u=new URL(raw);if(u.protocol==='http:')u.protocol='https:';
 const allowed=['opensubtitles.org','opensubtitles.com'].some(h=>u.hostname===h||u.hostname.endsWith('.'+h))||['opensubtitles-v3.strem.io','opensubtitles.strem.io'].includes(u.hostname);
 if(u.protocol!=='https:'||u.username||u.password||(u.port&&u.port!=='443')||!allowed)throw Error('Unsupported subtitle host');return u;
}
async function subtitleBytes(url:URL,limit:number):Promise<Uint8Array> {
 let current=url;
 for(let hop=0;hop<4;hop++){
  const r=await fetch(current,{redirect:'manual',signal:AbortSignal.timeout(7000)});
  if([301,302,303,307,308].includes(r.status)){const location=r.headers.get('location');await r.body?.cancel();if(!location)throw Error('Missing redirect');current=subtitleURL(new URL(location,current).href);continue;}
  if(!r.ok){await r.body?.cancel();throw Error('Subtitle provider unavailable');}
  if(Number(r.headers.get('content-length'))>limit){await r.body?.cancel();throw Error('Subtitle too large');}
  const reader=r.body?.getReader();if(!reader)throw Error('Empty subtitle');let size=0;const chunks:Uint8Array[]=[];
  while(true){const {done,value}=await reader.read();if(done)break;size+=value.byteLength;if(size>limit){await reader.cancel();throw Error('Subtitle too large');}chunks.push(value);}
  const bytes=new Uint8Array(size);let offset=0;for(const part of chunks){bytes.set(part,offset);offset+=part.length;}return bytes;
 }
 throw Error('Too many subtitle redirects');
}
function webSubtitleVtt(text:string) {
 text=text.replace(/^\uFEFF/,'').replace(/\r\n?/g,'\n').trim();
 if(!/^WEBVTT(?:[ \t]|\n|$)/.test(text))text='WEBVTT\n\n'+text.replace(/^(\d+:\d{2}:\d{2}),(\d{3})([ \t]+-->[ \t]+)(\d+:\d{2}:\d{2}),(\d{3})/gm,'$1.$2$3$4.$5');
 if(!/\d{2}:\d{2}\.\d{3}[ \t]+-->[ \t]+/.test(text))throw Error('Unsupported subtitle format');return text;
}
function subtitleLanguage(raw:string,label='') {
 const value=raw.toLowerCase().replace(/_/g,'-');
 if(/^(es-419|es-la|es-mx|spl|spn)$/.test(value)||/latino|latam|latin american/i.test(label))return 'es-419';
 if(/^(spa|es|es-es|spanish|español)$/.test(value))return 'es';
 if(/^(eng|en|en-us|en-gb|english)$/.test(value))return 'en';return '';
}
async function preloadSubtitles(input:any,language:string) {
 if(!/^tmdb:(movie|series):[1-9][0-9]{0,8}$/.test(input.cloud||''))throw Error('Invalid title');
 const [,kind,id]=input.cloud.split(':'),season=kind==='series'?Number(input.season):0,episode=kind==='series'?Number(input.episode):0;
 if(kind==='series'&&(!Number.isSafeInteger(season)||season<0||season>10000||!Number.isSafeInteger(episode)||episode<1||episode>10000))throw Error('Invalid episode');
 const key=[input.cloud,season,episode,language].join(':'),cached=subtitleCache.get(key);if(cached&&Date.now()-cached.at<(cached.data.tracks?.length?1800000:60000))return cached.data;
 if(subtitlePending.has(key))return subtitlePending.get(key);
 const work=(async()=>{
  const external=await catalog(`/${kind==='series'?'tv':'movie'}/${id}/external_ids`),imdb=external.imdb_id;
  if(!/^tt[0-9]{5,12}$/.test(imdb||''))return {tracks:[]};
  const video=kind==='series'?`${imdb}:${season}:${episode}`:imdb;
  async function lookup(base:string){try{const bytes=await subtitleBytes(subtitleURL(`${base}/subtitles/${kind}/${video}/*.json`),500000);const data=JSON.parse(new TextDecoder().decode(bytes));return (Array.isArray(data.subtitles)?data.subtitles:[]).slice(0,200).flatMap((row:any)=>{try{const label=String(row.label||row.lang||'').slice(0,120),lang=subtitleLanguage(String(row.lang||''),label);return lang?[{url:subtitleURL(String(row.url)).href,lang,label}]:[];}catch{return [];}});}catch{return [];}}
  let rows=await lookup('https://opensubtitles-v3.strem.io');
  if(!rows.some((r:any)=>r.lang.startsWith('es'))||!rows.some((r:any)=>r.lang==='en'))rows.push(...await lookup('https://opensubtitles.strem.io/stremio/v1'));
  rows=[...new Map(rows.map((r:any)=>[r.url,r])).values()];
  rows.sort((a:any,b:any)=>{const rank=(r:any)=>language==='en'?(r.lang==='en'?0:r.lang==='es-419'?1:2):(r.lang==='es-419'?0:r.lang==='es'?1:2);return rank(a)-rank(b);});
  const chosen=[...rows.filter((r:any)=>language==='en'?r.lang==='en':r.lang.startsWith('es')).slice(0,2),...rows.filter((r:any)=>language==='en'?r.lang.startsWith('es'):r.lang==='en').slice(0,1)];
  const tracks=(await Promise.all(chosen.map(async(row:any,index:number)=>{try{
   const bytes=await subtitleBytes(subtitleURL(row.url),500000);let text:string;
   if(bytes[0]===0x1f&&bytes[1]===0x8b){const stream=new Blob([bytes.buffer as ArrayBuffer]).stream().pipeThrough(new DecompressionStream('gzip'));const reader=stream.getReader();let size=0;const chunks:Uint8Array[]=[];while(true){const part=await reader.read();if(part.done)break;size+=part.value.length;if(size>500000){await reader.cancel();throw Error('Subtitle too large');}chunks.push(part.value);}const all=new Uint8Array(size);let at=0;for(const chunk of chunks){all.set(chunk,at);at+=chunk.length;}text=new TextDecoder().decode(all);}else text=new TextDecoder().decode(bytes);
   return {id:'online-'+index,lang:row.lang,label:(row.lang==='es-419'?'Español latino':row.lang==='es'?'Español':'English')+' · '+(index+1),vtt:webSubtitleVtt(text)};
  }catch{return null;}}))).filter(Boolean);
  const data={tracks};if(subtitleCache.size>=16)subtitleCache.delete(subtitleCache.keys().next().value!);subtitleCache.set(key,{at:Date.now(),data});return data;
 })();subtitlePending.set(key,work);try{return await work;}finally{subtitlePending.delete(key);}
}

Deno.serve(async(request:Request)=>{
 if(request.method==='OPTIONS')return new Response('ok',{headers:cors});if(request.method!=='POST')return reply({error:'POST required'},405);
 let language='es';
 try {
  const authorization=request.headers.get('Authorization')||'';if(!authorization.startsWith('Bearer '))return reply({error:'Sign in required'},401);
  const auth=await fetch(`${Deno.env.get('SUPABASE_URL')}/auth/v1/user`,{headers:{apikey:Deno.env.get('SUPABASE_ANON_KEY')!,Authorization:authorization},signal:AbortSignal.timeout(12000)});if(!auth.ok)return reply({error:'Session expired'},401);
  const input=await request.json();language=input.language==='en'?'en':'es';const lang={language:language==='en'?'en-US':'es-ES'};
  const region=typeof input.region==='string'&&/^[A-Z]{2}$/.test(input.region)?input.region:'US';
  if(input.action==='subtitles'){if(!/^tmdb:(movie|series):[1-9][0-9]{0,8}$/.test(input.cloud||''))return reply({error:'Invalid title'},400);if(input.cloud.startsWith('tmdb:series:')&&(!Number.isSafeInteger(Number(input.season))||Number(input.season)<0||Number(input.season)>10000||!Number.isSafeInteger(Number(input.episode))||Number(input.episode)<1||Number(input.episode)>10000))return reply({error:'Invalid episode'},400);return reply(await preloadSubtitles(input,language));}
  if(input.action==='home')return reply(await homeShelves(input,language,catalog,media,platforms));
  if(input.action==='config') {
   const [m,t,p,pt]=await Promise.all([catalog('/genre/movie/list',lang),catalog('/genre/tv/list',lang),catalog('/watch/providers/movie',lang),catalog('/watch/providers/tv',lang)]);
   const providers=Object.entries(platforms).map(([key,v])=>{const provider=[...(p.results||[]),...(pt.results||[])].find((x:any)=>v.ids.includes(x.provider_id));return {key,name:v.name,logo:provider?.logo_path?`https://image.tmdb.org/t/p/w185${provider.logo_path}`:null};});
   return reply({genres:{movie:m.genres,series:t.genres},providers});
  }
  if(['details','episodes','filmography','randomEpisode'].includes(input.action)) {
   if(input.action==='filmography') {
    const person=Number(input.person),page=Number(input.page||1);if(!Number.isSafeInteger(person)||person<1||!Number.isSafeInteger(page)||page<1||page>1000)return reply({error:'Invalid person/page'},400);
    const data=await catalog(`/person/${person}/combined_credits`,lang);
    const rows=[...new Map((data.cast||[]).filter((x:any)=>!x.adult&&['movie','tv'].includes(x.media_type)).map((x:any)=>[x.media_type+':'+x.id,x])).values()] as any[];
    rows.sort((a,b)=>(b.popularity||0)-(a.popularity||0));return reply({items:rows.slice((page-1)*25,page*25).map(x=>media(x,undefined,language)).filter(Boolean),hasMore:rows.length>page*25});
   }
   if(!/^tmdb:(movie|series):[1-9][0-9]{0,8}$/.test(input.cloud||''))return reply({error:'Invalid title'},400);
   const [,kind,id]=input.cloud.split(':'),path=`/${kind==='series'?'tv':'movie'}/${id}`;
   if(input.action==='randomEpisode') {
    if(kind!=='series')return reply({error:'Series required'},400);
    const show=await catalog(path,lang),today=new Date().toISOString().slice(0,10),seasons=(show.seasons||[]).filter((x:any)=>x.season_number>0&&x.episode_count>0&&x.air_date&&x.air_date<=today);
    while(seasons.length){const total=seasons.reduce((n:number,x:any)=>n+x.episode_count,0);let draw=Math.random()*total,index=0;for(;index<seasons.length-1;index++){draw-=seasons[index].episode_count;if(draw<0)break;}const season=seasons.splice(index,1)[0];const data=await catalog(`${path}/season/${season.season_number}`,lang),episodes=(data.episodes||[]).filter((x:any)=>x.air_date&&x.air_date<=today);if(episodes.length){const others=episodes.filter((x:any)=>x.season_number!==Number(input.excludeSeason)||x.episode_number!==Number(input.excludeEpisode)),pool=others.length?others:episodes;const ep=pool[Math.floor(Math.random()*pool.length)];return reply({episode:{season:ep.season_number,episode:ep.episode_number}});}}
    return reply({episode:null});
   }
   if(input.action==='episodes') {
    const season=Number(input.season),page=Number(input.page||1);if(kind!=='series'||!Number.isInteger(season)||season<0||season>10000||!Number.isInteger(page)||page<1||page>1000)return reply({error:'Invalid season/page'},400);
    const data=await catalog(`${path}/season/${season}`,lang),episodes=(data.episodes||[]).map((x:any)=>({id:x.id,season:x.season_number,episode:x.episode_number,title:x.name,overview:x.overview||'',date:x.air_date||'',runtime:x.runtime||0,still:x.still_path?`https://image.tmdb.org/t/p/w500${x.still_path}`:null}));
    return reply({episodes:episodes.slice((page-1)*25,page*25),hasMore:episodes.length>page*25});
   }
   const data=await catalog(path,{...lang,append_to_response:'credits,recommendations,images',include_image_language:language+',en,null'}),item=media(data,kind,language);
   if(!item)return reply({error:'Title unavailable'},404);
   return reply({item:{...item,full:true,tagline:data.tagline||'',status:data.status||'',logo:data.images?.logos?.[0]?.file_path?`https://image.tmdb.org/t/p/w500${data.images.logos[0].file_path}`:null,
    cast:(data.credits?.cast||[]).slice(0,60).map((x:any)=>({id:x.id,name:x.name,character:x.character||'',photo:x.profile_path?`https://image.tmdb.org/t/p/w185${x.profile_path}`:null})),
    seasons:(data.seasons||[]).filter((x:any)=>x.season_number>0).map((x:any)=>({number:x.season_number,name:x.name,count:x.episode_count,date:x.air_date||''})),
    related:(data.recommendations?.results||[]).slice(0,25).map((x:any)=>media(x,kind,language)).filter(Boolean)}});
  }
  if(input.action==='people') {const q=String(input.query||'').trim().slice(0,150);if(q.length<2)return reply({people:[]});const data=await catalog('/search/person',{...lang,query:q,include_adult:'false'});return reply({people:(data.results||[]).filter((x:any)=>!x.adult).slice(0,8).map((x:any)=>({id:x.id,name:x.name,department:x.known_for_department}))});}
  if(input.action==='titles') {
   if(!Array.isArray(input.ids)||input.ids.length>25||input.ids.some((id:unknown)=>typeof id!=='string'||!/^tmdb:(movie|series):\d+$/.test(id)))return reply({error:'Invalid title batch'},400);
   const items=await Promise.all(input.ids.map(async(id:string)=>{const [,kind,n]=id.split(':');try{return media(await catalog(`/${kind==='series'?'tv':'movie'}/${n}`,lang),kind,language);}catch{return {cloudId:id,id:Number(n),type:kind,title:language==='en'?'Title unavailable':'Título no disponible',poster:null,overview:'',year:'',rating:0,genres:[]};}}));return reply({items:items.filter(Boolean)});
  }
  if(!['search','discover','upcoming'].includes(input.action))return reply({error:'Invalid action'},400);
  const page=Number(input.page||1);if(!Number.isInteger(page)||page<1||page>500)return reply({error:'Invalid page'},400);
  const type=input.type==='movie'?'movie':input.type==='series'?'series':'all';
  const year=input.year?Number(input.year):0,genre=input.genre?Number(input.genre):0,person=input.person?Number(input.person):0;
  if((input.year&&(!Number.isInteger(year)||year<1880||year>new Date().getUTCFullYear()+10))||[genre,person].some(x=>!Number.isInteger(x)||x<0))return reply({error:'Invalid filters'},400);
  if(input.provider&&!platforms[input.provider])return reply({error:'Invalid provider'},400);
  const provider=input.provider?platforms[input.provider]:null;
  const q=typeof input.query==='string'?input.query.trim().slice(0,150):'';
  const sort=['popularity.desc','vote_average.desc','date.desc'].includes(input.sort)?input.sort:'popularity.desc';
  const today=new Date().toISOString().slice(0,10);let credits:any[]=[];
  if(person){const c=await catalog(`/person/${person}/combined_credits`,lang);credits=[...(c.cast||[]),...(c.crew||[])];}
  const matches=(x:any)=>{const kind=x.media_type==='tv'?'series':'movie',date=x.release_date||x.first_air_date||'';return !x.adult&&['movie','tv'].includes(x.media_type)&&(type==='all'||kind===type)&&(!genre||x.genre_ids?.includes(genre))&&(!year||date.slice(0,4)===String(year))&&(input.action!=='upcoming'||date>today)&&(!person||credits.some(c=>c.id===x.id&&c.media_type===x.media_type));};
  if(person&&!provider&&!q){const unique=[...new Map(credits.filter(matches).map(x=>[x.media_type+':'+x.id,x])).values()];unique.sort((a:any,b:any)=>input.action==='upcoming'?(a.release_date||a.first_air_date||'').localeCompare(b.release_date||b.first_air_date||''):sort==='date.desc'?(b.release_date||b.first_air_date||'').localeCompare(a.release_date||a.first_air_date||''):sort==='vote_average.desc'?(b.vote_average||0)-(a.vote_average||0):(b.popularity||0)-(a.popularity||0));return reply({items:unique.slice((page-1)*20,page*20).map(x=>media(x,undefined,language)),hasMore:unique.length>page*20,nextPage:page+1});}
  let raw:any[]=[],hasMore=false;
  if(q){const data=await catalog('/search/multi',{...lang,query:q,page:String(page),include_adult:'false'});raw=(data.results||[]).filter(matches);hasMore=page<Math.min(data.total_pages||1,500);
   if(provider){raw=(await Promise.all(raw.map(async x=>{const data=await catalog(`/${x.media_type}/${x.id}/watch/providers`);return data.results?.[region]?.flatrate?.some((p:any)=>provider.ids.includes(p.provider_id))?x:null;}))).filter(Boolean);}
  }else{
   const kinds=type==='all'?['movie','series']:[type];const batches=await Promise.all(kinds.map(async kind=>{const tv=kind==='series';const params:Record<string,string>={...lang,page:String(page),include_adult:'false',sort_by:sort==='date.desc'?(tv?'first_air_date.desc':'primary_release_date.desc'):sort};if(sort==='vote_average.desc')params['vote_count.gte']='100';if(genre)params.with_genres=String(genre);if(year)params[tv?'first_air_date_year':'primary_release_year']=String(year);if(provider){params.with_watch_providers=provider.ids.join('|');params.watch_region=region;params.with_watch_monetization_types='flatrate';}if(input.action==='upcoming'){params[tv?'first_air_date.gte':'primary_release_date.gte']=new Date(Date.now()+86400000).toISOString().slice(0,10);params.sort_by=tv?'first_air_date.asc':'primary_release_date.asc';}const data=await catalog(`/discover/${tv?'tv':'movie'}`,params);return {items:(data.results||[]).map((x:any)=>({...x,media_type:tv?'tv':'movie'})).filter(matches),more:page<Math.min(data.total_pages||1,500)};}));raw=batches.flatMap(b=>b.items);hasMore=batches.some(b=>b.more);if(input.action==='upcoming')raw.sort((a,b)=>(a.release_date||a.first_air_date||'').localeCompare(b.release_date||b.first_air_date||''));
  }
  return reply({items:raw.map(x=>media(x,undefined,language)).filter(Boolean),hasMore,nextPage:page+1});
 }catch{return reply({error:language==='en'?'Could not load the catalog. Check the function and TMDB_TOKEN.':'No se pudo cargar el catálogo. Verifica la función y TMDB_TOKEN.'},502);}
});
