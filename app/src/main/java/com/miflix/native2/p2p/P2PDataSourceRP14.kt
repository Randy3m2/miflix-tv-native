package com.miflix.native2.p2p

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import java.io.IOException

/** Direct HTTP/subtitle requests keep their existing Media3 transport. */
class P2PDataSourceRP14(private val engine:TorrentPlaybackRP14):BaseDataSource(true) {
    private var uri:Uri?=null
    private var position=0L
    private var remaining=0L
    private var opened=false
    override fun open(dataSpec:DataSpec):Long {
        transferInitializing(dataSpec)
        val size=engine.open(dataSpec.uri.toString())
        if(dataSpec.position>size) throw IOException("P2P seek is outside the file")
        uri=dataSpec.uri;position=dataSpec.position
        remaining=if(dataSpec.length==C.LENGTH_UNSET.toLong()) size-position else minOf(dataSpec.length,size-position)
        opened=true;transferStarted(dataSpec)
        return remaining
    }
    override fun read(buffer:ByteArray,offset:Int,length:Int):Int {
        if(length==0) return 0
        if(remaining==0L) return C.RESULT_END_OF_INPUT
        val n=try {engine.read(position,buffer,offset,minOf(length.toLong(),remaining).toInt())} catch(e:IOException) {engine.close();throw e}
        if(n>0) {position+=n;remaining-=n;bytesTransferred(n)}
        return n
    }
    override fun getUri():Uri?=uri
    override fun close() {uri=null;if(opened) {opened=false;transferEnded()}}
    class Factory(private val direct:DataSource.Factory,private val engine:TorrentPlaybackRP14):DataSource.Factory {
        override fun createDataSource():DataSource = Routing(direct.createDataSource(),engine)
    }
    private class Routing(private val direct:DataSource,engine:TorrentPlaybackRP14):DataSource {
        private val torrent=P2PDataSourceRP14(engine)
        private var current:DataSource=direct
        override fun addTransferListener(listener:androidx.media3.datasource.TransferListener) {direct.addTransferListener(listener);torrent.addTransferListener(listener)}
        override fun open(dataSpec:DataSpec):Long {current=if(dataSpec.uri.scheme=="brunio-p2p") torrent else direct;return current.open(dataSpec)}
        override fun read(buffer:ByteArray,offset:Int,length:Int)=current.read(buffer,offset,length)
        override fun getUri()=current.uri
        override fun getResponseHeaders()=current.responseHeaders
        override fun close()=current.close()
    }
}
