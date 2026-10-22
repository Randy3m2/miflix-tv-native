'use strict';
// Home pagination is by shelves; search and My List retain their title pagination.
const home39={unsupported:false,key:'',shelves:[],next:0,more:true,busy:false,seen:new Map()};
function isHome39(){return view==='explore'&&!$('search').value.trim()&&!$('genre').value&&!$('year').value&&!selectedPerson&&!$('cinemaSearch38')?.open&&['popularity.desc','added.desc'].includes($('sort').value);}
function homeSeed39(){return Object.entries(recentProgress).filter(([id,p])=>/^tmdb:(movie|series):[1-9][0-9]*$/.test(id)&&(Number(p.position)>=5||Number(p.percent)>0)).sort((a,b)=>Number(b[1].updatedAt||0)-Number(a[1].updatedAt||0))[0]?.[0]||'';}
function homeKey39(){return [session?.user.id,selected(),language,activeProvider,filter,$('region').value,homeSeed39()].join('|');}
function homeFresh39(data,key){
 const storageKey='brunio-home-seen39:'+key,now=Date.now();let old;
 try{old=JSON.parse(localStorage.getItem(storageKey)||'null');}catch{}
 const current=[...new Set(data.shelves.flatMap(s=>s.items.map(x=>x.cloudId)))];
 const baseline=old?.ids||[],added=old?.added||{};
 if(old)for(const id of current)if(!baseline.includes(id)&&!added[id])added[id]=now;
 for(const [id,at]of Object.entries(added))if(now-Number(at)>30*86400000)delete added[id];
 home39.seen=new Map(Object.entries(added));
 try{localStorage.setItem(storageKey,JSON.stringify({ids:[...new Set([...baseline,...current])].slice(-2000),added}));}catch{}
}
const homeLoad39=load;
load=async function(reset){
 if(!isHome39()){home39.key='';home39.shelves=[];return homeLoad39(reset);}
 const key=homeKey39();if(!reset&&home39.busy)return;
 const ticket=++generation;home39.busy=true;loading=true;$('more').disabled=true;
 if(reset||key!==home39.key){home39.key=key;home39.shelves=[];home39.next=0;home39.more=true;items=[];render();}
 $('status').textContent=cinemaWords('Preparando tu inicio…','Preparing your home…');
 try{
  const [data,continued]=await Promise.all([requestHomeShelves41({action:'home',offset:home39.next,provider:activeProvider,type:filter,region:$('region').value,seed:homeSeed39()}),reset?loadContinue44().catch(()=>[]):Promise.resolve(continue44.items)]);
  if(ticket!==generation||homeKey39()!==key||!isHome39())return;
  continue44.items=continued;
  if(!Array.isArray(data.shelves))throw Error(cinemaWords('Actualiza la función brunio-catalog para cargar el nuevo inicio.','Update brunio-catalog to load the new home.'));
  if(home39.next===0)homeFresh39(data,key);
  const known=new Set(home39.shelves.map(s=>s.id));home39.shelves.push(...data.shelves.filter(s=>!known.has(s.id)&&s.items?.length));
  home39.next=data.nextOffset;home39.more=!!data.hasMore;
  items=[...new Map(home39.shelves.flatMap(s=>s.items).map(x=>[x.cloudId,x])).values()];hasMore=home39.more;
  $('status').textContent=data.partial?cinemaWords('Algunas filas no respondieron. Vuelve a cargar el inicio para reintentar.','Some rows did not respond. Reload home to retry.'):'';
  loading=false;render();
 }catch(e){if(ticket===generation){
  $('status').textContent=e.message;loading=false;home39.more=true;render();
 }}
 finally{if(ticket===generation){home39.busy=false;loading=false;$('more').disabled=false;}}
};
const homeRender39=render;
render=function(){
 continueShelf44.hidden=!isHome39()||!!activeProvider||!continue44.items.length;
 if(!isHome39()||home39.key!==homeKey39()){homeRender39();return;}
 closeCinemaHover();cinemaTabs.hidden=$('app').hidden;document.body.classList.add('cinema-browse');cinemaRenderHero(items[0]);
 cinemaRailCleanup.forEach(dispose=>dispose());cinemaRailCleanup=[];cinemaRails.replaceChildren();renderContinue44();cinemaRails.hidden=false;$('grid').hidden=true;cinemaHero.hidden=!items.length;
 for(const shelf of home39.shelves){
  const section=el('section',undefined,'cinema-shelf'+(shelf.ranked?' home-top10':'')),heading=el('div',undefined,'cinema-shelf-heading'),rail=el('div',undefined,'cinema-rail'),wrap=el('div',undefined,'cinema-rail-wrap');
  heading.append(el('h2',shelf.title));if(shelf.ranked){const note=el('small',cinemaWords('Popularidad de TMDB','TMDB popularity'),'home-ranking-note');heading.append(note);}
  shelf.items.forEach((item,index)=>{const node=card(item);if(shelf.ranked){node.classList.add('home-ranked-card');
    const artwork=node.querySelector('.poster img'),portrait=safeImage(item.poster);
    if(artwork&&portrait){artwork.src=portrait;artwork.onerror=()=>{artwork.onerror=null;const fallback=safeImage(item.backdrop);if(fallback)artwork.src=fallback;};}
const number=el('span',String(index+1),'home-rank');number.setAttribute('aria-hidden','true');node.prepend(number);node.setAttribute('aria-label',String(index+1)+'. '+item.title);}
   let badge=item.freshness==='recent-release'?cinemaWords('Recién estrenada','Recently released'):item.freshness==='new-season'?cinemaWords('Nueva temporada','New season'):item.freshness==='new-episode'?cinemaWords('Nuevo episodio','New episode'):home39.seen.has(item.cloudId)?cinemaWords('Recién añadido','Recently added'):'';
   if(badge){const tag=el('span',badge,'home-freshness');if(!item.freshness)tag.title=cinemaWords('Nuevo en tus recomendaciones de BruniO','New in your BruniO recommendations');node.querySelector('.poster')?.append(tag);}rail.append(node);});
  wrap.append(rail);section.append(heading,wrap);cinemaRails.append(section);cinemaRailCleanup.push(cinemaInlineNavigation(rail,wrap));
 }
 if(loading&&!home39.shelves.length){const skeleton=el('div',undefined,'home-skeleton');skeleton.setAttribute('aria-label',cinemaWords('Cargando filas','Loading rows'));for(let i=0;i<15;i++)skeleton.append(el('div',undefined,'home-skeleton-row'));cinemaRails.append(skeleton);}
 $('more').hidden=!home39.more;$('more').disabled=loading;$('more').textContent=cinemaWords('Cargar más filas','Load more rows');
};
const more39=$('more').onclick;$('more').onclick=()=>isHome39()?load(false):more39();
const setView39=setView;setView=function(next){if(next!=='explore'){home39.key='';home39.shelves=[];$('more').textContent=t('Ver más');}return setView39(next);};
// Profile disclosure uses real account profiles and existing account actions.
(function(){
 const menu=document.querySelector('.header-menu'),summary=menu?.querySelector('summary'),panel=menu?.querySelector('.header-menu-panel');if(!menu||!summary||!panel)return;
 menu.classList.add('profile-menu39');const list=el('div',undefined,'profile-list39'),label=el('p',undefined,'profile-label39');panel.prepend(label,list);$('profile').hidden=true;
 function avatar(profile){const node=el('span',String(profile?.name||'B').slice(0,1).toUpperCase(),'profile-avatar39');const raw=profile?.avatarUrl||profile?.avatar;
  if(typeof raw==='string'){try{const u=new URL(raw);if(u.protocol==='https:'&&!u.username&&!u.password){const img=el('img');img.src=u.href;img.alt='';img.referrerPolicy='no-referrer';img.onerror=()=>img.remove();node.append(img);}}catch{}}return node;}
 function sync(){const current=profiles.find(p=>p.id===selected()),signed=!$('account').hidden;summary.replaceChildren(avatar(current),el('span','▾','profile-caret39'));summary.setAttribute('aria-label',cinemaWords('Perfil y ajustes','Profile & settings'));list.replaceChildren();label.textContent=cinemaWords('Cambiar perfil','Switch profile');label.hidden=!signed;list.hidden=!signed;
  for(const p of profiles){const row=button('',async()=>{$('profile').value=p.id;menu.open=false;summary.focus();await changeProfile();sync();},'profile-choice39');row.append(avatar(p),el('span',p.name));if(p.id===selected()){row.append(el('span','✓'));row.setAttribute('aria-current','true');}list.append(row);}
 }
 const profileChange39=changeProfile;changeProfile=async function(){const result=await profileChange39();sync();return result;};
 new MutationObserver(sync).observe($('account'),{attributes:true,attributeFilter:['hidden']});new MutationObserver(sync).observe($('profile'),{childList:true});menu.addEventListener('toggle',()=>{if(menu.open)sync();});$('language').addEventListener('change',()=>setTimeout(sync,0));sync();
})();
if(session&&profiles.length&&!$('app').hidden)load(true);

$('cinemaSearch38')?.addEventListener('close',()=>{if(isHome39()&&home39.key!==homeKey39())load(true);});
