package me.shouheng.icamera.enums

import androidx.annotation.IntDef

/**
 * Camera1 or camera2
 *
 * @author [WngShhng](mailto:shouheng2015@gmail.com)
 * @version 2019-12-28 16:39
 */
@IntDef(value = [CameraType.TYPE_CAMERA1, CameraType.TYPE_CAMERA2])
@kotlin.annotation.Retention(AnnotationRetention.SOURCE)
annotation class CameraType {
    companion object {
        /** Camera1  */
        const val TYPE_CAMERA1 = 0x0100
        /** Camera2  */
        const val TYPE_CAMERA2 = 0x0200
    }
}