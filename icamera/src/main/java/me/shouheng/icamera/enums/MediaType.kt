package me.shouheng.icamera.enums

import androidx.annotation.IntDef

/**
 * Media type
 *
 * @author [WngShhng](mailto:shouheng2015@gmail.com)
 * @version 2019-12-28 16:29
 */
@IntDef(value = [MediaType.TYPE_PICTURE, MediaType.TYPE_VIDEO])
@kotlin.annotation.Retention(AnnotationRetention.SOURCE)
annotation class MediaType {
    companion object {
        /** Picture  */
        const val TYPE_PICTURE = 0
        /** Video  */
        const val TYPE_VIDEO = 1
    }
}