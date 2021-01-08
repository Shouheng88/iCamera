package me.shouheng.icamera.enums

import android.support.annotation.IntDef

/**
 * Camera preview view type.
 *
 * @author [WngShhng](mailto:shouheng2015@gmail.com)
 * @version 2019-12-28 11:26
 */
@IntDef(value = [PreviewViewType.SURFACE_VIEW, PreviewViewType.TEXTURE_VIEW])
@kotlin.annotation.Retention(AnnotationRetention.SOURCE)
annotation class PreviewViewType {
    companion object {
        /** [android.view.SurfaceView] will be used  */
        const val SURFACE_VIEW = 0

        /** [android.view.TextureView] will be used  */
        const val TEXTURE_VIEW = 1
    }
}