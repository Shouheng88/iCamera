package me.shouheng.camerax.enums;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author WngShhng (shouheng2015@gmail.com)
 * @version 2019/4/14 11:58
 */
public final class Preview {

    public static final int SURFACE_VIEW = 0;
    public static final int TEXTURE_VIEW = 1;

    @IntDef(value = {SURFACE_VIEW, TEXTURE_VIEW})
    @Retention(value = RetentionPolicy.SOURCE)
    public @interface Type {
    }
}
