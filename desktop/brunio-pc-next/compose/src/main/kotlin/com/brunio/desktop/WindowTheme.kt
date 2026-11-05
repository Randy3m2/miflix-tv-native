package com.brunio.desktop
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.ptr.IntByReference
import com.sun.jna.win32.StdCallLibrary
import java.awt.Window
import javax.swing.SwingUtilities

internal interface DwmTheme:StdCallLibrary { fun DwmSetWindowAttribute(window:Pointer,attribute:Int,value:IntByReference,size:Int):Int }
fun applyWindowTheme(window:Window) {
    if(!System.getProperty("os.name").startsWith("Windows"))return
    SwingUtilities.invokeLater {
        runCatching {
            val api=Native.load("dwmapi",DwmTheme::class.java)
            val pointer=Native.getComponentPointer(window)
            val value=IntByReference(1)
            if(api.DwmSetWindowAttribute(pointer,20,value,4)!=0)api.DwmSetWindowAttribute(pointer,19,value,4)
        }
    }
}
