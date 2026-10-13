#!/usr/bin/env python3
"""Keep the guide under the app's 12 MB limit: only title/desc, window yesterday..+3 days, trimmed descriptions."""
import sys, datetime as dt, xml.etree.ElementTree as ET
src,dst=sys.argv[1],sys.argv[2]
LIMIT=int(sys.argv[3]) if len(sys.argv)>3 else 11_000_000
def parse(s):
    try: return dt.datetime.strptime(s.split()[0][:14].ljust(14,"0"),"%Y%m%d%H%M%S")
    except Exception: return None
now=dt.datetime.now(dt.timezone.utc).replace(tzinfo=None); lo=now-dt.timedelta(days=1,hours=14); hi=now+dt.timedelta(days=3,hours=14)
root=ET.parse(src).getroot()
def build(desc_len):
    out=ET.Element("tv")
    for ch in root.findall("channel"):
        c=ET.SubElement(out,"channel",{"id":ch.get("id","")})
        n=ch.find("display-name");d=ET.SubElement(c,"display-name");d.text=(n.text if n is not None else "") or ch.get("id")
    for p in root.findall("programme"):
        s=parse(p.get("start","")); e=parse(p.get("stop",""))
        if not s or not e or e<lo or s>hi: continue
        q=ET.SubElement(out,"programme",{"start":p.get("start"),"stop":p.get("stop"),"channel":p.get("channel")})
        t=p.find("title")
        if t is None or not (t.text or "").strip(): out.remove(q);continue
        ET.SubElement(q,"title").text=t.text.strip()
        de=p.find("desc")
        if desc_len and de is not None and (de.text or "").strip():
            ET.SubElement(q,"desc").text=de.text.strip()[:desc_len]
    return out
for desc_len in (220,120,0):
    tree=ET.ElementTree(build(desc_len)); tree.write(dst,encoding="utf-8",xml_declaration=True)
    import os; size=os.path.getsize(dst); print("desc",desc_len,"size",size)
    if size<=LIMIT: break
else:
    sys.exit("Guide still too large; lower MAX_CHANNELS")
