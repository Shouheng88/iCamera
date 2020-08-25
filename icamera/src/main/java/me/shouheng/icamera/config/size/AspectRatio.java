package me.shouheng.icamera.config.size;

import android.support.annotation.IntRange;
import android.support.annotation.NonNull;

import me.shouheng.icamera.util.XLog;

/**
 * Aspect ratio
 *
 * @author WngShhng (shouheng2015@gmail.com)
 * @version 2019/4/13 23:08
 */
public class AspectRatio {

    private static final String TAG = "AspectRatio";

    private double ratio;

    public final int widthRatio;

    public final int heightRatio;

    public static AspectRatio of(@NonNull Size size) {
        return new AspectRatio(size.width, size.height);
    }

    public static AspectRatio of(@IntRange(from = 1) int widthRatio, @IntRange(from = 0) int heightRatio) {
        return new AspectRatio(widthRatio, heightRatio);
    }

    public static AspectRatio parse(String s) {
        int position = s.indexOf(':');
        if (position == -1) {
            throw new IllegalArgumentException("Malformed aspect ratio: " + s);
        }
        try {
            int x = Integer.parseInt(s.substring(0, position));
            int y = Integer.parseInt(s.substring(position + 1));
            XLog.d(TAG, "Parsed aspect ratio from string as " + x + " : " + y);
            return AspectRatio.of(x, y);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Malformed aspect ratio: " + s, e);
        }
    }

    private AspectRatio(int widthRatio, int heightRatio) {
        this.widthRatio = widthRatio;
        this.heightRatio = heightRatio;
    }

    public double ratio() {
        if (ratio == 0) {
            ratio = (double) heightRatio / widthRatio;
        }
        return ratio;
    }

    public AspectRatio inverse() {
        return AspectRatio.of(heightRatio, widthRatio);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AspectRatio that = (AspectRatio) o;

        return Double.compare(that.ratio(), ratio()) == 0;
    }

    @Override
    public int hashCode() {
        long temp = Double.doubleToLongBits(ratio());
        return (int) (temp ^ (temp >>> 32));
    }

    @NonNull
    @Override
    public String toString() {
        return "AspectRatio{" +
                "ratio=" + ratio +
                ", widthRatio=" + widthRatio +
                ", heightRatio=" + heightRatio +
                '}';
    }
}
