const cors={'Access-Control-Allow-Origin':'*','Access-Control-Allow-Headers':'authorization,apikey,content-type','Access-Control-Allow-Methods':'POST,OPTIONS'};
const reply=(value:unknown,status=200)=>new Response(JSON.stringify(value),{status,headers:{...cors,'Content-Type':'application/json'}});
const cache=new Map<string,{at:number;data:unknown}>();
async function catalog(path:string,params:Record<string,string>={}) {
 const token=Deno.env.get('TMDB_TOKEN');if(!token)throw Error('Configure TMDB_TOKEN');
 const u=new URL(`https://api.themoviedb.org/3${path}`);u.searchParams.set('language','es-ES');
 for(const [k,v] of Object.entries(params))u.searchParams.set(k,v);
 if(!token.startsWith('eyJ'))u.searchParams.set('api_key',token);
 const key=path+JSON.stringify(params);const old=cache.get(key);if(old&&Date.now()-old.at<300000)return old.data;
 const r=await fetch(u,{headers:token.startsWith('eyJ')?{Authorization:`Bearer ${token}`}:{},signal:AbortSignal.timeout(15000)});
 if(!r.ok)throw Error(`Catalog HTTP ${r.status}`);const data=await r.json();
 if(cache.size>=200)cache.delete(cache.keys().next().value!);cache.set(key,{at:Date.now(),data});return data;
}
function media(x:any,type?:string) {
 const kind=type||(x.media_type==='tv'?'series':x.media_type==='movie'?'movie':null);if(!kind||x.adult)return null;
 return {id:x.id,type:kind,cloudId:`tmdb:${kind}:${x.id}`,title:x.title||x.name||'Sin título',overview:x.overview||'',
 poster:x.poster_path?`https://image.tmdb.org/t/p/w500${x.poster_path}`:null,
 backdrop:x.backdrop_path?`https://image.tmdb.org/t/p/w1280${x.backdrop_path}`:null,
 year:(x.release_date||x.first_air_date||'').slice(0,4),rating:x.vote_average||0};
}
Deno.serve(async(request:Request)=>{
 if(request.method==='OPTIONS')return new Response('ok',{headers:cors});
 if(request.method!=='POST')return reply({error:'POST required'},405);
 try {
  const base=Deno.env.get('SUPABASE_URL')!;const key=Deno.env.get('SUPABASE_ANON_KEY')!;
  const authorization=request.headers.get('Authorization')||'';
  if(!authorization.startsWith('Bearer '))return reply({error:'Inicia sesión'},401);
  const auth=await fetch(`${base}/auth/v1/user`,{headers:{apikey:key,Authorization:authorization},signal:AbortSignal.timeout(12000)});
  if(!auth.ok)return reply({error:'Tu sesión venció'},401);
  const input=await request.json();
  if(input.action==='titles') {
   if(!Array.isArray(input.ids)||input.ids.length>20)return reply({error:'Hasta 20 títulos por solicitud'},400);
   const ids=input.ids as unknown[];
   if(ids.some(id=>typeof id!=='string'||!/^tmdb:(movie|series):\d+$/.test(id)))return reply({error:'Invalid title'},400);
   const items=await Promise.all(ids.map(async(id:any)=>{const [,kind,n]=id.split(':');try{return media(await catalog(`/${kind==='series'?'tv':'movie'}/${n}`),kind);}catch{return {cloudId:id,id:Number(n),type:kind,title:'Título no disponible',poster:null,overview:'',year:'',rating:0};}}));
   return reply({items});
  }
  const page=Number(input.page||1);if(!Number.isInteger(page)||page<1||page>500)return reply({error:'Invalid page'},400);
  const q=typeof input.query==='string'?input.query.trim().slice(0,150):'';
  if(input.action!=='search'&&input.action!=='discover')return reply({error:'Invalid action'},400);
  if(input.action==='search'&&q.length<2)return reply({items:[],hasMore:false});
  const data:any=await catalog(q?'/search/multi':'/trending/all/week',{page:String(page),include_adult:'false',...(q?{query:q}:{})});
  return reply({items:(data.results||[]).map((x:any)=>media(x)).filter(Boolean),hasMore:page<Math.min(data.total_pages||1,500)});
 }catch{return reply({error:'No se pudo cargar el catálogo. Verifica la función y TMDB_TOKEN.'},502);}
});
