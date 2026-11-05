"""Generate a tiny original uncompressed AVI; no downloaded media or encoder needed."""
import struct,sys
from pathlib import Path

def chunk(tag,data):return tag+struct.pack('<I',len(data))+data+(b'\0' if len(data)%2 else b'')
def listing(kind,data):return chunk(b'LIST',kind+data)
def create(path):
    w,h,fps,count=320,180,10,30
    row=((w*3+3)//4)*4;size=row*h
    avih=struct.pack('<14I',100000, size*fps,0,16,count,0,1,size,w,h,0,0,0,0)
    strh=struct.pack('<4s4sIHH8I4h',b'vids',b'DIB ',0,0,0,0,1,fps,0,count,size,0xffffffff,0,0,0,w,h)
    strf=struct.pack('<IiiHHIIiiII',40,w,h,1,24,0,size,0,0,0,0)
    header=listing(b'hdrl',chunk(b'avih',avih)+listing(b'strl',chunk(b'strh',strh)+chunk(b'strf',strf)))
    frames=[];index=[];offset=4
    for number in range(count):
        pixels=bytes(v for y in range(h) for x in range(w) for v in ((x+number*5)%256,y%256,(number*9)%256))
        frame=chunk(b'00db',pixels);frames.append(frame)
        index.append(struct.pack('<4sIII',b'00db',16,offset,len(pixels)));offset+=len(frame)
    body=b'AVI '+header+listing(b'movi',b''.join(frames))+chunk(b'idx1',b''.join(index))
    Path(path).parent.mkdir(parents=True,exist_ok=True)
    Path(path).write_bytes(b'RIFF'+struct.pack('<I',len(body))+body)
if __name__=='__main__':create(sys.argv[1])
