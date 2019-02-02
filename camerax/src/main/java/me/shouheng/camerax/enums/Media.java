package me.shouheng.camerax.enums;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public final class Media {

    public static final int MEDIA_ACTION_UNSPECIFIED    = 0;
    public static final int MEDIA_ACTION_PHOTO          = 1;
    public static final int MEDIA_ACTION_VIDEO          = 2;

    public static final int MEDIA_QUALITY_AUTO          = 0;
    public static final int MEDIA_QUALITY_LOWEST        = 1;
    public static final int MEDIA_QUALITY_LOW           = 2;
    public static final int MEDIA_QUALITY_MEDIUM        = 3;
    public static final int MEDIA_QUALITY_HIGH          = 4;
    public static final int MEDIA_QUALITY_HIGHEST       = 5;

    @IntDef({MEDIA_ACTION_PHOTO, MEDIA_ACTION_VIDEO, MEDIA_ACTION_UNSPECIFIED})
    @Retention(RetentionPolicy.SOURCE)
    public @interface MediaAction {
    }

    @IntDef({MEDIA_QUALITY_AUTO, MEDIA_QUALITY_LOWEST, MEDIA_QUALITY_LOW, MEDIA_QUALITY_MEDIUM, MEDIA_QUALITY_HIGH, MEDIA_QUALITY_HIGHEST})
    @Retention(RetentionPolicy.SOURCE)
    public @interface MediaQuality {
    }
}
