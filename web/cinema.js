'use strict';
// v36 presentation layer. Existing authentication, catalogs and source resolution stay in app.js.
const cinemaWords=(es,en)=>language==='en'?en:es;
const cinemaHeader=document.querySelector('body>header');
const cinemaTabs=document.querySelector('#app .tabs');
cinemaHeader.insertBefore(cinemaTabs,cinemaHeader.querySelector('.header-tools'));
cinemaHeader.classList.add('cinema-header');
const cinemaHero=el('section',undefined,'cinema-hero');cinemaHero.hidden=true;
$('app').insertBefore(cinemaHero,$('app').firstChild);
const cinemaRails=el('div',undefined,'cinema-rails');$('grid').before(cinemaRails);
let cinemaHover=null,cinemaHoverTimer=null;
function closeCinemaHover(){clearTimeout(cinemaHoverTimer);cinemaHover?.remove();cinemaHover=null;}
function cinemaMetadata(item){return [item.year,item.type==='series'?t('Serie'):t('Película'),item.rating?'★ '+Number(item.rating).toFixed(1):''].filter(Boolean).join(' · ');}
function cinemaImage(item,cls=''){const image=el('img',undefined,cls);image.src=safeImage(item.backdrop)||safeImage(item.poster)||'';image.alt='';image.onerror=()=>{const fallback=safeImage(item.poster);if(fallback&&image.src!==fallback)image.src=fallback;};image.loading='lazy';image.decoding='async';return image;}
function cinemaPlay(item){closeCinemaHover();openDetail(item).then(()=>{if(currentDetail?.cloudId===item.cloudId&&$('detail').open)openDownloads(currentDetail);});}
function cinemaHoverCard(item,anchor){
 closeCinemaHover();if(!window.matchMedia('(hover:hover) and (min-width:901px)').matches)return;
 cinemaHoverTimer=setTimeout(()=>{
  if(!anchor.isConnected)return;
  const r=anchor.getBoundingClientRect();if(r.bottom<100||r.top>innerHeight)return;
  const pop=el('aside',undefined,'cinema-hover');pop.setAttribute('aria-label',item.title);
  pop.append(cinemaImage(item));const body=el('div',undefined,'cinema-hover-body'),actions=el('div',undefined,'cinema-hover-actions');
  const play=button('▶',()=>cinemaPlay(item),'cinema-round cinema-play');play.setAttribute('aria-label',cinemaWords('Reproducir','Play'));
  const save=button(favorites.has(item.cloudId)?'✓':'+',()=>toggle(item,save),'cinema-round');save.setAttribute('aria-label',t('Mi lista'));save.dataset.favoriteId=item.cloudId;save.setAttribute('aria-pressed',String(favorites.has(item.cloudId)));
  const details=button('⌄',()=>{closeCinemaHover();openDetail(item);},'cinema-round');details.setAttribute('aria-label',cinemaWords('Ver detalles','View details'));
  actions.append(play,save,details);body.append(actions,el('h3',item.title),el('p',cinemaMetadata(item)),el('p',String(item.overview||'').slice(0,150)));pop.append(body);
  const width=Math.min(370,innerWidth-32);pop.style.width=width+'px';pop.style.left=Math.max(16,Math.min(r.left-(width-r.width)/2,innerWidth-width-16))+'px';pop.style.top=Math.max(88,Math.min(r.top-30,innerHeight-365))+'px';
  pop.onpointerenter=()=>clearTimeout(cinemaHoverTimer);pop.onpointerleave=()=>{cinemaHoverTimer=setTimeout(closeCinemaHover,120);};document.body.append(pop);cinemaHover=pop;
 },280);
}
const cinemaOldCard=card;
card=function(item){const node=cinemaOldCard(item),poster=node.querySelector('.poster'),img=poster.querySelector('img');
 if(img){img.src=safeImage(item.backdrop)||safeImage(item.poster)||img.src;img.onerror=()=>{const fallback=safeImage(item.poster);if(fallback&&img.src!==fallback)img.src=fallback;};}
 node.classList.add('cinema-card');node.onpointerenter=()=>cinemaHoverCard(item,node);node.onpointerleave=()=>{clearTimeout(cinemaHoverTimer);if(cinemaHover)cinemaHoverTimer=setTimeout(closeCinemaHover,180);};return node;};
function cinemaRenderHero(item){
 cinemaHero.replaceChildren();if(!item){cinemaHero.hidden=true;return;}
 const image=cinemaImage(item);image.loading='eager';image.fetchPriority='high';const content=el('div',undefined,'cinema-hero-content');
 content.append(el('p','BRUNIO · '+cinemaWords('TU PRÓXIMA HISTORIA','YOUR NEXT STORY'),'eyebrow'),el('h2',item.title),el('p',cinemaMetadata(item),'cinema-meta'),el('p',item.overview||cinemaWords('Encuentra algo que te encante. Tu próxima historia empieza aquí.','Find something you love. Your next story starts here.'),'cinema-synopsis'));
 const actions=el('div',undefined,'cinema-hero-actions');actions.append(button('▶ '+cinemaWords('Reproducir','Play'),()=>cinemaPlay(item),'primary'),button('ⓘ '+cinemaWords('Más información','More information'),()=>openDetail(item),'quiet'));content.append(actions);cinemaHero.append(image,content);cinemaHero.hidden=false;
}
const cinemaOldRender=render;
render=function(){closeCinemaHover();cinemaOldRender();const browse=view==='explore'&&!$('search').value.trim()&&!$('genre').value&&!$('year').value&&!selectedPerson;
 cinemaTabs.hidden=$('app').hidden;
 cinemaRails.replaceChildren();cinemaRails.hidden=!browse;$('grid').hidden=browse||['friends','calendar','live'].includes(view);
 cinemaHero.hidden=!browse;document.body.classList.toggle('cinema-browse',browse);
 if(!browse)return;
 const visible=items.filter(x=>filter==='all'||x.type===filter);cinemaRenderHero(visible[0]);
 const remaining=[...visible],groups=[];
 // Partition loaded titles by their actual metadata, without additional network calls.
 const genres=catalogConfig?.genres?.[filter==='series'?'series':'movie']||[];
 for(const g of genres){const matches=remaining.filter(x=>x.genres?.includes(g.id));if(matches.length<4)continue;groups.push([g.name,matches]);const ids=new Set(matches.map(x=>x.cloudId));for(let i=remaining.length-1;i>=0;i--)if(ids.has(remaining[i].cloudId))remaining.splice(i,1);if(groups.length===3)break;}
 if(remaining.length)groups.unshift([activeProvider?cinemaWords('Selección de la plataforma','Platform picks'):cinemaWords('Descubre tu próxima historia','Discover your next story'),remaining]);
 for(const [title,rows] of groups){const section=el('section',undefined,'cinema-shelf'),heading=el('div',undefined,'cinema-shelf-heading'),rail=el('div',undefined,'cinema-rail');heading.append(el('h2',title));const controls=el('div'),left=button('‹',()=>rail.scrollBy({left:-rail.clientWidth*.85,behavior:'smooth'}),'quiet'),right=button('›',()=>rail.scrollBy({left:rail.clientWidth*.85,behavior:'smooth'}),'quiet');left.setAttribute('aria-label',cinemaWords('Anterior','Previous'));right.setAttribute('aria-label',cinemaWords('Siguiente','Next'));controls.append(left,right);heading.append(controls);rail.append(...rows.map(card));section.append(heading,rail);cinemaRails.append(section);}
};
const cinemaOldSetView=setView;
setView=function(next){closeCinemaHover();cinemaHero.hidden=true;cinemaRails.hidden=true;$('grid').hidden=['friends','calendar','live'].includes(next);document.body.classList.remove('cinema-browse');cinemaOldSetView(next);};
const cinemaOldDetail=showDetail;
showDetail=function(item){closeCinemaHover();cinemaOldDetail(item);const hero=$('detailBody').querySelector('.detail-hero'),heading=hero?.querySelector('.detail-hero-title'),actions=$('detailBody').querySelector('.detail-info>.detail-actions');if(actions&&heading){heading.append(actions);actions.querySelector('.detail-primary').textContent='▶ '+cinemaWords('Reproducir','Play');}};
function syncCinemaHeader(){cinemaTabs.hidden=$('app').hidden;cinemaHeader.classList.toggle('signed-in',!$('app').hidden);if($('app').hidden){cinemaHero.hidden=true;closeCinemaHover();}}
new MutationObserver(syncCinemaHeader).observe($('app'),{attributes:true,attributeFilter:['hidden']});syncCinemaHeader();
window.addEventListener('scroll',()=>{closeCinemaHover();cinemaHeader.classList.toggle('scrolled',scrollY>16);},{passive:true});
window.addEventListener('resize',closeCinemaHover);document.addEventListener('keydown',e=>{if(e.key==='Escape')closeCinemaHover();});
// All player messages are bound to the active iframe, origin, owner and random channel.
let cinemaNextBusy=false;
window.addEventListener('message',async event=>{
 const current=activeWebPlayer,data=event.data;
 if(!current||event.source!==current.frame.contentWindow||event.origin!==location.origin||data?.channel!==current.channel||session?.user.id!==current.owner||selected()!==current.profile)return;
 if(data.brunio==='player-close'){closeBrowserPlayer();return;}
 if(data.brunio==='player-sources'){closeBrowserPlayer();if(current.target?.live&&current.target.liveChannel)openLiveSources(current.target.liveChannel);else if(current.target?.cloudId){await openDetail(current.target);if(currentDetail?.cloudId===current.target.cloudId){$('castSeason').value=String(current.target.season||1);$('castEpisode').value=String(current.target.episode||1);openDownloads(currentDetail);}}return;}
 if(data.brunio!=='player-next'||cinemaNextBusy||current.target?.type!=='series')return;
 cinemaNextBusy=true;try{
  const target=current.target,season=Number(target.season||1),episode=Number(target.episode||1),details=await catalog({action:'details',cloud:target.cloudId});
  let nextSeason=season,nextEpisode=episode+1,next=null;
  for(let page=1;page<=8;page++){const result=await catalog({action:'episodes',cloud:target.cloudId,season,page});next=result.episodes?.find(x=>Number(x.episode)===nextEpisode);if(next||!result.hasMore)break;}
  if(!next){nextSeason=details.item?.seasons?.map(s=>Number(s.number)).filter(n=>n>season).sort((a,b)=>a-b)[0];if(nextSeason){const result=await catalog({action:'episodes',cloud:target.cloudId,season:nextSeason,page:1});next=result.episodes?.find(x=>Number(x.episode)===1);nextEpisode=1;}}
  if(activeWebPlayer!==current||session?.user.id!==current.owner||selected()!==current.profile)return;
  if(!next||!next.date||next.date>todayLocal()){toast(cinemaWords('No hay otro episodio estrenado disponible.','No next released episode is available.'));return;}
  closeBrowserPlayer();await openDetail(details.item||target);if(currentDetail?.cloudId!==target.cloudId)return;$('castSeason').value=String(nextSeason);$('castEpisode').value=String(nextEpisode);openDownloads(currentDetail);
 }catch(error){toast(error.message);}finally{cinemaNextBusy=false;}
});
if(items.length)render();
