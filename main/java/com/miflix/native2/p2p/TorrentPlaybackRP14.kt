package com.miflix.native2.p2p

import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.security.MessageDigest
import java.util.UUID
import org.libtorrent4j.*

/** One player owns one session. Media3's loader calls this blocking API, never the UI. */
class TorrentPlaybackRP14(private val cache:File,private val limit:Long,private val listenInterfaces:String?=null) {
    @Volatile private var closed=false
    @Volatile private var foreground=true
    private var manager:SessionManager?=null
    private var handle:TorrentHandle?=null
    private var info:TorrentInfo?=null
    private var spec:TorrentSpecRP14?=null
    private var directory:File?=null
    private var selected=0
    private var window=-1
    private var pieceIndex=-1
    private var pieceBytes=ByteArray(0)
    private fun alive() {if(closed || Thread.currentThread().isInterrupted) throw IOException("P2P playback closed")}
    private fun sleep() {alive();try {Thread.sleep(100)} catch(_:InterruptedException) {Thread.currentThread().interrupt();throw IOException("P2P playback interrupted")}}
    private fun deadline(seconds:Int)=System.nanoTime()+seconds*1_000_000_000L
    private fun checkTime(end:Long) {alive();if(System.nanoTime()>end) throw IOException("P2P timed out; try another source")}

    @Synchronized fun open(value:String):Long {
        alive()
        val incoming=TorrentSpecRP14.parse(value) ?: throw IOException("Invalid P2P source")
        if(spec!=null) {if(spec!=incoming) throw IOException("P2P source changed");return size()}
        try {
            cache.mkdirs()
            val dir=File(cache,UUID.randomUUID().toString()).also {it.mkdirs()};directory=dir
            val m=SessionManager(false);manager=m
            val settings=SettingsPack().connectionsLimit(60).uploadRateLimit(64*1024)
                .activeDownloads(0).activeSeeds(0).activeLimit(0)
            // Android may restrict native route notifications; playback reconnects on source retry.
            settings.setBoolean(org.libtorrent4j.swig.settings_pack.bool_types.enable_ip_notifier.swigValue(),false)
            if(listenInterfaces!=null) settings.listenInterfaces(listenInterfaces)
            // Prefer TCP immediately rather than waiting for a blocked uTP handshake on TVs.
            settings.setBoolean(org.libtorrent4j.swig.settings_pack.bool_types.enable_outgoing_utp.swigValue(),false)
            settings.setInteger(org.libtorrent4j.swig.settings_pack.int_types.min_reconnect_time.swigValue(),2)
            settings.setInteger(org.libtorrent4j.swig.settings_pack.int_types.peer_connect_timeout.swigValue(),5)
            settings.setEnableDht(true)
            settings.setDhtBootstrapNodes("router.bittorrent.com:6881,dht.transmissionbt.com:6881,router.utorrent.com:6881")
            m.start(SessionParams(settings))
            // Keep the same metadata-only handle: avoids rebuilding hybrid .torrent piece layers.
            val hash=Sha1Hash.fromBytes(incoming.hash.chunked(2).map {it.toInt(16).toByte()}.toByteArray())
            m.download(incoming.magnet(),dir,TorrentFlags.UPLOAD_MODE.or_(TorrentFlags.PAUSED))
            val addEnd=deadline(5)
            var h=m.find(hash)
            while(h==null || !h.isValid) {checkTime(addEnd);sleep();h=m.find(hash)}
            h.unsetFlags(TorrentFlags.AUTO_MANAGED)
            if(!foreground) m.pause()
            h.resume()
            var metadataEnd=deadline(30)
            var metadata=h.torrentFile()
            while(metadata==null || !metadata.isValid) {if(!foreground) metadataEnd=deadline(30);checkTime(metadataEnd);sleep();metadata=h.torrentFile()}
            val ti=metadata
            h.pause()
            alive()
            if(ti.infoHash().toHex()!=incoming.hash) throw IOException("P2P metadata hash mismatch")
            if(ti.pieceLength() !in 16384..(16*1024*1024) || ti.numPieces()>2_000_000 || ti.numFiles()>10000) throw IOException("Unsupported P2P layout")
            val fs=ti.files()
            for(i in 0 until fs.numFiles()) {
                val f=File(dir,fs.filePath(i)).canonicalFile
                if(!f.path.startsWith(dir.canonicalPath+File.separator) || fs.fileFlags(i).and_(FileStorage.FLAG_SYMLINK).non_zero()) throw IOException("Unsafe P2P file path")
            }
            selected=incoming.fileIndex ?: (0 until fs.numFiles()).filter {
                fs.filePath(it).substringAfterLast('.').lowercase() in setOf("mp4","mkv","avi","webm","m4v","mov","ts") &&
                    !Regex("(?i)(^|[/._ -])sample([/._ -]|$)").containsMatchIn(fs.filePath(it))
            }.maxByOrNull {fs.fileSize(it)} ?: throw IOException("No video file in torrent")
            if(selected !in 0 until fs.numFiles() || fs.fileSize(selected)<=0 || fs.fileFlags(selected).and_(FileStorage.FLAG_PAD_FILE).non_zero()) throw IOException("Invalid P2P video file")
            val need=fs.fileSize(selected)+2L*ti.pieceLength()
            if(fs.fileSize(selected)>limit-2L*ti.pieceLength()) throw IOException("P2P file exceeds cache limit; increase it in Settings")
            if(need+128L*1024*1024>dir.usableSpace) throw IOException("Not enough free storage for this P2P file")
            val priorities=Priority.array(Priority.IGNORE,ti.numPieces())
            val firstPiece=(fs.fileOffset(selected)/ti.pieceLength()).toInt()
            val lastPiece=((fs.fileOffset(selected)+fs.fileSize(selected)-1)/ti.pieceLength()).toInt()
            for(i in firstPiece..lastPiece) priorities[i]=Priority.DEFAULT
            h.prioritizePieces(priorities)
            h.setFlags(TorrentFlags.SEQUENTIAL_DOWNLOAD)
            h.unsetFlags(TorrentFlags.UPLOAD_MODE.or_(TorrentFlags.STOP_WHEN_READY))
            info=ti;handle=h;spec=incoming
            requestWindow(0)
            if(foreground) h.resume()
            return size()
        } catch(e:IOException) {close();throw e} catch(e:LinkageError) {close();throw IOException("P2P engine unavailable on this device",e)} catch(e:Exception) {close();throw IOException("P2P preparation failed",e)}
    }
    private fun size()=info!!.files().fileSize(selected)
    private fun requestWindow(position:Long) {
        val ti=info!!;val h=handle!!
        val first=((ti.files().fileOffset(selected)+position)/ti.pieceLength()).toInt()
        if(window==first) return
        val last=((ti.files().fileOffset(selected)+size()-1)/ti.pieceLength()).toInt()
        // Download only the selected video range; deadline the current window for immediate playback/seek.
        h.clearPieceDeadlines()
        if(window>=0) {for(i in window..minOf(window+7,last)) {if(!h.havePiece(i)) h.piecePriority(i,Priority.DEFAULT)}}
        for(i in first..minOf(first+7,last)) {h.piecePriority(i,Priority.TOP_PRIORITY);h.setPieceDeadline(i,(i-first)*300)}
        window=first
    }
    @Synchronized fun read(position:Long,target:ByteArray,offset:Int,length:Int):Int {
        alive();if(length==0) return 0
        if(position<0 || position>size()) throw IOException("Invalid P2P read position")
        if(position==size()) return -1
        requestWindow(position)
        val ti=info!!;val fs=ti.files();val absolute=fs.fileOffset(selected)+position
        val p=(absolute/ti.pieceLength()).toInt()
        if(pieceIndex!=p) {
            var end=deadline(45)
            while(true) {
                checkTime(end)
                if(!foreground) {end=deadline(45);sleep();continue}
                if(cache.usableSpace<32L*1024*1024) throw IOException("P2P cache storage is full")
                if(handle!!.havePiece(p)) {
                    val bytes=readDiskPiece(p)
                    // Never expose sparse/unwritten bytes, even if the native hash alert preceded a disk flush.
                    if(bytes!=null && MessageDigest.getInstance("SHA-1").digest(bytes).joinToString("") {"%02x".format(it)}==ti.hashForPiece(p).toHex()) {
                        pieceBytes=bytes;pieceIndex=p;break
                    }
                }
                sleep()
            }
        }
        val within=(absolute%ti.pieceLength()).toInt()
        val n=minOf(length,pieceBytes.size-within,(size()-position).coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
        System.arraycopy(pieceBytes,within,target,offset,n)
        return n
    }
    private fun readDiskPiece(p:Int):ByteArray? {
        val ti=info!!;val fs=ti.files();val start=p.toLong()*ti.pieceLength();val out=ByteArray(ti.pieceSize(p));val end=start+out.size
        try {
            for(i in 0 until fs.numFiles()) {
                val a=maxOf(start,fs.fileOffset(i));val b=minOf(end,fs.fileOffset(i)+fs.fileSize(i))
                if(a>=b || fs.fileFlags(i).and_(FileStorage.FLAG_PAD_FILE).non_zero()) continue
                RandomAccessFile(File(directory,fs.filePath(i)),"r").use {f ->f.seek(a-fs.fileOffset(i));f.readFully(out,(a-start).toInt(),(b-a).toInt())}
            }
            return out
        } catch(_:IOException) {return null}
    }
    fun setForeground(value:Boolean) {foreground=value;val m=manager ?: return;synchronized(m) {if(!closed) {if(value) m.resume() else m.pause()}}}
    fun close() {
        if(closed) return
        closed=true
        Thread({synchronized(this) {
            runCatching {manager?.let {m ->synchronized(m) {m.stop()}}};manager=null;handle=null;info=null;pieceBytes=ByteArray(0)
            directory?.deleteRecursively()
        }},"brunio-p2p-close").apply {isDaemon=true;start()}
    }
}
