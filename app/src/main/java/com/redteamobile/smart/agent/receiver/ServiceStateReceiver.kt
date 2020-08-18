package com.redteamobile.smart.agent.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.text.TextUtils
import android.util.Log
import com.redteamobile.smart.agent.util.LogUtil
import com.redteamobile.smart.agent.util.LooperUtil
import com.redteamobile.smart.agent.util.SharePrefSetting

class ServiceStateReceiver : BroadcastReceiver() {

    private val TAG = "ServiceStateReceiver"
    private val ACTION_SERVICE_STATE = "android.intent.action.SERVICE_STATE"
    private var telephonyManager: TelephonyManager? = null

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.i(TAG, "action is ${intent?.action}")
        if (ACTION_SERVICE_STATE != intent!!.action) {
            return
        }
        if (context != null) {
            telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            retrieveMcc()
        }
    }

    private fun retrieveMcc() {
        val networkOperator = telephonyManager?.networkOperator
        if (networkOperator != null) {
            if (!TextUtils.isEmpty(networkOperator) && (networkOperator.length == 5 || networkOperator.length == 6)) {
                saveMccMnc(networkOperator.substring(0, 5))
            }
        }
    }

    private fun saveMccMnc(mccMnc: String) {
        LogUtil.i(TAG, "mccMnc：$mccMnc")
        var currentMcc = mccMnc.substring(0, 3).toInt()
        val lastMcc = SharePrefSetting.getMcc()
        if (currentMcc > 0 && currentMcc != lastMcc) {
            LogUtil.i(TAG, String.format("saveMcc(currentMcc: %s)", currentMcc))
            SharePrefSetting.putMcc(currentMcc)
        }

        var currentMnc = mccMnc.substring(3).toInt()
        val lastMnc = SharePrefSetting.getMnc()
        if (currentMnc >= 0 && currentMnc != lastMnc) {
            LogUtil.i(TAG, String.format("saveMnc(currentMnc: %s)", currentMnc))
            SharePrefSetting.putMnc(currentMnc)
        }
    }

}
