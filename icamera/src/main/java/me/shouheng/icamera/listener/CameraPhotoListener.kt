package me.shouheng.icamera.listener

import java.io.File

/**
 * @author WngShhng (shouheng2015@gmail.com)
 * @version 2019/4/14 14:30
 */
interface CameraPhotoListener {
    fun onPictureTaken(data: ByteArray, picture: File)
    fun onCaptureFailed(throwable: Throwable)
}