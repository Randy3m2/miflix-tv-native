#!/usr/bin/env python3
"""Build custom.channels.xml: for every channel in the app's 5 IPTV lists, pick one site from iptv-org/epg that has a guide for it."""
import glob, os, re, sys, urllib.request, xml.etree.ElementTree as ET
from xml.sax.saxutils import escape, quoteattr

# Same lists the app uses (IptvRepository.kt). Override with PLAYLISTS="url1,url2" for tests.
DEFAULT=["https://iptv-org.github.io/iptv/countries/do.m3u","https://iptv-org.github.io/iptv/languages/tur.m3u",
         "https://iptv-org.github.io/iptv/countries/mx.m3u","https://iptv-org.github.io/iptv/countries/es.m3u",
         "https://iptv-org.github.io/iptv/countries/us.m3u"]
PLAYLISTS=[u for u in os.environ.get("PLAYLISTS","").split(",") if u] or DEFAULT
# Sites tried first, in this order (more reliable / no login). Others follow alphabetically.
PREFER=["tvtv.us","tvguide.com","i.mjh.nz","gatotv.com","mi.tv","digiturk.com.tr","dsmart.com.tr","tvplus.com.tr",
        "turksatkablo.com.tr","programacion-tv.elpais.com","orangetv.orange.es","directv.com.uy"]
MAX_CHANNELS=int(os.environ.get("MAX_CHANNELS","600"))
epg_dir=sys.argv[1]; out=sys.argv[2]

ids=[]  # ordered, unique, without @feed
seen=set()
for url in PLAYLISTS:
    req=urllib.request.Request(url,headers={"User-Agent":"miflix-epg-builder"})
    text=urllib.request.urlopen(req,timeout=60).read().decode("utf-8","ignore")
    for m in re.findall(r'tvg-id="([^"]*)"',text):
        i=m.split("@")[0]
        if i and i not in seen: seen.add(i);ids.append(i)
print("playlist ids:",len(ids))

def rank(site):
    return (PREFER.index(site),site) if site in PREFER else (len(PREFER),site)
best={}  # id -> (rank, attrs, name)
for f in glob.glob(os.path.join(epg_dir,"sites","*","*.channels.xml")):
    site=os.path.basename(os.path.dirname(f))
    try: root=ET.parse(f).getroot()
    except ET.ParseError: continue
    for ch in root.iter("channel"):
        i=(ch.get("xmltv_id") or "").split("@")[0]
        if i not in seen or not ch.get("site_id"): continue
        r=rank(ch.get("site") or site)
        if i not in best or r<best[i][0]:
            best[i]=(r,dict(ch.attrib),(ch.text or "").strip())
chosen=[i for i in ids if i in best][:MAX_CHANNELS]
print("channels with guide source:",len(chosen))
with open(out,"w",encoding="utf-8") as fh:
    fh.write('<?xml version="1.0" encoding="UTF-8"?>\n<channels>\n')
    for i in chosen:
        _,a,name=best[i];a["xmltv_id"]=i
        attrs=" ".join(f"{k}={quoteattr(v)}" for k,v in a.items())
        fh.write(f"  <channel {attrs}>{escape(name)}</channel>\n")
    fh.write("</channels>\n")
