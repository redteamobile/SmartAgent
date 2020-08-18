package com.redteamobile.smart.agent.cellular

interface TelephonySetting {

    fun getImei(): String

    // Privileged system permission required
    fun getDataEnabled(subId: Int): Boolean

    // Privileged system permission required
    fun setDataEnabled(subId: Int, enable: Boolean)

    fun getDataRoamingEnabled(subId: Int): Boolean

    // Privileged system permission required
    fun setDataRoamingEnabled(subId: Int, enable: Boolean)

    fun setNonRoamingForOperator(mccMnc: String): Boolean

    fun setOperatorBrandName(name: String): Boolean

    fun getSubId(slotId: Int): Int

    fun getImsi(subId: Int): String

    fun initIccid()

    fun isSimReady(slotId: Int): Boolean

    fun getDefaultDataSubId(): Int

    // Privileged system permission required
    fun setDefaultDataSubId(subId: Int)

    // Privileged system permission required
    fun setDisplayName(subId: Int, name: String)

    fun getModel(): String

    fun getNetworkType(): String

    fun configureApn(apn: String, mccMnc: String): Boolean

    fun clearApn(): Boolean
}