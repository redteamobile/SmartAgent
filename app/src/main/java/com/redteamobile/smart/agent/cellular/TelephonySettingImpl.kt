package com.redteamobile.smart.agent.cellular

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.telephony.TelephonyManager.*
import android.text.TextUtils
import android.util.Log
import com.redteamobile.smart.agent.util.LogUtil
import com.redteamobile.smart.agent.util.SharePrefSetting
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import kotlin.math.log


class TelephonySettingImpl() : TelephonySetting {

    private val TAG = "TelephonySettingImpl"
    private val APN_NAME = "Redtea Mobile"
    private val APN_URI = Uri.parse("content://telephony/carriers")
    private val PREFERRED_APN_URI = Uri.parse("content://telephony/carriers/preferapn")

    private var telephonyManager: TelephonyManager? = null
    private var subscriptionManager: SubscriptionManager? = null
    private var connectivityManager: ConnectivityManager? = null
    private var resolver: ContentResolver? = null
    private var isMultiSim: Boolean = false

    constructor(context: Context) : this() {
        telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        subscriptionManager = SubscriptionManager.from(context)
        resolver = context.contentResolver
        var isMultiSim = false
        try {
            val method = TelephonyManager::class.java.getDeclaredMethod("isMultiSimEnabled")
            isMultiSim = method.invoke(
                context.getSystemService(Context.TELEPHONY_SERVICE)
            ) as Boolean
            LogUtil.d(TAG, String.format("isMultiSimEnabled: %b", isMultiSim))
        } catch (e: NoSuchMethodException) {
            LogUtil.e(TAG, "Reflection failed", e)
        } catch (e: IllegalAccessException) {
            LogUtil.e(TAG, "Reflection failed", e)
        } catch (e: InvocationTargetException) {
            LogUtil.e(TAG, "Reflection failed", e)
        }

        this.isMultiSim = isMultiSim
    }

    override fun getImei(): String {
        var getImei: Method? = null
        try {
            getImei = TelephonyManager::class.java.getDeclaredMethod(
                "getImei", Int::class.javaPrimitiveType
            )
        } catch (e: NoSuchMethodException) {
            LogUtil.e(TAG, "Reflection exception", e)
        }

        var any: Any? = null
        try {
            any = getImei!!.invoke(telephonyManager, 0)
        } catch (e: IllegalAccessException) {
            LogUtil.e(TAG, "Reflection exception", e)
        } catch (e: InvocationTargetException) {
            LogUtil.e(TAG, "Reflection exception", e)
        }
        LogUtil.e(TAG, "imei:" + any.toString())
        return if (any != null && any is String) {
            any.toString()
        } else ""
    }

    override fun getDataEnabled(subId: Int): Boolean {
        var result = false
        try {
            val method = TelephonyManager::class.java.getDeclaredMethod(
                "getDataEnabled",
                Int::class.javaPrimitiveType
            )
            result = method.invoke(telephonyManager, subId) as Boolean
        } catch (e: NoSuchMethodException) {
            LogUtil.e(TAG, "Reflection failed", e)
        } catch (e: IllegalAccessException) {
            LogUtil.e(TAG, "Reflection failed", e)
        } catch (e: InvocationTargetException) {
            LogUtil.e(TAG, "Reflection failed", e)
        }
        LogUtil.i(TAG, String.format("getDataEnabled(subId: %d) returns %b", subId, result))
        return result
    }

    override fun setDataEnabled(subId: Int, enable: Boolean) {
        LogUtil.i(TAG, String.format("setDataEnabled(subId: %d, enable: %b)", subId, enable))
        try {
            val method = TelephonyManager::class.java.getDeclaredMethod(
                "setDataEnabled", Int::class.javaPrimitiveType, Boolean::class.javaPrimitiveType
            )
            method.invoke(telephonyManager, subId, enable)
        } catch (e: NoSuchMethodException) {
            LogUtil.e(TAG, "Reflection failed", e)
        } catch (e: IllegalAccessException) {
            LogUtil.e(TAG, "Reflection failed", e)
        } catch (e: InvocationTargetException) {
            LogUtil.e(TAG, "Reflection failed", e)
        }

    }

    override fun getDataRoamingEnabled(subId: Int): Boolean {
        var result = false
        if (!isMultiSim) {
            result = Settings.Global.getInt(resolver, Settings.Global.DATA_ROAMING, 0) != 0
        } else {
            try {
                val method = TelephonyManager::class.java.getDeclaredMethod(
                    "getIntWithSubId",
                    ContentResolver::class.java,
                    String::class.java,
                    Int::class.javaPrimitiveType
                )
                result = method.invoke(
                    telephonyManager, resolver, Settings.Global.DATA_ROAMING, subId
                ) as Int != 0
            } catch (e: NoSuchMethodException) {
                LogUtil.e(TAG, "Reflection failed", e)
            } catch (e: IllegalAccessException) {
                LogUtil.e(TAG, "Reflection failed", e)
            } catch (e: InvocationTargetException) {
                LogUtil.e(TAG, "Reflection failed", e)
            }

        }
        LogUtil.i(TAG, String.format("getDataRoamingEnabled(subId: %d) returns %b", subId, result))
        return result
    }

    override fun setDataRoamingEnabled(subId: Int, enable: Boolean) {
        LogUtil.i(
            TAG, String.format(
                "setDataRoamingEnabled(subId: %d, enable: %b)", subId, enable
            )
        )
        val roaming = if (enable) 1 else 0
        try {
            Settings.Global.putInt(
                resolver, if (isMultiSim)
                    Settings.Global.DATA_ROAMING + subId
                else
                    Settings.Global.DATA_ROAMING, roaming
            )
        } catch (e: SecurityException) {
            LogUtil.e(TAG, "Permission denied", e)
        }

        try {
            val method = SubscriptionManager::class.java.getDeclaredMethod(
                "setDataRoaming", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType
            )
            method.invoke(subscriptionManager, roaming, subId)
        } catch (e: NoSuchMethodException) {
            LogUtil.e(TAG, "Reflection failed", e)
        } catch (e: IllegalAccessException) {
            LogUtil.e(TAG, "Reflection failed", e)
        } catch (e: InvocationTargetException) {
            LogUtil.e(TAG, "Reflection failed", e)
        }

    }

    override fun setNonRoamingForOperator(mccMnc: String): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun setOperatorBrandName(name: String): Boolean {
        var result = false
        try {
            result = telephonyManager!!.setOperatorBrandOverride(name)
        } catch (e: SecurityException) {
            LogUtil.e(TAG, "Permission denied with setOperatorBrandName()")
        }

        LogUtil.i(
            TAG, String.format(
                "setOperatorBrandName(name: %s) returns %b", name, result
            )
        )
        return result
    }

    @SuppressLint("MissingPermission")
    override fun getSubId(slotId: Int): Int {
        try {
            val clz = Class.forName("android.telephony.SubscriptionManager")
            val method = clz.getDeclaredMethod("getSubId", Int::class.javaPrimitiveType)
            val result = method.invoke(null, slotId)
                ?: return subscriptionManager?.getActiveSubscriptionInfoForSimSlotIndex(slotId)?.subscriptionId!!
            val subId: IntArray = result as IntArray
            if (subId != null && subId.isNotEmpty()) {
                return subId[0]
            }

        } catch (e: NoSuchMethodException) {
            LogUtil.e(TAG, "Reflection failed", e)
        } catch (e: IllegalAccessException) {
            LogUtil.e(TAG, "Reflection failed", e)
        } catch (e: InvocationTargetException) {
            LogUtil.e(TAG, "Reflection failed", e)
        }

        return -1
    }

    override fun getImsi(subId: Int): String {
        var imsi: String? = null
        try {
            val method = TelephonyManager::class.java.getDeclaredMethod(
                "getSubscriberId",
                Int::class.javaPrimitiveType
            )
            imsi = method.invoke(telephonyManager, subId) as String?
        } catch (e: NoSuchMethodException) {
            LogUtil.e(TAG, "Reflection failed", e)
        } catch (e: IllegalAccessException) {
            LogUtil.e(TAG, "Reflection failed", e)
        } catch (e: InvocationTargetException) {
            LogUtil.e(TAG, "Reflection failed", e)
        }

        if (imsi == null) {
            imsi = ""
        }
        return imsi
    }

    @SuppressLint("MissingPermission")
    override fun initIccid() {
        var iccId: String
        iccId = telephonyManager?.simSerialNumber.toString()
        var info = subscriptionManager?.getActiveSubscriptionInfoForSimSlotIndex(0)
        Log.d(TAG, if (info == null) "NULL" else info.iccId)
        if (info != null) {
            iccId = info.iccId
        }
        if (iccId.isNotEmpty() && !TextUtils.equals(iccId, SharePrefSetting.getCurrentIccId())) {
            SharePrefSetting.putCurrentIccId(iccId.toUpperCase())
        }
    }

    override fun isSimReady(slotId: Int): Boolean {
        var simState = SIM_STATE_UNKNOWN
        try {
            val method = TelephonyManager::class.java.getDeclaredMethod(
                "getSimState",
                Int::class.javaPrimitiveType
            )
            simState = method.invoke(telephonyManager, slotId) as Int
        } catch (e: NoSuchMethodException) {
            LogUtil.e(TAG, "Reflection failed", e)
        } catch (e: IllegalAccessException) {
            LogUtil.e(TAG, "Reflection failed", e)
        } catch (e: InvocationTargetException) {
            LogUtil.e(TAG, "Reflection failed", e)
        }

        val result = simState == SIM_STATE_READY
        LogUtil.i(TAG, String.format("isSimReady(slotId: %d) returns %b", slotId, result))
        return result
    }

    override fun getDefaultDataSubId(): Int {
        var subId = -1
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            subId = SubscriptionManager.getDefaultDataSubscriptionId()
            LogUtil.i(TAG, String.format("getDefaultDataSubId() returns %d", subId))
            return subId
        }
        try {
            val method = SubscriptionManager::class.java.getDeclaredMethod("getDefaultDataSubId")
            subId = method.invoke(subscriptionManager) as Int
        } catch (e: NoSuchMethodException) {
            LogUtil.e(TAG, "Reflection failed", e)
        } catch (e: IllegalAccessException) {
            LogUtil.e(TAG, "Reflection failed", e)
        } catch (e: InvocationTargetException) {
            LogUtil.e(TAG, "Reflection failed", e)
        }

        LogUtil.i(TAG, String.format("getDefaultDataSubId() returns %d", subId))
        return subId
    }

    override fun setDefaultDataSubId(subId: Int) {
        LogUtil.i(TAG, String.format("setDefaultDataSubId(subId: %d)", subId))
        if (subId < 0) {
            return
        }
        try {
            val method = SubscriptionManager::class.java.getDeclaredMethod(
                "setDefaultDataSubId", Int::class.javaPrimitiveType
            )
            method.invoke(subscriptionManager, subId)
        } catch (e: NoSuchMethodException) {
            LogUtil.e(TAG, "Reflection failed", e)
        } catch (e: IllegalAccessException) {
            LogUtil.e(TAG, "Reflection failed", e)
        } catch (e: InvocationTargetException) {
            LogUtil.e(TAG, "Reflection failed", e)
        }

    }

    @SuppressLint("SoonBlockedPrivateApi")
    override fun setDisplayName(subId: Int, name: String) {
        LogUtil.i(TAG, String.format("setDisplayName(subId: %d, name: %s)", subId, name))
        try {
            val method = SubscriptionManager::class.java.getDeclaredMethod(
                "setDisplayName", String::class.java, Int::class.javaPrimitiveType
            )
            method.invoke(subscriptionManager, name, subId)
        } catch (e: NoSuchMethodException) {
            LogUtil.e(TAG, "Reflection failed", e)
        } catch (e: IllegalAccessException) {
            LogUtil.e(TAG, "Reflection failed", e)
        } catch (e: InvocationTargetException) {
            LogUtil.e(TAG, "Reflection failed", e)
        }

    }

    override fun getModel(): String {
        return Build.BRAND
    }

    override fun getNetworkType(): String {
        val info = connectivityManager?.activeNetworkInfo
        var netType = ""
        if (info != null && info.isConnected) {
            if (info.type == ConnectivityManager.TYPE_WIFI) {
                netType = "WIFI"
            } else if (info.type == ConnectivityManager.TYPE_MOBILE) {
                when (info.subtype) {
                    NETWORK_TYPE_GSM, NETWORK_TYPE_GPRS, NETWORK_TYPE_CDMA, NETWORK_TYPE_EDGE,
                    NETWORK_TYPE_1xRTT, NETWORK_TYPE_IDEN -> netType = "2G"

                    NETWORK_TYPE_TD_SCDMA, NETWORK_TYPE_EVDO_A, NETWORK_TYPE_UMTS,
                    NETWORK_TYPE_EVDO_0, NETWORK_TYPE_HSDPA, NETWORK_TYPE_HSUPA,
                    NETWORK_TYPE_HSPA, NETWORK_TYPE_EVDO_B, NETWORK_TYPE_EHRPD, NETWORK_TYPE_HSPAP -> netType =
                        "3G"

                    NETWORK_TYPE_IWLAN, NETWORK_TYPE_LTE -> netType = "4G"
                    else -> {
                        val subtypeName = info.subtypeName
                        netType = if (subtypeName.equals("TD-SCDMA", ignoreCase = true)
                            || subtypeName.equals("WCDMA", ignoreCase = true)
                            || subtypeName.equals("CDMA2000", ignoreCase = true)
                        ) {
                            "3G"
                        } else {
                            "UNKNOWN"
                        }
                    }
                }

            }
        } else {
            netType = "UNKNOWN"
        }
        return netType
    }

    override fun configureApn(apn: String, mccMnc: String): Boolean {
        val subId = getSubId(0)
        LogUtil.e(TAG, "subId:$subId mccMnc:$mccMnc")
        if (subId < 0) {
            return false
        }
        if (TextUtils.isEmpty(mccMnc) || TextUtils.isEmpty(apn)) {
            return false
        }
        var apnId = lookupApn(apn, mccMnc)
        if (apnId < 0) {
            apnId = insertApn(apn, mccMnc)
        }

        val result = apnId >= 0 && setPreferredApn(subId, apnId)
        LogUtil.i(
            TAG, String.format(
                "configureApn(apn: %s, mccMnc: %s, subId: %d) returns %b",
                apn, mccMnc, subId, result
            )
        )
        return result
    }

    override fun clearApn(): Boolean {
        var result = false
        try {
            result = resolver?.delete(APN_URI, "name = ?", arrayOf(APN_NAME))!! >= 0
        } catch (e: SecurityException) {
            LogUtil.e(TAG, "Permission denied", e)
        }

        LogUtil.i(TAG, String.format("clearApn() returns %b", result))
        return result
    }

    private fun lookupApn(apn: String, mccMnc: String): Int {
        LogUtil.i(TAG, "lookupApn()")

        var cursor: Cursor? = null
        try {
            cursor = resolver?.query(
                APN_URI, null, "name = ? AND apn = ? AND numeric = ?",
                arrayOf(APN_NAME, apn, mccMnc), null
            )
        } catch (e: SecurityException) {
            LogUtil.e(TAG, "Permission denied", e)
        }

        if (cursor == null) {
            return -1
        }
        var id = -1
        if (cursor.moveToFirst()) {
            id = cursor.getShort(cursor.getColumnIndex("_id")).toInt()
        }
        cursor.close()
        return id
    }

    private fun insertApn(apn: String, mccMnc: String): Int {
        LogUtil.i(TAG, "insertApn()")
        val values = ContentValues()
        values.put("name", APN_NAME)
        values.put("apn", apn)
        values.put("numeric", mccMnc)
        values.put("mcc", mccMnc.substring(0, 3))
        values.put("mnc", mccMnc.substring(3))
        values.put("type", "default")

        var newRow: Uri? = null
        try {
            newRow = resolver?.insert(APN_URI, values)
        } catch (e: SecurityException) {
            LogUtil.e(TAG, "Permission denied", e)
        }

        if (newRow == null) {
            return -1
        }

        var cursor: Cursor? = null
        try {
            cursor = resolver?.query(newRow, null, null, null, null)
        } catch (e: SecurityException) {
            LogUtil.e(TAG, "Permission denied", e)
        }

        if (cursor == null) {
            return -1
        }
        var id = -1
        if (cursor.moveToFirst()) {
            id = cursor.getShort(cursor.getColumnIndex("_id")).toInt()
        }
        cursor.close()
        return id
    }

    private fun setPreferredApn(subId: Int, apnId: Int): Boolean {
        LogUtil.i(TAG, "setPreferredApn()")

        val values = ContentValues()
        values.put("apn_id", apnId)
        val uri = if (isMultiSim)
            Uri.withAppendedPath(PREFERRED_APN_URI, "subId/$subId")
        else
            PREFERRED_APN_URI

        var result = false
        try {
            result = resolver?.update(uri, values, null, null)!! > 0
        } catch (e: SecurityException) {
            LogUtil.e(TAG, "Permission denied", e)
        }

        return result
    }

}
