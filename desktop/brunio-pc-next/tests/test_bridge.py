import importlib.util
import json
import unittest
from pathlib import Path
from unittest.mock import Mock

spec = importlib.util.spec_from_file_location('desktop_bridge', Path(__file__).resolve().parents[1] / 'backend' / 'bridge.py')
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)


class Vault:
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

    def test_invalid_payload_is_rejected(self):
        self.assertIn('error', self.bridge.dispatch({'id': 2, 'method': 'catalog.search', 'params': []}))
        self.assertIn('error', self.bridge.dispatch(None))

if __name__ == '__main__':
    unittest.main()
