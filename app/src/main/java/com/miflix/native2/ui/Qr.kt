package com.miflix.native2.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

@Composable
fun QrCode(data: String, size: Dp = 230.dp, modifier: Modifier = Modifier) {
    val bitmap = remember(data) { createQr(data, 520) }
    Box(modifier.size(size).background(Color.White).padding(10.dp)) {
        Image(bitmap = bitmap.asImageBitmap(), contentDescription = "QR code", modifier = Modifier.size(size - 20.dp))
    }
}

private fun createQr(data: String, px: Int): Bitmap {
    val matrix = QRCodeWriter().encode(data, BarcodeFormat.QR_CODE, px, px)
    val bitmap = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
    for (x in 0 until px) for (y in 0 until px) bitmap.setPixel(x, y, if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
    return bitmap
}
