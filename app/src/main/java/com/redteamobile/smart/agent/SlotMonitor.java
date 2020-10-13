package com.redteamobile.smart.agent;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.redteamobile.smart.Constant;
import com.redteamobile.smart.cellular.TelephonySetting;
import com.redteamobile.smart.cellular.TelephonySettingImpl;
import com.redteamobile.smart.util.LooperUtil;
import com.redteamobile.smart.util.RetryUtil;
import com.redteamobile.smart.util.SharePrefSetting;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class SlotMonitor {

    private final static String TAG = "SlotMonitor";
    private Context context;
    private int slotId;
    private int subId;
    private boolean isRunning;
    private boolean dataRegistered;
    private boolean profileLoaded;
    private LooperUtil looperUtil;
    private TelephonySetting telephonySetting;
    private TelephonyManager telephonyManager;
    private SlotMonitor.DefaultPhoneStateListener phoneStateListener;
    private boolean simStateChangedBroadcastRegistered;

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
                if (profileLoaded != ("LOADED".equals(simState))) {
                    profileLoaded = "LOADED".equals(simState);
                    if (profileLoaded) {
                        onLoaded(subId);
                    }
                    check();
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
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_SERVICE_STATE);
    }

    private void unregisterPhoneStateListener() {
        if (phoneStateListener != null) {
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
        }
        phoneStateListener = null;
        dataRegistered = false;
    }

    private class DefaultPhoneStateListener extends PhoneStateListener {

        private boolean isCheck = false;

        @Override
        public void onServiceStateChanged(ServiceState serviceState) {
            super.onServiceStateChanged(serviceState);
            try {
                Method getDataRegState = ServiceState.class.getDeclaredMethod("getDataRegState");
                int state = (int) getDataRegState.invoke(serviceState);
                if (dataRegistered != (state == ServiceState.STATE_IN_SERVICE)) {
                    if (!isCheck) {
                        dataRegistered = state == ServiceState.STATE_IN_SERVICE;
                        check();
                        isCheck = true;
                        new RetryUtil(looperUtil.getLooper(), new Runnable() {
                            @Override
                            public void run() {
                                isCheck = false;
                            }
                        }, 0, 5000L).retry(false);
                    }
                }
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }

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
            registerSimStateChangedListener();
        }

    }

    public void stopMonitor() {
        isRunning = false;
        unregisterPhoneStateListener();
        unregisterSimStateChangedListener();
    }

    private void registerSimStateChangedListener() {
        if (!simStateChangedBroadcastRegistered) {
            context.registerReceiver(
                    simStateChangedReceiver,
                    new IntentFilter(Constant.ACTION_SIM_STATE_CHANGED));
            simStateChangedBroadcastRegistered = true;
        }
    }

    private void unregisterSimStateChangedListener() {
        if (simStateChangedBroadcastRegistered) {
            context.unregisterReceiver(simStateChangedReceiver);
            simStateChangedBroadcastRegistered = false;
        }
        profileLoaded = false;
    }

    private void check() {
        int networkState = -1;
        if (profileLoaded && dataRegistered) {
            telephonySetting.initIccid();
            networkState = Constant.DSI_STATE_CALL_CONNECTED;
        } else {
            networkState = Constant.DSI_STATE_CALL_IDLE;
        }
        Intent intent = new Intent(Constant.ACTION_NETWORK_STATE_CHANGED);
        intent.putExtra(Constant.TAG_NETWORK_STATE, networkState);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }
}
