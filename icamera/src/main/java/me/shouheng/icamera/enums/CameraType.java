package me.shouheng.icamera.enums;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static me.shouheng.icamera.enums.CameraType.*;

/**
 * Camera1 or camera2
 *
 * @author <a href="mailto:shouheng2015@gmail.com">WngShhng</a>
 * @version 2019-12-28 16:39
 */
@IntDef(value = {TYPE_CAMERA1, TYPE_CAMERA2})
@Retention(value = RetentionPolicy.SOURCE)
public @interface CameraType {

    /** Camera1 */
    int TYPE_CAMERA1        = 0x0100;

    /** Camera2 */
    int TYPE_CAMERA2        = 0x0200;
}
