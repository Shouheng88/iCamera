package me.shouheng.camerax.enums;

import android.graphics.ImageFormat;
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

    public static final int UNKNOWN          = ImageFormat.UNKNOWN;
    public static final int RGB_565          = ImageFormat.RGB_565;
    public static final int YV12             = ImageFormat.YV12;
    public static final int NV16             = ImageFormat.NV16;
    public static final int NV21             = ImageFormat.NV21;
    public static final int YUY2             = ImageFormat.YUY2;
    public static final int JPEG             = ImageFormat.JPEG;
    public static final int YUV_420_888      = ImageFormat.YUV_420_888;
    public static final int YUV_422_888      = ImageFormat.YUV_422_888;
    public static final int RYUV_444_888     = ImageFormat.YUV_444_888;
    public static final int FLEX_RGB_888     = ImageFormat.FLEX_RGB_888;
    public static final int FLEX_RGBA_8888   = ImageFormat.FLEX_RGBA_8888;
    public static final int RAW_SENSOR       = ImageFormat.RAW_SENSOR;
    public static final int RAW_PRIVATE      = ImageFormat.RAW_PRIVATE;
    public static final int RAW10            = ImageFormat.RAW10;
    public static final int RAW12            = ImageFormat.RAW12;
    public static final int DEPTH16          = ImageFormat.DEPTH16;
    public static final int DEPTH_POINT_CLOUD= ImageFormat.DEPTH_POINT_CLOUD;
    public static final int PRIVATE          = ImageFormat.PRIVATE;

    public static final int FOCUS_MODE_NONE                 = 0;
    public static final int FOCUS_MODE_AUTO                 = 1;
    public static final int FOCUS_MODE_INFINITY             = 2;
    public static final int FOCUS_MODE_MACRO                = 3;
    public static final int FOCUS_MODE_FIXED                = 4;
    public static final int FOCUS_MODE_EDOF                 = 5;
    public static final int FOCUS_MODE_CONTINUOUS_VIDEO     = 6;
    public static final int FOCUS_MODE_CONTINUOUS_PICTURE   = 7;
    public static final int FOCUS_MODE_ADAPTION             = 9;

    @IntDef({FOCUS_MODE_NONE, FOCUS_MODE_AUTO, FOCUS_MODE_INFINITY, FOCUS_MODE_MACRO, FOCUS_MODE_FIXED,
            FOCUS_MODE_EDOF, FOCUS_MODE_CONTINUOUS_VIDEO, FOCUS_MODE_CONTINUOUS_PICTURE, FOCUS_MODE_ADAPTION})
    @Retention(RetentionPolicy.SOURCE)
    public @interface FocusMode {
    }

    @IntDef({UNKNOWN, RGB_565, YV12, NV16, NV21, YUY2, JPEG, YUV_420_888, YUV_422_888,
            RYUV_444_888, FLEX_RGB_888, FLEX_RGBA_8888, RAW_SENSOR, RAW_PRIVATE,
            RAW10, RAW12, DEPTH16, DEPTH_POINT_CLOUD, PRIVATE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface PreviewFormat {
    }
    
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
