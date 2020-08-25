package me.shouheng.icamera.util;

import android.content.Context;

/**
 * Utils class for current library.
 *
 * @author WngShhng (shouheng2015@gmail.com)
 * @version 2019/4/14 9:34
 */
public final class XUtils {

    private XUtils() {
        throw new UnsupportedOperationException("U can't initialize me!");
    }

    public static int dp2Px(Context context, float dpValues){
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int)(dpValues * scale + 0.5f);
    }

    public static int sp2Px(Context context, float spValues){
        final float fontScale = context.getResources().getDisplayMetrics().scaledDensity;
        return (int)(spValues * fontScale + 0.5f);
    }
}
