package me.shouheng.icamera.enums

import android.support.annotation.IntDef

/**
 * Flash mode
 *
 * @author WngShhng (shouheng2015@gmail.com)
 * @version 2019/4/14 21:57
 */
@IntDef(FlashMode.FLASH_ON, FlashMode.FLASH_OFF, FlashMode.FLASH_AUTO)
@kotlin.annotation.Retention(AnnotationRetention.SOURCE)
annotation class FlashMode {
    companion object {
        /** Flash on  */
        const val FLASH_ON = 0
        /** Flash off  */
        const val FLASH_OFF = 1
        /** Auto  */
        const val FLASH_AUTO = 2
    }
}