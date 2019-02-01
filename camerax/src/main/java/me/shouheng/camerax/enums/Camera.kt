package me.shouheng.camerax.enums

import android.support.annotation.IntDef

/**
 * Created on 2019/2/1.
 */
const val CAMERA_TYPE_BACK = 0
const val CAMERA_TYPE_FRONT = 1

@IntDef(CAMERA_TYPE_BACK, CAMERA_TYPE_FRONT)
@Retention(AnnotationRetention.SOURCE)
annotation class Facing