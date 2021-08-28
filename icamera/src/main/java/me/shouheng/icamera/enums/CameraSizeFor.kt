package me.shouheng.icamera.enums

import androidx.annotation.IntDef

/**
 * Camera size for preview, picture, video, etc.
 *
 * @author [WngShhng](mailto:shouheng2015@gmail.com)
 * @version 2019-12-28 16:38
 */
@IntDef(value = [
    CameraSizeFor.SIZE_FOR_PREVIEW,
    CameraSizeFor.SIZE_FOR_PICTURE,
    CameraSizeFor.SIZE_FOR_VIDEO])
@kotlin.annotation.Retention(AnnotationRetention.SOURCE)
annotation class CameraSizeFor {
    companion object {
        /** Camera size for preview  */
        const val SIZE_FOR_PREVIEW = 0x0010

        /** Camera size for picture  */
        const val SIZE_FOR_PICTURE = 0x0020

        /** Camera size for video  */
        const val SIZE_FOR_VIDEO = 0x0040
    }
}