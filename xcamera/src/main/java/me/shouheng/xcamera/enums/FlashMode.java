package me.shouheng.xcamera.enums;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static me.shouheng.xcamera.enums.FlashMode.*;

/**
 * Flash mode
 *
 * @author WngShhng (shouheng2015@gmail.com)
 * @version 2019/4/14 21:57
 */
@IntDef({FLASH_ON, FLASH_OFF, FLASH_AUTO})
@Retention(RetentionPolicy.SOURCE)
public @interface FlashMode {

    /**
     * Flash on
     */
    int FLASH_ON    = 0;

    /**
     * Flash off
     */
    int FLASH_OFF   = 1;

    /**
     * Auto
     */
    int FLASH_AUTO  = 2;
}
