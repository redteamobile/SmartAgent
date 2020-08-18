package com.redteamobile.smart.agent

import android.annotation.TargetApi
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.telephony.PhoneStateListener
import android.telephony.ServiceState
import android.telephony.SignalStrength
import android.telephony.TelephonyManager
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.redteamobile.smart.Constant
import com.redteamobile.smart.Constant.Companion.DSI_STATE_CALL_CONNECTED
import com.redteamobile.smart.Constant.Companion.DSI_STATE_CALL_IDLE
import com.redteamobile.smart.agent.cellular.TelephonySetting
import com.redteamobile.smart.agent.cellular.TelephonySettingImpl
import com.redteamobile.smart.agent.util.LogUtil
import com.redteamobile.smart.agent.util.LooperUtil
import com.redteamobile.smart.agent.util.RetryUtil
import com.redteamobile.smart.agent.util.SharePrefSetting

class SlotMonitor {

    private val TAG = "SlotMonitor"

    private val context: Context
    private val slotId: Int = 0
    private var subId: Int = 0
    private var isRunning: Boolean = false
    private var dataRegistered: Boolean = false
    private var profileLoaded: Boolean = false
    private val looperUtil: LooperUtil
    private val telephonySetting: TelephonySetting
    private var telephonyManager: TelephonyManager? = null
    private var phoneStateListener: DefaultPhoneStateListener? = null
    private var simStateChangedBroadcastRegistered: Boolean = false

    private val simStateChangedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (Constant.ACTION_SIM_STATE_CHANGED == intent.action) {
                var slotId = intent.getIntExtra("phone", -1) // Android Q版本字段更新为 phone，Q之前版本为 slot
                if (slotId == -1) {
                    slotId = intent.getIntExtra("slot", -1)
                }
                subId = intent.getIntExtra("subscription", -1)
                val simState = intent.getStringExtra("ss")
                LogUtil.i(TAG, "onReceive(slotId: $slotId, ss: $simState)")
                if (profileLoaded != ("LOADED" == simState)) {
                    profileLoaded = "LOADED" == simState
                    LogUtil.i(TAG, "onReceive: profileLoaded $profileLoaded")
                    if (profileLoaded) {
                        onLoaded(subId)
                    }
                    check()
                }
            }
        }
    }

    private inner class DefaultPhoneStateListener @TargetApi(Build.VERSION_CODES.N) constructor() :
        PhoneStateListener() {

        var isCheck = false

        override fun onServiceStateChanged(serviceState: ServiceState) {
            super.onServiceStateChanged(serviceState)
            try {
                val getDataRegState = ServiceState::class.java.getDeclaredMethod("getDataRegState")
                val state = getDataRegState.invoke(serviceState) as Int
                LogUtil.d(TAG, "isCheck:$isCheck state:$state")
                if (dataRegistered != (state == ServiceState.STATE_IN_SERVICE)) {
                    if (!isCheck) {
                        dataRegistered = state == ServiceState.STATE_IN_SERVICE
                        LogUtil.d(TAG, "onServiceStateChanged: dataRegistered $dataRegistered")

                        check()
                        isCheck = true
                        RetryUtil(looperUtil.looper, Runnable {
                            isCheck = false
                        }, 0, 5000L).retry(false)
                    }
                }

            } catch (e: Exception) {
                LogUtil.e(TAG, "Invocation exception", e)
            }
        }

        override fun onSignalStrengthsChanged(signalStrength: SignalStrength?) {
            super.onSignalStrengthsChanged(signalStrength)
            try {
                val getLevel = SignalStrength::class.java.getDeclaredMethod("getLevel")
                val getDbm = SignalStrength::class.java.getDeclaredMethod("getDbm")
                val level = getLevel.invoke(signalStrength) as Int
                val dbm = getDbm.invoke(signalStrength) as Int
                SharePrefSetting.putSignalLevel(level)
                SharePrefSetting.putSignalDbm(dbm)
            } catch (e: Exception) {
                LogUtil.e(TAG, "Invocation exception", e)
            }
        }

    }

    constructor(context: Context) {
        this.context = context
        looperUtil = LooperUtil()
        telephonySetting = TelephonySettingImpl(context)
        telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager?
    }

    fun startMonitor() {
        if (isRunning) {
            return
        }
        isRunning = true
        registerSimStateChangedListener()
    }

    fun stopMonitor() {
        isRunning = false
        unregisterPhoneStateListener()
        unregisterSimStateChangedListener()
    }

    private fun registerPhoneStateListener() {
        if (phoneStateListener != null) {
            unregisterPhoneStateListener()
        }
        phoneStateListener = DefaultPhoneStateListener()
        LogUtil.d(TAG, "registerPhoneStateListener: $slotId")
        telephonyManager?.listen(phoneStateListener, PhoneStateListener.LISTEN_SERVICE_STATE)
    }

    private fun unregisterPhoneStateListener() {
        if (phoneStateListener != null) {
            LogUtil.d(TAG, "unregisterPhoneStateListener: $slotId")
            telephonyManager?.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE)
        }
        phoneStateListener = null
        dataRegistered = false
    }

    private fun registerSimStateChangedListener() {
        if (!simStateChangedBroadcastRegistered) {
            context?.registerReceiver(
                simStateChangedReceiver,
                IntentFilter(Constant.ACTION_SIM_STATE_CHANGED)
            )
            simStateChangedBroadcastRegistered = true
        }
    }

    private fun unregisterSimStateChangedListener() {
        if (simStateChangedBroadcastRegistered) {
            context?.unregisterReceiver(simStateChangedReceiver)
            simStateChangedBroadcastRegistered = false
        }
        profileLoaded = false
    }

    private fun onLoaded(subId: Int) {
        LogUtil.i(TAG, "onLoaded()")

        // Set DefaultDataSubId
        telephonySetting.setDefaultDataSubId(subId)

        // Set DataRoaming
        telephonySetting.setDataRoamingEnabled(subId, true)

        // Set DataEnable
        RetryUtil(looperUtil.looper, Runnable {
            telephonySetting.setDataEnabled(subId, true)
        }, 0 /* maxRetries */, 2000L /* delayMillis */).retry(false)

        registerPhoneStateListener()
    }

    private fun check() {
        LogUtil.i(TAG, "check(): profileLoaded[$profileLoaded] dataRegistered[$dataRegistered]")
        var networkState: Int = if (profileLoaded && dataRegistered) {
            telephonySetting.initIccid()
            DSI_STATE_CALL_CONNECTED
        } else {
            DSI_STATE_CALL_IDLE
        }
        val intent = Intent(Constant.ACTION_NETWORK_STATE_CHANGED)
        intent.putExtra(Constant.TAG_NETWORK_STATE, networkState)
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
    }
}
