package com.redteamobile.smart

class Constant {

    companion object {

        const val DSI_STATE_CALL_IDLE = 0
        const val DSI_STATE_CALL_CONNECTING = 1
        const val DSI_STATE_CALL_CONNECTED = 2
        const val DSI_STATE_CALL_DISCONNECTING = 3
        const val DSI_STATE_CALL_MAX = 4

        const val ACTION_SIM_STATE_CHANGED = "android.intent.action.SIM_STATE_CHANGED"
        const val ACTION_NETWORK_STATE_CHANGED = "android.intent.action.NETWORK_STATE_CHANGED"
        const val ACTION_SERVICE_CONNECTED = "redtea.intent.action.SERVICE_CONNECTED"
        const val ACTION_BOOTSTRAP_READY = "redtea.intent.action.BOOTSTRAP_READY"
        const val MONITOR_PACKAGE_NAME: String = "com.redteamobile.monitor"
        const val MONITOR_SERVICE_NAME = "$MONITOR_PACKAGE_NAME.dispatcher.DispatcherService"
        const val TAG_NETWORK_STATE = "network_state"

        // todo 获取当前包名
        const val FILE_STORAGE_PATH = "/data/data/com.redteamobile.smart/"
    }
}
