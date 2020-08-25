package me.shouheng.icamera.enums;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static me.shouheng.icamera.enums.CameraSizeFor.*;

/**
 * Camera size for preview, picture, video, etc.
 *
 * @author <a href="mailto:shouheng2015@gmail.com">WngShhng</a>
 * @version 2019-12-28 16:38
 */
@IntDef(value = {SIZE_FOR_PREVIEW, SIZE_FOR_PICTURE, SIZE_FOR_VIDEO})
@Retention(value = RetentionPolicy.SOURCE)
public @interface CameraSizeFor {

    /** Camera size for preview */
    int SIZE_FOR_PREVIEW    = 0x0010;

    /** Camera size for picture */
    int SIZE_FOR_PICTURE    = 0x0020;

    /** Camera size for video */
    int SIZE_FOR_VIDEO      = 0x0040;
}
