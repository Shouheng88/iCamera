package me.shouheng.icamera.enums;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static me.shouheng.icamera.enums.DeviceDefaultOrientation.ORIENTATION_LANDSCAPE;
import static me.shouheng.icamera.enums.DeviceDefaultOrientation.ORIENTATION_PORTRAIT;

/**
 * @author <a href="mailto:shouheng2015@gmail.com">WngShhng</a>
 * @version 2020-08-27 10:07
 */
@IntDef({ORIENTATION_PORTRAIT, ORIENTATION_LANDSCAPE})
@Retention(RetentionPolicy.SOURCE)
public @interface DeviceDefaultOrientation {
    int ORIENTATION_PORTRAIT    = 0x01;
    int ORIENTATION_LANDSCAPE   = 0x02;
}
