package com.redteamobile.smart.util;

import android.util.Log;

import com.redteamobile.smart.BuildConfig;

public class LogUtil {

    private static final String TAG = LogUtil.class.getSimpleName();
    private static final int LOG_NONE = 0;
    private static final int LOG_ERR = 1;
    private static final int LOG_WARN = 2;
    private static final int LOG_DBG = 3;
    private static final int LOG_INFO = 4;
    private static final int LOG_ALL = 5;


    public static void e(String tag, String msg) {
        if (BuildConfig.LOG) {
            Log.e(tag, msg);
        }
    }
    public static void i(String tag, String msg) {
        if (BuildConfig.LOG) {
            Log.i(tag, msg);
        }
    }
    public static void d(String tag, String msg) {
        if (BuildConfig.LOG) {
            Log.d(tag, msg);
        }
    }

    public static void w(String tag, String msg) {
        if (BuildConfig.LOG) {
            Log.w(tag, msg);
        }
    }
}
