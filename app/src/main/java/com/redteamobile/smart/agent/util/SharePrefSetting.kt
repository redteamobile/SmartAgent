package com.redteamobile.smart.agent.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import com.redteamobile.smart.App.Companion.context

object SharePrefSetting {

    private const val PREF_NAME = "preferences"
    private const val MCC_KEY = "mcc"
    private const val MNC_KEY = "mnc"
    private val SIGNAL_LEVEL = "signal_level"
    private val SIGNAL_DBM = "signal_dbm"
    private val CURRENT_ICCID = "current_iccId"

    @SuppressLint("InlinedApi")
    private fun getPrefs(): SharedPreferences? {
        var code = Context.MODE_PRIVATE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            code = Context.MODE_MULTI_PROCESS
        }
        code = code or Context.MODE_APPEND
        return context?.getSharedPreferences(PREF_NAME, code)
    }

    fun getMcc(): Int {
        return getPrefs()!!.getInt(MCC_KEY, 0)
    }

    fun putMcc(mcc: Int) {
        getPrefs()?.edit()?.putInt(MCC_KEY, mcc)?.apply()
    }

    fun getMnc(): Int {
        return getPrefs()!!.getInt(MNC_KEY, 0)
    }

    fun putMnc(mnc: Int) {
        getPrefs()?.edit()?.putInt(MNC_KEY, mnc)?.apply()
    }

    fun putSignalLevel(signalLevel: Int) {
        getPrefs()?.edit()?.putInt(SIGNAL_LEVEL, signalLevel)?.apply()
    }

    fun getSignalLevel(): Int {
        return getPrefs()!!.getInt(SIGNAL_LEVEL, 0)
    }

    fun putSignalDbm(signalDbm: Int) {
        getPrefs()?.edit()?.putInt(SIGNAL_DBM, signalDbm)?.apply()
    }

    fun getSignalDbm(): Int {
        return getPrefs()?.getInt(SIGNAL_DBM, 1)!!
    }

    fun putCurrentIccId(iccId: String) {
        getPrefs()?.edit()?.putString(CURRENT_ICCID, iccId)?.apply()
    }

    fun getCurrentIccId(): String {
        return getPrefs()?.getString(CURRENT_ICCID, "").toString()
    }

}
