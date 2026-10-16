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
Deno.serve(async(request:Request)=>{
 if(request.method==='OPTIONS')return new Response('ok',{headers:cors});if(request.method!=='POST')return reply({error:'POST required'},405);
 let language='es';
 try {
  const authorization=request.headers.get('Authorization')||'';if(!authorization.startsWith('Bearer '))return reply({error:'Sign in required'},401);
  const auth=await fetch(`${Deno.env.get('SUPABASE_URL')}/auth/v1/user`,{headers:{apikey:Deno.env.get('SUPABASE_ANON_KEY')!,Authorization:authorization},signal:AbortSignal.timeout(12000)});if(!auth.ok)return reply({error:'Session expired'},401);
  const input=await request.json();language=input.language==='en'?'en':'es';const lang={language:language==='en'?'en-US':'es-ES'};
  const region=typeof input.region==='string'&&/^[A-Z]{2}$/.test(input.region)?input.region:'US';
  if(input.action==='config') {
   const [m,t,p,pt]=await Promise.all([catalog('/genre/movie/list',lang),catalog('/genre/tv/list',lang),catalog('/watch/providers/movie',lang),catalog('/watch/providers/tv',lang)]);
   const providers=Object.entries(platforms).map(([key,v])=>{const provider=[...(p.results||[]),...(pt.results||[])].find((x:any)=>v.ids.includes(x.provider_id));return {key,name:v.name,logo:provider?.logo_path?`https://image.tmdb.org/t/p/w185${provider.logo_path}`:null};});
   return reply({genres:{movie:m.genres,series:t.genres},providers});
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
