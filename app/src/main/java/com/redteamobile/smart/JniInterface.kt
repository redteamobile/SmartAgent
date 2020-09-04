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
    /**
     * Print function
     * @param level log leve
     * @param log  The print string
     *
     * @return int  0:Success , -1:error
     */
    external fun logPrintString(level: Int, log: String): Int

    /**
     * The main function of agent
     * @param path  The packet name of app
     *
     * @return int  0:Success , -1:error
     */
    external fun main(path: String): Int

    /**
     * Notify network status
     * @param networkState  Defined in constant class
     *
     * @return int  0:Success , -1:error
     */
    external fun networkUpdateState(networkState: Int): Int

    /**
     * Get the uicc mode
     * @param path  The path of configuration file
     *
     * @return int  Mode, 0:vUICC ，1:eUICC
     */
    external fun getUiccMode(path: String): Int

    /**
     * Set the uicc mode
     * @param path  The mode of configuration file
     * @parammeaning: 0:vUICC ，1:eUICC
     *
     * @return int  0:Success , -1:error
     */
    external fun setUiccMode(mode: Int): Int

    /**
     * Get eid from eUICC/vUICC
     * @param eId  The array of eid
     * @param eIdLength  The length of eid
     *
     * @return int  0:Success , -1:error
     */

    external fun getEId(eId: ByteArray, eIdLength: IntArray): Int

    /**
     * Get profile information
     * @param profile  profiles
     * @param profileLength  The length of profiles
     *
     * @return int  0:Success , -1:error
     */
    external fun getProfiles(profile: ByteArray, profileLength: IntArray): Int

    /**
     * Delete one profile
     * @param iccid  The iccid of profile
     *
     * @return int  0:Success , -1:error
     */
    external fun deleteProfile(iccid: String): Int

    /**
     * Enable one Profile
     * @param iccid  The iccid of profile
     *
     * @return int  0:Success , -1:error
     */
    external fun enableProfile(iccid: String): Int

    /**
     * Close one Profile
     * @param iccid  The iccid of profile
     *
     * @return Int  0:Success , -1:error
     */
    external fun disableProfile(iccid: String): Int

    /**
     * Get mcc
     *
     * @return Int  Return the value of mnc
     */
    fun getMcc(): Int {
        return SharePrefSetting.getMcc()
    }

    /**
     * Get mnc
     *
     * @return Int  Return the value of mnc
     */

    fun getMnc(): Int {
        return SharePrefSetting.getMnc()
    }

    /**
     * Get device imei
     *
     * @return String  The string of imei
     */
    fun getImei(): String {
        return telephonySetting.getImei()
    }

    /**
     * Get the device Model
     *
     * @return String  The string of model
     */
    fun getModel(): String {
        return telephonySetting.getModel()
    }

    /**
     * Get moniotr version
     *
     * @return String  The string of version
     */
    fun getMonitorVersion(): String {
        return dispatcherService?.versionName.toString()
    }

    /**
     * Open a logic channel
     *
     * @return Int  Return channel id
     */
    fun openChannel(): Int {
        return libraryService.openChannel()
    }

    /**
     * Close a Logic channel
     * @param channelId  要关闭的channel id
     *
     * @return  Int  正确 0，错误 -1
     */
    fun closeChannel(channelId: Int): Int {
        return if (libraryService.closeChannel(channelId))
            0
        else {
            1
        }
    }

    /**
     * Agent transmit with eUICC
     * @param data  Request data for apdu
     * @param channelId  Logical channel id
     *
     * @return  ByteArray  Response data for apdu
     */
    fun transmitApdu(data: ByteArray, channelId: Int): ByteArray {
        return libraryService.transmit(data, channelId)!!
    }

    /**
     * Agent transmit with vUICC
     * @param apdu  Request data for apdu
     *
     * @return  ByteArray  Response data for apdu
     */
    fun commandApdu(apdu: ByteArray): ByteArray {
        if (!checkMonitorServiceReady()) {
            return ByteArray(0)
        }
        return dispatcherService!!.commandApdu(apdu)
    }

    /**
     * Get current used iccid
     *
     * @return  String  The string of iccid
     */
    fun getCurrentIccid(): String {
        telephonySetting.initIccid()
        //return SharePrefSetting.getCurrentIccId()
        return null
    }

    /**
     * Get current used imsi
     *
     * @return  String  The string of imsi
     */
    fun getCurrentImsi(): String {
        return telephonySetting.getImsi(0)
    }

    /**
     * Get the dbm
     *
     * @return  Int  The value of dbm
     */
    fun getSignalDbm(): Int {
        return SharePrefSetting.getSignalDbm()
    }

    /**
     * Get signal level
     *
     * @return  Int  The value of leve
     */
    fun getSignalLevel(): Int {
        return SharePrefSetting.getSignalLevel()
    }

    /**
     * Get network type(4G/3G/2G)
     *
     * @return  String  The String of type
     */
    fun getNetworkType(): String {
        return telephonySetting.getNetworkType()
    }

    /**
     * Setting apn name
     * @param apn  apn name
     * @param mccMnc  mccmnc(eg:46000)
     *
     * @return  Int  0:success , -1:error
     */
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
