"""Windows DPAPI protects account tokens and private add-on settings at rest."""
import ctypes, json, os, threading
from pathlib import Path

class Vault:
    def __init__(self):
        self.path=Path(os.environ.get('LOCALAPPDATA',Path.home()))/'BruniO'/'account.dat'
        self.lock=threading.RLock()
    def crypt(self,data,decrypt=False):
        if os.name!='nt':raise RuntimeError('Persistent sign-in requires Windows')
        from ctypes import wintypes
        class Blob(ctypes.Structure):_fields_=[('size',wintypes.DWORD),('data',ctypes.POINTER(ctypes.c_ubyte))]
        buf=(ctypes.c_ubyte*len(data)).from_buffer_copy(data);source=Blob(len(data),buf);result=Blob()
        api=ctypes.WinDLL('crypt32',use_last_error=True);kernel=ctypes.WinDLL('kernel32',use_last_error=True)
        func=api.CryptUnprotectData if decrypt else api.CryptProtectData
        func.argtypes=[ctypes.POINTER(Blob),ctypes.c_void_p,ctypes.c_void_p,ctypes.c_void_p,ctypes.c_void_p,wintypes.DWORD,ctypes.POINTER(Blob)]
        func.restype=wintypes.BOOL
        kernel.LocalFree.argtypes=[ctypes.c_void_p];kernel.LocalFree.restype=ctypes.c_void_p
        if not func(ctypes.byref(source),None,None,None,None,1,ctypes.byref(result)):raise ctypes.WinError(ctypes.get_last_error())
        try:return ctypes.string_at(result.data,result.size)
        finally:kernel.LocalFree(ctypes.cast(result.data,ctypes.c_void_p))
    def load(self):
        with self.lock:
            if not self.path.exists():return {}
            try:return json.loads(self.crypt(self.path.read_bytes(),True))
            except Exception:return {}
    def save(self,value):
        with self.lock:
            self.path.parent.mkdir(parents=True,exist_ok=True)
            temp=self.path.with_suffix('.tmp');temp.write_bytes(self.crypt(json.dumps(value).encode()));temp.replace(self.path)
    def clear(self):
        with self.lock:self.path.unlink(missing_ok=True)
