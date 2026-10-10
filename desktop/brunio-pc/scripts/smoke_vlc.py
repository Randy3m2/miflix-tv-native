"""Windows CI: exercise bundled VLC decoding a local WAV; no streaming credentials or real accounts."""
import os,time,wave,tempfile
from pathlib import Path
folder=Path('vendor/vlc-3.0.23').resolve()
os.environ['PYTHON_VLC_LIB_PATH']=str(folder/'libvlc.dll');os.environ['PYTHON_VLC_MODULE_PATH']=str(folder/'plugins');os.environ['VLC_PLUGIN_PATH']=str(folder/'plugins')
handle=os.add_dll_directory(str(folder))
import vlc
with tempfile.TemporaryDirectory() as tmp:
 path=Path(tmp)/'silence.wav'
 with wave.open(str(path),'wb') as wav:wav.setnchannels(1);wav.setsampwidth(2);wav.setframerate(8000);wav.writeframes(b'\x00\x00'*16000)
 instance=vlc.Instance('--aout=dummy','--no-video');player=instance.media_player_new();media=instance.media_new(str(path));player.set_media(media);player.play()
 deadline=time.monotonic()+10;played=False
 while time.monotonic()<deadline:
  state=player.get_state()
  if state==vlc.State.Playing:played=True
  if state==vlc.State.Ended:break
  if state==vlc.State.Error:raise RuntimeError('VLC decoding failed')
  time.sleep(.05)
 player.stop();player.release();media.release();instance.release()
 if not played:raise RuntimeError('VLC never reached Playing')
print('PASS: bundled Windows VLC library/plugins loaded and local WAV decoded')
