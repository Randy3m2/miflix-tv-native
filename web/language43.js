'use strict';
// Keep the original select as the single source of truth and reuse its existing change handler.
(function(){
 const select=document.getElementById('language');if(!select)return;
 const root=document.createElement('div');root.className='language-menu43';
 const trigger=document.createElement('button');trigger.type='button';trigger.className='quiet language-trigger43';trigger.setAttribute('aria-expanded','false');trigger.setAttribute('aria-controls','language-options43');
 const options=document.createElement('div');options.id='language-options43';options.className='language-options43';options.hidden=true;options.setAttribute('role','radiogroup');options.setAttribute('aria-label','Idioma / Language');
 const rows=[];for(const [value,label]of [['es','Español'],['en','English']]){
  const row=document.createElement('button');row.type='button';row.className='language-option43';row.setAttribute('role','radio');
  const text=document.createElement('span');text.textContent=label;const tick=document.createElement('span');tick.className='language-check43';tick.setAttribute('aria-hidden','true');tick.textContent='✓';row.append(text,tick);
  row.onclick=()=>{if(select.value!==value){select.value=value;select.dispatchEvent(new Event('change',{bubbles:true}));}sync();open(false);trigger.focus();};
  row.onkeydown=event=>{if(['ArrowDown','ArrowUp','Home','End'].includes(event.key)){event.preventDefault();const index=rows.findIndex(x=>x.row===row),next=event.key==='Home'?0:event.key==='End'?rows.length-1:(index+(event.key==='ArrowDown'?1:-1)+rows.length)%rows.length;rows[next].row.focus();}};
  rows.push({row,value,label});options.append(row);
 }
 function sync(){const chosen=rows.find(x=>x.value===select.value)||rows[0];trigger.textContent=chosen.label;trigger.setAttribute('aria-label',(select.value==='en'?'Language: ':'Idioma: ')+chosen.label);for(const x of rows){const checked=x.value===select.value;x.row.setAttribute('aria-checked',String(checked));x.row.tabIndex=checked?0:-1;x.row.querySelector('.language-check43').hidden=!checked;}}
 function open(value){options.hidden=!value;trigger.setAttribute('aria-expanded',String(value));}
 trigger.onclick=()=>{const expand=options.hidden;open(expand);if(expand)(rows.find(x=>x.value===select.value)||rows[0]).row.focus();};
 trigger.onkeydown=event=>{if(event.key==='ArrowDown'||event.key==='ArrowUp'){event.preventDefault();open(true);(rows.find(x=>x.value===select.value)||rows[0]).row.focus();}};
 root.onkeydown=event=>{if(event.key==='Escape'&&!options.hidden){event.preventDefault();event.stopPropagation();open(false);trigger.focus();}};
 root.append(trigger,options);select.before(root);select.hidden=true;select.classList.add('language-native43');select.addEventListener('change',sync);
 document.addEventListener('click',event=>{if(!root.contains(event.target))open(false);});root.addEventListener('focusout',event=>{if(event.relatedTarget&&!root.contains(event.relatedTarget))open(false);});
 document.querySelector('.header-menu')?.addEventListener('toggle',event=>{if(!event.target.open)open(false);sync();});sync();
})();
