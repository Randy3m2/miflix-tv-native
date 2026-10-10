from pathlib import Path
import hashlib,json,sys
sys.path.insert(0,str(Path(__file__).resolve().parent.parent))
from brunio import VERSION
from brunio.core import REPO
from brunio.updater import validate
p=Path('installer')/f'BruniO-PC-Setup-{VERSION}.exe'
if p.read_bytes()[:2]!=b'MZ':raise ValueError('Not a Windows installer')
info=validate({'channel':'pc-windows-x64','version':VERSION,'sha256':hashlib.sha256(p.read_bytes()).hexdigest(),'sizeBytes':p.stat().st_size,'downloadUrl':f'https://github.com/{REPO}/releases/download/pc-v{VERSION}/{p.name}'})
Path('installer/pc-update.json').write_text(json.dumps(info,indent=2))
