// Authenticated by index.ts. No database writes, per-card requests or polling.
export async function homeShelves(input:any,language:string,tmdb:(path:string,params?:Record<string,string>)=>Promise<any>,media:(x:any,type?:string,language?:string)=>any,platforms:Record<string,{ids:number[]}>) {
 const offset=Number(input.offset||0),type=['movie','series'].includes(input.type)?input.type:'all';
 if(!Number.isInteger(offset)||offset<0||offset>60)throw Error('Invalid shelf offset');
 const provider=String(input.provider||'');if(provider&&!platforms[provider])throw Error('Invalid provider');
 const region=['US','DO','ES','MX'].includes(input.region)?input.region:'US',lang={language:language==='en'?'en-US':'es-MX'},today=new Date().toISOString().slice(0,10);
 const since=new Date(Date.now()-30*86400000).toISOString().slice(0,10),words=(es:string,en:string)=>language==='en'?en:es;
 // Bound all upstream work, including recommendations and provider availability checks.
 let running=0;const waiters:(()=>void)[]=[];
 async function get(path:string,params:Record<string,string>={}){if(running>=5)await new Promise<void>(r=>waiters.push(r));running++;try{return await tmdb(path,params);}finally{running--;waiters.shift()?.();}}
 const availability=new Map<string,Promise<boolean>>();
 async function scoped(rows:any[],kind:string){const unique=[...new Map(rows.filter(x=>x&&!x.adult&&x.id).map(x=>[x.id,x])).values()].slice(0,20);if(!provider)return unique;
  const checked=await Promise.all(unique.map(async x=>{const key=kind+':'+x.id;if(!availability.has(key))availability.set(key,get(`/${kind==='series'?'tv':'movie'}/${x.id}/watch/providers`).then(d=>!!d.results?.[region]?.flatrate?.some((p:any)=>platforms[provider].ids.includes(p.provider_id))).catch(()=>false));return await availability.get(key)?x:null;}));return checked.filter(Boolean);
 }
 async function discover(kind:string,params:Record<string,string>={}){const tv=kind==='series',base:Record<string,string>={...lang,include_adult:'false',sort_by:'popularity.desc',page:'1',[tv?'first_air_date.lte':'primary_release_date.lte']:today,...params};if(provider)Object.assign(base,{with_watch_providers:platforms[provider].ids.join('|'),watch_region:region,with_watch_monetization_types:'flatrate'});return (await get(`/discover/${tv?'tv':'movie'}`,base)).results||[];}
 const defs:any[]=[];
 const add=(id:string,es:string,en:string,kind:string,params:Record<string,string>={},extra:any={})=>{if(type==='all'||type===kind)defs.push({id,title:words(es,en),kind,params,...extra});};
 add('top-movies','Top 10 de películas','Top 10 movies','movie',{}, {ranked:true});add('top-series','Top 10 de series','Top 10 series','series',{}, {ranked:true});
 add('recent-movies','Películas recién estrenadas','Recently released movies','movie',{'primary_release_date.gte':since});
 add('ongoing','Nuevos episodios y temporadas','New episodes & seasons','series',{'air_date.gte':since,'air_date.lte':today},{fresh:true});
 const seed=/^tmdb:(movie|series):[1-9][0-9]*$/.test(String(input.seed||''))?String(input.seed):'';
 if(seed&&(type==='all'||seed.split(':')[1]===type))defs.push({id:'because',kind:seed.split(':')[1],seed,title:''});else add('acclaimed','Historias que dejan huella','Critically acclaimed',type==='series'?'series':'movie',{sort_by:'vote_average.desc','vote_count.gte':'1000'});
 if(type!=='series'){defs.push({id:'star-wars',title:'Star Wars',kind:'movie',collection:10});defs.push({id:'x-men',title:'X-Men',kind:'movie',collection:748});}
 if(type!=='movie'){defs.push({id:'walking-dead',title:'The Walking Dead',kind:'series',queries:['The Walking Dead'],match:'walking dead'});defs.push({id:'thrones',title:'Game of Thrones · House of the Dragon',kind:'series',queries:['Game of Thrones','House of the Dragon'],match:'throne|house of the dragon'});}
 const genres:any[]=[['action','Acción sin pausa','Nonstop action','movie',28],['comedy-series','Comedias para maratonear','Comfort comedies','series',35],['thriller','Suspenso que atrapa','Gripping thrillers','movie',53],['animation','Mundos animados','Animated worlds','movie',16],['documentaries','Historias reales','True stories','series',99],['romance','Una dosis de romance','A little romance','movie',10749],['horror','No apagues la luz','Keep the lights on','movie',27],['scifi','Más allá de este mundo','Beyond this world','movie',878],['family','Para toda la familia','For the whole family','movie',10751],['crime','Crimen e investigación','Crime & investigation','series',80],['mystery','Misterios por resolver','Unsolved mysteries','series',9648],['fantasy','Fantasía y ciencia ficción','Sci-fi & fantasy','series',10765],['adventure','La aventura te espera','Adventure awaits','movie',12],['history','Viajes al pasado','Journeys into history','movie',36],['war','Historias de guerra','War stories','movie',10752],['western','El lejano oeste','The wild west','movie',37],['music','Que suene la música','Turn up the music','movie',10402],['reality','Un episodio más','Just one more episode','series',10764],['animated-series','Series animadas','Animated series','series',16],['drama','Grandes dramas','Great dramas','series',18]];
 for(const [id,es,en,kind,genre]of genres)add(id,es,en,kind,{with_genres:String(genre)});
 // Keep 15 real shelves even when movie/series-only removes other definitions.
 if(type!=='all')for(const g of genres.filter(g=>g[3]===type))add('rated-'+g[0],language==='en'?g[2]+' · mejor valoradas':g[1]+' · mejor valoradas',g[2]+' · top rated',type,{with_genres:String(g[4]),sort_by:'vote_average.desc','vote_count.gte':'100'});
 const limit=offset===0?15:6;let cursor=offset,failures=0;
 const loadShelf=async (def:any)=>{try{let rows:any[]=[],title=def.title;
  if(def.collection)rows=await scoped((await get(`/collection/${def.collection}`,lang)).parts||[],def.kind);
  else if(def.queries){const results=await Promise.all(def.queries.map((query:string)=>get('/search/tv',{...lang,query,include_adult:'false'})));rows=await scoped(results.flatMap(d=>d.results||[]).filter(x=>new RegExp(def.match,'i').test(x.original_name||x.name||'')),def.kind);}
  else if(def.seed){const [,kind,id]=def.seed.split(':'),path=`/${kind==='series'?'tv':'movie'}/${id}`,data=await get(path,{...lang,append_to_response:'recommendations'});title=words('Porque viste ','Because you watched ')+(data.title||data.name||'');rows=await scoped(data.recommendations?.results||[],kind);}
  else rows=await discover(def.kind,def.params);
  rows=rows.filter(x=>{const date=x.release_date||x.first_air_date;return date&&date<=today;}).slice(0,def.ranked?10:16);
  const items=rows.map(x=>media(x,def.kind,language)).filter(Boolean);
  if(def.id==='recent-movies')items.forEach((item:any)=>{item.freshness='recent-release';});
  if(def.fresh)await Promise.all(items.slice(0,8).map(async (item:any)=>{try{const details=await get(`/tv/${item.id}`,lang),last=details.last_episode_to_air;if(last?.air_date>=since&&last.air_date<=today){const season=(details.seasons||[]).find((s:any)=>s.season_number===last.season_number);item.freshness=season?.air_date>=since&&last.season_number>1?'new-season':'new-episode';}}catch{}}));
  return {id:def.id,title,ranked:!!def.ranked,items};
 }catch{failures++;return {id:def.id,title:def.title,items:[],failed:true};}};
 const shelves:any[]=[];
 while(cursor<defs.length&&shelves.length<limit){const batch=defs.slice(cursor,cursor+limit-shelves.length);cursor+=batch.length;const loaded=await Promise.all(batch.map(loadShelf));shelves.push(...loaded.filter(s=>s.items.length));}
 const freshness=new Map<string,string>();
 for(const shelf of shelves)for(const item of shelf.items)if(item.freshness)freshness.set(item.cloudId,item.freshness);
 for(const shelf of shelves)for(const item of shelf.items)if(freshness.has(item.cloudId))item.freshness=freshness.get(item.cloudId);
 return {shelves,nextOffset:cursor,hasMore:cursor<defs.length,partial:failures>0,ranking:'tmdb-popularity',freshnessWindowDays:30};
}
