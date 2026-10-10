import unittest
from urllib.parse import urlsplit,parse_qs
from brunio.core import Client,StaleWork,filter_streams,release_year,stream_size
from brunio.updater import validate,version

SESSION={'accessToken':'fixture-access','refreshToken':'fixture-refresh','userId':'owner','email':'fixture@example.test'}
class ProtocolTests(unittest.TestCase):
    def make(self,response=None):
        calls=[]
        def http(url,method='GET',headers=None,body=None,**kwargs):
            calls.append((url,method,headers,body));return response(url,method,body) if response else None
        c=Client(http);c.session=SESSION.copy();return c,calls
    def test_pc_host_uses_exact_tv_state_and_milliseconds(self):
        c,calls=self.make();c.party={'room_code':'123456','host_user_id':'owner','state':{}}
        state={'cloudId':'tmdb:series:14','season':2,'episode':3,'positionMs':723000,'playing':False,'updatedAt':1,'liveChannelId':'','liveProvider':'','liveType':'tv','liveTitle':''}
        c.host_update(state);url,method,headers,body=calls[-1]
        self.assertEqual(method,'PATCH');self.assertEqual(body['state'],state)
        self.assertEqual(body['state']['positionMs'],723000);self.assertEqual(headers['Authorization'],'Bearer fixture-access')
    def test_pc_guest_pause_and_resume_match_tv_rpc(self):
        c,calls=self.make();c.party={'room_code':'123456','host_user_id':'other','state':{}}
        for playing in [False,True]:
            c.request_playback({'cloudId':'tmdb:movie:44'},0,0,playing)
            self.assertTrue(calls[-1][0].endswith('/rpc/miflix_request_playback'))
            self.assertEqual(calls[-1][3],{'code':'123456','media':'tmdb:movie:44','s':0,'e':0,'playing':playing})
    def test_request_access_does_not_join_without_approval(self):
        c,calls=self.make(lambda u,m,b:'pending');self.assertEqual(c.party_join('123456'),'pending')
        self.assertIsNone(c.party);self.assertEqual(c.pending_room,'123456');self.assertEqual(len(calls),1)
    def test_approved_join_loads_tv_room(self):
        def http(url,method,body):
            if 'miflix_request_access' in url:return 'approved'
            if 'miflix_watch_parties?' in url:return [{'room_code':'123456','host_user_id':'tv-host','state':{'cloudId':'tmdb:movie:11','positionMs':55000,'playing':True}}]
        c,calls=self.make(http);self.assertEqual(c.party_join('123456'),'approved');self.assertEqual(c.party['state']['positionMs'],55000)
        self.assertTrue(any('/rpc/miflix_join_room' in x[0] for x in calls))
    def test_leaving_guest_clears_local_room_before_network(self):
        c,calls=self.make();c.party={'room_code':'123456','host_user_id':'tv-host'}
        def failing(url,*args,**kwargs):self.assertIsNone(c.party);raise RuntimeError('Offline')
        c.transport=failing
        with self.assertRaises(RuntimeError):c.party_leave()
        self.assertIsNone(c.party);self.assertEqual(c.epoch,1)
    def test_late_join_cannot_restore_departed_room(self):
        c,calls=self.make()
        def http(url,method='GET',headers=None,body=None,**kwargs):
            c.detach();return 'approved'
        c.transport=http
        with self.assertRaises(StaleWork):c.party_join('123456')
        self.assertIsNone(c.party)
    def test_profile_state_uses_seconds_and_preserves_other_fields(self):
        c,calls=self.make();state={'favorites':['tmdb:movie:44'],'progress':{'tmdb:movie:44':{'position':723.0,'duration':7200.0,'percent':10}}}
        c.save_state(state);self.assertEqual(calls[-1][3]['state'],state);self.assertEqual(calls[-1][3]['profile_id'],'default')
    def test_private_provider_is_not_bundled(self):
        c=Client();self.assertEqual(c.manifests,[]);self.assertEqual(c.token,'')
    def test_refreshes_expired_token_then_retries_same_request(self):
        c,calls=self.make();count=0
        def http(url,method='GET',headers=None,body=None,**kwargs):
            nonlocal count
            if '/auth/v1/token?' in url:return {'access_token':'new-access','refresh_token':'new-refresh'}
            count+=1
            if count==1:raise RuntimeError('HTTP 401: request failed')
            self.assertEqual(headers['Authorization'],'Bearer new-access');return []
        c.transport=http;self.assertEqual(c.rest('fixture'),[]);self.assertEqual(count,2)

class StreamTests(unittest.TestCase):
    def stream(self,name,filename='',size=0):return {'name':'Torrentio','title':name,'url':'https://example.test/'+name,'behaviorHints':{'filename':filename,'videoSize':size}}
    def test_year_size_order_download_filters(self):
        item={'type':'movie','title':'Dune','year':'2021'}
        wrong=self.stream('1080p wrong','Dune.1984.mkv',10**9);big=self.stream('2160p big','Dune.2021.mkv',9*10**9);ok=self.stream('1080p okay','Dune.2021.mkv',4*10**9);download=self.stream('TB Download','Dune.2021.mkv',10**9)
        self.assertEqual(filter_streams([wrong,big,ok,download],item,5),[ok])
        self.assertEqual(filter_streams([ok,big],item),[big,ok])
    def test_numeric_movie_title_is_not_release_year(self):
        self.assertIsNone(release_year('1917.1080p.mkv','1917'));self.assertEqual(release_year('Blade.Runner.2049.2017.1080p','Blade Runner 2049'),2017)
    def test_series_year_remains_unfiltered(self):self.assertEqual(len(filter_streams([self.stream('Show.2024','Show.2024.mkv')],{'type':'series','title':'Show','year':'2020'})),1)
    def test_gib_and_unknown_size(self):
        self.assertEqual(stream_size(self.stream('1 GiB')),2**30);self.assertEqual(filter_streams([self.stream('unknown')],{'type':'movie'},5),[])

class UpdateTests(unittest.TestCase):
    def test_versions_numeric_and_stable_order(self):
        self.assertGreater(version('1.0.0-rc10'),version('1.0.0-rc2'));self.assertGreater(version('1.0.0'),version('1.0.0-rc999'))
    def test_rejects_tv_and_off_repository_installers(self):
        from brunio.core import REPO
        info={'channel':'pc-windows-x64','version':'1.0.0-rc2','sizeBytes':500,'sha256':'a'*64,'downloadUrl':f'https://github.com/{REPO}/releases/download/pc-v1.0.0-rc2/BruniO-PC-Setup-1.0.0-rc2.exe'}
        self.assertEqual(validate(info),info)
        for url in [info['downloadUrl'].replace(REPO,'attacker/app'),info['downloadUrl'].replace('https:','http:'),info['downloadUrl'].replace('pc-v','native-v')]:
            with self.assertRaises(ValueError):validate({**info,'downloadUrl':url})
        with self.assertRaises(ValueError):validate({**info,'channel':'tv'})

class PairingTests(unittest.TestCase):
    def test_qr_encryption_matches_tv_aes_gcm_format(self):
        import json
        from brunio.pairing import Pairing,encode
        from cryptography.hazmat.primitives.ciphers.aead import AESGCM
        def transport(*args,**kwargs):return [row]
        p=Pairing(transport);iv=bytes(range(12));payload={'accessToken':'fixture','userId':'fixture','addonManifest':'https://example.test/manifest.json'}
        row={'status':'approved','iv':encode(iv),'payload_enc':encode(AESGCM(p.key).encrypt(iv,json.dumps(payload).encode(),None))}
        self.assertEqual(p.poll(),payload);self.assertIn('#key=',p.url);self.assertNotIn('fixture',p.url)
