package com.redteamobile.smart.util;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

public class RetryUtil extends Handler {

    private final Runnable runnable;
    private final int maxRetries;
    private final long delayMillis;
    private int retries;

    public RetryUtil(Runnable runnable, int maxRetries, long delayMillis) {
        this.runnable = runnable;
        this.maxRetries = maxRetries;
        this.delayMillis = delayMillis;
    }

    public RetryUtil(Looper looper, Runnable runnable, int maxRetries, long delayMillis) {
        super(looper);
        this.runnable = runnable;
        this.maxRetries = maxRetries;
        this.delayMillis = delayMillis;
    }

    public void retry(boolean runImmediately) {
        retries = 0;
        if (runImmediately) {
            handleMessage(null);
        } else {
            sendEmptyMessageDelayed(0, delayMillis);
        }
    }

    @Override
    public void handleMessage(Message msg) {
        try {
            runnable.run();
        } catch (Exception e) {
            if (retries < maxRetries) {
                sendEmptyMessageDelayed(0, delayMillis);
            }
        }
        retries++;
    }

}
