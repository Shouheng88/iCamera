package me.shouheng.icamera.config.size

import android.support.annotation.IntRange
import me.shouheng.icamera.util.XLog.d

/**
 * Aspect ratio
 *
 * @author WngShhng (shouheng2015@gmail.com)
 * @version 2019/4/13 23:08
 */
class AspectRatio private constructor(val widthRatio: Int, val heightRatio: Int) {

    private var ratio = 0.0

    fun ratio(): Double {
        if (ratio == 0.0) {
            ratio = heightRatio.toDouble() / widthRatio
        }
        return ratio
    }

    fun inverse(): AspectRatio = of(heightRatio, widthRatio)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as AspectRatio
        return java.lang.Double.compare(that.ratio(), ratio()) == 0
    }

    override fun hashCode(): Int {
        val temp = java.lang.Double.doubleToLongBits(ratio())
        return (temp xor (temp ushr 32)).toInt()
    }

    override fun toString(): String {
        return "AspectRatio{" +
                "ratio=" + ratio +
                ", widthRatio=" + widthRatio +
                ", heightRatio=" + heightRatio +
                '}'
    }

    companion object {

        fun of(size: Size): AspectRatio = AspectRatio(size.width, size.height)

        @JvmStatic
        fun of(@IntRange(from = 1) wr: Int, @IntRange(from = 0) hr: Int): AspectRatio {
            return AspectRatio(wr, hr)
        }

        fun parse(s: String): AspectRatio {
            val position = s.indexOf(':')
            require(position != -1) { "Malformed aspect ratio: $s" }
            return try {
                val x = s.substring(0, position).toInt()
                val y = s.substring(position + 1).toInt()
                d("AspectRatio", "Parsed aspect ratio from string as $x : $y")
                of(x, y)
            } catch (e: NumberFormatException) {
                throw IllegalArgumentException("Malformed aspect ratio: $s", e)
            }
        }
    }

}