package me.shouheng.icamera.util

import android.graphics.*
import android.media.Image
import android.os.Build
import androidx.annotation.RequiresApi
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.ReadOnlyBufferException
import kotlin.experimental.and
import kotlin.experimental.inv

object ImageHelper {

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    fun convertYUV_420_888toNV21(image: Image): ByteArray {
        val width = image.width
        val height = image.height
        val ySize = width * height
        val uvSize = width * height / 4
        val nv21 = ByteArray(ySize + uvSize * 2)
        val yBuffer = image.planes[0].buffer // Y
        val uBuffer = image.planes[1].buffer // U
        val vBuffer = image.planes[2].buffer // V
        var rowStride = image.planes[0].rowStride
        var pos = 0
        if (rowStride == width) { // likely
            yBuffer[nv21, 0, ySize]
            pos += ySize
        } else {
            var yBufferPos = -rowStride // not an actual position
            while (pos < ySize) {
                yBufferPos += rowStride
                yBuffer.position(yBufferPos)
                yBuffer[nv21, pos, width]
                pos += width
            }
        }
        rowStride = image.planes[2].rowStride
        val pixelStride = image.planes[2].pixelStride
        if (pixelStride == 2 && rowStride == width && uBuffer[0] == vBuffer[1]) {
            // maybe V an U planes overlap as per NV21, which means vBuffer[1] is alias of uBuffer[0]
            val savePixel = vBuffer[1]
            try {
                vBuffer.put(1, savePixel.inv())
                if (uBuffer[0] == savePixel.inv()) {
                    vBuffer.put(1, savePixel)
                    vBuffer[nv21, ySize, uvSize]
                    return nv21 // shortcut
                }
            } catch (ex: ReadOnlyBufferException) {
                XLog.e("ImageHelper", "ReadOnlyBufferException :$ex")
            }
            vBuffer.put(1, savePixel)
        }
        var row = 0
        val row_len = height / 2
        while (row < row_len) {
            var col = 0
            val col_len = width / 2
            while (col < col_len) {
                val vuPos = col * pixelStride + row * rowStride
                nv21[pos++] = vBuffer[vuPos]
                nv21[pos++] = uBuffer[vuPos]
                col++
            }
            row++
        }
        return nv21
    }

    fun convertYUV420_NV21toARGB8888(data: ByteArray, width: Int, height: Int): Int {
        val size = width * height
        val bri = IntArray(4)
        var u: Int
        var v: Int
        val y1: Int
        val y2: Int
        val y3: Int
        val y4: Int

        // i along Y and the final pixels
        // k along pixels U and V
        var i = 0
        y1 = (data[i] and 0xff.toByte()).toInt()
        y2 = (data[i + 1] and 0xff.toByte()).toInt()
        y3 = (data[width + i] and 0xff.toByte()).toInt()
        y4 = (data[width + i + 1] and 0xff.toByte()).toInt()
        bri[0] = y1
        bri[1] = y2
        bri[2] = y3
        bri[3] = y4
        var max = 0
        val j = 0
        while (i < bri.size) {
            if (bri[j] > max) {
                max = bri[j]
            }
            i++
        }
        return max
    }

    fun convertNV21ToBitmap(nv21: ByteArray?, width: Int, height: Int): Bitmap? {
        var bitmap: Bitmap? = null
        try {
            val image = YuvImage(nv21, ImageFormat.NV21, width, height, null)
            val stream = ByteArrayOutputStream()
            image.compressToJpeg(Rect(0, 0, width, height), 80, stream)
            bitmap = BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size())
            stream.close()
        } catch (e: IOException) {
            XLog.e("ImageHelper", "nv21ToBitmap : $e")
        }
        return bitmap
    }
}