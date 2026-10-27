"""PC-only services. Transfers run independently of serialized account RPCs."""
import hashlib
import os
import re
import threading
from pathlib import Path
from urllib.parse import urlsplit, urlencode
from urllib.request import urlopen
from brunio.core import addon_id, valid_url
from brunio.downloads import DownloadStore, file_extension
from brunio import updater
from sports import scoreboard,follows
import datetime as dt
import json
import sys

from build_version import PC_VERSION
from pc_updates import get_latest

class Remaining:
    def remaining_operations(self):
        self._download_store=None; self._jobs={}; self._job_lock=threading.Lock(); self._transfer_slots=threading.Semaphore(2)
        self._update_info=None;self._update_path=None;self._update_status='idle';self._update_lock=threading.Lock()
        return {'downloads.list':self.download_list,'downloads.start':self.download_start,
                'downloads.cancel':self.download_cancel,'downloads.remove':self.download_remove,
                'downloads.open':self.download_open,'downloads.folder':self.download_folder,'downloads.season':self.download_season,
                'party.create':self.party_create,'party.join':self.party_join,'party.leave':self.party_leave,
                'party.poll':self.party_poll,'party.send':self.party_send,'party.decide':self.party_decide,'party.tick':self.party_tick,'party.state':self.party_state,
                'addons.list':self.addons_list,'addons.add':self.addons_add,'addons.remove':self.addons_remove,
                'addons.order':self.addons_order,'profiles.save':self.profile_save,'profiles.delete':self.profile_delete,
                'calendar.subscriptions':self.subscriptions,'calendar.subscribe':self.subscribe,'calendar.sports':self.calendar_sports,
                'scores.list':self.scores,'notifications.list':self.notices,'notifications.scan':self.scan_notice,'notifications.clear':self.clear_notices,'notifications.plan':self.notice_plan,
                'account.configure':self.configure_account,'updates.check':self.update_check,
                'updates.prepare':self.update_prepare,'updates.poll':self.update_poll,'updates.install':self.update_install}

    def scope_id(self):return str(self.client.session.get('userId','local'))+':'+self.client.profile
    def require_scope(self,scope):
        if scope!=self.scope_id():raise ValueError('Profile changed')
    def download_store(self):
        if self._download_store is None:
            root=Path(os.environ.get('LOCALAPPDATA',str(Path.home())))/'BruniO'
            if not getattr(sys,'frozen',False):root=root/'PCNext'
            root=root/'Downloads'
            self._download_store=DownloadStore(root)
        return self._download_store
    def download_list(self):
        rows=self.download_store().list(self.scope_id())
        for row in rows:
            digest=hashlib.sha256(row['poster'].encode()).hexdigest()
            if (self.download_store().root/('cover-'+digest+'.img')).is_file():row['cover']='cover:'+digest
        return {'scope':self.scope_id(),'files':rows}
    def owned_download(self,id):
        row=next((r for r in self.download_store().list(self.scope_id()) if r['id']==id),None)
        if not row:raise ValueError('Unknown download')
        return row
    def cancel_transfers(self):
        with self._job_lock:
            for cancel in self._jobs.values():cancel.set()
    def download_start(self,cloud_id,scope,season=0,episode=0):
        self.require_scope(scope)
        item=self.client.by_id(cloud_id)
        streams=self.sources(cloud_id,season,episode,download=True)['streams']
        self.require_scope(scope)
        stream=next((s for s in streams if file_extension(s)),None)
        if stream is None:raise ValueError('No direct downloadable file')
        store=self.download_store();entry,created=store.create(scope,item,season,episode,file_extension(stream))
        if not created:return {'id':entry['id'],'created':False}
        cancel=threading.Event()
        with self._job_lock:self._jobs[entry['id']]=cancel
        def work():
            try:
                with self._transfer_slots:
                    if cancel.is_set():
                        store.update(entry['id'],scope,state='interrupted',error='Cancelled');return
                    poster=item.get('poster','')
                    if poster.startswith('https://image.tmdb.org/'):
                        try:
                            dest=store.root/('cover-'+hashlib.sha256(poster.encode()).hexdigest()+'.img')
                            if not dest.exists():
                                with urlopen(poster,timeout=8) as response:data=response.read(2*1024*1024+1)
                                if 0<len(data)<=2*1024*1024:dest.write_bytes(data)
                        except Exception:pass
                    store.transfer(entry,stream,cancel=lambda:cancel.is_set())
            except Exception:pass # DownloadStore writes a sanitized failure state.
            finally:
                with self._job_lock:self._jobs.pop(entry['id'],None)
        threading.Thread(target=work,name='brunio-download',daemon=True).start()
        return {'id':entry['id'],'created':True}
    def download_cancel(self,id,scope):
        self.require_scope(scope);self.owned_download(id)
        with self._job_lock:
            cancel=self._jobs.get(id)
            if cancel:cancel.set()
        return {'cancelled':bool(cancel)}
    def download_remove(self,id,scope):
        self.require_scope(scope);return {'removed':self.download_store().remove(id,scope)}
    def download_open(self,id,scope):
        self.require_scope(scope);row=self.owned_download(id);path=self.download_store().path(row)
        if row['state']!='complete' or not path.is_file():raise ValueError('File unavailable')
        return {'item':{'cloudId':row['cloud'],'title':row['title'],'type':row['kind'],'id':row['media_id']},
                'streams':[{'url':path.resolve().as_uri(),'name':'Offline'}], 'season':row['season'],'episode':row['episode'],'scope':scope}
    def download_season(self,cloud_id,scope,season):
        self.require_scope(scope)
        today=__import__('datetime').date.today().isoformat()
        rows=[e['episode_number'] for e in self.episodes(cloud_id,season) if e.get('air_date') and e['air_date']<=today]
        return {'scope':scope,'cloud_id':cloud_id,'season':season,'episodes':rows}

    def party_create(self):
        self.client.party_create();return self.party_view()
    def party_view(self):
        p=self.client.party
        return {'scope':self.scope_id(),'room':p.get('room_code') if p else None,'host':self.client.host() if p else False,
                'state':p.get('state',{}) if p else {},'pending':self.client.pending_room}
    def party_join(self,code):
        decision=self.client.party_join(code)
        return {**self.party_view(),'decision':decision}
    def party_leave(self):
        self.client.party_leave();return self.party_view()
    def party_poll(self,after=-1):
        if not isinstance(after,int) or isinstance(after,bool) or after< -1:raise ValueError('Invalid event cursor')
        if self.client.pending_room:self.client.party_join(self.client.pending_room)
        p=self.client.party;events=[];requests=self.client.social_rpc('rp1_miflix_access_requests',{}) or []
        if p:
            current=self.client.party_read(p['room_code'])
            if current is None:
                self.client.detach();return {**self.party_view(),'events':[],'requests':requests}
            self.client.party=current
            q={'room_code':'eq.'+p['room_code'],'select':'id,user_id,kind,body','order':'id.asc' if after>=0 else 'id.desc','limit':60}
            if after>=0:q['id']='gt.'+str(after)
            events=self.client.social_rest('rp1_miflix_party_events?'+urlencode(q)) or []
            events=sorted(events,key=lambda e:e['id'])
        return {**self.party_view(),'events':events,'requests':requests}
    def party_send(self,scope,room,kind,body):
        self.require_scope(scope)
        if not self.client.party or self.client.party['room_code']!=room:raise ValueError('Room changed')
        if kind not in ('chat','emoji','phrase') or not isinstance(body,str) or not body.strip() or len(body)>400:raise ValueError('Invalid event')
        self.client.social_rest('rp1_miflix_party_events','POST',{'room_code':room,'user_id':self.client.social_identity(),'kind':kind,'body':body.strip()})
        return {'sent':True}
    def party_decide(self,scope,code,guest,approve):
        self.require_scope(scope)
        if not isinstance(approve,bool):raise ValueError('Invalid decision')
        return self.client.social_rpc('rp1_miflix_decide_access',{'code':code,'guest':guest,'approve':approve})

    def addons_list(self):
        self.client.load_account()
        language='es' if self.settings_get()['audio']=='es' else 'en'
        selected=self.local.get('preferences',{}).get('profile_playback',{}).get(self.scope_id(),{})
        return {'scope':self.scope_id(),'language':language,'order':selected.get('addon_order_'+language,[]),
                'items':[{'id':addon_id(m),'name':urlsplit(m).hostname or 'Complemento'} for m in self.client.manifests]}
    def addons_add(self,manifest,scope):
        self.require_scope(scope)
        if not isinstance(manifest,str) or len(manifest)>16000 or not valid_url(manifest) or urlsplit(manifest).scheme!='https' or not urlsplit(manifest).path.endswith('/manifest.json'):raise ValueError('Invalid addon')
        # Never return private URL contents to the UI or error output.
        self.client.edit_setup(add=manifest);return self.addons_list()
    def addons_remove(self,id,scope):
        self.require_scope(scope);self.client.load_account()
        manifest=next((m for m in self.client.manifests if addon_id(m)==id),None)
        if manifest is None:raise ValueError('Unknown addon')
        self.client.edit_setup(remove=manifest);return self.addons_list()
    def addons_order(self,ids,language,scope):
        self.require_scope(scope);self.client.load_account()
        if language not in ('es','en') or not isinstance(ids,list) or any(not isinstance(k,str) for k in ids) or len(set(ids))!=len(ids):raise ValueError('Invalid order')
        known={addon_id(m) for m in self.client.manifests}
        if set(ids)!=known:raise ValueError('Order must include each installed addon')
        self.local.setdefault('preferences',{}).setdefault('profile_playback',{}).setdefault(scope,{})['addon_order_'+language]=ids
        self.persist();return self.addons_list()

    def update_check(self):
        with self._update_lock:
            if self._update_status=='downloading':raise ValueError('Update already downloading')
        info=get_latest()
        self._update_info=info if info and updater.version(info['version'])>updater.version(PC_VERSION) else None
        self._update_path=None
        return {'current':PC_VERSION,'available':self._update_info['version'] if self._update_info else None}
    def update_prepare(self):
        with self._update_lock:
            if self._update_info is None:raise ValueError('Check updates first')
            if self._update_status=='downloading':return {'status':'downloading'}
            info=dict(self._update_info);self._update_status='downloading';self._update_path=None
        def work():
            try:
                path=updater.download(info)
                with self._update_lock:self._update_path=path;self._update_status='verified'
            except Exception:
                with self._update_lock:self._update_status='failed'
        threading.Thread(target=work,name='brunio-update',daemon=True).start()
        return {'status':'downloading'}
    def update_poll(self):
        with self._update_lock:return {'status':self._update_status,'version':self._update_info['version'] if self._update_info else None}
    def update_install(self):
        if os.name!='nt' or not self._update_path or not self._update_info:raise ValueError('No verified installer')
        # Recheck the bytes immediately before launching; never accept a renderer-supplied path.
        digest=hashlib.sha256();size=0
        with self._update_path.open('rb') as handle:
            for chunk in iter(lambda:handle.read(1024*1024),b''):digest.update(chunk);size+=len(chunk)
        if size!=self._update_info['sizeBytes'] or digest.hexdigest()!=self._update_info['sha256']:raise ValueError('Installer changed')
        os.startfile(str(self._update_path));return {'launched':True}

    def profile_avatar(self,value):
        if isinstance(value,str) and value.startswith('https://') and valid_url(value):return value
        names={'ai:astronaut':'brunio_avatar_astronaut.png','ai:fox':'brunio_avatar_fox.png','ai:robot':'brunio_avatar_robot.png','ai:dragon':'brunio_avatar_dragon.png','special:photo':'brunio_avatar_special.png','special:extra':'brunio_avatar_extra.png','special:extra3':'brunio_avatar_special3.jpg'}
        if isinstance(value,str) and re.fullmatch(r'special:extra(?:[4-9]|1[0-5])',value):names[value]='brunio_avatar_special'+value[13:]+'.png'
        return 'asset:'+names.get(value,'brunio_avatar_astronaut.png')
    def profile_save(self,scope,name,id=None,avatar=None):
        self.require_scope(scope)
        if not isinstance(name,str) or not 1<=len(name.strip())<=24:raise ValueError('Invalid name')
        if avatar is not None and avatar not in ('ai:astronaut','ai:fox','ai:robot','ai:dragon'):raise ValueError('Invalid avatar')
        self.client.load_account();rows=[dict(p) for p in self.client.account['profiles']]
        if id is None:
            id=str(__import__('uuid').uuid4());rows.append({'id':id,'name':name.strip(),'avatarValue':avatar or 'ai:astronaut','primary':False})
        else:
            row=next((p for p in rows if p['id']==id),None)
            if row is None:raise ValueError('Unknown profile')
            row['name']=name.strip()
            if avatar is not None:row['avatarValue']=avatar
        self.client.profiles(rows)
        return self.account_view()
    def profile_delete(self,scope,id):
        self.require_scope(scope);self.client.load_account()
        rows=self.client.account['profiles'];target=next((p for p in rows if p['id']==id),None)
        if not target or target.get('primary') or len(rows)<2:raise ValueError('Cannot remove profile')
        self.cancel_transfers()
        if self.client.party:self.client.party_leave()
        rows=self.client.rpc('miflix_delete_profile',{'target_profile':id})
        if not isinstance(rows,list) or not rows:raise ValueError('Profile deletion failed')
        self.client.account['profiles']=rows
        if self.client.profile==id:self.client.detach();self.client.profile=rows[0]['id']
        self.persist();return self.account_view()

    def party_tick(self,scope,room,playback=None,request=None):
        self.require_scope(scope)
        if not self.client.party or self.client.party['room_code']!=room:raise ValueError('Room changed')
        desired=None
        if self.client.host():
            desired=self.client.social_rpc('rp1_miflix_take_playback',{'code':room})
            if playback is not None:
                if not isinstance(playback,dict):raise ValueError('Invalid playback')
                cloud=playback.get('cloudId','')
                if not isinstance(cloud,str) or not re.fullmatch(r'tmdb:(movie|series|live):-?[0-9]+',cloud):raise ValueError('Invalid media')
                for key in ('positionMs','season','episode'):
                    v=playback.get(key,0)
                    if not isinstance(v,int) or isinstance(v,bool) or v<0:raise ValueError('Invalid playback time')
                if not isinstance(playback.get('playing'),bool):raise ValueError('Invalid playback status')
                state={key:playback.get(key,default) for key,default in {'cloudId':'','season':0,'episode':0,'positionMs':0,'playing':False,'liveChannelId':'','liveProvider':'','liveTitle':'','liveType':'tv'}.items()}
                for key in ('liveChannelId','liveProvider','liveTitle','liveType'):
                    if not isinstance(state[key],str) or len(state[key])>1000:raise ValueError('Invalid live metadata')
                state['updatedAt']=__import__('time').time_ns()//1_000_000
                if isinstance(desired,bool):state['playing']=desired
                self.client.host_update(state)
        elif request is not None:
            if not isinstance(request,bool):raise ValueError('Invalid request')
            state=self.client.party.get('state',{})
            if state.get('cloudId'):self.client.request_playback({'cloudId':state['cloudId']},state.get('season',0),state.get('episode',0),request)
        current=self.client.party_read(room)
        if current is None:self.client.detach()
        else:self.client.party=current
        return {**self.party_view(),'controlPlaying':desired if isinstance(desired,bool) else None}

    def party_state(self):
        if self.client.pending_room:self.client.party_join(self.client.pending_room)
        if self.client.party:
            current=self.client.party_read(self.client.party['room_code'])
            if current is None:self.client.detach()
            else:self.client.party=current
        return self.party_view()

    def subscriptions(self):
        entries=self.client.calendar_subscriptions()
        folder=Path(sys._MEIPASS)/'assets' if getattr(sys,'frozen',False) else Path(__file__).resolve().parents[1]/'assets'
        teams=json.loads((folder/'calendar_teams.json').read_text())
        return {'scope':self.scope_id(),'entries':entries,'teams':teams}
    def subscribe(self,scope,key,enabled):
        self.require_scope(scope)
        if not isinstance(enabled,bool) or not isinstance(key,str) or not re.fullmatch(r'(?:EVENT:)?(?:NBA|MLB|LIDOM|F1):(?:\*|[A-Za-z0-9_-]{1,40})',key):raise ValueError('Invalid subscription')
        entries=self.client.calendar_subscriptions();old=entries.get(key,{})
        stamp=max(__import__('time').time_ns()//1_000_000,int(old.get('at',0))+1)
        self.client.calendar_subscriptions({key:{'enabled':enabled,'at':stamp}})
        return self.subscriptions()
    def scores(self,league):
        if league not in ('NBA','MLB','LIDOM','F1'):raise ValueError('Unknown league')
        return scoreboard(self.client.transport,league)
    def calendar_sports(self,month):
        if not re.fullmatch(r'\d{4}-\d{2}',month):raise ValueError('Invalid month')
        dt.date.fromisoformat(month+'-01')
        entries=self.client.calendar_subscriptions();active={k for k,v in entries.items() if isinstance(v,dict) and v.get('enabled')}
        events=[];warnings=[]
        for league in ('NBA','MLB','LIDOM','F1'):
            if not any(k.startswith(league+':') or k.startswith('EVENT:'+league+':') for k in active):continue
            try:
                for event in scoreboard(self.client.transport,league,month):
                    if follows(active,event):events.append({'id':league+':'+event['id'],'title':event['title'],'starts_at':event['starts'],'cloud_id':'','league':league})
            except Exception:warnings.append(league)
        return {'events':events,'warnings':warnings}
    def notices(self):
        prefs=self.local.get('preferences',{})
        return {'scope':self.scope_id(),'items':prefs.get('notifications',{}).get(self.scope_id(),[])}
    def notice_plan(self):
        return {'scope':self.scope_id(),'ids':self.client.state().get('favorites',[])[:40]}
    def scan_notice(self,scope,cloud_id):
        self.require_scope(scope)
        if cloud_id not in self.client.state().get('favorites',[]):raise ValueError('Not in list')
        item=self.client.by_id(cloud_id);details=self.client.details(item)
        episode=details.get('last_episode_to_air') or {}
        key=str(episode.get('id')) if episode else details.get('release_date','')
        date=episode.get('air_date','') if episode else details.get('release_date','')
        prefs=self.local.setdefault('preferences',{});known=prefs.setdefault('known_releases',{}).setdefault(scope,{})
        notices=prefs.setdefault('notifications',{}).setdefault(scope,[])
        if date and date<=dt.date.today().isoformat():
            if cloud_id in known and known[cloud_id]!=key:
                text=item['title']+' · '+date
                if text not in notices:notices.insert(0,text)
            known[cloud_id]=key
        del notices[100:];self.persist();return self.notices()
    def clear_notices(self,scope):
        self.require_scope(scope)
        self.local.setdefault('preferences',{}).setdefault('notifications',{})[scope]=[]
        self.persist();return self.notices()

    def download_folder(self):
        return {'path':str(self.download_store().root.resolve())}
    def configure_account(self,scope,token):
        self.require_scope(scope)
        if not isinstance(token,str) or not re.fullmatch(r'[A-Za-z0-9_.-]{20,8192}',token):raise ValueError('Invalid token')
        self.client.edit_setup(token=token)
        return {'configured':True}
