package me.shouheng.icamera.util;

import android.util.Log;

/**
 * A simple logger to use in library.
 *
 * @author WngShhng (shouheng2015@gmail.com)
 * @version 2019/4/14 9:33
 */
public final class XLog {

    private static boolean isDebug;

    public static void v(String tag, String msg) {
        if (isDebug) {
            Log.v("XLog-" + tag, msg);
        }
    }

    public static void d(String tag, String msg) {
        if (isDebug) {
            Log.d("XLog-" + tag, msg);
        }
    }

    public static void i(String tag, String msg) {
        if (isDebug) {
            Log.i("XLog-" + tag, msg);
        }
    }

    public static void w(String tag, String msg) {
        if (isDebug) {
            Log.w("XLog-" + tag, msg);
        }
    }

    public static void e(String tag, String msg) {
        if (isDebug) {
            Log.e("XLog-" + tag, msg);
        }
    }

    public static void setDebug(boolean debug) {
        isDebug = debug;
    }

    /*----------------------------------------- Inner Region -----------------------------------------*/

    private XLog() {
        throw new UnsupportedOperationException("U can't initialize me!");
    }

}
