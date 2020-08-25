package me.shouheng.icamera.enums;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static me.shouheng.icamera.enums.CameraFace.*;

/**
 * Camera face
 *
 * @author <a href="mailto:shouheng2015@gmail.com">WngShhng</a>
 * @version 2019-12-28 16:38
 */
@IntDef(value = {FACE_REAR, FACE_FRONT})
@Retention(value = RetentionPolicy.SOURCE)
public @interface CameraFace {

    /** Rear camera */
    int FACE_REAR           = 0x0000;

    /** Front camera */
    int FACE_FRONT          = 0x0001;
}
