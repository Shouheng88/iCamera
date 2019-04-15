package me.shouheng.camerax.config.sizes;

/**
 * @author WngShhng (shouheng2015@gmail.com)
 * @version 2019/4/13 23:08
 */
public class AspectRatio {

    private float ratio;

    public final int widthRatio;

    public final int heightRatio;

    public static AspectRatio of(int widthRatio, int heightRatio) {
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
            return AspectRatio.of(x, y);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Malformed aspect ratio: " + s, e);
        }
    }

    private AspectRatio(int widthRatio, int heightRatio) {
        this.widthRatio = widthRatio;
        this.heightRatio = heightRatio;
    }
}
