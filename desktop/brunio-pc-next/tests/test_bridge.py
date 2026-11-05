import importlib.util
import json
import io
from unittest.mock import patch
import unittest
from pathlib import Path
from unittest.mock import Mock

spec = importlib.util.spec_from_file_location('desktop_bridge', Path(__file__).resolve().parents[1] / 'backend' / 'bridge.py')
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)


class Vault:
    def save(self,value): self.saved=value

    def load(self):
        return {'session': {'accessToken': 'secret'}, 'preferences': {'language': 'es'}}


class BridgeTests(unittest.TestCase):
    def setUp(self):
        self.client = Mock()
        self.client.account = {'profiles': [{'id': 'one', 'name': 'Randy'}],
                               'privateSetup': {'tmdbToken': 'supersecret', 'addonManifests': ['private-url']}}
        self.bridge = module.Bridge(self.client, Vault())

    def request(self, method, **params):
        return self.bridge.dispatch({'id': 1, 'method': method, 'params': params})

    def test_account_response_does_not_expose_secrets(self):
        result = json.dumps(self.request('session.restore'))
        for value in ('secret', 'private-url', 'privateSetup'):
            self.assertNotIn(value, result)
        self.assertIn('Randy', result)

    def test_profile_switch_requires_known_profile(self):
        self.assertIn('error', self.request('profiles.select', profile_id='unknown'))
        self.assertNotEqual(self.client.profile, 'unknown')
        self.assertIn('result', self.request('profiles.select', profile_id='one'))
        self.client.detach.assert_called_once()

    def test_operations_cannot_call_arbitrary_client_methods(self):
        self.assertIn('error', self.request('rest', path='anything'))
        self.client.rest.assert_not_called()

    def test_exception_details_do_not_leak(self):
        self.client.search.side_effect = RuntimeError('secret-url')
        self.assertNotIn('secret-url', json.dumps(self.request('catalog.search', query='test')))

    def test_search_calls_client_and_returns_results(self):
        self.client.search.return_value = [{'title': 'Example'}]
        self.assertEqual(self.request('catalog.search', query='Example')['result'][0]['title'], 'Example')

    def test_home_reuses_recent_catalog_and_read_does_not_rewrite_vault(self):
        self.client.catalog.return_value=[{'title':'Example'}]
        self.assertEqual(self.request('catalog.home')['result'][0]['title'],'Example')
        self.assertEqual(self.request('catalog.home')['result'][0]['title'],'Example')
        self.client.catalog.assert_called_once()
        self.assertFalse(hasattr(self.bridge.vault,'saved'))

    def test_expired_tmdb_token_has_actionable_error(self):
        self.client.catalog.side_effect=RuntimeError('HTTP 401: request failed')
        self.assertEqual(self.request('catalog.home')['code'],'setup.tmdb.invalid')

    def test_invalid_payload_is_rejected(self):
        self.assertIn('error', self.bridge.dispatch({'id': 2, 'method': 'catalog.search', 'params': []}))
        self.assertIn('error', self.bridge.dispatch(None))

    def test_malformed_upstream_result_does_not_kill_the_local_service(self):
        class FakeBridge:
            def dispatch(self,request):
                if request['id']==1:return {'id':1,'result':object()}
                return {'id':request['id'],'result':[{'title':'Recovered'}]}
            def cancel_transfers(self):pass
        stdout=io.StringIO()
        stdin=io.StringIO('{"id":1}\n{"id":2}\n')
        with patch.object(module,'Bridge',FakeBridge),patch.object(module.sys,'stdin',stdin),patch.object(module.sys,'stdout',stdout):
            module.main()
        first,second=[json.loads(line) for line in stdout.getvalue().splitlines()]
        self.assertEqual(first['code'],'serialization.failed')
        self.assertEqual(second['result'][0]['title'],'Recovered')

if __name__ == '__main__':
    unittest.main()
