package com.redteamobile.smart.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import com.redteamobile.smart.Constant;
import com.redteamobile.smart.util.SharePrefSetting;


public class ServiceStateReceiver extends BroadcastReceiver {
    private static final String TAG = ServiceStateReceiver.class.getSimpleName();
    private TelephonyManager telephonyManager;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Constant.ACTION_SERVICE_STATE.equals(intent.getAction())) {
            return;
        }
        if (context != null) {
            telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            retrieveMcc();
        }
    }

    private void retrieveMcc() {
        String networkOperator = telephonyManager.getNetworkOperator();
        if (networkOperator != null) {
            if (!TextUtils.isEmpty(networkOperator) && (networkOperator.length() == 5
                    || networkOperator.length() == 6)) {
                saveMccMnc(networkOperator.substring(0, 5));
            }
        }
    }

    private void saveMccMnc(String mccMnc) {
        int mcc = Integer.parseInt(mccMnc.substring(0, 3));
        int lastMcc = SharePrefSetting.getMcc();
        if (mcc > 0 && mcc != lastMcc) {
            SharePrefSetting.putMcc(mcc);
        }
        int currentMnc = Integer.parseInt(mccMnc.substring(3));
        int lastMnc = SharePrefSetting.getMnc();
        if (currentMnc >= 0 && currentMnc != lastMnc) {
            SharePrefSetting.putMnc(currentMnc);
        }
    }
}
