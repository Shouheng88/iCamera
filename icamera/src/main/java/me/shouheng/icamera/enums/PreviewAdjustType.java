package me.shouheng.icamera.enums;

import android.support.annotation.IntDef;

import static me.shouheng.icamera.enums.PreviewAdjustType.*;

/**
 * Camera preview adjust type, will be used to decide how
 * the imagery is displayed when the preview view size diffs
 * from the imagery size.
 *
 * @author <a href="mailto:shouheng2015@gmail.com">WngShhng</a>
 * @version 2019-12-28 11:55
 */
@IntDef({NONE, WIDTH_FIRST, HEIGHT_FIRST, SMALLER_FIRST, LARGER_FIRST})
public @interface PreviewAdjustType {

    /** The imagery will be stretched to fit the view */
    int NONE                   = 0;

    /**
     * Use the width of view,
     * the height will be stretched to meet the aspect ratio.
     *
     * Example:
     *
     * +-------------+
     * |             |
     * |/////////////|
     * |/////////////|
     * |//  image  //|
     * |/////////////|
     * |/////////////|
     * |             |
     * +-------------+ */
    int WIDTH_FIRST            = 1;

    /**
     * Use the height of view,
     * the width will be stretched to meet the aspect ratio.
     *
     * Example:
     *
     * +-------------+
     * |  /////////  |
     * |  /////////  |
     * |  /////////  |
     * |  / image /  |
     * |  /////////  |
     * |  /////////  |
     * |  /////////  |
     * +-------------+ */
    int HEIGHT_FIRST           = 2;

    /**
     * Use the smaller side between height and width,
     * another will be stretched to meet the aspect ratio.
     *
     * @see #WIDTH_FIRST
     * @see #HEIGHT_FIRST */
    int SMALLER_FIRST          = 3;

    /**
     * Use the larger side between height and width,
     * another will be stretched to meet the aspect ratio.
     *
     * @see #WIDTH_FIRST
     * @see #HEIGHT_FIRST */
    int LARGER_FIRST           = 4;
}
