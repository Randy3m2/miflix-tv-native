"""Read user-supplied M3U without publishing playlist links or credentials."""
from __future__ import annotations
import re
from pathlib import Path
from .core import valid_url

ATTRIBUTE=re.compile(r'([\w-]+)="([^"]*)"')

def parse_m3u(text):
    if len(text)>15_000_000:raise ValueError('Playlist is too large')
    rows=[];info={};title=''
    for line in text.splitlines():
        line=line.strip()
        if line.startswith('#EXTINF:'):
            attrs=dict(ATTRIBUTE.findall(line));title=line.split(',',1)[1].strip() if ',' in line else ''
            info={'id':attrs.get('tvg-id',''),'name':attrs.get('tvg-name') or title or 'Channel','category':attrs.get('group-title') or 'Others','logo':attrs.get('tvg-logo','')}
        elif line and not line.startswith('#'):
            if valid_url(line):rows.append({**info,'id':info.get('id') or str(len(rows)+1),'name':info.get('name') or title or 'Channel','category':info.get('category') or 'Others','type':'tv','url':line})
            info={};title=''
            if len(rows)>=10000:break
    return rows

def read_m3u(path):
    path=Path(path)
    if path.stat().st_size>15_000_000:raise ValueError('Playlist is too large')
    return parse_m3u(path.read_text(encoding='utf-8-sig',errors='replace'))
