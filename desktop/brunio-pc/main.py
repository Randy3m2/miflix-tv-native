"""Entry point. VLC is loaded from the installer’s bundled x64 directory."""
import os,sys
from pathlib import Path
base=Path(getattr(sys,'_MEIPASS',Path(__file__).resolve().parent))
vlc=base/'vlc'
if not vlc.exists():vlc=base/'vendor'/'vlc-3.0.23'
_dll_directory=None
if os.name=='nt' and vlc.exists():
    os.environ['PYTHON_VLC_LIB_PATH']=str(vlc/'libvlc.dll')
    os.environ['VLC_PLUGIN_PATH']=str(vlc/'plugins')
    os.environ['PYTHON_VLC_MODULE_PATH']=str(vlc/'plugins')
    os.environ['PATH']=str(vlc)+os.pathsep+os.environ.get('PATH','')
    _dll_directory=os.add_dll_directory(str(vlc))
if __name__=='__main__':
    from brunio.ui import main
    raise SystemExit(main('--smoke' in sys.argv or '--smoke-vlc' in sys.argv,'--smoke-vlc' in sys.argv))
