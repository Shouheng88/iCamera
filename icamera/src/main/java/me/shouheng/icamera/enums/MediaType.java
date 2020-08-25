package me.shouheng.icamera.enums;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static me.shouheng.icamera.enums.MediaType.*;

/**
 * Media type
 * 
 * @author <a href="mailto:shouheng2015@gmail.com">WngShhng</a>
 * @version 2019-12-28 16:29
 */
@IntDef(value = {TYPE_PICTURE, TYPE_VIDEO})
@Retention(value = RetentionPolicy.SOURCE)
public @interface MediaType {

    /** Picture */
    int TYPE_PICTURE    = 0;

    /** Video */
    int TYPE_VIDEO      = 1;
}
