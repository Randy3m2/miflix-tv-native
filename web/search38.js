'use strict';
// Reuse the existing search/filter handlers. One set of inputs, no extra queries.
(()=>{
 const searchDialog=el('dialog',undefined,'cinema-search-dialog');searchDialog.id='cinemaSearch38';document.body.append(searchDialog);
 searchDialog.setAttribute('aria-label',cinemaWords('Buscar películas y series','Search movies and series'));
 const heading=el('div',undefined,'search38-heading'),name=el('h2',cinemaWords('Encuentra tu próxima historia','Find your next story'));
 const trigger=button('',()=>openSearch(),'cinema-search-trigger');trigger.id='cinemaSearchTrigger38';trigger.setAttribute('aria-label',cinemaWords('Buscar y filtrar','Search and filter'));trigger.setAttribute('aria-haspopup','dialog');trigger.setAttribute('aria-controls',searchDialog.id);
 trigger.innerHTML='<svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="10.5" cy="10.5" r="6.5"/><path d="m15.5 15.5 5 5"/></svg>';
 cinemaHeader.insertBefore(trigger,cinemaHeader.querySelector('.header-tools'));
 const close=button('×',()=>searchDialog.close(),'search38-close');close.setAttribute('aria-label',t('Cerrar'));heading.append(name,close);
 const controls=el('div',undefined,'search38-controls');searchDialog.append(heading,controls);for(const id of ['searchForm','toolbar','advancedFilters','status'])controls.append($(id));
 const resultSlot=el('div',undefined,'search38-results'),gridHome=document.createComment('catalog-grid-home'),moreHome=document.createComment('catalog-more-home');$('grid').before(gridHome);$('more').before(moreHome);
 const catalogStatus=el('p',undefined,'search38-catalog-status');cinemaRails.before(catalogStatus);catalogStatus.setAttribute('role','status');
 function syncStatus(){catalogStatus.textContent=loading||!items.length?$('status').textContent:'';}
 new MutationObserver(syncStatus).observe($('status'),{childList:true,subtree:true,characterData:true});
 searchDialog.append(heading,controls,resultSlot);document.body.append(searchDialog);
 function openSearch(){if(!session)return;closeCinemaHover();if(view!=='explore')setView('explore');for(const id of ['searchForm','toolbar','advancedFilters'])$(id).hidden=false;$('advancedFilters').querySelector('details').open=true;resultSlot.append($('grid'),$('more'));$('grid').hidden=false;searchDialog.showModal();trigger.setAttribute('aria-expanded','true');$('search').focus();}
 searchDialog.addEventListener('close',()=>{gridHome.after($('grid'));moreHome.after($('more'));trigger.setAttribute('aria-expanded','false');render();trigger.focus();});
 searchDialog.addEventListener('click',e=>{if(e.target===searchDialog)searchDialog.close();});
 const oldRender=render;render=function(){oldRender();if(searchDialog.open)$('grid').hidden=false;syncStatus();trigger.classList.toggle('search38-filtered',!!($('search').value.trim()||$('genre').value||$('year').value||selectedPerson));};
 const sync=()=>{trigger.hidden=$('app').hidden;if($('app').hidden&&searchDialog.open)searchDialog.close();};new MutationObserver(sync).observe($('app'),{attributes:true,attributeFilter:['hidden']});sync();
})();
