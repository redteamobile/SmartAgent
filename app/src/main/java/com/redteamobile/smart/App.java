package com.redteamobile.smart;

import android.app.Application;
import android.content.Context;

public class App extends Application {
    public static  Context mContext = null;

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = getApplicationContext();
    }
}
