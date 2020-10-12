package me.shouheng.icamera.config.convert;

import android.graphics.ImageFormat;
import android.media.Image;
import android.os.Build;
import android.support.annotation.RequiresApi;

import me.shouheng.icamera.util.ImageHelper;

/**
 * The default image raw data converter implementation.
 *
 * @author Jeff
 * @time 2020/10/12 23:49
 */
public class ImageRawDataConverterImpl implements ImageRawDataConverter {

    @Override
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public byte[] convertToNV21(Image image) {
        if (image.getFormat() == ImageFormat.YUV_420_888) {
            return ImageHelper.convertYUV_420_888toNV21(image);
        }
        return new byte[0];
    }
}
