// Private add-on configuration never leaves this function. File bytes are not proxied.
const cors={'Access-Control-Allow-Origin':'*','Access-Control-Allow-Headers':'authorization,apikey,content-type','Access-Control-Allow-Methods':'POST,OPTIONS'};
const reply=(body:unknown,status=200)=>new Response(JSON.stringify(body),{status,headers:{...cors,'Content-Type':'application/json','Cache-Control':'no-store'}});
const cache=new Map<string,{at:number;rows:Source[]}>();
type Source={url:string;provider:string;name:string;filename:string;size:number};
const defaults=['torrentio.strem.fun','comet.feels.legal','comet.elfhosted.com'];
function allowedHosts(){return new Set([...defaults,...(Deno.env.get('DOWNLOAD_ADDON_HOSTS')||'').split(',').map(x=>x.trim().toLowerCase()).filter(Boolean)]);}
function https(raw:string){const u=new URL(raw);if(u.protocol!=='https:'||u.username||u.password||(u.port&&u.port!=='443'))throw Error('Unsafe URL');return u;}
function torbox(u:URL){return u.hostname.endsWith('.torbox.app');}
function addon(raw:string){const u=https(raw);if(!allowedHosts().has(u.hostname)||!u.pathname.endsWith('/manifest.json'))throw Error('Unsupported add-on');return u;}
const clean=(value:unknown,max=220)=>String(value||'').replace(/https?:\/\/\S+/gi,'').replace(/[\x00-\x1f]/g,' ').slice(0,max);
function sourceSize(row:any){
 const explicit=Number(row.behaviorHints?.videoSize);if(Number.isFinite(explicit)&&explicit>0)return explicit;
 const text=String(row.title||'')+' '+String(row.description||'');const match=/\b(\d+(?:[.,]\d+)?)\s*(TiB|GiB|MiB|KiB|TB|GB|MB|KB)\b/i.exec(text);if(!match)return 0;
 const unit=match[2].toLowerCase(),power=unit.startsWith('t')?4:unit.startsWith('g')?3:unit.startsWith('m')?2:1;return Number(match[1].replace(',','.'))*Math.pow(unit.includes('i')?1024:1000,power);
}
function sizeLimit(value:unknown){const cap=value===undefined?5:Number(value);if(!Number.isFinite(cap)||cap<0||cap>1000)throw Error('Invalid size limit');return cap*1e9;}
function withinLimit(size:number,limit:number){return limit===0||(Number.isFinite(size)&&size>0&&size<=limit);}
async function json(response:Response){
 if(!response.ok)throw Error('Upstream unavailable');const reader=response.body?.getReader();if(!reader)throw Error('Empty response');let size=0;const chunks:Uint8Array[]=[];
 try{while(true){const part=await reader.read();if(part.done)break;size+=part.value.length;if(size>2*1024*1024)throw Error('Response too large');chunks.push(part.value);}}finally{await reader.cancel().catch(()=>{});}
 const bytes=new Uint8Array(size);let offset=0;for(const chunk of chunks){bytes.set(chunk,offset);offset+=chunk.length;}return JSON.parse(new TextDecoder().decode(bytes));
}
const digest=async(value:string)=>Array.from(new Uint8Array(await crypto.subtle.digest('SHA-256',new TextEncoder().encode(value)))).map(x=>x.toString(16).padStart(2,'0')).join('');
function encode(bytes:Uint8Array){return btoa(String.fromCharCode(...bytes)).replace(/\+/g,'-').replace(/\//g,'_').replace(/=+$/,'');}
function decode(text:string){return Uint8Array.from(atob(text.replace(/-/g,'+').replace(/_/g,'/')),c=>c.charCodeAt(0));}
async function ticketKey(){const secret=Deno.env.get('SUPABASE_SERVICE_ROLE_KEY');if(!secret)throw Error('Missing runtime key');return crypto.subtle.importKey('raw',await crypto.subtle.digest('SHA-256',new TextEncoder().encode('brunio-download-ticket-v1:'+secret)),'AES-GCM',false,['encrypt','decrypt']);}
async function seal(data:unknown){const iv=crypto.getRandomValues(new Uint8Array(12)),encrypted=await crypto.subtle.encrypt({name:'AES-GCM',iv},await ticketKey(),new TextEncoder().encode(JSON.stringify(data)));return encode(iv)+'.'+encode(new Uint8Array(encrypted));}
async function unseal(ticket:string){if(ticket.length>18000)throw Error('Invalid ticket');const parts=ticket.split('.');if(parts.length!==2)throw Error('Invalid ticket');return JSON.parse(new TextDecoder().decode(await crypto.subtle.decrypt({name:'AES-GCM',iv:decode(parts[0])},await ticketKey(),decode(parts[1]))));}
async function tmdb(path:string){const token=Deno.env.get('TMDB_TOKEN');if(!token)throw Error('Missing TMDB_TOKEN');const u=new URL('https://api.themoviedb.org/3'+path);if(!token.startsWith('eyJ'))u.searchParams.set('api_key',token);return json(await fetch(u,{headers:token.startsWith('eyJ')?{Authorization:'Bearer '+token}:{},signal:AbortSignal.timeout(12000),redirect:'error'}));}
// Follow only configured provider hosts and TorBox, with no auth-header forwarding.
async function finalLink(raw:string,providers:Set<string>){
 let current=https(raw);const secrets=new Set<string>();
 for(let hop=0;hop<6;hop++){
  if(!torbox(current)&&!providers.has(current.hostname))throw Error('Unsupported download host');
  if(current.hostname==='api.torbox.app'){
   if(!/^\/v1\/api\/(torrents|usenet|webdl)\/requestdl$/.test(current.pathname))throw Error('Unsupported TorBox route');
   const key=current.searchParams.get('token');if(key)secrets.add(key);current.searchParams.set('redirect','false');
   const data=await json(await fetch(current,{redirect:'error',signal:AbortSignal.timeout(20000)}));
   if(!data.success||typeof data.data!=='string')throw Error('Not ready');current=https(data.data);continue;
  }
  if(torbox(current)){
   if(/\/(?:v\d+\/)?api\//i.test(current.pathname)||current.searchParams.has('apikey')||current.searchParams.has('api_key')||[...secrets].some(x=>current.href.includes(x)))throw Error('Unsafe download link');
   return current.href;
  }
  const response=await fetch(current,{method:'GET',headers:{Range:'bytes=0-0'},redirect:'manual',signal:AbortSignal.timeout(20000)});
  await response.body?.cancel();const location=response.headers.get('Location');
  if(response.status<300||response.status>=400||!location)throw Error('Not ready');current=https(new URL(location,current).href);
 }
 throw Error('Too many redirects');
}
Deno.serve(async(request:Request)=>{
 if(request.method==='OPTIONS')return new Response('ok',{headers:cors});if(request.method!=='POST')return reply({error:'POST required'},405);
 let en=false;
 try{
  if(Number(request.headers.get('content-length')||0)>24000)return reply({error:'Request too large'},413);
  const authorization=request.headers.get('Authorization')||'';if(!authorization.startsWith('Bearer '))return reply({error:'Sign in required'},401);
  const base=Deno.env.get('SUPABASE_URL')!,key=Deno.env.get('SUPABASE_ANON_KEY')!,headers={apikey:key,Authorization:authorization};
  const auth=await fetch(base+'/auth/v1/user',{headers,signal:AbortSignal.timeout(10000)});if(!auth.ok)return reply({error:'Session expired'},401);const user=await auth.json();
  const input=await request.json();en=input.language==='en';const limit=sizeLimit(input.maxGb);
  if(typeof input.profile!=='string'||input.profile.length>100)return reply({error:en?'Select a profile.':'Selecciona un perfil.'},400);
  const stateRows=await json(await fetch(base+'/rest/v1/miflix_user_state?user_id=eq.'+encodeURIComponent(user.id)+'&profile_id=eq.__account__&select=state',{headers,signal:AbortSignal.timeout(10000)}));
  const account=stateRows[0]?.state||{},profile=(account.profiles||[]).find((p:any)=>p.id===input.profile);
  if(!profile||profile.isKids||profile.id==='brunio-kids')return reply({error:en?'Downloads are unavailable for this profile.':'Descargas no disponibles para este perfil.'},403);
  const setup=account.privateSetup||{},manifests=[...new Set([...(setup.addonManifests||[]),setup.torrentioManifest].filter((x:unknown)=>typeof x==='string'&&x))] as string[];
  const valid=manifests.flatMap(raw=>{try{return [addon(raw)];}catch{return [];}}).slice(0,4);
  if(!valid.length)return reply({error:en?'Configure a supported Comet/Torrentio TorBox add-on in BruniO TV first.':'Configura primero un add-on Comet/Torrentio con TorBox en BruniO TV.'},409);
  const fingerprint=await digest(valid.map(x=>x.href).sort().join('|')),providers=new Set(valid.map(x=>x.hostname));
  if(input.action==='link'){
   const data=await unseal(String(input.ticket||''));
   if(data.owner!==user.id||data.profile!==input.profile||data.fingerprint!==fingerprint||data.expires<Date.now())return reply({error:en?'Links expired. Load sources again.':'Enlaces caducados. Carga las fuentes otra vez.'},410);
   if(!withinLimit(data.size,limit))return reply({error:en?'Source exceeds the size limit or has unknown size.':'La fuente supera el límite o su tamaño es desconocido.'},400);
   const url=await finalLink(data.url,providers);return reply({url,filename:data.filename});
  }
  if(input.action!=='sources'||!/^tmdb:(movie|series):[1-9][0-9]{0,8}$/.test(input.cloud||''))return reply({error:'Invalid title'},400);
  const [,kind,id]=input.cloud.split(':'),season=Number(input.season||1),episode=Number(input.episode||1);
  if(!Number.isInteger(season)||!Number.isInteger(episode)||season<1||episode<1||season>10000||episode>10000)return reply({error:'Invalid episode'},400);
  const cacheKey=user.id+':'+fingerprint+':'+input.cloud+':'+season+':'+episode;let rows=cache.get(cacheKey)?.rows;
  if(input.refresh===true||!rows||Date.now()-(cache.get(cacheKey)?.at||0)>3*3600000){
   const details=await tmdb('/'+(kind==='series'?'tv':'movie')+'/'+id+'/external_ids'),imdb=details.imdb_id;
   if(!/^tt\d+$/.test(imdb||''))return reply({sources:[]});
   const media=kind==='series'?imdb+':'+season+':'+episode:imdb;
   const batches=await Promise.allSettled(valid.map(async manifest=>{
    const u=new URL(manifest.href);u.pathname=u.pathname.slice(0,-'/manifest.json'.length)+'/stream/'+kind+'/'+media+'.json';
    const body=await json(await fetch(u,{signal:AbortSignal.timeout(20000),redirect:'error'}));
    return (body.streams||[]).slice(0,120).flatMap((row:any)=>{
     const label=String(row.name||'')+' '+String(row.title||'');if(!/\bTB\b|torbox/i.test(label)||/TB\s*Download|\buncached\b/i.test(label))return [];
     try{const url=https(row.url);if(!torbox(url)&&!providers.has(url.hostname))return [];
      return [{url:url.href,provider:manifest.hostname,name:clean(row.name||'TorBox',100),filename:clean(row.behaviorHints?.filename||row.title||'Download'),size:sourceSize(row)}];
     }catch{return [];}
    }) as Source[];
   }));
   if(batches.every(x=>x.status==='rejected'))return reply({error:en?'Providers are unavailable. Try again later.':'Proveedores no disponibles. Reintenta luego.'},502);
   rows=[...new Map(batches.flatMap(x=>x.status==='fulfilled'?x.value:[]).map(x=>[x.url,x])).values()].slice(0,480);
   if(cache.size>=100)cache.delete(cache.keys().next().value!);cache.set(cacheKey,{at:Date.now(),rows});
  }
  const sources=await Promise.all(rows.filter(row=>withinLimit(row.size,limit)).slice(0,80).map(async row=>({provider:row.provider,name:row.name,filename:row.filename,size:row.size,ticket:await seal({owner:user.id,profile:input.profile,fingerprint,url:row.url,filename:row.filename,size:row.size,expires:Date.now()+15*60000})})));
  return reply({sources});
 }catch{return reply({error:en?'Download unavailable or still processing. Try another source, or load links again.':'Descarga no disponible o todavía procesándose. Prueba otra fuente o carga los enlaces otra vez.'},502);}
});
