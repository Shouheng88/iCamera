package me.shouheng.icamera.listener;

import android.hardware.Camera;

import me.shouheng.icamera.config.size.Size;

/**
 * The camera preview callback
 *
 * @author WngShhng (shouheng2015@gmail.com)
 * @version 2020/9/1 21:36
 */
public interface CameraPreviewListener {

    /**
     * On get the preview frame data. For camera2, the image format will always be
     * {@link android.graphics.ImageFormat#NV21}. For camera1 the image format will be
     * NV21 if you didn't set preview image format by calling
     * {@link Camera.Parameters#setPreviewFormat(int)}, else the returned byte array and
     * image format will be the format you set.
     *
     * @param data   the image data byte array
     * @param size   the preview image size
     * @param format the format of {@link android.graphics.ImageFormat}
     *
     * @see android.graphics.ImageFormat
     */
    void onPreviewFrame(byte[] data, Size size, int format);
}
