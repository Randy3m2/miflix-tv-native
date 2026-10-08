// Trakt secrets and OAuth tokens stay on the server. Every request verifies MiFlix auth.
const cors = {"Access-Control-Allow-Origin":"*","Access-Control-Allow-Headers":"authorization, apikey, content-type, x-client-info","Access-Control-Allow-Methods":"POST, OPTIONS"};
const url = Deno.env.get("SUPABASE_URL")!;
const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
const clientId = Deno.env.get("TRAKT_CLIENT_ID");
const clientSecret = Deno.env.get("TRAKT_CLIENT_SECRET");
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
  const fresh=await trakt("/oauth/token",undefined,{refresh_token:t.refresh_token,client_id:clientId,client_secret:clientSecret,redirect_uri:"urn:ietf:wg:oauth:2.0:oob",grant_type:"refresh_token"});
  if(fresh.status!==200)throw new Error("Reconnect Trakt: token refresh failed");t=fresh.data;await saveTokens(uid,t);
 }
 return t.access_token as string;
}
Deno.serve(async(req: Request)=>{
 if(req.method==="OPTIONS")return new Response("ok",{headers:cors});
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
   return response({connected:!!rows?.length,configured:!!clientId&&!!clientSecret});
  }
  if(!clientId||!clientSecret)return response({error:"Configure TRAKT_CLIENT_ID and TRAKT_CLIENT_SECRET in Supabase Edge Function secrets"},503);
  if(action==="start") {
   const r=await trakt("/oauth/device/code",undefined,{client_id:clientId});
   if(r.status!==200)return response({error:`Trakt device code failed (${r.status})`},502);
   await db("miflix_trakt_pending?on_conflict=user_id","POST",{user_id:uid,state:{...r.data,started:Date.now()}});
   const {user_code,verification_url,expires_in,interval}=r.data;
   return response({user_code,verification_url,expires_in,interval});
  }
  if(action==="poll") {
   const rows=await db(`miflix_trakt_pending?user_id=eq.${uid}`);const pending=rows?.[0]?.state;
   if(!pending||Date.now()>pending.started+pending.expires_in*1000)return response({error:"Code expired. Connect again."},410);
   const r=await trakt("/oauth/device/token",undefined,{code:pending.device_code,client_id:clientId,client_secret:clientSecret});
   if(r.status===400)return response({pending:true});
   if(r.status===429)return response({pending:true,slow_down:true});
   if(r.status!==200)return response({error:`Trakt authorization failed (${r.status})`},r.status===410?410:502);
   await saveTokens(uid,r.data);await db(`miflix_trakt_pending?user_id=eq.${uid}`,"DELETE");
   return response({connected:true});
  }
  const token=await access(uid);
  if(action==="disconnect"){
   await trakt("/oauth/revoke",undefined,{token,client_id:clientId,client_secret:clientSecret});
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
