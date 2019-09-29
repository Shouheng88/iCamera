package me.shouheng.shining.util;

import android.util.Log;
import me.shouheng.shining.config.ConfigurationProvider;

/**
 * The logger to use in library.
 *
 * @author WngShhng (shouheng2015@gmail.com)
 * @version 2019/4/14 9:33
 */
public final class SLogger {

    private static boolean isDebug;

    public static void v(String tag, String msg) {
        if (isDebug) {
            Log.v(tag, msg);
        }
    }

    public static void d(String tag, String msg) {
        if (isDebug) {
            Log.d(tag, msg);
        }
    }

    public static void i(String tag, String msg) {
        if (isDebug) {
            Log.i(tag, msg);
        }
    }

    public static void w(String tag, String msg) {
        if (isDebug) {
            Log.w(tag, msg);
        }
    }

    public static void e(String tag, String msg) {
        if (isDebug) {
            Log.e(tag, msg);
        }
    }

    public static void setDebug(boolean debug) {
        isDebug = debug;
    }

    /*----------------------------------------- Inner Region -----------------------------------------*/

    private SLogger() {
        throw new UnsupportedOperationException("U can't initialize me!");
    }

}
