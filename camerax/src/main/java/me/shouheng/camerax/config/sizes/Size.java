package me.shouheng.camerax.config.sizes;

import android.annotation.TargetApi;
import android.hardware.Camera;
import android.os.Build;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * @author WngShhng (shouheng2015@gmail.com)
 * @version 2019/4/13 23:09
 */
public class Size {

    public final int width;

    public final int height;

    private int area = -1;

    private double ratio;

    public static Size of(@IntRange(from = 1) int width, @IntRange(from = 0) int height) {
        return new Size(width, height);
    }

    public static List<Size> fromList(@NonNull List<Camera.Size> cameraSizes) {
        List<Size> sizes = new ArrayList<>(cameraSizes.size());
        for (Camera.Size size : cameraSizes) {
            sizes.add(of(size.width, size.height));
        }
        return sizes;
    }

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

    public int area() {
        if (area == -1) {
            area = width * height;
        }
        return area;
    }

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
