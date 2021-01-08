package me.shouheng.icamera.enums

import android.support.annotation.IntDef

/**
 * Camera face
 *
 * @author [WngShhng](mailto:shouheng2015@gmail.com)
 * @version 2019-12-28 16:38
 */
@IntDef(value = [CameraFace.FACE_REAR, CameraFace.FACE_FRONT])
@kotlin.annotation.Retention(AnnotationRetention.SOURCE)
annotation class CameraFace {
    companion object {
        /** Rear camera  */
        const val FACE_REAR = 0x0000
        /** Front camera  */
        const val FACE_FRONT = 0x0001
    }
}