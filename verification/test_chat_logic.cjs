const fs=require('fs'),vm=require('vm'),assert=require('assert');
class Element {
 constructor(){this.children=[];this.value='';this.hidden=false;this.scrollHeight=0;this.scrollTop=0;this.clientHeight=0;this._text='';}
 set textContent(v){this._text=String(v);this.children=[]} get textContent(){return this._text+this.children.map(x=>x.textContent).join('')}
 append(...nodes){for(const n of nodes){n.parent=this;this.children.push(n)}}replaceChildren(...nodes){this.children=[];this._text='';this.append(...nodes)}
 get firstChild(){return this.children[0]}remove(){this.parent.children=this.parent.children.filter(n=>n!==this)}
}
const elements=new Map();const document={getElementById:id=>{if(!elements.has(id))elements.set(id,new Element());return elements.get(id)},createElement:()=>new Element()};
const events=[],requests=[],profiles=[{user_id:'a',nickname:'randy'},{user_id:'b',nickname:'friend_b'}];let next=1;
class Query {
 constructor(table){this.table=table;this.filters=[];this.operation='select'}select(){return this}eq(k,v){this.filters.push([k,v]);return this}in(){return this}gt(k,v){this.after=v;return this}order(){return this}limit(){return this}maybeSingle(){this.single=true;return this}
 insert(body){this.operation='insert';this.body=body;return this}upsert(body){this.operation='upsert';this.body=body;return this}update(body){this.operation='update';this.body=body;return this}
 then(resolve){let data=[];if(this.table==='miflix_watch_parties')data=[{room_code:'123456',state:{}}];if(this.table==='miflix_party_members')data=[{user_id:'a',room_code:'123456'},{user_id:'b',room_code:'123456'}];if(this.table==='miflix_social_profiles')data=profiles;
 if(this.table==='miflix_friendships'){if(this.operation==='insert')requests.push({...this.body,status:'pending'});data=requests;}
 if(this.table==='miflix_party_events'){if(this.operation==='insert')events.push({...this.body,id:next++});data=events.filter(e=>this.after===undefined||e.id>this.after);}
 if(this.operation==='select')for(const[k,v]of this.filters)data=data.filter(x=>x[k]===v);resolve({data:this.single?data[0]||null:data,error:null});}
}
const client={from:t=>new Query(t),rpc:async()=>({data:null,error:null}),auth:{getSession:async()=>({data:{session:null}}),signOut:async()=>({})}};
const html=fs.readFileSync('docs/party/index.html','utf8');let js=html.match(/<script type="module">([\s\S]*?)<\/script>/)[1];fs.writeFileSync('verification/chat.mjs',js);js=js.replace(/^import .*;\n/m,'');
const context={document,createClient:()=>client,URLSearchParams,location:{search:'?room=123456'},localStorage:{getItem:()=>null,setItem:()=>{}},window:{addEventListener:()=>{}},setInterval:()=>1,clearInterval:()=>{},Map,JSON,Date,encodeURIComponent};
(async()=>{const api=await vm.runInNewContext('(async()=>{'+js+';return {enter,send,add,poll};})()',context);
 await api.enter({user:{id:'a'}});assert.equal(elements.get('login').hidden,true);assert(elements.get('members').textContent.includes('friend_b'));
 await api.send('chat','<img src=x onerror=alert(1)>');assert(elements.get('messages').textContent.includes('<img'));assert(!elements.get('messages').children.some(x=>x.tagName==='img'));
 // Same-room cursor avoids duplicates on subsequent polls.
 await api.poll();assert.equal(elements.get('messages').children.length,1);
 await api.add('b');assert.equal(requests.length,1);await api.add('b');assert.equal(requests.length,1);
 elements.get('phraseEdit').value='My phrase|Second phrase|My phrase';elements.get('savePhrases').onclick();assert.equal(elements.get('phrases').children.length,2);
 assert.equal(elements.get('code').textContent,'123456');console.log('PASS: browser logic with simulated DOM/API: room login, participants, safe message rendering, cursor deduplication, friend requests, custom phrase deduplication');
})().catch(e=>{console.error(e);process.exitCode=1});
