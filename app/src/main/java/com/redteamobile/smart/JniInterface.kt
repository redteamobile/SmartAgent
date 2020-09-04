package com.redteamobile.smart

import android.content.Context
import com.redteamobile.smart.agent.AgentService
import com.redteamobile.smart.agent.cellular.TelephonySetting
import com.redteamobile.smart.agent.cellular.TelephonySettingImpl
import com.redteamobile.smart.agent.util.SharePrefSetting
import java.util.concurrent.TimeUnit
import com.redteamobile.monitor.IDispatcherService
import com.redteamobile.smart.code.service.LibraryService

class JniInterface {

    private val TAG = JniInterface::class.java.simpleName

    private var telephonySetting: TelephonySetting
    private var libraryService: LibraryService
    private var dispatcherService: IDispatcherService? = null

    constructor(context: Context) {
        System.loadLibrary("bridge")
        this.telephonySetting = TelephonySettingImpl(context)
        this.libraryService = LibraryService(context)
    }

    fun setService(dispatcherService: IDispatcherService) {
        this.dispatcherService = dispatcherService
    }

    external fun logPrintString(level: Int, log: String): Int

    external fun main(path: String): Int

    external fun networkUpdateState(networkState: Int): Int

    external fun getUiccMode(path: String): Int

    external fun setUiccMode(mode: Int): Int

    external fun getEId(eId: ByteArray, eIdLength: IntArray): Int

    external fun getProfiles(profile: ByteArray, profileLength: IntArray): Int

    external fun deleteProfile(iccid: String): Int

    external fun enableProfile(iccid: String): Int

    external fun disableProfile(iccid: String): Int

    fun getMcc(): Int {
        return SharePrefSetting.getMcc()
    }

    fun getMnc(): Int {
        return SharePrefSetting.getMnc()
    }

    fun getImei(): String {
        return telephonySetting.getImei()
    }

    fun getModel(): String {
        return telephonySetting.getModel()
    }

    fun getMonitorVersion(): String {
        return dispatcherService?.versionName.toString()
    }

    fun openChannel(): Int {
        return libraryService.openChannel()
    }

    fun closeChannel(channelId: Int): Int {
        return if (libraryService.closeChannel(channelId))
            0
        else {
            1
        }
    }

    fun transmitApdu(data: ByteArray, channelId: Int): ByteArray {
        return libraryService.transmit(data, channelId)!!
    }

    fun commandApdu(apdu: ByteArray): ByteArray {
        if (!checkMonitorServiceReady()) {
            return ByteArray(0)
        }
        return dispatcherService!!.commandApdu(apdu)
    }

    fun getCurrentIccid(): String ?{
        telephonySetting.initIccid()
        //return SharePrefSetting.getCurrentIccId()
        return null
    }

    fun getCurrentImsi(): String {
        return telephonySetting.getImsi(0)
    }

    fun getSignalDbm(): Int {
        return SharePrefSetting.getSignalDbm()
    }

    fun getSignalLevel(): Int {
        return SharePrefSetting.getSignalLevel()
    }

    fun getNetworkType(): String {
        return telephonySetting.getNetworkType()
    }

    fun setApn(apn: String, mccMnc: String): Int {
        return if (telephonySetting.configureApn(apn, mccMnc))
            0
        else {
            -1
        }
    }

    private fun checkMonitorServiceReady(): Boolean {
        return try {
            dispatcherService != null && AgentService.monitorReady.await(
                5000L,
                TimeUnit.MILLISECONDS
            )
        } catch (e: InterruptedException) {
            false
        }
    }

}
