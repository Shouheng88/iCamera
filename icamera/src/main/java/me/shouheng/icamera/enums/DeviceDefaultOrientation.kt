package me.shouheng.icamera.enums

import androidx.annotation.IntDef

/**
 * @author [WngShhng](mailto:shouheng2015@gmail.com)
 * @version 2020-08-27 10:07
 */
@IntDef(DeviceDefaultOrientation.ORIENTATION_PORTRAIT,
    DeviceDefaultOrientation.ORIENTATION_LANDSCAPE)
@kotlin.annotation.Retention(AnnotationRetention.SOURCE)
annotation class DeviceDefaultOrientation {
    companion object {
        const val ORIENTATION_PORTRAIT = 0x01
        const val ORIENTATION_LANDSCAPE = 0x02
    }
}