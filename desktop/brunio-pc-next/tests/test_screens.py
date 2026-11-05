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

class SocialAndDetailTests(unittest.TestCase):
    setUp=ScreenTests.setUp
    call=ScreenTests.call
    def test_nickname_uses_selected_social_actor_and_preserves_profile(self):
        self.client.social_identity.return_value='kids-actor'
        result=self.call('friends.nickname',scope='owner:kids',nickname='  Randy_PC  ')
        self.assertEqual(result['result']['nickname'],'randy_pc')
        self.client.social_rest.assert_called_once_with('rp1_miflix_social_profiles?on_conflict=user_id','POST',{'user_id':'kids-actor','nickname':'randy_pc'},prefer='resolution=merge-duplicates,return=minimal')
    def test_stale_profile_cannot_publish_presence(self):
        self.assertIn('error',self.call('friends.presence',scope='owner:other',cloud_id='tmdb:movie:1'))
        self.client.presence.assert_not_called()
    def test_presence_clear_does_not_fetch_metadata(self):
        self.call('friends.presence',scope='owner:kids')
        self.client.by_id.assert_not_called();self.client.presence.assert_called_once_with(None,0,0)
    def test_marking_an_episode_preserves_unrelated_progress(self):
        self.client.state.return_value={'favorites':['saved'],'custom':True,'progress':{'other':{'position':12}}}
        result=self.call('catalog.mark',scope='owner:kids',cloud_id='tmdb:series:9',season=2,episode=3,watched=True)['result']
        state=self.client.save_state.call_args.args[0]
        self.assertEqual(state['progress']['other'],{'position':12});self.assertTrue(state['custom'])
        self.assertEqual(state['progress']['tmdb:series:9:s2e3']['percent'],100)
        self.assertEqual(result['key'],'tmdb:series:9:s2e3')
    def test_details_returns_scoped_saved_rating_and_progress(self):
        self.client.details.return_value={'id':1,'title':'One','release_date':'2026-01-01'}
        self.client.state.return_value={'favorites':['tmdb:movie:1'],'progress':{'tmdb:movie:1':{'position':20},'tmdb:movie:2':{'position':99}}}
        self.client.rest.return_value=[{'score':9}]
        result=self.call('catalog.details',cloud_id='tmdb:movie:1')['result']
        self.assertTrue(result['saved']);self.assertEqual(result['user_rating'],9)
        self.assertEqual(result['scope'],'owner:kids');self.assertEqual(list(result['progress']),['tmdb:movie:1'])
    def test_provider_artwork_has_real_local_or_tv_cover_source(self):
        rows=self.call('catalog.providers')['result']
        amc=next(row for row in rows if row['id']=='provider:526')
        netflix=next(row for row in rows if row['id']=='provider:8')
        self.assertEqual(amc['poster'],'asset:streaming_amcplus_rp48.png')
        self.assertTrue(netflix['poster'].endswith('/streaming/cover/netflix.png'))

if __name__=='__main__':unittest.main()
