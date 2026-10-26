"""Read user-supplied M3U without publishing playlist links or credentials."""
from __future__ import annotations
import re
from pathlib import Path
from .core import valid_url

ATTRIBUTE=re.compile(r'([\w-]+)="([^"]*)"')

def parse_m3u(text):
    if len(text)>15_000_000:raise ValueError('Playlist is too large')
    rows=[];info={};title='';headers={};drm=False
    for line in text.splitlines():
        line=line.strip()
        if line.startswith('#EXTINF:'):
            attrs=dict(ATTRIBUTE.findall(line));quoted=False;comma=-1
            for i,ch in enumerate(line):
                if ch=='"':quoted=not quoted
                elif ch==',' and not quoted:comma=i;break
            title=line[comma+1:].strip() if comma>=0 else '';headers={};drm=False
            if attrs.get('http-user-agent'):headers['User-Agent']=attrs['http-user-agent']
            if attrs.get('http-referrer'):headers['Referer']=attrs['http-referrer']
            info={'id':attrs.get('tvg-id',''),'name':attrs.get('tvg-name') or title or 'Channel','category':attrs.get('group-title') or 'Others','logo':attrs.get('tvg-logo','')}
        elif line.lower().startswith('#extvlcopt:http-user-agent='):headers['User-Agent']=line.split('=',1)[1]
        elif line.lower().startswith('#extvlcopt:http-referrer='):headers['Referer']=line.split('=',1)[1]
        elif line.lower().startswith('#kodiprop:') and 'license' in line.lower():drm=True
        elif line and not line.startswith('#'):
            if valid_url(line) and not drm:rows.append({**info,'id':info.get('id') or str(len(rows)+1),'name':info.get('name') or title or 'Channel','category':info.get('category') or 'Others','type':'tv','url':line,'headers':dict(headers)})
            info={};title=''
            if len(rows)>=10000:break
    return rows

def read_m3u(path):
    path=Path(path)
    if path.stat().st_size>15_000_000:raise ValueError('Playlist is too large')
    return parse_m3u(path.read_text(encoding='utf-8-sig',errors='replace'))
