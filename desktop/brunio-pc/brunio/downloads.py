"""Profile-scoped, restart-safe downloads of direct video files only."""
from __future__ import annotations
import hashlib, json, os, re, sqlite3, threading, time, uuid
from pathlib import Path
from contextlib import contextmanager
from urllib.parse import urlsplit
from urllib.request import Request, build_opener, HTTPRedirectHandler
from .core import valid_url

EXTENSIONS={'.mp4','.mkv','.webm','.mov','.ts','.avi','.m4v'}

def file_extension(stream):
    url=stream.get('url','')
    if not valid_url(url):return None
    path=urlsplit(url).path.lower()
    if path.endswith(('.m3u8','.mpd')):return None
    name=(stream.get('behaviorHints') or {}).get('filename') or stream.get('filename') or ''
    ext=Path(name).suffix.lower() or Path(path).suffix.lower()
    return ext if ext in EXTENSIONS else None

def safe_error(exc):
    from urllib.error import HTTPError, URLError
    if isinstance(exc,HTTPError):return f'HTTP {exc.code}'
    if isinstance(exc,URLError):return 'Network error'
    return str(exc).split('https://')[0].split('http://')[0][:160] or 'Download failed'

class SafeRedirects(HTTPRedirectHandler):
    def redirect_request(self,req,fp,code,msg,headers,newurl):
        before=urlsplit(req.full_url);after=urlsplit(newurl)
        if not valid_url(newurl) or (before.scheme=='https' and after.scheme!='https'):raise ValueError('Unsafe download redirect')
        redirected=super().redirect_request(req,fp,code,msg,headers,newurl)
        if redirected is not None and (before.hostname,before.port)!=(after.hostname,after.port):
            for key in list(redirected.headers):
                if key.lower() not in ('accept','user-agent'):redirected.remove_header(key)
            for key in list(redirected.unredirected_hdrs):
                if key.lower() not in ('accept','user-agent'):redirected.remove_header(key)
        return redirected

class DownloadStore:
    def __init__(self,root=None):
        self.root=Path(root or Path(os.getenv('LOCALAPPDATA') or Path.home())/'BruniO'/'Downloads')
        self.root.mkdir(parents=True,exist_ok=True);self.db=self.root/'downloads.sqlite3';self.lock=threading.Lock()
        with self.connect() as db:
            db.execute('''CREATE TABLE IF NOT EXISTS files(id TEXT PRIMARY KEY,owner TEXT NOT NULL,cloud TEXT NOT NULL,title TEXT NOT NULL,poster TEXT NOT NULL,kind TEXT NOT NULL,media_id INTEGER NOT NULL,year TEXT,season INTEGER NOT NULL,episode INTEGER NOT NULL,filename TEXT NOT NULL,state TEXT NOT NULL,done INTEGER NOT NULL DEFAULT 0,total INTEGER NOT NULL DEFAULT 0,error TEXT NOT NULL DEFAULT '',created REAL NOT NULL)''')
            db.execute("UPDATE files SET state='interrupted',error='Interrupted; retry to continue' WHERE state IN ('queued','downloading')")
    @contextmanager
    def connect(self):
        db=sqlite3.connect(self.db,timeout=20)
        try:
            db.row_factory=sqlite3.Row
            with db:
                yield db
        finally:
            db.close()
    def list(self,owner):
        with self.connect() as db:return [dict(x) for x in db.execute('SELECT * FROM files WHERE owner=? ORDER BY created DESC',(owner,))]
    def create(self,owner,item,season,episode,ext):
        with self.lock, self.connect() as db:
            old=db.execute("SELECT * FROM files WHERE owner=? AND cloud=? AND season=? AND episode=? AND state IN ('queued','downloading','complete')",(owner,item['cloudId'],season,episode)).fetchone()
            if old:
                if old['state']!='complete' or self.path(old).is_file():return dict(old),False
                db.execute("UPDATE files SET state='interrupted',error='File missing; retry' WHERE id=?",(old['id'],))
            id=uuid.uuid4().hex;relative=id+ext
            db.execute('INSERT INTO files VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)',(id,owner,item['cloudId'],item['title'],item.get('poster',''),item['type'],int(item['id']),item.get('year',''),season,episode,relative,'queued',0,0,'',time.time()))
            return dict(db.execute('SELECT * FROM files WHERE id=?',(id,)).fetchone()),True
    def update(self,id,owner,**values):
        allowed={'state','done','total','error'}
        if not values or set(values)-allowed:raise ValueError('Invalid update')
        with self.connect() as db:
            db.execute('UPDATE files SET '+','.join(x+'=?' for x in values)+' WHERE id=? AND owner=?',(*values.values(),id,owner))
    def path(self,entry):
        name=entry['filename']
        if not re.fullmatch(r'[a-f0-9]{32}\.[a-z0-9]{2,5}',name):raise ValueError('Invalid file')
        return self.root/name
    def remove(self,id,owner):
        with self.lock,self.connect() as db:
            row=db.execute('SELECT * FROM files WHERE id=? AND owner=?',(id,owner)).fetchone()
            if not row:return False
            if row['state'] in ('queued','downloading'):raise ValueError('A download is still running')
            self.path(row).unlink(missing_ok=True);self.path(row).with_suffix(self.path(row).suffix+'.part').unlink(missing_ok=True);db.execute('DELETE FROM files WHERE id=? AND owner=?',(id,owner));return True
    def transfer(self,entry,stream,cancel=lambda:False,progress=lambda:None):
        dest=self.path(entry);partial=dest.with_suffix(dest.suffix+'.part');owner=entry['owner'];id=entry['id']
        hints=(stream.get('behaviorHints') or {}).get('proxyHeaders',{}).get('request',{})
        headers={k:v for k,v in hints.items() if isinstance(k,str) and re.fullmatch(r'[A-Za-z0-9-]{1,80}',k) and k.lower() not in ('host','connection','content-length','transfer-encoding') and isinstance(v,str) and '\n' not in v and '\r' not in v}
        try:
            self.update(id,owner,state='downloading',error='');last=0;done=0
            with build_opener(SafeRedirects()).open(Request(stream['url'],headers=headers),timeout=30) as response,partial.open('wb') as out:
                if not valid_url(response.geturl()):raise ValueError('Invalid download redirect')
                ctype=response.headers.get('Content-Type','').lower().split(';')[0]
                if ctype in ('text/html','application/json'):raise ValueError('Source returned a page instead of a video file')
                total=max(0,int(response.headers.get('Content-Length') or 0))
                while True:
                    if cancel():raise InterruptedError('Download interrupted')
                    chunk=response.read(256*1024)
                    if not chunk:break
                    out.write(chunk);done+=len(chunk)
                    if time.monotonic()-last>=1:
                        self.update(id,owner,done=done,total=total);progress();last=time.monotonic()
            if total and done!=total:raise IOError('Incomplete download')
            if not done:raise IOError('Empty download')
            partial.replace(dest);self.update(id,owner,state='complete',done=done,total=done);progress();return id
        except Exception as exc:
            partial.unlink(missing_ok=True);self.update(id,owner,state='failed',error=safe_error(exc));progress();raise
