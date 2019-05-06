package me.shouheng.camerax.enums;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author WngShhng (shouheng2015@gmail.com)
 * @version 2019/4/14 11:57
 */
public final class Camera {

    public static final int FACE_REAR           = 0x0;
    public static final int FACE_FRONT          = 0x1;

    public static final int SIZE_FOR_PREVIEW    = 0x10;
    public static final int SIZE_FOR_PICTURE    = 0x20;
    public static final int SIZE_FOR_VIDEO      = 0x40;

    public static final int TYPE_CAMERA1        = 0x100;
    public static final int TYPE_CAMERA2        = 0x200;

    @IntDef(value = {FACE_REAR, FACE_FRONT})
    @Retention(value = RetentionPolicy.SOURCE)
    public @interface Face {
    }

    @IntDef(value = {SIZE_FOR_PREVIEW, SIZE_FOR_PICTURE, SIZE_FOR_VIDEO})
    @Retention(value = RetentionPolicy.SOURCE)
    public @interface SizeFor {
    }

    @IntDef(value = {TYPE_CAMERA1, TYPE_CAMERA2})
    @Retention(value = RetentionPolicy.SOURCE)
    public @interface Type {
    }
}
