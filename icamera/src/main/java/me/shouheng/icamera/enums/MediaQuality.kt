package me.shouheng.icamera.enums

import android.support.annotation.IntDef

/**
 * Media quality
 *
 * @author [WngShhng](mailto:shouheng2015@gmail.com)
 * @version 2019-12-28 16:30
 */
@IntDef(value = [MediaQuality.QUALITY_AUTO,
    MediaQuality.QUALITY_LOWEST,
    MediaQuality.QUALITY_LOW,
    MediaQuality.QUALITY_MEDIUM,
    MediaQuality.QUALITY_HIGH,
    MediaQuality.QUALITY_HIGHEST])
@kotlin.annotation.Retention(AnnotationRetention.SOURCE)
annotation class MediaQuality {
    companion object {
        /** Auto  */
        const val QUALITY_AUTO = 0
        /** Lowest quality  */
        const val QUALITY_LOWEST = 1
        /** Low quality  */
        const val QUALITY_LOW = 2
        /** Medium quality  */
        const val QUALITY_MEDIUM = 3
        /** High quality  */
        const val QUALITY_HIGH = 4
        /** Highest quality  */
        const val QUALITY_HIGHEST = 5
    }
}