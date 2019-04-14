package me.shouheng.camerax.enums;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author WngShhng (shouheng2015@gmail.com)
 * @version 2019/4/14 15:33
 */
public final class Media {

    public static final int TYPE_PICTURE = 0;
    public static final int TYPE_VIDEO = 1;

    public static final int QUALITY_AUTO = 0;
    public static final int QUALITY_LOWEST = 1;
    public static final int QUALITY_LOW = 2;
    public static final int QUALITY_MEDIUM = 3;
    public static final int QUALITY_HIGH = 4;
    public static final int QUALITY_HIGHEST = 5;

    @IntDef(value = {TYPE_PICTURE, TYPE_VIDEO})
    @Retention(value = RetentionPolicy.SOURCE)
    public @interface Type {
    }

    @IntDef(value = {QUALITY_AUTO, QUALITY_LOWEST, QUALITY_LOW, QUALITY_MEDIUM, QUALITY_HIGH, QUALITY_HIGHEST})
    @Retention(value = RetentionPolicy.SOURCE)
    public @interface Quality {
    }
}
