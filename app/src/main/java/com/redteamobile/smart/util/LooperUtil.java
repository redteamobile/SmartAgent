package com.redteamobile.smart.util;

import android.os.HandlerThread;
import android.os.Looper;

public class LooperUtil {

    private static final String THREAD_NAME = "pax.vsim_";
    private HandlerThread mHandlerThread;

    public LooperUtil() {
        mHandlerThread = new HandlerThread(THREAD_NAME + System.currentTimeMillis());
        mHandlerThread.start();
    }

    public Looper getLooper() {
        return mHandlerThread.getLooper();
    }
}
