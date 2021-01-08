package me.shouheng.icamera.config.convert

import android.graphics.ImageFormat
import android.media.Image
import android.os.Build
import android.support.annotation.RequiresApi
import me.shouheng.icamera.util.ImageHelper.Companion.convertYUV_420_888toNV21

/**
 * The default image raw data converter implementation.
 *
 * @author Jeff
 * @time 2020/10/12 23:49
 */
class ImageRawDataConverterImpl : ImageRawDataConverter {
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    override fun convertToNV21(image: Image): ByteArray {
        return if (image.format == ImageFormat.YUV_420_888) {
            convertYUV_420_888toNV21(image)
        } else ByteArray(0)
    }
}