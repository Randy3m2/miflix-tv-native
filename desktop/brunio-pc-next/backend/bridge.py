"""Experimental local IPC for the desktop rewrite; not a public HTTP service.

Runs as a child process and exchanges one JSON object per line over stdio.
Only explicit operations are exposed; credentials and private addon URLs stay here.
"""
import json
import os
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[2] / 'brunio-pc'))
from brunio.core import Client,get_json
from brunio.vault import Vault
sys.path.insert(0, str(Path(__file__).resolve().parent))
from screens import Screens
from remaining import Remaining


def local_transport(url,method='GET',headers=None,body=None,timeout=8):
    return get_json(url,method,headers,body,min(timeout,8))

class Bridge(Screens, Remaining):
    def __init__(self, client=None, vault=None):
        self.client = client or Client(transport=local_transport)
        self.vault = vault or Vault()
        self.local = self.vault.load()
        self.offline=False
        self.client.session = self.local.get('session', {})
        self.client.profile = self.local.get('profile', 'default')
        self.operations = {
            'session.restore': self.restore, 'session.login': self.login,
            'session.logout': self.logout, 'profiles.list': self.profiles,
            'profiles.select': self.select_profile, 'catalog.home': self.home,
            'catalog.search': self.search, 'catalog.details': self.details,
        }

        self.operations.update(self.screen_operations())
        self.operations.update(self.remaining_operations())

    def persist(self):
        self.local.update(session=self.client.session, profile=self.client.profile)
        if isinstance(self.client.account,dict) and self.client.account.get('profiles'):
            self.local['cached_profiles']=self.client.account['profiles']
        if os.name == 'nt':
            self.vault.save(self.local)

    def account_view(self):
        # Never return account.privateSetup, sessions, tokens or addon URLs.
        return {'signedIn': bool(self.client.session), 'profile': self.client.profile, 'scope':self.scope_id(),'language':self.local.get('preferences',{}).get('language','es'),
                'profiles': [{'id': p.get('id', ''), 'name': p.get('name', ''),
                              'avatar': self.profile_avatar(p.get('avatarValue',p.get('avatar','ai:astronaut'))), 'primary': bool(p.get('primary'))}
                             for p in self.client.account.get('profiles', [])]}

    def restore(self):
        if self.client.session:
            try:
                self.client.load_account()
                self.offline=False
                try:self.flush_progress()
                except Exception:pass
                self.persist()
            except Exception:
                self.offline=True
                self.client.account={'profiles':self.local.get('cached_profiles',[])}
                return {**self.account_view(),'offline':True}
        return self.account_view()

    def login(self, email, password):
        self.client.login(email, password)
        self.offline=False
        self.persist()
        return self.account_view()

    def logout(self):
        self.cancel_transfers()
        try:
            if self.client.party:self.client.party_leave()
        except Exception:pass
        self.client.detach();self.client.session={};self.client.account={};self.client.profile='default'
        self.client.token='';self.client.manifests=[];self.local.pop('cached_profiles',None)
        self.persist();return self.account_view()

    def profiles(self):
        if not self.client.session:
            raise ValueError('Sign in first')
        return self.account_view()

    def select_profile(self, profile_id):
        if profile_id not in [p.get('id') for p in self.client.account.get('profiles', [])]:
            raise ValueError('Unknown profile')
        self.cancel_transfers()
        if self.client.party: self.client.party_leave()
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
        import re
        from urllib.parse import urlencode
        from brunio.core import media
        match=re.fullmatch(r'tmdb:(movie|series):([0-9]+)',cloud_id)
        if not match:raise ValueError('Unknown content ID')
        kind=match[1];details=self.client.details({'type':kind,'id':int(match[2])})
        item=media(details,kind)
        state=self.client.state()
        progress={key:value for key,value in state.get('progress',{}).items() if key==cloud_id or key.startswith(cloud_id+':s')}
        rating=0
        try:
            rows=self.client.rest('miflix_ratings?'+urlencode({'profile_id':'eq.'+self.client.profile,'cloud_id':'eq.'+cloud_id,'select':'score','limit':1})) or []
            if rows:rating=rows[0].get('score',0)
        except Exception:pass
        return {'item':item,'details':details,'saved':cloud_id in state.get('favorites',[]),'scope':self.scope_id(),'progress':progress,'user_rating':rating}

    def dispatch(self, request):
        request_id = request.get('id') if isinstance(request, dict) else None
        try:
            if not isinstance(request, dict) or request.get('method') not in self.operations:
                raise ValueError('Unknown operation')
            params = request.get('params', {})
            if not isinstance(params, dict):
                raise ValueError('Invalid parameters')
            if not request['method'].startswith('session.') and not self.client.session:
                raise ValueError('Sign in first')
            self.client.language = 'es-ES' if self.local.get('preferences', {}).get('language', 'es') == 'es' else 'en-US'
            result = self.operations[request['method']](**params)
            if self.client.session: self.persist()
            return {'id': request_id, 'result': result}
        except Exception as error:
            # Return only fixed codes; never upstream URLs, tokens or provider errors.
            import urllib.error
            message=str(error)
            method=request.get('method','') if isinstance(request,dict) else ''
            code='operation.failed'
            if message in ('Profile changed','Session changed'):code='scope.changed'
            elif message=='Configure your TMDB token in Settings':code='setup.tmdb'
            elif message=='Add your own Torrentio or Comet in Settings':code='setup.addons'
            elif method=='session.login' and message.startswith(('HTTP 400','HTTP 401')):code='auth.invalid'
            elif isinstance(error,(urllib.error.URLError,TimeoutError,ConnectionError)):code='network.unavailable'
            return {'id': request_id, 'error': 'Operation failed','code':code}


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
    bridge.cancel_transfers()


if __name__ == '__main__':
    main()
