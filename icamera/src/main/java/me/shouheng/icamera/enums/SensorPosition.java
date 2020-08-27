package me.shouheng.icamera.enums;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static me.shouheng.icamera.enums.SensorPosition.SENSOR_POSITION_LEFT;
import static me.shouheng.icamera.enums.SensorPosition.SENSOR_POSITION_RIGHT;
import static me.shouheng.icamera.enums.SensorPosition.SENSOR_POSITION_UNSPECIFIED;
import static me.shouheng.icamera.enums.SensorPosition.SENSOR_POSITION_UP;
import static me.shouheng.icamera.enums.SensorPosition.SENSOR_POSITION_UP_SIDE_DOWN;

/**
 * @author <a href="mailto:shouheng2015@gmail.com">WngShhng</a>
 * @version 2020-08-27 10:13
 */
@IntDef({SENSOR_POSITION_UP, SENSOR_POSITION_UP_SIDE_DOWN, SENSOR_POSITION_LEFT, SENSOR_POSITION_RIGHT, SENSOR_POSITION_UNSPECIFIED})
@Retention(RetentionPolicy.SOURCE)
public @interface SensorPosition {

    int SENSOR_POSITION_UP              = 90;
    int SENSOR_POSITION_UP_SIDE_DOWN    = 270;
    int SENSOR_POSITION_LEFT            = 0;
    int SENSOR_POSITION_RIGHT           = 180;
    int SENSOR_POSITION_UNSPECIFIED     = -1;
}

