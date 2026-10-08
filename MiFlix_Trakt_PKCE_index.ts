// Trakt secrets and OAuth tokens stay on the server. Every request verifies MiFlix auth.
const cors = {"Access-Control-Allow-Origin":"*","Access-Control-Allow-Headers":"authorization, apikey, content-type, x-client-info","Access-Control-Allow-Methods":"POST, OPTIONS"};
const url = Deno.env.get("SUPABASE_URL")!;
const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
const clientId = Deno.env.get("TRAKT_CLIENT_ID");
const redirectUri = `${url}/functions/v1/miflix-trakt`;
const base64url = (bytes: Uint8Array) => btoa(String.fromCharCode(...bytes)).replace(/\+/g,"-").replace(/\//g,"_").replace(/=+$/,"");
const response = (body: unknown, status=200) => new Response(JSON.stringify(body), {status,headers:{...cors,"Content-Type":"application/json"}});
async function db(path: string, method="GET", body?: unknown) {
 const r=await fetch(`${url}/rest/v1/${path}`,{method,headers:{apikey:serviceKey,Authorization:`Bearer ${serviceKey}`,"Content-Type":"application/json",Prefer:"resolution=merge-duplicates,return=representation"},body:body===undefined?undefined:JSON.stringify(body)});
 if(!r.ok)throw new Error(`Database request failed (${r.status})`);
 const t=await r.text();return t?JSON.parse(t):null;
}
async function trakt(path: string, token?: string, body?: unknown) {
 const r=await fetch(`https://api.trakt.tv${path}`,{method:body===undefined?"GET":"POST",headers:{"Content-Type":"application/json","trakt-api-version":"2","trakt-api-key":clientId!,...(token?{Authorization:`Bearer ${token}`}:{})},body:body===undefined?undefined:JSON.stringify(body)});
 const t=await r.text();let data;try{data=JSON.parse(t)}catch{data={}};
 return {status:r.status,data};
}
async function saveTokens(uid: string, tokens: any) { await db("miflix_trakt_tokens?on_conflict=user_id","POST",{user_id:uid,tokens}); }
async function access(uid: string) {
 const rows=await db(`miflix_trakt_tokens?user_id=eq.${uid}`);let t=rows?.[0]?.tokens;
 if(!t)throw new Error("Connect Trakt first");
 if((t.created_at+t.expires_in)*1000 < Date.now()+60000){
  const fresh=await trakt("/oauth/token",undefined,{refresh_token:t.refresh_token,client_id:clientId,redirect_uri:redirectUri,grant_type:"refresh_token"});
  if(fresh.status!==200)throw new Error("Reconnect Trakt: token refresh failed");t=fresh.data;await saveTokens(uid,t);
 }
 return t.access_token as string;
}
Deno.serve(async(req: Request)=>{
 if(req.method==="OPTIONS")return new Response("ok",{headers:cors});
 if(req.method==="GET") {
  const callback=new URL(req.url);
  const state=callback.searchParams.get("state")||"";
  if(!/^[0-9a-f-]{36}$/.test(state))return response({error:"Invalid authorization state"},400);
  try {
   const rows=await db(`miflix_trakt_pending?state->>oauth_state=eq.${state}`);
   const pending=rows?.[0];
   if(!pending||Date.now()>pending.state.started+pending.state.expires_in*1000)return response({error:"Authorization expired. Connect again from your TV."},410);
   if(callback.searchParams.has("error")) {
    await db(`miflix_trakt_pending?user_id=eq.${pending.user_id}&state->>oauth_state=eq.${state}`,"DELETE");
    return response({error:"Authorization cancelled. Connect again from your TV."},400);
   }
   const code=callback.searchParams.get("code");
   if(!code)return response({error:"Missing authorization code"},400);
   const result=await trakt("/oauth/token",undefined,{code,client_id:clientId,redirect_uri:redirectUri,code_verifier:pending.state.verifier,grant_type:"authorization_code"});
   if(result.status!==200)return response({error:"Trakt authorization failed. Connect again from your TV."},502);
   await saveTokens(pending.user_id,result.data);
   await db(`miflix_trakt_pending?user_id=eq.${pending.user_id}&state->>oauth_state=eq.${state}`,"DELETE");
   return new Response("MiFlix conectado a Trakt. Puedes cerrar esta ventana y regresar a tu TV.",{headers:{"Content-Type":"text/plain; charset=utf-8","Cache-Control":"no-store"}});
  }catch{return response({error:"Authorization failed. Connect again from your TV."},502);}
 }
 if(req.method!=="POST")return response({error:"POST required"},405);
 try {
  const authorization=req.headers.get("Authorization")||"";
  if(!authorization.startsWith("Bearer "))return response({error:"Sign in to MiFlix"},401);
  const auth=await fetch(`${url}/auth/v1/user`,{headers:{apikey:serviceKey,Authorization:authorization}});
  if(!auth.ok)return response({error:"MiFlix session expired; sign in again"},401);
  const uid=(await auth.json()).id;
  const input=await req.json();const action=input.action;
  if(action==="status"){
   const rows=await db(`miflix_trakt_tokens?user_id=eq.${uid}&select=user_id`);
   return response({connected:!!rows?.length,configured:!!clientId});
  }
  if(!clientId)return response({error:"Configure TRAKT_CLIENT_ID in Supabase Edge Function secrets"},503);
  if(action==="start") {
   const verifier=base64url(crypto.getRandomValues(new Uint8Array(32)));
   const challenge=base64url(new Uint8Array(await crypto.subtle.digest("SHA-256",new TextEncoder().encode(verifier))));
   const oauth_state=crypto.randomUUID();
   await db("miflix_trakt_pending?on_conflict=user_id","POST",{user_id:uid,state:{verifier,oauth_state,started:Date.now(),expires_in:600}});
   const authorize=new URL("https://auth.trakt.tv/oauth/authorize");
   for(const [key,value] of Object.entries({response_type:"code",client_id:clientId,redirect_uri:redirectUri,state:oauth_state,code_challenge:challenge,code_challenge_method:"S256"}))authorize.searchParams.set(key,value);
   return response({user_code:"Escanea el QR",verification_url:authorize.toString(),expires_in:600,interval:5});
  }
  if(action==="poll") {
   const linked=await db(`miflix_trakt_tokens?user_id=eq.${uid}&select=user_id`);
   const rows=await db(`miflix_trakt_pending?user_id=eq.${uid}`);const pending=rows?.[0]?.state;
   if(!pending&&linked?.length)return response({connected:true});
   if(!pending||Date.now()>pending.started+pending.expires_in*1000)return response({error:"Authorization expired. Connect again."},410);
   return response({pending:true});
  }
  const token=await access(uid);
  if(action==="disconnect"){
   await trakt("/oauth/revoke",undefined,{token,client_id:clientId});
   await db(`miflix_trakt_tokens?user_id=eq.${uid}`,"DELETE");return response({connected:false});
  }
  if(action==="history"){
   const r=await trakt("/sync/history?page=1&limit=30",token);
   if(r.status!==200)throw new Error(`Trakt history failed (${r.status})`);
   return response({items:r.data});
  }
  if(action==="watchlist"){
   const items:any[]=[];
   for(const kind of ["movies","shows"]){
    for(let page=1;page<=10;page++){
     const r=await trakt(`/sync/watchlist/${kind}/added?page=${page}&limit=100`,token);
     if(r.status!==200)throw new Error(`Trakt watchlist failed (${r.status})`);
     items.push(...r.data);if(r.data.length<100)break;
    }
   }
   return response({items,maximum:2000});
  }
  if(action==="watched"){
   const id=Number(input.tmdb);const season=Number(input.season||0);const episode=Number(input.episode||0);
   if(!Number.isInteger(id)||id<=0||!['movie','series'].includes(input.type))return response({error:"Invalid media"},400);
   const event=String(input.event||"");if(!/^[a-zA-Z0-9_-]{1,80}$/.test(event))return response({error:"Invalid event"},400);
   const previous=await db(`miflix_trakt_sent?user_id=eq.${uid}&event_id=eq.${event}`);
   if(previous?.length)return response({saved:true,duplicate:true});
   const payload=input.type==="movie"?{movies:[{ids:{tmdb:id}}]}:{shows:[{ids:{tmdb:id},seasons:[{number:season,episodes:[{number:episode}]}]}]};
   if(input.type==="series"&&(!(season>=0)||!(episode>0)))return response({error:"Invalid episode"},400);
   const r=await trakt("/sync/history",token,payload);
   if(r.status!==200&&r.status!==201)throw new Error(`Trakt history update failed (${r.status})`);
   if((r.data.not_found?.movies?.length||0)+(r.data.not_found?.shows?.length||0)>0)throw new Error("Title not found in Trakt");
   if((r.data.added?.movies||0)+(r.data.added?.episodes||0)<1)throw new Error("Trakt did not register this title");
   await db("miflix_trakt_sent?on_conflict=user_id,event_id","POST",{user_id:uid,event_id:event});
   return response({saved:true});
  }
  return response({error:"Unknown action"},400);
 }catch(e){return response({error:e instanceof Error?e.message:"Request failed"},400);}
});
