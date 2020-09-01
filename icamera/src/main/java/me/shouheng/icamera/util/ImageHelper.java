package me.shouheng.icamera.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.os.Build;
import android.support.annotation.RequiresApi;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ReadOnlyBufferException;

public final class ImageHelper {

    private static final String TAG = "ImageHelper";

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public static byte[] convertYUV_420_888toNV21(Image image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int ySize = width*height;
        int uvSize = width*height/4;

        byte[] nv21 = new byte[ySize + uvSize*2];

        ByteBuffer yBuffer = image.getPlanes()[0].getBuffer(); // Y
        ByteBuffer uBuffer = image.getPlanes()[1].getBuffer(); // U
        ByteBuffer vBuffer = image.getPlanes()[2].getBuffer(); // V

        int rowStride = image.getPlanes()[0].getRowStride();

        int pos = 0;

        if (rowStride == width) { // likely
            yBuffer.get(nv21, 0, ySize);
            pos += ySize;
        } else {
            int yBufferPos = -rowStride; // not an actual position
            for (; pos<ySize; pos+=width) {
                yBufferPos += rowStride;
                yBuffer.position(yBufferPos);
                yBuffer.get(nv21, pos, width);
            }
        }

        rowStride = image.getPlanes()[2].getRowStride();
        int pixelStride = image.getPlanes()[2].getPixelStride();

        if (pixelStride == 2 && rowStride == width && uBuffer.get(0) == vBuffer.get(1)) {
            // maybe V an U planes overlap as per NV21, which means vBuffer[1] is alias of uBuffer[0]
            byte savePixel = vBuffer.get(1);
            try {
                vBuffer.put(1, (byte) ~savePixel);
                if (uBuffer.get(0) == (byte) ~savePixel) {
                    vBuffer.put(1, savePixel);
                    vBuffer.get(nv21, ySize, uvSize);

                    return nv21; // shortcut
                }
            } catch (ReadOnlyBufferException ex) {
                XLog.e(TAG, "ReadOnlyBufferException :" + ex);
            }
            vBuffer.put(1, savePixel);
        }

        for (int row=0, row_len=height/2; row<row_len; row++) {
            for (int col=0, col_len=width/2; col<col_len; col++) {
                int vuPos = col*pixelStride + row*rowStride;
                nv21[pos++] = vBuffer.get(vuPos);
                nv21[pos++] = uBuffer.get(vuPos);
            }
        }
        return nv21;
    }

    public static int convertYUV420_NV21toARGB8888(byte[] data, int width, int height) {
        int size = width * height;
        int[] bri = new int[4];
        int u, v, y1, y2, y3, y4;

        // i along Y and the final pixels
        // k along pixels U and V
        int i = 0;
        y1 = data[i] & 0xff;
        y2 = data[i + 1] & 0xff;
        y3 = data[width + i] & 0xff;
        y4 = data[width + i + 1] & 0xff;

        bri[0] = y1;
        bri[1] = y2;
        bri[2] = y3;
        bri[3] = y4;

        int max = 0;
        for (int j = 0; i < bri.length; i++) {
            if (bri[j] > max) {
                max = bri[j];
            }
        }
        return max;
    }

    public static Bitmap convertNV21ToBitmap(byte[] nv21, int width, int height) {
        Bitmap bitmap = null;
        try {
            YuvImage image = new YuvImage(nv21, ImageFormat.NV21, width, height, null);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            image.compressToJpeg(new Rect(0, 0, width, height), 80, stream);
            bitmap = BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size());
            stream.close();
        } catch (IOException e) {
            e.printStackTrace();
            XLog.e(TAG, "nv21ToBitmap : " + e);
        }
        return bitmap;
    }

    private ImageHelper() {
        throw new UnsupportedOperationException("u can't initialize me!");
    }
}
