package com.redteamobile.smart;

public class Constant {
    public static final int DSI_STATE_CALL_IDLE = 0;
    public static final int DSI_STATE_CALL_CONNECTING = 1;
    public static final int DSI_STATE_CALL_CONNECTED = 2;
    public static final int DSI_STATE_CALL_DISCONNECTING = 3;
    public static final int DSI_STATE_CALL_MAX = 4;

    public static final String ACTION_SIM_STATE_CHANGED = "android.intent.action.SIM_STATE_CHANGED";
    public static final String ACTION_NETWORK_STATE_CHANGED = "android.intent.action.NETWORK_STATE_CHANGED";
    public static final String ACTION_NOTIFY_STATE = "redtea.intent.action.NOTIFY_STATE";
    public static final String ACTION_BOOTSTRAP_READY = "redtea.intent.action.BOOTSTRAP_READY";
    public static final String MONITOR_PACKAGE_NAME = "com.redteamobile.monitor";
    public static final String MONITOR_SERVICE_NAME = "com.redteamobile.monitor.dispatcher.DispatcherService";
    public static final String TAG_NETWORK_STATE = "network_state";
    public static final String ACTION_SERVICE_STATE = "android.intent.action.SERVICE_STATE";
    public static final int UNKNOWN_MODE = -1;
    public static final int EUICC_MODE = 1;
    public static final int VUICC_MODE = 0;

}
