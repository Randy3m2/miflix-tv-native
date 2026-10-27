"""PC presentation services over the existing BruniO TV-compatible client."""
from pathlib import Path
from urllib.parse import urlencode, quote
import datetime as dt
import re
import sys
from brunio.core import LIVE, valid_url, now_ms
from brunio.playlist import read_m3u

ASSETS = Path(sys._MEIPASS)/'assets' if getattr(sys,'frozen',False) else Path(__file__).resolve().parents[2] / 'brunio-pc' / 'assets'
PLATFORMS = [('Netflix',8),('Disney+',337),('Prime Video',9),('Apple TV+',350),('HBO Max',1899),('Paramount+',531),('AMC+',526),('Hulu',15),('Crunchyroll',283)]
GENRES = [('Acción',28),('Comedia',35),('Drama',18),('Terror',27),('Ciencia ficción',878),('Animación',16)]
SHELVES = {'popular_movies':('/movie/popular','movie'), 'popular_series':('/tv/popular','series'), 'top':('/movie/top_rated','movie')}

class Screens:
    def screen_operations(self):
        return {'catalog.continue':self.continued, 'catalog.saved':self.saved,
                'catalog.shelf':self.shelf, 'catalog.providers':self.providers,
                'catalog.genres':self.genres, 'catalog.discover':self.discover,
                'friends.list':self.friend_list, 'friends.add':self.friend_add,
                'friends.accept':self.friend_accept, 'calendar.list':self.calendar_list,
                'live.list':self.live_list, 'live.sources':self.live_sources,
                'catalog.sources':self.sources, 'catalog.favorite':self.favorite,
                'catalog.episodes':self.episodes, 'catalog.next':self.next_episode, 'playback.progress':self.progress,
                'settings.get':self.settings_get, 'settings.set':self.settings_set}

    def continued(self):
        progress=self.client.state().get('progress',{})
        ids=[k for k,v in sorted(progress.items(),key=lambda x:x[1].get('updatedAt',0),reverse=True)
             if ':s' not in k and v.get('position',0)>=5 and v.get('percent',0)<90][:12]
        return [{**self.client.by_id(k),'progress':progress[k]} for k in ids]

    def saved(self):
        return [self.client.by_id(k) for k in self.client.state().get('favorites',[])[:100]]

    def shelf(self, key):
        if key=='recommendations':
            ratings=self.client.rest('miflix_ratings?'+urlencode({'profile_id':'eq.'+self.client.profile,'select':'cloud_id,score','limit':1000})) or []
            seeds=[r['cloud_id'] for r in sorted(ratings,key=lambda r:r.get('score',0),reverse=True) if r.get('score',0)>=6]
            result={}
            for cloud in list(dict.fromkeys(seeds))[:3]:
                item=self.client.by_id(cloud);kind='tv' if item['type']=='series' else 'movie'
                for row in self.client.catalog(f"/{kind}/{item['id']}/recommendations",item['type']): result[row['cloudId']]=row
            return list(result.values())
        if key not in SHELVES:raise ValueError('Unknown shelf')
        path,kind=SHELVES[key];return self.client.catalog(path,kind)

    def providers(self):
        files=['netflix.png','disney-plus.png','prime-video.png','apple-tv.png','hbo-max.png','paramount-plus.png','streaming_amcplus_rp48.png','hulu.png','crunchyroll.png']
        return [{'id':f'provider:{pid}','title':name,'poster':'asset:'+file} for (name,pid),file in zip(PLATFORMS,files)]

    def genres(self):
        return [{'id':f'genre:{gid}','title':name} for name,gid in GENRES]

    def discover(self, provider=0, genre=0, page=1):
        if not isinstance(page,int) or not 1<=page<=500:raise ValueError('Invalid page')
        params={'page':page}
        if provider:
            if provider not in [p[1] for p in PLATFORMS]:raise ValueError('Unknown platform')
            params.update(with_watch_providers=provider,watch_region='US')
        if genre:
            if genre not in [g[1] for g in GENRES]:raise ValueError('Unknown genre')
            params['with_genres']=genre
        return self.client.catalog('/discover/movie','movie',params)+self.client.catalog('/discover/tv','series',params)

    def friend_list(self):
        activity=self.client.social_rpc('rp1_miflix_friend_activity',{}) or []
        identity=self.client.social_identity()
        pending=self.client.social_rest('rp1_miflix_friendships?select=sender_id,receiver_id,status') or []
        return {'activity':activity,'pending':[r for r in pending if r.get('receiver_id')==identity and r.get('status')=='pending']}

    def friend_add(self,nickname):
        nickname=nickname.strip().lower()
        if not re.fullmatch('[a-z0-9_]{3,24}',nickname):raise ValueError('Invalid nickname')
        rows=self.client.social_rest('rp1_miflix_social_profiles?'+urlencode({'nickname':'eq.'+nickname,'select':'user_id'}))
        if not rows or rows[0]['user_id']==self.client.social_identity():raise ValueError('Invalid recipient')
        self.client.social_rest('rp1_miflix_friendships','POST',{'sender_id':self.client.social_identity(),'receiver_id':rows[0]['user_id']})
        return {'sent':True}

    def friend_accept(self,sender_id):
        # Scope writes to the selected profile's social identity, as on TV.
        self.client.social_rest('rp1_miflix_friendships?'+urlencode({'sender_id':'eq.'+sender_id,'receiver_id':'eq.'+self.client.social_identity(),'status':'eq.pending'}),'PATCH',{'status':'accepted'})
        return {'accepted':True}

    def calendar_list(self,month):
        if not re.fullmatch(r'\d{4}-\d{2}',month):raise ValueError('Invalid month')
        dt.date.fromisoformat(month+'-01')
        rows=self.client.shared_calendar()
        def local_month(value):
            try:return dt.datetime.fromisoformat(value.replace('Z','+00:00')).astimezone().strftime('%Y-%m')
            except (ValueError,TypeError):return ''
        return [r for r in rows if local_month(r.get('starts_at',''))==month]

    def live_list(self,provider='tv'):
        if provider not in ('tv','sports'):raise ValueError('Unknown provider')
        if provider=='tv':rows=read_m3u(ASSETS/'final_channels_rp97.m3u')
        else:
            manifest=self.local.get('preferences',{}).get('sports_manifest') or LIVE['sports']
            body=self.client.transport(manifest);rows=[]
            for cat in body.get('catalogs',[]):
                url=manifest.removesuffix('/manifest.json')+f"/catalog/{quote(cat.get('type','tv'),safe='')}/{quote(cat['id'],safe='')}.json"
                entries=self.client.transport(url).get('metas',[])
                rows.extend({**r,'category':cat.get('name',cat['id'])} for r in entries)
        self.live_cache=getattr(self,'live_cache',{})
        self.live_cache[provider]={str(r['id']):r for r in rows}
        # Raw playback URLs remain in this process until an explicit playback request.
        return [{k:r.get(k,'') for k in ('id','name','logo','poster','category','description')} for r in rows]

    def live_sources(self,provider,channel_id):
        row=getattr(self,'live_cache',{}).get(provider,{}).get(channel_id)
        if not row:raise ValueError('Load channels first')
        if provider=='tv':return [{'url':row['url'],'name':row['name'],'behaviorHints':{'proxyHeaders':{'request':row.get('headers',{})}}}]
        manifest=self.local.get('preferences',{}).get('sports_manifest') or LIVE['sports']
        url=manifest.removesuffix('/manifest.json')+f"/stream/{quote(row.get('type','tv'),safe='')}/{quote(channel_id,safe='')}.json"
        return [r for r in self.client.transport(url).get('streams',[]) if valid_url(r.get('url',''))]

    def sources(self,cloud_id,season=0,episode=0,download=False):
        preferences=self.local.get('preferences',{})
        scope=str(self.client.session.get('userId','local'))+':'+self.client.profile
        selected=preferences.get('profile_playback',{}).get(scope,{})
        self.client.source_audio=selected.get('audio',preferences.get('audio','es'))
        language='es' if self.client.source_audio=='es' else 'en'
        self.client.source_order=selected.get('addon_order_'+language,[])
        rows,imdb=self.client.streams(self.client.by_id(cloud_id),season,episode,preferences.get('download_limit' if download else 'limit',0))
        saved=self.client.state().get('progress',{}).get(cloud_id+(f':s{season}e{episode}' if season else ''),{})
        resume=int(saved.get('position',0)*1000) if saved.get('percent',0)<90 else 0
        return {'resume_ms':resume,'streams':rows,'imdb':imdb,'scope':scope,'audio':self.client.source_audio,'sub':selected.get('sub',preferences.get('sub','es'))}

    def favorite(self,cloud_id,enabled):
        self.client.by_id(cloud_id) # Validate the content identifier.
        if not isinstance(enabled,bool):raise ValueError('Invalid favorite')
        state=self.client.state();ids=state.setdefault('favorites',[])
        if enabled and cloud_id not in ids:ids.append(cloud_id)
        if not enabled:state['favorites']=[k for k in ids if k!=cloud_id]
        self.client.save_state(state)
        return {'saved':enabled}

    def episodes(self,cloud_id,season):
        if not isinstance(season,int) or not 0<=season<=1000:raise ValueError('Invalid season')
        item=self.client.by_id(cloud_id)
        if item['type']!='series':raise ValueError('Not a series')
        return self.client.episodes(item,season)

    def progress(self,cloud_id,scope,position_ms,duration_ms,season=0,episode=0):
        current=str(self.client.session.get('userId','local'))+':'+self.client.profile
        if scope!=current:raise ValueError('Profile changed')
        if not re.fullmatch(r'tmdb:(movie|series):[0-9]+',cloud_id):raise ValueError('Invalid content')
        if not all(isinstance(v,int) and not isinstance(v,bool) for v in (position_ms,duration_ms,season,episode)):raise ValueError('Invalid times')
        if duration_ms<=0 or position_ms<0 or season<0 or episode<0:raise ValueError('Invalid times')
        position_ms=min(position_ms,duration_ms)
        value={'percent':position_ms/duration_ms*100,'position':position_ms/1000,'duration':duration_ms/1000,'updatedAt':now_ms(),'season':season,'episode':episode}
        pending=self.local.setdefault('pending_progress',{}).setdefault(scope,{})
        pending[cloud_id]=value
        if season:pending[cloud_id+f':s{season}e{episode}']=value
        self.persist()
        if self.offline:return {'saved':True,'queued':True}
        try:self.flush_progress()
        except Exception as error:
            if not isinstance(error,RuntimeError):self.offline=True
            return {'saved':True,'queued':True}
        return {'saved':True,'queued':False}

    def settings_get(self):
        preferences=self.local.get('preferences',{})
        scope=str(self.client.session.get('userId','local'))+':'+self.client.profile
        selected=preferences.get('profile_playback',{}).get(scope,{})
        return {'scope':scope,**{key:selected.get(key,preferences.get(key,default)) for key,default in {'audio':'es','sub':'es','autoplay':True,'retry':True,'limit':0,'language':'es','download_limit':0,'source_limit':7}.items()}}

    def settings_set(self,key,value,scope):
        import math
        if scope!=str(self.client.session.get('userId','local'))+':'+self.client.profile:raise ValueError('Profile changed')
        if key in ('autoplay','retry'):
            if not isinstance(value,bool):raise ValueError('Invalid switch')
        elif key=='source_limit':
            if isinstance(value,bool) or not isinstance(value,int) or not 1<=value<=500:raise ValueError('Invalid source limit')
        elif key in ('limit','download_limit'):
            if isinstance(value,bool) or not isinstance(value,(int,float)) or not math.isfinite(value) or not 0<=value<=500:raise ValueError('Invalid limit')
        elif key in ('audio','language'):
            if value not in ('es','en'):raise ValueError('Invalid language')
        elif key=='sub':
            if value not in ('es','en','off'):raise ValueError('Invalid subtitle')
        else:raise ValueError('Unknown setting')
        preferences=self.local.setdefault('preferences',{})
        if key in ('audio','sub'):
            scope=str(self.client.session.get('userId','local'))+':'+self.client.profile
            preferences.setdefault('profile_playback',{}).setdefault(scope,{})[key]=value
        else:preferences[key]=value
        self.persist()
        return self.settings_get()

    def next_episode(self,cloud_id,season,episode):
        item=self.client.by_id(cloud_id)
        if item['type']!='series':return None
        if not all(isinstance(v,int) and not isinstance(v,bool) and v>=0 for v in (season,episode)):raise ValueError('Invalid episode')
        today=dt.date.today().isoformat()
        def available(rows,after=0):
            return sorted((r for r in rows if r.get('episode_number',0)>after and r.get('air_date') and r['air_date']<=today),key=lambda r:r['episode_number'])
        rows=available(self.client.episodes(item,season),episode)
        if rows:return {'season':season,'episode':rows[0]['episode_number']}
        seasons=sorted(s['season_number'] for s in self.client.details(item).get('seasons',[]) if s.get('season_number',0)>season)
        if seasons:
            rows=available(self.client.episodes(item,seasons[0]))
            if rows:return {'season':seasons[0],'episode':rows[0]['episode_number']}
        return None

    def flush_progress(self):
        scope=str(self.client.session.get('userId','local'))+':'+self.client.profile
        pending=self.local.get('pending_progress',{}).get(scope,{})
        if not pending:return
        state=self.client.state();progress=state.setdefault('progress',{})
        for key,value in pending.items():
            if progress.get(key,{}).get('updatedAt',0)<=value['updatedAt']:progress[key]=value
        self.client.save_state(state)
        self.local['pending_progress'].pop(scope,None)
        self.persist()
