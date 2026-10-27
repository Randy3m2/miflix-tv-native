"""Experimental local IPC for the desktop rewrite; not a public HTTP service.

Runs as a child process and exchanges one JSON object per line over stdio.
Only explicit operations are exposed; credentials and private addon URLs stay here.
"""
import json
import os
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[2] / 'brunio-pc'))
from brunio.core import Client
from brunio.vault import Vault


class Bridge:
    def __init__(self, client=None, vault=None):
        self.client = client or Client()
        self.vault = vault or Vault()
        self.local = self.vault.load()
        self.client.session = self.local.get('session', {})
        self.client.profile = self.local.get('profile', 'default')
        self.operations = {
            'session.restore': self.restore, 'session.login': self.login,
            'session.logout': self.logout, 'profiles.list': self.profiles,
            'profiles.select': self.select_profile, 'catalog.home': self.home,
            'catalog.search': self.search, 'catalog.details': self.details,
        }

    def persist(self):
        self.local.update(session=self.client.session, profile=self.client.profile)
        if os.name == 'nt':
            self.vault.save(self.local)

    def account_view(self):
        # Never return account.privateSetup, sessions, tokens or addon URLs.
        return {'signedIn': bool(self.client.session), 'profile': self.client.profile,
                'profiles': [{'id': p.get('id', ''), 'name': p.get('name', ''),
                              'avatar': p.get('avatar', ''), 'primary': bool(p.get('primary'))}
                             for p in self.client.account.get('profiles', [])]}

    def restore(self):
        if self.client.session:
            self.client.load_account()
            self.persist()
        return self.account_view()

    def login(self, email, password):
        self.client.login(email, password)
        self.persist()
        return self.account_view()

    def logout(self):
        self.client.detach()
        self.client.session = {}
        self.client.account = {}
        self.client.profile = 'default'
        self.persist()
        return self.account_view()

    def profiles(self):
        if not self.client.session:
            raise ValueError('Sign in first')
        return self.account_view()

    def select_profile(self, profile_id):
        if profile_id not in [p.get('id') for p in self.client.account.get('profiles', [])]:
            raise ValueError('Unknown profile')
        self.client.detach()
        self.client.profile = profile_id
        self.persist()
        return self.account_view()

    def home(self):
        if not self.client.session:
            raise ValueError('Sign in first')
        return self.client.catalog('/trending/all/week')

    def search(self, query):
        if not self.client.session:
            raise ValueError('Sign in first')
        if not isinstance(query, str) or len(query) > 200:
            raise ValueError('Invalid search')
        return self.client.search(query) if query.strip() else []

    def details(self, cloud_id):
        if not self.client.session:
            raise ValueError('Sign in first')
        item = self.client.by_id(cloud_id)
        return {'item': item, 'details': self.client.details(item)}

    def dispatch(self, request):
        request_id = request.get('id') if isinstance(request, dict) else None
        try:
            if not isinstance(request, dict) or request.get('method') not in self.operations:
                raise ValueError('Unknown operation')
            params = request.get('params', {})
            if not isinstance(params, dict):
                raise ValueError('Invalid parameters')
            result = self.operations[request['method']](**params)
            return {'id': request_id, 'result': result}
        except Exception:
            # Upstream exceptions can contain passwords, tokens, or provider URLs.
            return {'id': request_id, 'error': 'Operation failed'}


def main():
    bridge = Bridge()
    for line in sys.stdin:
        try:
            request = json.loads(line)
            response = bridge.dispatch(request)
        except (ValueError, TypeError):
            response = {'id': None, 'error': 'Invalid request'}
        sys.stdout.write(json.dumps(response, ensure_ascii=False, separators=(',', ':')) + '\n')
        sys.stdout.flush()


if __name__ == '__main__':
    main()
