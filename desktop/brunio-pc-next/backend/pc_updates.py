"""Read only the existing PC channel; normalize owner/repository casing from GitHub."""
import time
from urllib.parse import urlsplit
from brunio.core import REPO,get_json
from brunio.updater import validate

def get_latest():
    release=get_json('https://api.github.com/repos/'+REPO+'/releases/tags/pc-latest',headers={'User-Agent':'BruniO-PC'})
    asset=next((a for a in release.get('assets',[]) if a.get('name')=='pc-update.json'),None)
    if asset is None:return None
    url=urlsplit(asset['browser_download_url'])
    parts=url.path.split('/')
    valid=(url.scheme=='https' and url.hostname=='github.com' and not url.username and not url.password and not url.port and not url.query and not url.fragment
           and len(parts)>=3 and '/'.join(parts[1:3]).lower()==REPO.lower()
           and '/'.join(parts[3:])=='releases/download/pc-latest/pc-update.json')
    if not valid:raise ValueError('Unexpected PC update metadata')
    canonical='https://github.com/'+REPO+'/releases/download/pc-latest/pc-update.json'
    return validate(get_json(canonical+'?t='+str(time.time_ns())))
