package me.shouheng.shining.enums;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author WngShhng (shouheng2015@gmail.com)
 * @version 2019/4/14 11:58
 */
public final class Preview {

    public static final int SURFACE_VIEW    = 0;
    public static final int TEXTURE_VIEW    = 1;

    public static final int NONE                   = 0;
    public static final int WIDTH_FIRST            = 1;
    public static final int HEIGHT_FIRST           = 2;
    public static final int SMALLER_FIRST          = 3;
    public static final int LARGER_FIRST           = 4;

    @IntDef(value = {SURFACE_VIEW, TEXTURE_VIEW})
    @Retention(value = RetentionPolicy.SOURCE)
    public @interface Type {
    }

    @IntDef({NONE, WIDTH_FIRST, HEIGHT_FIRST, SMALLER_FIRST, LARGER_FIRST})
    public @interface AdjustType {
    }
}
