package me.shouheng.icamera.enums

import androidx.annotation.IntDef

/**
 * Camera preview adjust type, will be used to decide how
 * the imagery is displayed when the preview view size diffs
 * from the imagery size.
 *
 * @author [WngShhng](mailto:shouheng2015@gmail.com)
 * @version 2019-12-28 11:55
 */
@IntDef(
    PreviewAdjustType.NONE,
    PreviewAdjustType.WIDTH_FIRST,
    PreviewAdjustType.HEIGHT_FIRST,
    PreviewAdjustType.SMALLER_FIRST,
    PreviewAdjustType.LARGER_FIRST
)
@kotlin.annotation.Retention(AnnotationRetention.SOURCE)
annotation class PreviewAdjustType {
    companion object {
        /** The imagery will be stretched to fit the view  */
        const val NONE = 0

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
         * +-------------+  */
        const val WIDTH_FIRST = 1

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
         * +-------------+  */
        const val HEIGHT_FIRST = 2

        /**
         * Use the smaller side between height and width,
         * another will be stretched to meet the aspect ratio.
         *
         * @see .WIDTH_FIRST
         *
         * @see .HEIGHT_FIRST
         */
        const val SMALLER_FIRST = 3

        /**
         * Use the larger side between height and width,
         * another will be stretched to meet the aspect ratio.
         *
         * @see .WIDTH_FIRST
         *
         * @see .HEIGHT_FIRST
         */
        const val LARGER_FIRST = 4
    }
}