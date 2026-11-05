import importlib.util,unittest
from pathlib import Path
from unittest.mock import Mock
spec=importlib.util.spec_from_file_location('party_bridge_test',Path(__file__).resolve().parents[1]/'backend'/'bridge.py')
module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module)
class Vault:
    def load(self):return {'session':{'userId':'owner'},'profile':'profile'}
    def save(self,value):pass
class PartySyncTests(unittest.TestCase):
    def setUp(self):
        self.client=Mock();self.client.account={};self.client.pending_room=None
        self.client.party={'room_code':'123456','state':{'cloudId':'tmdb:series:7','season':2,'episode':3,'positionMs':0}}
        self.client.party_read.side_effect=lambda room:self.client.party
        self.bridge=module.Bridge(self.client,Vault())
    def call(self,**fields):return self.bridge.dispatch({'id':1,'method':'party.tick','params':{'scope':'owner:profile','room':'123456',**fields}})
    def test_host_publishes_tv_milliseconds_without_conversion(self):
        self.client.host.return_value=True;self.client.social_rpc.return_value=None
        state={'cloudId':'tmdb:series:7','season':2,'episode':3,'positionMs':65001,'playing':True}
        self.assertIn('result',self.call(playback=state))
        sent=self.client.host_update.call_args.args[0];self.assertEqual(sent['positionMs'],65001)
        self.assertEqual(sent['cloudId'],'tmdb:series:7');self.assertIn('updatedAt',sent)
    def test_guest_requests_control_instead_of_writing_host_state(self):
        self.client.host.return_value=False
        self.assertIn('result',self.call(request=False))
        self.client.host_update.assert_not_called()
        self.client.request_playback.assert_called_once_with({'cloudId':'tmdb:series:7'},2,3,False)
    def test_stale_profile_cannot_control_party(self):
        result=self.bridge.dispatch({'id':1,'method':'party.tick','params':{'scope':'owner:other','room':'123456','request':False}})
        self.assertIn('error',result);self.client.request_playback.assert_not_called()
    def test_host_applies_tv_pause_request(self):
        self.client.host.return_value=True;self.client.social_rpc.return_value=False
        self.call(playback={'cloudId':'tmdb:movie:7','positionMs':1000,'playing':True})
        self.assertFalse(self.client.host_update.call_args.args[0]['playing'])
        self.client.social_rpc.assert_called_once_with('rp1_miflix_take_playback',{'code':'123456'})
    def test_invalid_host_position_is_rejected(self):
        self.client.host.return_value=True
        self.assertIn('error',self.call(playback={'cloudId':'tmdb:movie:7','positionMs':-100,'playing':True}))
        self.client.host_update.assert_not_called()
if __name__=='__main__':unittest.main()
