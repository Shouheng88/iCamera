package me.shouheng.camerax.enums;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public final class Camera {

    public static final int FLASH_AUTO                      = 0;
    public static final int FLASH_ON                        = 1;
    public static final int FLASH_OFF                       = 2;
    public static final int FLASH_TORCH                     = 3;
    public static final int FLASH_RED_EYE                   = 4;

    public static final int CAMERA_FACE_FRONT               = 0;
    public static final int CAMERA_FACE_REAR                = 1;

    public static final int SENSOR_POSITION_UNSPECIFIED     = -1;
    public static final int SENSOR_POSITION_LEFT            = 0;
    public static final int SENSOR_POSITION_UP              = 90;
    public static final int SENSOR_POSITION_RIGHT           = 180;
    public static final int SENSOR_POSITION_UP_SIDE_DOWN    = 270;

    public static final int ORIENTATION_PORTRAIT            = 0;
    public static final int ORIENTATION_LANDSCAPE           = 1;

    public static final int NONE                            = 0;
    public static final int FIXED_WIDTH                     = 1;
    public static final int FIXED_HEIGHT                    = 2;
    public static final int SCALE_SMALLER                   = 3;
    public static final int SCALE_LARGER                    = 4;

    @IntDef({FLASH_ON, FLASH_OFF, FLASH_AUTO, FLASH_TORCH, FLASH_RED_EYE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface FlashMode {
    }

    @IntDef({CAMERA_FACE_FRONT, CAMERA_FACE_REAR})
    @Retention(RetentionPolicy.SOURCE)
    public @interface CameraFace {
    }

    @IntDef({SENSOR_POSITION_UP, SENSOR_POSITION_UP_SIDE_DOWN, SENSOR_POSITION_LEFT, SENSOR_POSITION_RIGHT, SENSOR_POSITION_UNSPECIFIED})
    @Retention(RetentionPolicy.SOURCE)
    public @interface SensorPosition {
    }

    @IntDef({ORIENTATION_PORTRAIT, ORIENTATION_LANDSCAPE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ScreenOrientation {
    }

    @IntDef({NONE, FIXED_WIDTH, FIXED_HEIGHT, SCALE_SMALLER, SCALE_LARGER})
    public @interface AdjustType { }
}
