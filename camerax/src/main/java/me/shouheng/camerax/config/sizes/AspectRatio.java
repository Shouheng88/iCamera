package me.shouheng.camerax.config.sizes;

/**
 * Used to map from given ratio to sizes.
 *
 * @author WngShhng (shouheng2015@gmail.com)
 * @version 2019/4/13 23:08
 */
public class AspectRatio {

    private float ratio;

    private int width;

    private int height;

    public static AspectRatio of(int width, int height) {
        return new AspectRatio(width, height);
    }

    private AspectRatio(int width, int height) {
        this.width = width;
        this.height = height;
    }
}
