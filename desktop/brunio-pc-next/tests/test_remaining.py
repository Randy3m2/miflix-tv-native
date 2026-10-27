import importlib.util,json,tempfile,unittest
from pathlib import Path
from unittest.mock import Mock,patch
spec=importlib.util.spec_from_file_location('remaining_bridge_test',Path(__file__).resolve().parents[1]/'backend'/'bridge.py')
module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module)
from brunio.core import addon_id
from brunio.downloads import DownloadStore
import remaining
class Vault:
    def save(self,value): self.saved=value

    def load(self):return {'session':{'userId':'user'},'profile':'kids'}
class RemainingTests(unittest.TestCase):
    def setUp(self):
        self.client=Mock();self.client.account={};self.client.party=None;self.client.pending_room=None
        self.bridge=module.Bridge(self.client,Vault())
    def call(self,method,**params):return self.bridge.dispatch({'id':1,'method':method,'params':params})
    def test_addon_view_never_exposes_private_manifest(self):
        self.client.manifests=['https://addon.example/private-secret/manifest.json']
        result=self.call('addons.list')
        self.assertIn('result',result);self.assertNotIn('private-secret',json.dumps(result))
    def test_addon_order_must_include_all_installed_items(self):
        self.client.manifests=['https://a.example/manifest.json','https://b.example/manifest.json']
        self.assertIn('error',self.call('addons.order',ids=[addon_id(self.client.manifests[0])],language='es',scope='user:kids'))
        self.assertNotIn('preferences',self.bridge.local)
    def test_addon_priority_is_per_profile_and_audio(self):
        self.client.manifests=['https://a.example/manifest.json']
        order=[addon_id(self.client.manifests[0])]
        self.assertIn('result',self.call('addons.order',ids=order,language='en',scope='user:kids'))
        self.assertEqual(self.bridge.local['preferences']['profile_playback']['user:kids']['addon_order_en'],order)
    def test_chat_rejects_stale_room_without_writing(self):
        self.client.party={'room_code':'123456'}
        self.assertIn('error',self.call('party.send',scope='user:kids',room='999999',kind='chat',body='Hello'))
        self.client.social_rest.assert_not_called()
    def test_chat_uses_tv_event_schema_and_profile_identity(self):
        self.client.party={'room_code':'123456'};self.client.social_identity.return_value='profile-identity'
        self.assertIn('result',self.call('party.send',scope='user:kids',room='123456',kind='emoji',body='🔥'))
        self.assertEqual(self.client.social_rest.call_args.args,('rp1_miflix_party_events','POST',{'room_code':'123456','user_id':'profile-identity','kind':'emoji','body':'🔥'}))
    def test_pending_party_request_does_not_force_join(self):
        self.client.party_join.return_value='pending';self.client.pending_room='123456'
        result=self.call('party.join',code='123456')['result']
        self.assertEqual(result['decision'],'pending');self.assertIsNone(result['room'])
    def test_offline_file_cannot_be_opened_by_another_profile(self):
        with tempfile.TemporaryDirectory() as root:
            self.bridge._download_store=DownloadStore(root)
            item={'id':1,'cloudId':'tmdb:movie:1','title':'Movie','type':'movie'}
            row,_=self.bridge._download_store.create('user:other',item,0,0,'.mp4')
            self.assertIn('error',self.call('downloads.open',id=row['id'],scope='user:kids'))
    def test_download_store_does_not_keep_source_url(self):
        with tempfile.TemporaryDirectory() as root:
            self.bridge._download_store=DownloadStore(root)
            item={'id':1,'cloudId':'tmdb:movie:1','title':'Movie','type':'movie'}
            self.bridge._download_store.create('user:kids',item,0,0,'.mp4')
            result=self.call('downloads.list')['result']
            self.assertEqual(len(result['files']),1);self.assertNotIn('url',result['files'][0])
    def test_updates_never_offer_legacy_version_as_upgrade(self):
        with patch.object(remaining.updater,'check',return_value={'version':'1.0.0-rc99'}):
            self.assertIsNone(self.call('updates.check')['result']['available'])
    def test_update_prepare_requires_trusted_check(self):
        with patch.object(remaining.updater,'download') as download:
            self.assertIn('error',self.call('updates.prepare'));download.assert_not_called()
    def test_installer_changed_after_verification_is_rejected(self):
        with tempfile.TemporaryDirectory() as root:
            target=Path(root)/'installer.exe';target.write_bytes(b'MZchanged')
            self.bridge._update_path=target;self.bridge._update_info={'sizeBytes':3,'sha256':'0'*64}
            with patch.object(remaining.os,'name','nt'),patch.object(remaining.os,'startfile',create=True) as launch:
                self.assertIn('error',self.call('updates.install'));launch.assert_not_called()
    def test_season_plan_only_contains_released_episodes(self):
        self.client.by_id.return_value={'id':1,'type':'series'}
        self.client.episodes.return_value=[{'episode_number':1,'air_date':'2020-01-01'},{'episode_number':2,'air_date':'2099-01-01'}]
        result=self.call('downloads.season',cloud_id='tmdb:series:1',scope='user:kids',season=1)['result']
        self.assertEqual(result['episodes'],[1])
if __name__=='__main__':unittest.main()
