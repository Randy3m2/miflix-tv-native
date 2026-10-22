'use strict';
const continue44={items:[]};
const continueShelf44=el('section',undefined,'cinema-shelf continue-shelf44');continueShelf44.hidden=true;$('platforms').before(continueShelf44);
function continueProgress44(){
 let local={};try{local=JSON.parse(localStorage.getItem(webProgressStorage(session?.user.id,selected()))||'{}');}catch{}
 const merged={...recentProgress};for(const [id,row]of Object.entries(local||{}))if(Number(row?.updatedAt||0)>Number(merged[id]?.updatedAt||0))merged[id]=row;
 return Object.entries(merged).filter(([id,p])=>{if(!/^tmdb:(movie|series):[1-9][0-9]*$/.test(id)||!p)return false;const position=Number(p.position),percent=Number(p.percent??(p.duration>0?100*position/p.duration:0));return (filter==='all'||id.split(':')[1]===filter)&&Number.isFinite(position)&&position>=5&&Number.isFinite(percent)&&percent<90;}).sort((a,b)=>Number(b[1].updatedAt||0)-Number(a[1].updatedAt||0)).slice(0,20);
}
async function loadContinue44(){if(activeProvider||!session)return [];const progress=continueProgress44();if(!progress.length)return [];const data=await titleBatch(progress.map(x=>x[0]));return progress.flatMap(([id,p])=>{const item=data.items.find(x=>x.cloudId===id);return item?[{...item,resume44:p}]:[];});}
function renderContinue44(){
 continueShelf44.replaceChildren();continueShelf44.hidden=!isHome39()||!!activeProvider||!continue44.items.length;if(continueShelf44.hidden)return;
 const heading=el('div',undefined,'cinema-shelf-heading'),rail=el('div',undefined,'cinema-rail'),wrap=el('div',undefined,'cinema-rail-wrap');heading.append(el('h2',cinemaWords('Seguir viendo','Continue Watching')));
 for(const item of continue44.items){const node=card(item),p=item.resume44,track=el('div',undefined,'continue-track44'),fill=el('span'),percent=Math.max(0,Math.min(100,Number(p.percent??(p.duration>0?100*p.position/p.duration:0))));fill.style.width=percent+'%';track.setAttribute('role','progressbar');track.setAttribute('aria-label',cinemaWords('Progreso de ','Progress for ')+item.title);track.setAttribute('aria-valuemin','0');track.setAttribute('aria-valuemax','100');track.setAttribute('aria-valuenow',String(Math.round(percent)));track.append(fill);node.querySelector('.poster')?.append(track);rail.append(node);}
 wrap.append(rail);continueShelf44.append(heading,wrap);cinemaRailCleanup.push(cinemaInlineNavigation(rail,wrap));
}
const continueClose44=closeBrowserPlayer;
closeBrowserPlayer=function(){const hadPlayer=!!activeWebPlayer;continueClose44();if(!hadPlayer||!session||!isHome39())return;const key=homeKey39(),ticket=generation;loadContinue44().then(rows=>{if(session&&isHome39()&&homeKey39()===key&&generation===ticket){continue44.items=rows;render();}}).catch(()=>{});};
