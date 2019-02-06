package me.shouheng.camerax.utils;

import android.annotation.TargetApi;
import android.hardware.Camera;
import android.os.Build;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 * A wrapped class for picture size.
 */
public class Size {

    private int width;

    private int height;

    public static Size get() {
        return new Size(0, 0);
    }

    public static Size get(int width, int height) {
        return new Size(width, height);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static Size get(android.util.Size size) {
        return new Size(size.getWidth(), size.getHeight());
    }

    public static Size get(Camera.Size size) {
        return new Size(size.width, size.height);
    }

    private Size(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    @SuppressWarnings("deprecation")
    public static List<Size> fromList(List<Camera.Size> sizes) {
        if (sizes == null || sizes.isEmpty()) return new LinkedList<>();
        List<Size> result = new ArrayList<>(sizes.size());
        for (Camera.Size size : sizes) {
            result.add(Size.get(size));
        }
        return result;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static List<Size> fromList2(List<android.util.Size> sizes) {
        if (sizes == null || sizes.isEmpty()) return new LinkedList<>();
        List<Size> result = new ArrayList<>(sizes.size());
        for (android.util.Size size : sizes) {
            result.add(Size.get(size));
        }
        return result;
    }

    @SuppressWarnings("deprecation")
    public static Size[] fromArray(Camera.Size[] sizes) {
        if (sizes == null || sizes.length == 0) return new Size[0];
        Size[] result = new Size[sizes.length];
        for (int i = 0, length = sizes.length; i < length; ++i) {
            result[i] = Size.get(sizes[i]);
        }
        return result;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static Size[] fromArray2(android.util.Size[] sizes) {
        if (sizes == null || sizes.length == 0) return new Size[0];
        Size[] result = new Size[sizes.length];
        for (int i = 0, length = sizes.length; i < length; ++i) {
            result[i] = Size.get(sizes[i]);
        }
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Size)) return false;
        Size size = (Size) o;
        return width == size.width && height == size.height;
    }

    @Override
    public int hashCode() {
        return height ^ ((width << (Integer.SIZE / 2)) | (width >>> (Integer.SIZE / 2)));
    }

    @NonNull
    @Override
    public String toString() {
        return "Size{" +
                "width=" + width +
                ", height=" + height +
                '}';
    }
}
