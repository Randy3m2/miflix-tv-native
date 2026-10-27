import importlib.util
import unittest
from pathlib import Path
from unittest.mock import Mock

spec=importlib.util.spec_from_file_location('bridge_screens_test',Path(__file__).resolve().parents[1]/'backend'/'bridge.py')
module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module)

class MemoryVault:
    def save(self,value): self.saved=value

    def load(self):return {'session':{'userId':'owner','accessToken':'private'},'profile':'kids'}

class ScreenTests(unittest.TestCase):
    def setUp(self):
        self.client=Mock();self.bridge=module.Bridge(self.client,MemoryVault())
        self.client.by_id.side_effect=lambda key:{'cloudId':key,'type':'movie','id':1,'title':key}
    def call(self,method,**params):return self.bridge.dispatch({'id':7,'method':method,'params':params})
    def test_continue_uses_tv_threshold_and_hides_finished(self):
        self.client.state.return_value={'progress':{'tmdb:movie:1':{'position':5,'percent':1},'tmdb:movie:2':{'position':4,'percent':1},'tmdb:movie:3':{'position':30,'percent':90}}}
        rows=self.call('catalog.continue')['result']
        self.assertEqual([r['cloudId'] for r in rows],['tmdb:movie:1'])
    def test_acceptance_is_scoped_to_selected_profile_identity(self):
        self.client.social_identity.return_value='kids-social-id'
        self.call('friends.accept',sender_id='sender-id')
        path,method,body=self.client.social_rest.call_args.args
        self.assertIn('receiver_id=eq.kids-social-id',path);self.assertIn('status=eq.pending',path)
        self.assertEqual((method,body),('PATCH',{'status':'accepted'}))
    def test_invalid_month_cannot_reach_server(self):
        self.assertIn('error',self.call('calendar.list',month='2026-99'))
        self.client.shared_calendar.assert_not_called()
    def test_calendar_uses_shared_tv_calendar(self):
        self.client.shared_calendar.return_value=[{'starts_at':'2026-10-17T12:00:00Z','title':'TV event'}]
        self.assertEqual(self.call('calendar.list',month='2026-10')['result'][0]['title'],'TV event')
    def test_live_listing_keeps_urls_private(self):
        rows=self.call('live.list',provider='tv')['result']
        self.assertGreater(len(rows),0)
        self.assertTrue(all('url' not in row for row in rows))
    def test_unknown_live_channel_cannot_resolve_arbitrary_url(self):
        self.assertIn('error',self.call('live.sources',provider='tv',channel_id='http://anything'))
        self.client.transport.assert_not_called()
    def test_favorite_preserves_other_profile_fields(self):
        self.client.state.return_value={'favorites':[],'progress':{'example':12},'custom':'retained'}
        self.call('catalog.favorite',cloud_id='tmdb:movie:1',enabled=True)
        state=self.client.save_state.call_args.args[0]
        self.assertEqual(state['custom'],'retained');self.assertEqual(state['progress'],{'example':12})
    def test_logged_out_screen_request_is_rejected(self):
        self.client.session={}
        self.assertIn('error',self.call('friends.list'))
        self.client.social_rpc.assert_not_called()
    def test_discovery_is_limited_to_known_platforms(self):
        self.assertIn('error',self.call('catalog.discover',provider=999999))
        self.client.catalog.assert_not_called()

if __name__=='__main__':unittest.main()
