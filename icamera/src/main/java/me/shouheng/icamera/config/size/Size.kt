package me.shouheng.icamera.config.size

import android.annotation.TargetApi
import android.hardware.Camera
import android.os.Build
import android.support.annotation.IntRange
import java.util.*

/**
 * The standard size to use in camera.
 *
 * @author WngShhng (shouheng2015@gmail.com)
 * @version 2019/4/13 23:09
 */
class Size private constructor(
    /**  The width of size.  */
    @param:IntRange(from = 1) val width: Int,
    /** The height of size.  */
    @param:IntRange(from = 0) val height: Int
) {

    /** The area of size, call [.area] to get size.  */
    private var area = -1

    /**
     * The height width ratio of size: <pre>ratio = height / width</pre>.
     * Call [.ratio] to get ratio.
     */
    private var ratio = 0.0

    /**
     * The area of current size, calculated only when necessary.
     *
     * @return the area of size.
     */
    fun area(): Int {
        if (area == -1) {
            area = width * height
        }
        return area
    }

    /**
     * The ratio of current size, calculated only when necessary.
     *
     * @return the ratio
     */
    fun ratio(): Double {
        if (ratio == 0.0 && width != 0) {
            ratio = height.toDouble() / width
        }
        return ratio
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val size = other as Size
        return if (width != size.width) false else height == size.height
    }

    override fun hashCode(): Int {
        var result = width
        result = 31 * result + height
        return result
    }

    override fun toString(): String {
        return "($width, $height)"
    }

    companion object {
        /**
         * Get size of given width and height.
         *
         * @param width  the width
         * @param height the height
         * @return       the size
         */
        fun of(@IntRange(from = 1) width: Int, @IntRange(from = 0) height: Int): Size = Size(width, height)

        /**
         * Get sizes from [Camera.Size] for camera1.
         *
         * @param cameraSizes the camera sizes support of camera1
         * @return            the standard size
         */
        fun fromList(cameraSizes: List<Camera.Size>): List<Size> {
            val sizes: MutableList<Size> = ArrayList(cameraSizes.size)
            cameraSizes.forEach { sizes.add(of(it.width, it.height)) }
            return sizes
        }

        /**
         * Get sizes from [android.util.Size] for camera2.
         *
         * @param cameraSizes the camera sizes supported by camera2
         * @return            the standard size
         */
        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        fun fromList(cameraSizes: Array<android.util.Size>): List<Size> {
            val sizes: MutableList<Size> = ArrayList(cameraSizes.size)
            cameraSizes.forEach { sizes.add(of(it.width, it.height)) }
            return sizes
        }
    }

}