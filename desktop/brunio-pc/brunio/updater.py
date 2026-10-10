"""Only immutable PC releases from the existing repository are accepted."""
import hashlib,json,os,re,tempfile
from pathlib import Path
from urllib.request import Request,urlopen,HTTPRedirectHandler,build_opener
from urllib.parse import urlsplit
from .core import REPO,get_json
from . import VERSION
MAX=800*1024*1024
ALLOWED={'github.com','release-assets.githubusercontent.com','objects.githubusercontent.com'}
def version(v):
    m=re.fullmatch(r'(\d+)\.(\d+)\.(\d+)(?:-rc(\d+))?',v)
    if not m:raise ValueError('Invalid PC version')
    return tuple(map(int,m.groups()[:3]))+(int(m[4]) if m[4] else 10**9,)
def validate(info):
    if not isinstance(info,dict) or info.get('channel')!='pc-windows-x64':raise ValueError('Wrong update channel')
    version(info['version']);u=urlsplit(info['downloadUrl'])
    if u.scheme!='https' or u.hostname!='github.com' or u.username or u.password or u.port or u.query or u.fragment or not re.fullmatch('/'+re.escape(REPO)+r'/releases/download/pc-v[0-9.]+(?:-rc\d+)?/BruniO-PC-Setup-[0-9.]+(?:-rc\d+)?\.exe',u.path):raise ValueError('Untrusted PC installer URL')
    if not re.fullmatch('[a-f0-9]{64}',info.get('sha256','')) or not 0<int(info.get('sizeBytes',0))<=MAX:raise ValueError('Invalid installer hash/size')
    return info

def check():
    release=get_json('https://api.github.com/repos/'+REPO+'/releases/tags/pc-latest',headers={'User-Agent':'BruniO-PC'})
    asset=next((x for x in release.get('assets',[]) if x['name']=='pc-update.json'),None)
    if not asset:return None
    expected='https://github.com/'+REPO+'/releases/download/pc-latest/pc-update.json'
    if asset['browser_download_url']!=expected:raise ValueError('Unexpected PC update metadata')
    info=validate(get_json(expected+'?t='+str(__import__('time').time_ns())))
    return info if version(info['version'])>version(VERSION) else None
class Redirects(HTTPRedirectHandler):
    def redirect_request(self,req,fp,code,msg,headers,newurl):
        u=urlsplit(newurl)
        if u.scheme!='https' or u.hostname not in ALLOWED or u.username or u.password or u.port:raise ValueError('Untrusted installer redirect')
        return super().redirect_request(req,fp,code,msg,headers,newurl)
def download(info):
    validate(info);directory=Path(tempfile.gettempdir())/'BruniO-updates';directory.mkdir(exist_ok=True)
    temp=directory/'setup.part';target=directory/('BruniO-PC-Setup-'+info['version']+'.exe');total=0;sha=hashlib.sha256()
    try:
        with build_opener(Redirects()).open(Request(info['downloadUrl'],headers={'User-Agent':'BruniO-PC'}),timeout=30) as response,temp.open('wb') as output:
            while chunk:=response.read(65536):
                total+=len(chunk)
                if total>info['sizeBytes'] or total>MAX:raise ValueError('Installer too large')
                sha.update(chunk);output.write(chunk)
        if total!=info['sizeBytes'] or sha.hexdigest()!=info['sha256']:raise ValueError('Installer verification failed')
        with temp.open('rb') as handle:
            if handle.read(2)!=b'MZ':raise ValueError('Not a Windows installer')
        temp.replace(target);return target
    finally:temp.unlink(missing_ok=True)
