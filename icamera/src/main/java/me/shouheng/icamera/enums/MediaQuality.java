package me.shouheng.icamera.enums;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static me.shouheng.icamera.enums.MediaQuality.*;

/**
 * Media quality
 *
 * @author <a href="mailto:shouheng2015@gmail.com">WngShhng</a>
 * @version 2019-12-28 16:30
 */
@IntDef(value = {QUALITY_AUTO, QUALITY_LOWEST, QUALITY_LOW, QUALITY_MEDIUM, QUALITY_HIGH, QUALITY_HIGHEST})
@Retention(value = RetentionPolicy.SOURCE)
public @interface MediaQuality {

    /** Auto */
    int QUALITY_AUTO        = 0;

    /** Lowest quality */
    int QUALITY_LOWEST      = 1;

    /** Low quality */
    int QUALITY_LOW         = 2;

    /** Medium quality */
    int QUALITY_MEDIUM      = 3;

    /** High quality */
    int QUALITY_HIGH        = 4;

    /** Highest quality */
    int QUALITY_HIGHEST     = 5;
}
