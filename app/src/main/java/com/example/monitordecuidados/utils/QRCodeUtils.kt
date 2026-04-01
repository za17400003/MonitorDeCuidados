package com.example.monitordecuidados.utils

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix

object QRCodeUtils {
    fun generateQRCode(text: String, width: Int, height: Int): Bitmap? {
        return try {
            val bitMatrix: BitMatrix = MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, width, height)
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE)
                }
            }
            bitmap
        } catch (e: WriterException) {
            e.printStackTrace()
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
