package me.shouheng.camerax.utils;

import android.util.Log;

/**
 * @author WngShhng (shouheng2015@gmail.com)
 * @version $Id: LogUtils, v 0.1 2019/2/5 17:55 shouh Exp$
 */
public class LogUtils {

    private static boolean loggable = true;

    public static void setLoggable(boolean loggable) {
        LogUtils.loggable = loggable;
    }

    public static int v(String tag, String msg) {
        if (!loggable) return 0; 
        return Log.v(tag, msg);
    }

    public static int v(String tag, String msg, Throwable tr) {
        if (!loggable) return 0;
        return Log.v(tag, msg, tr);
    }

    public static int d(String tag, String msg) {
        if (!loggable) return 0;
        return Log.d(tag, msg);
    }

    public static int d(String tag, String msg, Throwable tr) {
        if (!loggable) return 0;
        return Log.d(tag, msg, tr);
    }

    public static int i(String tag, String msg) {
        if (!loggable) return 0;
        return Log.i(tag, msg);
    }

    public static int i(String tag, String msg, Throwable tr) {
        if (!loggable) return 0;
        return Log.i(tag, msg, tr);
    }

    public static int w(String tag, String msg) {
        if (!loggable) return 0;
        return Log.w(tag, msg);
    }

    public static int w(String tag, String msg, Throwable tr) {
        if (!loggable) return 0;
        return Log.w(tag, msg, tr);
    }

    public static int w(String tag, Throwable tr) {
        if (!loggable) return 0;
        return Log.w(tag, tr);
    }

    public static int e(String tag, String msg) {
        if (!loggable) return 0;
        return Log.e(tag, msg);
    }

    public static int e(String tag, String msg, Throwable tr) {
        if (!loggable) return 0;
        return Log.e(tag, msg, tr);
    }

    public static int wtf(String tag, String msg) {
        if (!loggable) return 0;
        return Log.wtf(tag, msg);
    }

    public static int wtf(String tag, Throwable tr) {
        if (!loggable) return 0;
        return Log.wtf(tag, tr);
    }

    public static int wtf(String tag, String msg, Throwable tr) {
        if (!loggable) return 0;
        return Log.wtf(tag, msg, tr);
    }
}
