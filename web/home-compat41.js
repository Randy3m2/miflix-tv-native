'use strict';
// Compatibility shelves use only actions already supported by older brunio-catalog deployments.
// The current server-side home action remains the preferred single-request path.
async function legacyHomeShelves41(input){
 const words=(es,en)=>language==='en'?en:es,type=input.type||'all',defs=[],year=String(new Date().getFullYear());
 const add=(id,es,en,kind,params={},extra={})=>{if(type==='all'||type===kind)defs.push({id,title:words(es,en),kind,params,...extra});};
 add('top-movies','Top 10 de películas','Top 10 movies','movie',{}, {ranked:true});
 add('top-series','Top 10 de series','Top 10 series','series',{}, {ranked:true});
 add('recent-movies','Películas de este año','Movies of this year','movie',{year});
 add('recent-series','Series de este año','Series of this year','series',{year});
 add('acclaimed-movies','Películas mejor valoradas','Acclaimed movies','movie',{sort:'vote_average.desc'});
 add('acclaimed-series','Series mejor valoradas','Acclaimed series','series',{sort:'vote_average.desc'});
 add('star-wars','Star Wars','Star Wars','movie',{action:'search',query:'Star Wars'});
 add('x-men','X-Men','X-Men','movie',{action:'search',query:'X-Men'});
 add('walking-dead','The Walking Dead','The Walking Dead','series',{action:'search',query:'The Walking Dead'});
 add('thrones','Game of Thrones','Game of Thrones','series',{action:'search',query:'Game of Thrones'});
 if(input.seed&&/^tmdb:(movie|series):[1-9][0-9]*$/.test(input.seed)&&(type==='all'||input.seed.split(':')[1]===type))defs.push({id:'because',seed:input.seed,kind:input.seed.split(':')[1]});
 else add('for-you','Para tu próxima noche de cine','Your next movie night',type==='series'?'series':'movie',{sort:'vote_average.desc',page:2});
 const genres=[['action','Acción sin pausa','Nonstop action','movie',28],['comedy','Comedias para maratonear','Comfort comedies','series',35],['thriller','Suspenso que atrapa','Gripping thrillers','movie',53],['animation','Mundos animados','Animated worlds','movie',16],['documentary','Historias reales','True stories','series',99],['romance','Una dosis de romance','A little romance','movie',10749],['horror','No apagues la luz','Keep the lights on','movie',27],['scifi','Más allá de este mundo','Beyond this world','movie',878],['family','Para toda la familia','For the whole family','movie',10751],['crime','Crimen e investigación','Crime & investigation','series',80],['mystery','Misterios por resolver','Unsolved mysteries','series',9648],['fantasy','Fantasía y ciencia ficción','Sci-fi & fantasy','series',10765],['adventure','La aventura te espera','Adventure awaits','movie',12],['history','Viajes al pasado','Journeys into history','movie',36],['war','Historias de guerra','War stories','movie',10752],['western','El lejano oeste','The wild west','movie',37],['music','Que suene la música','Turn up the music','movie',10402],['reality','Un episodio más','Just one more episode','series',10764],['animated-series','Series animadas','Animated series','series',16],['drama','Grandes dramas','Great dramas','series',18]];
 for(const [id,es,en,kind,genre]of genres)add(id,es,en,kind,{genre});
 for(const [id,es,en,kind,genre]of genres)add('rated-'+id,es+' · mejor valoradas',en+' · top rated',kind,{genre,sort:'vote_average.desc'});
 for(let ago=1;ago<=10;ago++){const y=String(Number(year)-ago);add('year-movie-'+y,'Películas · '+y,'Movies · '+y,'movie',{year:y});add('year-series-'+y,'Series · '+y,'Series · '+y,'series',{year:y});}
 const offset=Number(input.offset||0),limit=offset===0?15:6,today=new Date().toISOString().slice(0,10);let cursor=offset,partial=false;
 async function shelf(def){try{
  let params=def.params||{},title=def.title;
  if(def.seed){const d=await catalog({action:'details',cloud:def.seed});const item=d.item;if(!item)return null;title=words('Porque viste ','Because you watched ')+item.title;params={genre:item.genres?.[0]||'',sort:'popularity.desc'};}
  const data=await catalog({action:'discover',page:1,sort:'popularity.desc',...params,type:def.kind,provider:input.provider||'',region:input.region||'US'});
  const items=[...new Map((data.items||[]).filter(x=>x.type===def.kind&&x.date&&x.date<=today).map(x=>[x.cloudId,x])).values()].slice(0,def.ranked?10:16);
  return items.length?{id:def.id,title,ranked:!!def.ranked,items}:null;
 }catch(e){if(/session|sign in|sesión|inicia sesión|cuenta cambió/i.test(String(e?.message)))throw e;partial=true;return null;}}
 const shelves=[];
 while(cursor<defs.length&&shelves.length<limit){
  const batch=defs.slice(cursor,cursor+Math.min(3,limit-shelves.length));cursor+=batch.length;
  const loaded=await Promise.all(batch.map(shelf));shelves.push(...loaded.filter(Boolean));
 }
 return {shelves,nextOffset:cursor,hasMore:cursor<defs.length,partial};
}
async function requestHomeShelves41(input){
 if(home39.unsupported)return legacyHomeShelves41(input);
 try{return await catalog(input);}catch(e){
  if(!/^invalid action$/i.test(String(e?.message||'').trim()))throw e;
  home39.unsupported=true;return legacyHomeShelves41(input);
 }
}
