'use strict';
const fs=require('fs'),vm=require('vm'),assert=require('assert');
(async()=>{
 const html=fs.readFileSync(process.argv[2],'utf8');const code=html.match(/<script>([\s\S]*?)<\/script>/)[1].replaceAll('__BRUNIO_ENDPOINT__','http://192.168.1.20:8765').replaceAll('__BRUNIO_LINK_TICKET__','a'.repeat(48));
 const posted=[],requests=[],listeners={};const contentWindow={postMessage:(data,origin)=>posted.push({data,origin})};const status={textContent:''};const frame={contentWindow,src:''};
 const context=vm.createContext({URLSearchParams,document:{getElementById:id=>id==='catalog'?frame:status},window:{addEventListener:(name,callback)=>listeners[name]=callback},fetch:async(path,options)=>{requests.push({path,options});return{ok:true,json:async()=>({token:'private-origin-token'})};}});
 vm.runInContext(code,context);assert(frame.src.startsWith('https://miflix-tv-native-web.vercel.app/#tv='));
 const message=event=>listeners.message(event);
 await message({origin:'https://evil.example',source:contentWindow,data:{brunio:'tv-client',action:'ready'}});assert.equal(posted.length,0);
 await message({origin:'https://miflix-tv-native-web.vercel.app',source:contentWindow,data:{brunio:'tv-client',action:'ready'}});assert.equal(posted[0].data.connected,true);
 assert(!JSON.stringify(posted).includes('private-origin-token'),'session token must stay in local parent');
 const payload={cloud:'tmdb:series:1434',action:'play',season:2,episode:4};
 await message({origin:'https://miflix-tv-native-web.vercel.app',source:contentWindow,data:{brunio:'tv-client',action:'send',id:'request-123',payload}});
 assert.equal(requests.at(-1).path,'/send');assert.equal(requests.at(-1).options.headers.Authorization,'Bearer private-origin-token');assert.equal(posted.at(-1).data.ok,true);
 const count=requests.length;await message({origin:'https://miflix-tv-native-web.vercel.app',source:contentWindow,data:{brunio:'tv-client',action:'send',id:'request-456',payload:{cloud:'https://bad.example',action:'play'}}});assert.equal(requests.length,count);assert.equal(posted.at(-1).data.ok,false);
 await message({origin:'https://miflix-tv-native-web.vercel.app',source:contentWindow,data:{brunio:'tv-client',action:'disconnect'}});
 await message({origin:'https://miflix-tv-native-web.vercel.app',source:contentWindow,data:{brunio:'tv-client',action:'send',id:'request-789',payload}});assert.equal(requests.length,count);assert.equal(posted.at(-1).data.ok,false);
 console.log('PASS: automatic link pairing, embedded catalog, strict message origin/source, private token isolation, title validation, send and disconnect');
})().catch(e=>{console.error(e);process.exitCode=1;});
