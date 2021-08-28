package me.shouheng.icamera.enums

import androidx.annotation.IntDef

/**
 * @author [WngShhng](mailto:shouheng2015@gmail.com)
 * @version 2020-08-27 10:13
 */
@IntDef(
    SensorPosition.SENSOR_POSITION_UP,
    SensorPosition.SENSOR_POSITION_UP_SIDE_DOWN,
    SensorPosition.SENSOR_POSITION_LEFT,
    SensorPosition.SENSOR_POSITION_RIGHT,
    SensorPosition.SENSOR_POSITION_UNSPECIFIED
)
@kotlin.annotation.Retention(AnnotationRetention.SOURCE)
annotation class SensorPosition {
    companion object {
        const val SENSOR_POSITION_UP = 90
        const val SENSOR_POSITION_UP_SIDE_DOWN = 270
        const val SENSOR_POSITION_LEFT = 0
        const val SENSOR_POSITION_RIGHT = 180
        const val SENSOR_POSITION_UNSPECIFIED = -1
    }
}