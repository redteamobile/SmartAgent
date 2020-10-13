package com.redteamobile.smart.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import com.redteamobile.smart.App;

public class SharePrefSetting {

    private static final String PREF_NAME = "preferences";
    private static final String MCC_KEY = "mcc";
    private static final String MNC_KEY = "mnc";
    private static final String SIGNAL_LEVEL = "signal_level";
    private static final String SIGNAL_DBM = "signal_dbm";
    private static final String CURRENT_ICCID = "current_iccId";
    public static SharedPreferences INSTANCE;

    private static SharedPreferences getPrefs() {
        if (INSTANCE != null) {
            return INSTANCE;
        }
        int code = Context.MODE_PRIVATE;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            code = Context.MODE_MULTI_PROCESS;
        }
        code = code | Context.MODE_APPEND;
        INSTANCE = App.mContext.getSharedPreferences(PREF_NAME, code);
        return INSTANCE;
    }

    public static int getMcc() {
        return getPrefs().getInt(MCC_KEY, 0);
    }

    @SuppressLint("CommitPrefEdits")
    public static void putMcc(int mcc) {
        SharedPreferences.Editor edit = getPrefs().edit();
        edit.putInt(MCC_KEY, mcc);
        edit.apply();
    }

    public static int getMnc() {
        return getPrefs().getInt(MNC_KEY, 0);
    }

    public static void putMnc(int mnc) {
        SharedPreferences.Editor edit = getPrefs().edit();
        edit.putInt(MNC_KEY, mnc);
        edit.apply();
    }

    public static void putSignalLevel(int signalLevel) {
        SharedPreferences.Editor edit = getPrefs().edit();
        edit.putInt(SIGNAL_LEVEL, signalLevel);
        edit.apply();
    }

    public static int getSignalLevel() {
        return getPrefs().getInt(SIGNAL_LEVEL, 0);
    }

    public static void putSignalDbm(int signalDbm) {
        SharedPreferences.Editor edit = getPrefs().edit();
        edit.putInt(SIGNAL_DBM, signalDbm);
        edit.apply();
    }

    public static int getSignalDbm() {
        return getPrefs().getInt(SIGNAL_DBM, 1);
    }

    public static void putCurrentIccId(String iccId) {
        SharedPreferences.Editor edit = getPrefs().edit();
        edit.putString(CURRENT_ICCID, iccId);
        edit.apply();
    }

    public static String getCurrentIccId() {
        return getPrefs().getString(CURRENT_ICCID, "");
    }
}
