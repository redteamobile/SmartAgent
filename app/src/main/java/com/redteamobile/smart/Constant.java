package com.redteamobile.smart;

public class Constant {
    public static final int DSI_STATE_CALL_IDLE = 0;
    public static final int DSI_STATE_CALL_CONNECTING = 1;
    public static final int DSI_STATE_CALL_CONNECTED = 2;
    public static final int DSI_STATE_CALL_DISCONNECTING = 3;
    public static final int DSI_STATE_CALL_MAX = 4;

    public static final String ACTION_SIM_STATE_CHANGED = "android.intent.action.SIM_STATE_CHANGED";
    public static final String ACTION_NETWORK_STATE_CHANGED = "android.intent.action.NETWORK_STATE_CHANGED";
    public static final String ACTION_SERVICE_CONNECTED = "redtea.intent.action.SERVICE_CONNECTED";
    public static final String ACTION_BOOTSTRAP_READY = "redtea.intent.action.BOOTSTRAP_READY";
    public static final String MONITOR_PACKAGE_NAME = "com.redteamobile.monitor";
    public static final String MONITOR_SERVICE_NAME = "com.redteamobile.monitor.dispatcher.DispatcherService";
    public static final String TAG_NETWORK_STATE = "network_state";

    // todo 获取当前包名
    public static final String FILE_STORAGE_PATH = "/data/data/com.redteamobile.smart/";
}
