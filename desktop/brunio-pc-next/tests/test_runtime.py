import importlib.util
import unittest
from pathlib import Path
from unittest.mock import Mock
spec=importlib.util.spec_from_file_location('runtime_bridge_test',Path(__file__).resolve().parents[1]/'backend'/'bridge.py')
module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module)
class Vault:
    def load(self):return {'session':{'userId':'user'},'profile':'kids','preferences':{'profile_playback':{'user:kids':{'audio':'en','addon_order_en':['comet-first']}}}}
class RuntimeTests(unittest.TestCase):
    def setUp(self):
        self.client=Mock();self.bridge=module.Bridge(self.client,Vault())
        self.client.by_id.return_value={'type':'series','cloudId':'tmdb:series:1','id':1}
        self.client.state.return_value={}
    def request(self,method,**params):return self.bridge.dispatch({'id':1,'method':method,'params':params})
    def test_progress_uses_seconds_and_preserves_state(self):
        self.client.state.return_value={'favorites':['retained'],'progress':{}}
        result=self.request('playback.progress',cloud_id='tmdb:series:1',scope='user:kids',position_ms=65000,duration_ms=100000,season=2,episode=3)
        self.assertIn('result',result)
        state=self.client.save_state.call_args.args[0];p=state['progress']['tmdb:series:1']
        self.assertEqual(p['position'],65);self.assertEqual(p['duration'],100);self.assertEqual(p['percent'],65)
        self.assertEqual(state['progress']['tmdb:series:1:s2e3'],p);self.assertEqual(state['favorites'],['retained'])
    def test_stale_player_cannot_write_into_another_profile(self):
        self.assertIn('error',self.request('playback.progress',cloud_id='tmdb:series:1',scope='user:other',position_ms=10,duration_ms=100))
        self.client.save_state.assert_not_called()
    def test_invalid_progress_cannot_write(self):
        self.assertIn('error',self.request('playback.progress',cloud_id='tmdb:series:1',scope='user:kids',position_ms=-1,duration_ms=100))
        self.client.save_state.assert_not_called()
    def test_settings_do_not_cross_profile(self):
        self.assertIn('error',self.request('settings.set',key='audio',value='es',scope='user:other'))
        self.assertEqual(self.bridge.local['preferences']['profile_playback']['user:kids']['audio'],'en')
    def test_setting_rejects_nonfinite_file_limit(self):
        for value in (float('nan'),float('inf'),True,-1,501):
            self.assertIn('error',self.request('settings.set',key='limit',value=value,scope='user:kids'))
    def test_sources_use_saved_profile_language_and_order(self):
        self.client.streams.return_value=([], 'tt1')
        self.assertIn('result',self.request('catalog.sources',cloud_id='tmdb:series:1',season=1,episode=1))
        self.assertEqual(self.client.source_audio,'en');self.assertEqual(self.client.source_order,['comet-first'])
    def test_setting_updates_only_selected_audio_preference(self):
        self.assertIn('result',self.request('settings.set',key='audio',value='es',scope='user:kids'))
        self.assertEqual(self.bridge.local['preferences']['profile_playback']['user:kids']['audio'],'es')
    def test_next_episode_does_not_autoplay_future_release(self):
        self.client.episodes.return_value=[{'episode_number':2,'air_date':'2099-01-01'}]
        self.client.details.return_value={'seasons':[]}
        self.assertIsNone(self.request('catalog.next',cloud_id='tmdb:series:1',season=1,episode=1)['result'])
    def test_next_episode_can_cross_seasons(self):
        self.client.episodes.side_effect=[[],[{'episode_number':1,'air_date':'2020-01-01'}]]
        self.client.details.return_value={'seasons':[{'season_number':2}]}
        self.assertEqual(self.request('catalog.next',cloud_id='tmdb:series:1',season=1,episode=10)['result'],{'season':2,'episode':1})
    def test_resume_is_episode_specific_and_skips_completed(self):
        self.client.streams.return_value=([], 'tt1')
        self.client.state.return_value={'progress':{'tmdb:series:1:s1e2':{'position':44,'percent':20},'tmdb:series:1':{'position':999,'percent':1}}}
        self.assertEqual(self.request('catalog.sources',cloud_id='tmdb:series:1',season=1,episode=2)['result']['resume_ms'],44000)
        self.client.state.return_value={'progress':{'tmdb:series:1:s1e2':{'position':44,'percent':95}}}
        self.assertEqual(self.request('catalog.sources',cloud_id='tmdb:series:1',season=1,episode=2)['result']['resume_ms'],0)
if __name__=='__main__':unittest.main()
