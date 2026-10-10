"""Pure Python data/protocol layer. Party wire times are milliseconds; profile progress is seconds.
No shared provider credentials ship with the client. The TV schema is reused without migration.
"""
from __future__ import annotations
import concurrent.futures, datetime as dt, hashlib, json, os, re, threading, time, unicodedata, uuid
from pathlib import Path
from urllib.parse import urlencode, quote, urlsplit
from urllib.request import Request, urlopen
from urllib.error import HTTPError

SUPABASE_URL = 'https://iwvhigqxsvvgyzdygiqa.supabase.co'
SUPABASE_KEY = 'sb_publishable_TdzEQXtDadVMKhSLVe0ihw_EAQ4iNsd'
REPO = 'randy3m2/miflix-tv-native'
LIVE = {'channels':'https://stremio-addon-wheat.vercel.app/manifest.json','sports':'https://sportsfree-us2.highfly.to/manifest.json'}
def now_ms(): return int(time.time()*1000)
def iso(): return dt.datetime.now(dt.timezone.utc).isoformat()
def valid_url(value):
    u=urlsplit(value)
    return u.scheme in ('https','http') and bool(u.hostname) and not u.username and not u.password

def get_json(url, method='GET', headers=None, body=None, timeout=20):
    if not valid_url(url): raise ValueError('Invalid HTTP address')
    data=None if body is None else json.dumps(body).encode()
    h={'Accept':'application/json',**(headers or {})}
    if data is not None:h['Content-Type']='application/json'
    try:
        with urlopen(Request(url,data=data,headers=h,method=method),timeout=timeout) as response:
            raw=response.read(8*1024*1024+1)
            if len(raw)>8*1024*1024:raise ValueError('Response too large')
            return json.loads(raw) if raw.strip() else None
    except HTTPError as e:
        # Do not display request URLs: add-on URLs may contain account credentials.
        raise RuntimeError(f'HTTP {e.code}: request failed') from None

def normalize(text):
    return ''.join(c for c in unicodedata.normalize('NFD',str(text).lower()) if not unicodedata.combining(c)).strip()
def media(row, kind=None):
    kind=kind or ('series' if row.get('media_type')=='tv' else 'movie')
    return {'id':int(row['id']),'type':kind,'cloudId':f"tmdb:{kind}:{row['id']}",
            'title':row.get('title') or row.get('name') or 'Untitled','overview':row.get('overview',''),
            'year':(row.get('release_date') or row.get('first_air_date') or '')[:4],
            'poster':'https://image.tmdb.org/t/p/w342'+row['poster_path'] if row.get('poster_path') else '',
            'backdrop':'https://image.tmdb.org/t/p/w1280'+row['backdrop_path'] if row.get('backdrop_path') else '',
            'rating':row.get('vote_average',0)}
def stream_size(stream):
    hints=stream.get('behaviorHints') or {}
    if hints.get('videoSize',0)>0:return int(hints['videoSize'])
    m=re.search(r'(?<![\d.])(\d+(?:[.,]\d+)?)\s*(GiB|GB|MiB|MB|TiB|TB)\b',stream.get('title','')+' '+stream.get('name','')+' '+hints.get('filename',''),re.I)
    if not m:return 0
    return int(float(m[1].replace(',','.'))*{'gb':10**9,'gib':2**30,'mb':10**6,'mib':2**20,'tb':10**12,'tib':2**40}[m[2].lower()])
def stream_quality(stream):
    text=stream.get('title','')+' '+stream.get('name','')+' '+(stream.get('behaviorHints') or {}).get('filename','')
    values=[int(x) for x in re.findall(r'(?<!\d)(4320|2160|1440|1080|720|576|480|360)p?(?!\d)',text,re.I)]
    if re.search(r'\b(4k|uhd)\b',text,re.I):values.append(2160)
    if re.search(r'\b8k\b',text,re.I):values.append(4320)
    return max(values,default=0)
def release_year(text,title=''):
    text=text.split('\n')[0]
    words=re.findall(r'\w+',title)
    if words:text=re.sub(r'^\s*'+r'[\W_]+'.join(map(re.escape,words))+r'(?=[\W_]|$)','',text,flags=re.I)
    text=re.split(r'\b(?:4320|2160|1440|1080|720|480)p?\b|\b(?:WEB[- .]?DL|BluRay|BDRip|REMUX|HDTV|x26[45]|Remaster(?:ed)?)\b',text,flags=re.I)[0]
    matches=[int(m[1]) for m in re.finditer(r'(?<![\w])((?:18|19|20)\d{2})(?![A-Za-z\d])',text.replace('_','.')) if not re.match(r'\s*(?:[xX×]\d|(?:GB|GiB|MB|MiB|TB|TiB)\b)',text[m.end():],re.I)]
    return matches[-1] if matches else None

def filter_streams(rows, item, limit_gb=0):
    kept={}
    for s in rows:
        if not valid_url(s.get('url','')):continue
        if re.search(r'\bTB[\W_]*Download\b',s.get('name','')+' '+s.get('title',''),re.I):continue
        size=stream_size(s)
        if limit_gb and (size<=0 or size>limit_gb*10**9):continue
        actual=release_year((s.get('behaviorHints') or {}).get('filename',''),item.get('title','')) or release_year(s.get('title',''),item.get('title',''))
        if item['type']=='movie' and item.get('year') and actual and str(actual)!=str(item['year']):continue
        kept.setdefault(s['url'],s)
    return sorted(kept.values(),key=lambda x:(stream_quality(x),stream_size(x)),reverse=True)

class StaleWork(RuntimeError):pass
class Client:
    def __init__(self, transport=get_json):
        self.transport=transport;self.session={};self.account={};self.profile='default';self.epoch=0
        self.party=None;self.pending_room=None;self.token='';self.language='en-US';self.manifests=[]
        self.lock=threading.RLock();self.refresh_lock=threading.Lock()
    def context(self):return self.epoch,self.session.get('userId'),self.profile
    def check(self,context):
        if context!=self.context():raise StaleWork('Session changed')
    def detach(self):
        with self.lock:self.epoch+=1;self.party=None;self.pending_room=None
    def rest(self,path,method='GET',body=None,prefer='return=minimal'):
        context=self.context();session=dict(self.session)
        if not session:raise ValueError('Sign in first')
        def request():return self.transport(SUPABASE_URL+'/rest/v1/'+path,method,{'apikey':SUPABASE_KEY,'Authorization':'Bearer '+self.session['accessToken'],'Prefer':prefer},body)
        try:result=request()
        except RuntimeError as e:
            if not str(e).startswith('HTTP 401'):raise
            with self.refresh_lock:
                self.check(context)
                if self.session['accessToken']==session['accessToken']:
                    j=self.transport(SUPABASE_URL+'/auth/v1/token?grant_type=refresh_token','POST',{'apikey':SUPABASE_KEY},{'refresh_token':session['refreshToken']})
                    self.check(context);self.session.update(accessToken=j['access_token'],refreshToken=j['refresh_token'])
            result=request()
        self.check(context);return result
    def login(self,email,password):
        self.detach();self.session={};self.account={};self.manifests=[];self.token=''
        context=self.context()
        j=self.transport(SUPABASE_URL+'/auth/v1/token?grant_type=password','POST',{'apikey':SUPABASE_KEY},{'email':email,'password':password})
        self.check(context)
        self.session={'accessToken':j['access_token'],'refreshToken':j['refresh_token'],'userId':j['user']['id'],'email':j['user'].get('email',email)}
        return self.load_account()
    def load_account(self):
        rows=self.rest('miflix_user_state?'+urlencode({'user_id':'eq.'+self.session['userId'],'profile_id':'eq.__account__','select':'state'})) or []
        self.account=rows[0]['state'] if rows else {'profiles':[{'id':'default','name':'Main','primary':True}],'privateSetup':{}}
        setup=self.account.get('privateSetup',{});self.token=setup.get('tmdbToken','')
        self.manifests=list(dict.fromkeys(x for x in setup.get('addonManifests',[])+[setup.get('torrentioManifest','')] if x))
        profiles=self.account.get('profiles') or [{'id':'default','name':'Main','primary':True}]
        self.account['profiles']=profiles
        if self.profile not in [p['id'] for p in profiles]:self.profile=profiles[0]['id']
        return self.account
    def save_account(self):
        # Re-read first to preserve remote settings changed on TV.
        return self.rest('miflix_user_state?on_conflict=user_id,profile_id','POST',{'user_id':self.session['userId'],'profile_id':'__account__','state':self.account,'updated_at':iso()},'resolution=merge-duplicates,return=minimal')
    def setup(self,token,manifests):
        self.load_account();self.account['privateSetup']={'tmdbToken':token,'addonManifests':manifests,'torrentioManifest':next(iter(manifests),'')};self.save_account();self.token=token;self.manifests=manifests
    def profiles(self,profiles):
        self.load_account();self.account['profiles']=profiles;self.save_account();return profiles
    def state(self):
        rows=self.rest('miflix_user_state?'+urlencode({'user_id':'eq.'+self.session['userId'],'profile_id':'eq.'+self.profile,'select':'state'})) or []
        return rows[0]['state'] if rows else {'favorites':[],'progress':{}}
    def save_state(self,state):
        self.rest('miflix_user_state?on_conflict=user_id,profile_id','POST',{'user_id':self.session['userId'],'profile_id':self.profile,'state':state,'updated_at':iso()},'resolution=merge-duplicates,return=minimal')
    def tmdb(self,path,params=None):
        if not self.token:raise ValueError('Configure your TMDB token in Settings')
        q={'language':self.language,**(params or {})};headers={}
        if self.token.startswith('eyJ'):headers['Authorization']='Bearer '+self.token
        else:q['api_key']=self.token
        return self.transport('https://api.themoviedb.org/3'+path+'?'+urlencode(q),headers=headers)
    def catalog(self,path,kind=None,params=None):
        return [media(x,kind) for x in self.tmdb(path,params).get('results',[]) if x.get('media_type','movie')!='person']
    def search(self,query):
        from difflib import SequenceMatcher
        query=normalize(query);rows=self.catalog('/search/multi',params={'query':query})
        if len(rows)<5:
            words=query.split()
            variants=list(dict.fromkeys([' '.join(words[:-1]),' '.join(words[1:])])) if len(words)>1 else []
            for variant in variants:
                if len(variant)<3:continue
                for item in self.catalog('/search/multi',params={'query':variant}):
                    title=normalize(item['title'])
                    if SequenceMatcher(None,title,query).ratio()>=0.55 or any(w in title for w in words):rows.append(item)
        unique={x['cloudId']:x for x in rows}
        return sorted(unique.values(),key=lambda x:SequenceMatcher(None,normalize(x['title']),query).ratio(),reverse=True)[:60]
    def by_id(self,cloud):
        m=re.fullmatch(r'tmdb:(movie|series):(\d+)',cloud)
        if not m:raise ValueError('Unknown content ID')
        return media(self.tmdb('/'+('tv' if m[1]=='series' else 'movie')+'/'+m[2]),m[1])
    def details(self,item):return self.tmdb('/'+('tv' if item['type']=='series' else 'movie')+'/'+str(item['id']),{'append_to_response':'external_ids,credits,videos'})
    def episodes(self,item,season):return self.tmdb(f"/tv/{item['id']}/season/{season}").get('episodes',[])
    def streams(self,item,season=0,episode=0,limit=0):
        d=self.details(item);imdb=d.get('imdb_id') or d.get('external_ids',{}).get('imdb_id')
        if not imdb:raise ValueError('IMDb ID unavailable')
        if not self.manifests:raise ValueError('Add your own Torrentio or Comet in Settings')
        vid=imdb+(f':{season}:{episode}' if item['type']=='series' else '')
        def fetch(manifest):
            try:return self.transport(manifest.removesuffix('/manifest.json')+f"/stream/{item['type']}/{vid}.json").get('streams',[])
            except Exception:return []
        with concurrent.futures.ThreadPoolExecutor(max_workers=4) as pool:rows=sum(list(pool.map(fetch,list(self.manifests))),[])
        return filter_streams(rows,item,limit),imdb
    def rpc(self,name,body):return self.rest('rpc/'+name,'POST',body)
    def party_read(self,room):
        rows=self.rest('miflix_watch_parties?'+urlencode({'room_code':'eq.'+room,'select':'room_code,host_user_id,state,expires_at'})) or []
        return rows[0] if rows else None
    def party_create(self):
        import secrets
        room=str(secrets.randbelow(900000)+100000);context=self.context()
        p={'room_code':room,'host_user_id':self.session['userId'],'state':{'cloudId':'','season':0,'episode':0,'positionMs':0,'playing':False,'updatedAt':now_ms(),'liveChannelId':'','liveProvider':'','liveTitle':'','liveType':'tv'},'updated_at':iso(),'expires_at':(dt.datetime.now(dt.timezone.utc)+dt.timedelta(hours=8)).isoformat()}
        self.rest('miflix_watch_parties','POST',p);self.rpc('miflix_join_room',{'code':room});self.check(context);self.epoch+=1;self.party=p;return p
    def party_join(self,room):
        if not re.fullmatch(r'\d{6}',room):raise ValueError('Enter the six-digit room code')
        if self.party and self.party['room_code']!=room:raise ValueError('Leave your current Party first')
        context=self.context();decision=self.rpc('miflix_request_access',{'code':room});self.check(context)
        if decision!='approved':self.pending_room=room if decision=='pending' else None;return decision
        self.rpc('miflix_join_room',{'code':room});p=self.party_read(room);self.check(context)
        if not p:raise ValueError('Room closed or expired')
        self.epoch+=1;self.party=p;self.pending_room=None;return 'approved'
    def party_leave(self,snapshot=None):
        if snapshot is None:p=self.party;session=dict(self.session);self.detach()
        else:p,session=snapshot
        if not p or not session:return
        h={'apikey':SUPABASE_KEY,'Authorization':'Bearer '+session['accessToken']}
        if p['host_user_id']==session['userId']:
            self.transport(SUPABASE_URL+'/rest/v1/miflix_watch_parties?'+urlencode({'room_code':'eq.'+p['room_code']}),'DELETE',h)
        self.transport(SUPABASE_URL+'/rest/v1/miflix_party_members?'+urlencode({'room_code':'eq.'+p['room_code'],'user_id':'eq.'+session['userId']}),'DELETE',h)
    def host(self):return bool(self.party and self.party['host_user_id']==self.session.get('userId'))
    def host_update(self,state):
        if not self.host():return
        p=self.party;context=self.context()
        self.rest('miflix_watch_parties?'+urlencode({'room_code':'eq.'+p['room_code']}),'PATCH',{'state':state,'updated_at':iso(),'expires_at':(dt.datetime.now(dt.timezone.utc)+dt.timedelta(hours=8)).isoformat()})
        self.check(context)
        if self.party and self.party['room_code']==p['room_code']:self.party['state']=state
    def request_playback(self,item,season,episode,playing):
        if self.party:self.rpc('miflix_request_playback',{'code':self.party['room_code'],'media':item['cloudId'],'s':season,'e':episode,'playing':playing})
    def presence(self,item=None,season=0,episode=0):
        self.rpc('miflix_set_presence',{'title':item['title'] if item else '', 'media':item['cloudId'] if item else '', 's':season,'e':episode,'room':self.party['room_code'] if self.party else None})
    def trakt(self,action,**fields):
        return self.transport(SUPABASE_URL+'/functions/v1/miflix-trakt','POST',{'apikey':SUPABASE_KEY,'Authorization':'Bearer '+self.session['accessToken']},{'action':action,**fields})
