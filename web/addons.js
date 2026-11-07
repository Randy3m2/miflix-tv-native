'use strict';
// Private installation URLs stay in the account record, never localStorage.
function addonURL(raw){
 const u=new URL(String(raw).trim().replace(/^stremio:\/\//i,'https://'));
 const h=u.hostname.toLowerCase();
 if(u.protocol!=='https:'||u.username||u.password||(u.port&&u.port!=='443')||!h.includes('.')||h.endsWith('.local')||h.endsWith('.internal')||h.endsWith('.localhost')||/^\d+(?:\.\d+){3}$/.test(h)||h.includes(':')||!u.pathname.endsWith('/manifest.json')||u.hash||u.href.length>8192)throw Error(words('Use a public HTTPS manifest.json installation URL.','Usa un enlace público HTTPS de instalación manifest.json.'));
 return u.href;
}
function addonAccountPath(owner){return '/rest/v1/miflix_user_state?user_id=eq.'+encodeURIComponent(owner)+'&profile_id=eq.__account__';}
function accountAddons(state){return [...new Set([...(Array.isArray(state.privateSetup?.addonManifests)?state.privateSetup.addonManifests:[]),state.privateSetup?.torrentioManifest].filter(x=>typeof x==='string'&&x.trim()))];}
async function saveWebAddons(owner,urls,expected){
 if(session?.user.id!==owner)throw Error(t('La cuenta cambió'));
 const normalized=[...new Set(urls.map(addonURL))];
 if(normalized.length>50)throw Error(words('Maximum 50 add-ons.','Máximo 50 add-ons.'));
 const path=addonAccountPath(owner),rows=await api(path+'&select=state,updated_at');
 const row=rows?.[0];if(!row?.updated_at)throw Error(words('Create your account profiles in BruniO first.','Crea primero tus perfiles en BruniO.'));
 if(expected&&JSON.stringify(accountAddons(row.state||{}))!==JSON.stringify(expected))throw Error('Account configuration changed');
 const state={...row.state,privateSetup:{...row.state.privateSetup,addonManifests:normalized,torrentioManifest:normalized[0]||''},updatedAt:Date.now()};
 const saved=await api(path+'&updated_at=eq.'+encodeURIComponent(row.updated_at),'PATCH',{state,updated_at:new Date().toISOString()},true,{Prefer:'return=representation'});
 if(!Array.isArray(saved)||saved.length!==1)throw Error(words('Your account changed on another device. Try saving again.','Tu cuenta cambió en otro dispositivo. Intenta guardar de nuevo.'));
 return normalized;
}
async function showWebAddons(){
 if(!session){toast(t('Inicia sesión'));return;}
 const owner=session.user.id,dialog=experienceDialog(words('Add-ons · your account','Add-ons · tu cuenta'));
 const loading=el('p',t('Cargando…'));dialog.append(loading);
 try{
  const rows=await api(addonAccountPath(owner)+'&select=state,updated_at');
  if(!dialog.open||!dialog.contains(loading)||session?.user.id!==owner)return;
  let urls=accountAddons(rows?.[0]?.state||{}),baseline=[...urls],busy=false;
  const content=el('div',undefined,'web-addon-settings'),status=el('p');status.setAttribute('role','status');status.setAttribute('aria-live','polite');
  dialog.replaceChildren(button(t('Cerrar'),()=>dialog.close(),'quiet close'),el('h2',words('Add-ons','Add-ons')),el('p',words('Shared with TV and mobile when they sync your account. Web playback currently supports configured Torrentio and Comet providers; other installed providers are retained for your apps.','Se comparten con TV y móvil cuando sincronizan tu cuenta. El reproductor web admite actualmente Torrentio y Comet configurados; los demás proveedores se conservan para tus apps.')),content,status);
  const render=()=>{
   content.replaceChildren();
   urls.forEach((url,index)=>{
    const row=el('article',undefined,'addon-row'),host=(()=>{try{return new URL(url).hostname;}catch{return words('Invalid saved URL','Enlace guardado inválido');}})();
    row.append(el('strong',(index+1)+'. '+host));
    const edit=el('input');edit.type='password';edit.value=url;edit.autocomplete='off';edit.spellcheck=false;edit.setAttribute('aria-label',words('Private installation URL','Enlace privado de instalación'));edit.maxLength=8192;
    edit.onchange=()=>{urls[index]=edit.value;};row.append(edit);
    const actions=el('div',undefined,'addon-actions');
    if(index>0)actions.append(button('↑ '+words('Move up','Subir'),()=>{[urls[index-1],urls[index]]=[urls[index],urls[index-1]];render();}));
    actions.append(button(words('Remove','Quitar'),()=>{urls.splice(index,1);render();}));row.append(actions);content.append(row);
   });
   if(!urls.length)content.append(el('p',words('No add-ons saved.','No hay add-ons guardados.')));
   const form=el('form'),label=el('label',words('Paste the configured installation URL','Pega el enlace de instalación configurado')),input=el('input');input.type='password';input.autocomplete='off';input.placeholder='https://…/manifest.json';input.required=true;input.maxLength=8192;input.spellcheck=false;label.append(input);
   const add=button(words('Add to list','Agregar a la lista'),()=>{});add.type='submit';form.append(label,add);
   form.onsubmit=e=>{e.preventDefault();try{const value=addonURL(input.value);if(urls.includes(value))throw Error(words('Already added.','Ya está agregado.'));if(urls.length>=50)throw Error(words('Maximum 50 add-ons.','Máximo 50 add-ons.'));urls.push(value);status.textContent=words('Pending changes. Press Save.','Cambios pendientes. Pulsa Guardar.');render();}catch{status.textContent=words('Check the manifest.json URL; it must use public HTTPS.','Revisa el enlace manifest.json; debe usar HTTPS público.');}};
   content.append(form,el('p',words('Configure your provider on its website first, then paste its installation link. Private tokens are hidden. Nothing is saved until you press Save.','Configura primero el proveedor en su página y pega su enlace de instalación. Los tokens privados están ocultos. No se guarda nada hasta pulsar Guardar.')));
   const save=button(words('Save to my account','Guardar en mi cuenta'),async()=>{
    if(busy)return;busy=true;content.querySelectorAll('button,input').forEach(n=>n.disabled=true);status.textContent=t('Cargando…');
    try{urls=await saveWebAddons(owner,urls,baseline);baseline=[...urls];if(!dialog.open||!dialog.contains(content)||session?.user.id!==owner)return;status.textContent=words('Saved. Sync or reopen BruniO on TV/mobile to load the changes.','Guardado. Sincroniza o vuelve a abrir BruniO en TV/móvil para cargar los cambios.');render();}
    catch{if(session?.user.id===owner)status.textContent=words('Could not save. Check your session and URL, then retry. Another device may have changed your account.','No se pudo guardar. Revisa tu sesión y enlace e intenta de nuevo. Otro dispositivo puede haber cambiado tu cuenta.');}
    finally{busy=false;content.querySelectorAll('button,input').forEach(n=>n.disabled=false);}
   },'primary');content.append(save);
  };render();
 }catch{if(dialog.open)dialog.append(el('p',words('Could not load your add-ons. Sign in and try again.','No se pudieron cargar tus add-ons. Inicia sesión e intenta de nuevo.')));}
}
const addonsButton=button('Add-ons',showWebAddons);addonsButton.id='webAddons';document.querySelector('.preferences').append(addonsButton);
// Unload private URLs when the dialog closes, on logout, or on account changes.
$('experienceDialog').addEventListener('close',()=>{$('experienceDialog').replaceChildren();});
const logoutBeforeAddons=logout;logout=function(){if($('experienceDialog').open)$('experienceDialog').close();$('experienceDialog').replaceChildren();logoutBeforeAddons();};$('logout').onclick=logout;
