package com.redteamobile.smart.agent;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.redteamobile.smart.Constant;
import com.redteamobile.smart.cellular.TelephonySetting;
import com.redteamobile.smart.cellular.TelephonySettingImpl;
import com.redteamobile.smart.util.LogUtil;
import com.redteamobile.smart.util.LooperUtil;
import com.redteamobile.smart.util.RetryUtil;
import com.redteamobile.smart.util.SharePrefSetting;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class SlotMonitor {

    private final static String TAG = "SlotMonitor";
    private Context context;
    private Intent netStateIntent;
    private int slotId;
    private int subId;
    private boolean isRunning;
    private LooperUtil looperUtil;
    private TelephonySetting telephonySetting;
    private TelephonyManager telephonyManager;
    private SlotMonitor.DefaultPhoneStateListener phoneStateListener;
    private boolean changedBroadcastRegistered;

    private BroadcastReceiver simStateChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Constant.ACTION_SIM_STATE_CHANGED.equals(intent.getAction())) {
                int slotId = intent.getIntExtra("phone", -1); // Android Q版本字段更新为 phone，Q之前版本为 slot
                if (slotId == -1) {
                    slotId = intent.getIntExtra("slot", -1);
                }
                subId = intent.getIntExtra("subscription", -1);
                String simState = intent.getStringExtra("ss");
                if ("LOADED".equals(simState)) {
                    onLoaded(subId);
                }
            }
        }
    };

    private void onLoaded(final int subId) {
        // Set DefaultDataSubId
        telephonySetting.setDefaultDataSubId(subId);

        // Set DataRoaming
        telephonySetting.setDataRoamingEnabled(subId, true);

        // Set DataEnable
        new RetryUtil(looperUtil.getLooper(), new Runnable() {
            @Override
            public void run() {
                telephonySetting.setDataEnabled(subId, true);
            }
        }, 0 /* maxRetries */, 2000L /* delayMillis */).retry(false);
        registerPhoneStateListener();
    }

    private void registerPhoneStateListener() {
        if (phoneStateListener != null) {
            unregisterPhoneStateListener();
        }
        phoneStateListener = new SlotMonitor.DefaultPhoneStateListener();
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
    }

    private void unregisterPhoneStateListener() {
        if (phoneStateListener != null) {
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
        }
        phoneStateListener = null;
    }

    private static class DefaultPhoneStateListener extends PhoneStateListener {

        @Override
        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
            super.onSignalStrengthsChanged(signalStrength);
            try {
                Method getLevel = SignalStrength.class.getDeclaredMethod("getLevel");
                @SuppressLint("SoonBlockedPrivateApi") Method getDbm = SignalStrength.class
                        .getDeclaredMethod("getDbm");
                int level = (int) getLevel.invoke(signalStrength);
                int dbm = (int) getDbm.invoke(signalStrength);
                SharePrefSetting.putSignalLevel(level);
                SharePrefSetting.putSignalDbm(dbm);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }
    }

    public SlotMonitor(Context context) {
        super();
        this.context = context;
        looperUtil = new LooperUtil();
        telephonySetting = new TelephonySettingImpl(context);
        telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
    }

    public void startMonitor() {
        if (!isRunning) {
            isRunning = true;
            registerChangedListener();
        }

    }

    public void stopMonitor() {
        isRunning = false;
        unregisterPhoneStateListener();
        unregisterChangedListener();
    }

    private void registerChangedListener() {
        if (!changedBroadcastRegistered) {
            context.registerReceiver(
                    simStateChangedReceiver,
                    new IntentFilter(Constant.ACTION_SIM_STATE_CHANGED));
            registerNetworkCallback(context);
            changedBroadcastRegistered = true;
        }
    }

    private void unregisterChangedListener() {
        if (changedBroadcastRegistered) {
            context.unregisterReceiver(simStateChangedReceiver);
            unregisterNetworkCallback(context);
            changedBroadcastRegistered = false;
        }
    }

    // 发送网络更新广播
    private void check(int networkState) {
        if (netStateIntent == null) {
            netStateIntent = new Intent(Constant.ACTION_NETWORK_STATE_CHANGED);
        }
        netStateIntent.putExtra(Constant.TAG_NETWORK_STATE, networkState);
        LocalBroadcastManager.getInstance(context).sendBroadcast(netStateIntent);
    }

    /**
     * Android10监听网络变化广播
     */
    ConnectivityManager.NetworkCallback callback = new ConnectivityManager.NetworkCallback() {
        // 可用网络接入
        public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {
            // 一般在此处获取网络类型然后判断网络类型，就知道时哪个网络可以用connected
        }

        @Override
        public void onAvailable(@NonNull Network network) {
            super.onAvailable(network);
            check(Constant.DSI_STATE_CALL_CONNECTED);
        }

        // 网络断开
        public void onLost(Network network) {
            // 如果通过ConnectivityManager#getActiveNetwork()返回null，表示当前已经没有其他可用网络了。
            check(Constant.DSI_STATE_CALL_IDLE);
        }
    };

    // 注册回调
    private void registerNetworkCallback(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkRequest.Builder builder = new NetworkRequest.Builder();
        builder.addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR);
        builder.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);
        cm.registerNetworkCallback(builder.build(), callback);
    }

    // 注销回调
    private void unregisterNetworkCallback(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        cm.unregisterNetworkCallback(callback);
    }


}
