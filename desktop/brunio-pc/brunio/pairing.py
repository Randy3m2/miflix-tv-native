"""Same encrypted one-time QR sign-in protocol as TV; QR key stays in URL fragment."""
import base64,json,secrets,time
from datetime import datetime,timezone,timedelta
from urllib.parse import urlencode
from .core import SUPABASE_URL,SUPABASE_KEY
from cryptography.hazmat.primitives.ciphers.aead import AESGCM

def encode(value):return base64.urlsafe_b64encode(value).decode().rstrip('=')
def decode(value):return base64.urlsafe_b64decode(value+'='*(-len(value)%4))
class Pairing:
    def __init__(self,transport):self.transport=transport;self.code=secrets.token_hex(9).upper();self.key=secrets.token_bytes(32);self.expires=time.monotonic()+600
    @property
    def url(self):return 'https://randy3m2.github.io/miflix-tv-native/pair/?code='+self.code+'#key='+encode(self.key)
    def create(self):self.transport(SUPABASE_URL+'/rest/v1/miflix_pairing','POST',{'apikey':SUPABASE_KEY,'Prefer':'return=minimal'},{'pair_code':self.code,'status':'pending','expires_at':(datetime.now(timezone.utc)+timedelta(minutes=10)).isoformat()});return self.url
    def poll(self):
        if time.monotonic()>self.expires:raise ValueError('QR expired. Create a new QR')
        rows=self.transport(SUPABASE_URL+'/rest/v1/miflix_pairing?'+urlencode({'pair_code':'eq.'+self.code,'select':'status,payload_enc,iv'}),headers={'apikey':SUPABASE_KEY}) or []
        if not rows or rows[0].get('status')!='approved':return None
        row=rows[0];raw=AESGCM(self.key).decrypt(decode(row['iv']),decode(row['payload_enc']),None);return json.loads(raw)
    def finish(self):self.transport(SUPABASE_URL+'/rest/v1/miflix_pairing?'+urlencode({'pair_code':'eq.'+self.code}),'DELETE',{'apikey':SUPABASE_KEY})
