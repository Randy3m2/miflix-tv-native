from pathlib import Path
import hashlib,json,sys
root=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(root/'backend'))
sys.path.insert(0,str(root.parent/'brunio-pc'))
from build_version import PC_VERSION
from brunio.updater import validate
from brunio.core import REPO
path=root/'installer'/f'BruniO-PC-Setup-{PC_VERSION}.exe'
with path.open('rb') as stream:
    if stream.read(2)!=b'MZ':raise ValueError('Invalid installer')
    stream.seek(0);digest=hashlib.sha256()
    for chunk in iter(lambda:stream.read(1024*1024),b''):digest.update(chunk)
info=validate({'channel':'pc-windows-x64','version':PC_VERSION,'sha256':digest.hexdigest(),'sizeBytes':path.stat().st_size,
               'downloadUrl':f'https://github.com/{REPO}/releases/download/pc-v{PC_VERSION}/{path.name}'})
(root/'installer'/'pc-update.json').write_text(json.dumps(info,indent=2),encoding='utf-8')
