package me.shouheng.icamera.config.size;

import android.annotation.TargetApi;
import android.hardware.Camera;
import android.os.Build;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * The standard size to use in camera.
 *
 * @author WngShhng (shouheng2015@gmail.com)
 * @version 2019/4/13 23:09
 */
public class Size {

    /**  The width of size. */
    public final int width;

    /** The height of size. */
    public final int height;

    /** The area of size, call {@link #area()} to get size. */
    private int area = -1;

    /**
     * The height width ratio of size: <pre>ratio = height / width</pre>.
     * Call {@link #ratio()} to get ratio.
     */
    private double ratio;

    /**
     * Get size of given width and height.
     *
     * @param width  the width
     * @param height the height
     * @return       the size
     */
    public static Size of(@IntRange(from = 1) int width, @IntRange(from = 0) int height) {
        return new Size(width, height);
    }

    /**
     * Get sizes from {@link Camera.Size} for camera1.
     *
     * @param cameraSizes the camera sizes support of camera1
     * @return            the standard size
     */
    public static List<Size> fromList(@NonNull List<Camera.Size> cameraSizes) {
        List<Size> sizes = new ArrayList<>(cameraSizes.size());
        for (Camera.Size size : cameraSizes) {
            sizes.add(of(size.width, size.height));
        }
        return sizes;
    }

    /**
     * Get sizes from {@link android.util.Size} for camera2.
     *
     * @param cameraSizes the camera sizes supported by camera2
     * @return            the standard size
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static List<Size> fromList(@NonNull android.util.Size[] cameraSizes) {
        List<Size> sizes = new ArrayList<>(cameraSizes.length);
        for (android.util.Size size : cameraSizes) {
            sizes.add(of(size.getWidth(), size.getHeight()));
        }
        return sizes;
    }

    private Size(@IntRange(from = 1) int width, @IntRange(from = 0) int height) {
        this.width = width;
        this.height = height;
    }

    /**
     * The area of current size, calculated only when necessary.
     *
     * @return the area of size.
     */
    public int area() {
        if (area == -1) {
            area = width * height;
        }
        return area;
    }

    /**
     * The ratio of current size, calculated only when necessary.
     *
     * @return the ratio
     */
    public double ratio() {
        if (ratio == 0 && width != 0) {
            ratio = (double) height / width;
        }
        return ratio;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Size size = (Size) o;

        if (width != size.width) return false;
        return height == size.height;
    }

    @Override
    public int hashCode() {
        int result = width;
        result = 31 * result + height;
        return result;
    }

    @NonNull
    @Override
    public String toString() {
        return "(" + width +  ", " + height + ")";
    }
}
