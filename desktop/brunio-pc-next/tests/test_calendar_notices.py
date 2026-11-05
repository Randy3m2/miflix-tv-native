import importlib.util,unittest
from pathlib import Path
from unittest.mock import Mock,patch
spec=importlib.util.spec_from_file_location('calendar_notice_bridge',Path(__file__).resolve().parents[1]/'backend'/'bridge.py')
module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module)
import remaining
class Vault:
    def load(self):return {'session':{'userId':'user'},'profile':'kids'}
    def save(self,value):pass
class CalendarNoticeTests(unittest.TestCase):
    def setUp(self):
        self.client=Mock();self.client.account={};self.bridge=module.Bridge(self.client,Vault())
    def call(self,method,**params):return self.bridge.dispatch({'id':1,'method':method,'params':params})
    def test_subscription_keeps_tv_timestamp_monotonic(self):
        self.client.calendar_subscriptions.return_value={'NBA:*':{'enabled':True,'at':9999999999999}}
        self.assertIn('result',self.call('calendar.subscribe',scope='user:kids',key='NBA:*',enabled=False))
        change=next(call.args[0] for call in self.client.calendar_subscriptions.call_args_list if call.args)
        self.assertEqual(change,{'NBA:*':{'enabled':False,'at':10000000000000}})
    def test_subscription_does_not_cross_profile(self):
        self.assertIn('error',self.call('calendar.subscribe',scope='user:other',key='NBA:*',enabled=True))
        self.client.calendar_subscriptions.assert_not_called()
    def test_calendar_only_fetches_followed_leagues_and_teams(self):
        self.client.calendar_subscriptions.return_value={'NBA:2':{'enabled':True},'MLB:*':{'enabled':False}}
        events=[{'league':'NBA','id':'1','title':'Match','starts':'2026-10-20T20:00:00Z','teams':['2','3']},{'league':'NBA','id':'2','title':'Other','starts':'2026-10-20T20:00:00Z','teams':['4']}]
        with patch.object(remaining,'scoreboard',return_value=events) as scores:
            result=self.call('calendar.sports',month='2026-10')['result']
            self.assertEqual([r['title'] for r in result['events']],['Match']);scores.assert_called_once_with(self.client.transport,'NBA','2026-10')
    def test_missing_sports_source_returns_partial_warning(self):
        self.client.calendar_subscriptions.return_value={'NBA:*':{'enabled':True}}
        with patch.object(remaining,'scoreboard',side_effect=RuntimeError('private-url')):
            result=self.call('calendar.sports',month='2026-10')['result']
            self.assertEqual(result['warnings'],['NBA']);self.assertNotIn('private-url',str(result))
    def test_first_release_scan_seeds_without_duplicate_notice(self):
        self.client.state.return_value={'favorites':['tmdb:series:1']}
        self.client.by_id.return_value={'title':'Show'}
        self.client.details.return_value={'last_episode_to_air':{'id':1,'air_date':'2020-01-01'}}
        result=self.call('notifications.scan',scope='user:kids',cloud_id='tmdb:series:1')['result']
        self.assertEqual(result['items'],[])
        self.client.details.return_value={'last_episode_to_air':{'id':2,'air_date':'2020-02-01'}}
        result=self.call('notifications.scan',scope='user:kids',cloud_id='tmdb:series:1')['result']
        self.assertEqual(result['items'],['Show · 2020-02-01'])
        self.assertEqual(self.call('notifications.scan',scope='user:kids',cloud_id='tmdb:series:1')['result']['items'],result['items'])
    def test_notice_scan_requires_title_in_current_list(self):
        self.client.state.return_value={'favorites':[]}
        self.assertIn('error',self.call('notifications.scan',scope='user:kids',cloud_id='tmdb:series:999'))
        self.client.by_id.assert_not_called()
    def test_clear_notices_keeps_other_profile(self):
        self.bridge.local['preferences']={'notifications':{'user:kids':['one'],'user:other':['two']}}
        self.assertEqual(self.call('notifications.clear',scope='user:kids')['result']['items'],[])
        self.assertEqual(self.bridge.local['preferences']['notifications']['user:other'],['two'])
    def test_logout_clears_credentials_even_when_party_network_fails(self):
        self.client.party={'room_code':'123456'};self.client.party_leave.side_effect=RuntimeError('offline')
        result=self.call('session.logout')
        self.assertIn('result',result);self.assertEqual(self.client.session,{});self.assertEqual(self.client.manifests,[])
if __name__=='__main__':unittest.main()
