package me.shouheng.icamera.listener

import me.shouheng.icamera.config.size.Size

/**
 * The camera preview callback
 *
 * @author WngShhng (shouheng2015@gmail.com)
 * @version 2020/9/1 21:36
 */
interface CameraPreviewListener {
    /**
     * On get the preview frame data. For camera2, the image format will always be
     * [android.graphics.ImageFormat.NV21]. For camera1 the image format will be
     * NV21 if you didn't set preview image format by calling
     * [Camera.Parameters.setPreviewFormat], else the returned byte array and
     * image format will be the format you set.
     *
     * @param data   the image data byte array
     * @param size   the preview image size
     * @param format the format of [android.graphics.ImageFormat]
     *
     * @see android.graphics.ImageFormat
     */
    fun onPreviewFrame(data: ByteArray, size: Size, format: Int)
}