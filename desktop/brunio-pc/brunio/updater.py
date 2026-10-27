"""Only immutable PC releases from the existing repository are accepted."""
import hashlib,json,os,re,tempfile,time
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
def trusted_release_url(url, tag, filename):
    """GitHub canonicalizes Randy3m2's case; validate the exact release path."""
    u=urlsplit(url)
    if (u.scheme!='https' or u.hostname!='github.com' or u.username or
            u.password or u.port or u.query or u.fragment):
        return False
    parts=u.path.split('/')
    return (len(parts)==7 and parts[0]=='' and
            '/'.join(parts[1:3]).casefold()==REPO.casefold() and
            parts[3:]==['releases','download',tag,filename])

def validate(info):
    if not isinstance(info,dict) or info.get('channel')!='pc-windows-x64':
        raise ValueError('Wrong update channel')
    release_version=info['version']
    version(release_version)
    if not trusted_release_url(
        info['downloadUrl'], 'pc-v'+release_version,
        'BruniO-PC-Setup-'+release_version+'.exe'
    ):
        raise ValueError('Untrusted PC installer URL or installer version mismatch')
    if not re.fullmatch('[a-f0-9]{64}',info.get('sha256','')) or not 0<int(info.get('sizeBytes',0))<=MAX:
        raise ValueError('Invalid installer hash/size')
    return info

def check():
    release=get_json(
        'https://api.github.com/repos/'+REPO+'/releases/tags/pc-latest',
        headers={'User-Agent':'BruniO-PC'}
    )
    if not isinstance(release,dict):
        raise ValueError('Invalid PC update release response')
    asset=next((x for x in release.get('assets',[]) if x.get('name')=='pc-update.json'),None)
    if not asset:
        raise ValueError('PC update metadata is missing from pc-latest release')
    actual=asset.get('browser_download_url','')
    if not trusted_release_url(actual,'pc-latest','pc-update.json'):
        raise ValueError('Unexpected PC update metadata URL')
    # Use the URL as returned by GitHub, preserving its account casing.
    info=validate(get_json(actual+'?t='+str(time.time_ns()),
                           headers={'User-Agent':'BruniO-PC'}))
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
