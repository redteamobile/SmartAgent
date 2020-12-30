package com.redteamobile.smart.util;

import android.util.Log;

import com.redteamobile.smart.BuildConfig;

public class LogUtil {

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
