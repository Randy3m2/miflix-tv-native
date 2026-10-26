import tempfile, threading, unittest
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from brunio.downloads import DownloadStore, file_extension
from brunio.playlist import parse_m3u
from brunio.desktop_features import scoreboard, follows
from brunio.core import Client

ITEM={'cloudId':'tmdb:movie:7','title':'Example','type':'movie','id':7,'poster':''}
class Handler(BaseHTTPRequestHandler):
    def do_GET(self):
        data=b'fixture-video-content';self.send_response(200);self.send_header('Content-Type','video/mp4');self.send_header('Content-Length',str(len(data)));self.end_headers();self.wfile.write(data)
    def log_message(self,*args):pass
class NewFeatures(unittest.TestCase):
    def test_download_storage_is_profile_scoped_and_never_stores_source_url(self):
        server=ThreadingHTTPServer(('127.0.0.1',0),Handler);thread=threading.Thread(target=server.serve_forever,daemon=True);thread.start()
        try:
            with tempfile.TemporaryDirectory() as root:
                store=DownloadStore(root);stream={'url':f'http://127.0.0.1:{server.server_port}/movie.mp4?token=private-secret'}
                entry,created=store.create('account:profile-a',ITEM,0,0,file_extension(stream));self.assertTrue(created)
                store.transfer(entry,stream);self.assertEqual(store.path(entry).read_bytes(),b'fixture-video-content')
                self.assertEqual(store.list('account:profile-a')[0]['state'],'complete');self.assertEqual(store.list('account:profile-b'),[])
                self.assertNotIn(b'private-secret',store.db.read_bytes());self.assertFalse(store.remove(entry['id'],'account:profile-b'))
        finally:server.shutdown();server.server_close();thread.join()
    def test_only_direct_video_and_m3u_parsing(self):
        self.assertIsNone(file_extension({'url':'https://example.test/live.m3u8'}));self.assertIsNone(file_extension({'url':'file:///secret.mp4'}))
        rows=parse_m3u('#EXTM3U\n#EXTINF:-1 tvg-id="one" group-title="Sports",Canal\nhttps://example.test/live.m3u8\nfile:///bad\n')
        self.assertEqual(len(rows),1);self.assertEqual(rows[0]['category'],'Sports')
    def test_f1_calendar_and_subscription(self):
        race={'MRData':{'RaceTable':{'Races':[{'round':'3','raceName':'Grand Prix','date':'2026-05-04','time':'13:00:00Z'}]}}}
        events=scoreboard(lambda url:race,'F1','2026-05');self.assertEqual(events[0]['title'],'Grand Prix');self.assertTrue(follows({'F1:*'},events[0]))
    def test_social_headers_follow_selected_profile(self):
        calls=[]
        def transport(url,method='GET',headers=None,body=None):
            calls.append((url,headers,body));return {'actor':'actor-'+body['p_profile']} if url.endswith('brunio_rp1_ensure') else []
        c=Client(transport);c.session={'userId':'account','accessToken':'token'};c.profile='one';c.social_rest('rp1_miflix_friendships');c.profile='two';c.social_rest('rp1_miflix_friendships')
        self.assertEqual([x[1]['x-brunio-profile'] for x in calls],['one','one','two','two'])
