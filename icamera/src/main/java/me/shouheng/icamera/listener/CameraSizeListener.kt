package me.shouheng.icamera.listener

import me.shouheng.icamera.config.size.Size

/**
 * @author WngShhng (shouheng2015@gmail.com)
 * @version 2019/4/16 22:46
 */
interface CameraSizeListener {
    fun onPreviewSizeUpdated(previewSize: Size)
    fun onVideoSizeUpdated(videoSize: Size)
    fun onPictureSizeUpdated(pictureSize: Size)
}