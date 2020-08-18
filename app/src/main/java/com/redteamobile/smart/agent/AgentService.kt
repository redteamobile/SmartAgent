package com.redteamobile.smart.agent

import android.app.Notification
import android.app.Service
import android.content.*
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.redteamobile.monitor.IDispatcherService
import com.redteamobile.smart.Constant
import com.redteamobile.smart.Constant.Companion.ACTION_BOOTSTRAP_READY
import com.redteamobile.smart.Constant.Companion.ACTION_NETWORK_STATE_CHANGED
import com.redteamobile.smart.Constant.Companion.TAG_NETWORK_STATE
import com.redteamobile.smart.JniInterface
import com.redteamobile.smart.agent.util.*
import java.util.concurrent.CountDownLatch
import kotlin.system.exitProcess

class AgentService : Service() {

    private val TAG = AgentService::class.java.simpleName

    private var dispatcherService: IDispatcherService? = null

    private var jniInterface: JniInterface? = null

    private var slotMonitor: SlotMonitor? = null
    private var isInitAgent = false
    private val looperUtil = LooperUtil()

    companion object {
        private val MONITOR_NOT_READY = CountDownLatch(1)
        var monitorReady = MONITOR_NOT_READY
    }

    private val broadcastReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action
            if (ACTION_NETWORK_STATE_CHANGED == action) {
                val networkState = intent?.getIntExtra(TAG_NETWORK_STATE, 0)
                networkUpdateState(networkState)
            } else if (ACTION_BOOTSTRAP_READY == action) {
                initAgent()
            }
        }

    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            dispatcherService = IDispatcherService.Stub.asInterface(service)
            monitorReady.countDown()
            LogUtil.e(TAG, "onServiceConnected()")

            dispatcherService.let {
                if (it != null) {
                    jniInterface!!.setService(it)
                }
            }
            if (!isInitAgent) {
                isInitAgent = true
                initJniInterface()
            }
        }

        override fun onServiceDisconnected(name: ComponentName) {
            LogUtil.e(TAG, "onServiceDisconnected()")
            dispatcherService = null
            monitorReady = MONITOR_NOT_READY
            if (checkMonitorInstalled()) {
                Log.e(TAG, "monitor is not null")
                bindService()
            } else {
                Toast.makeText(
                    this@AgentService,
                    "Monitor uninstalled, please restart.",
                    Toast.LENGTH_LONG
                ).show()

                RetryUtil(looperUtil.looper, Runnable {
                    Log.e(TAG, "monitor is null")
                    android.os.Process.killProcess(android.os.Process.myPid())
                    exitProcess(0)
                }, 0 /* maxRetries */, 3000L /* delayMillis */).retry(false)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        AssetManager(this).preloadedToCache()

        var intentFilter = IntentFilter()
        intentFilter.addAction(ACTION_NETWORK_STATE_CHANGED)
        intentFilter.addAction(ACTION_BOOTSTRAP_READY)
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(broadcastReceiver, intentFilter)
    }

    private fun initAgent() {
        jniInterface = JniInterface(this)
        val mode = jniInterface!!.getUiccMode(Constant.FILE_STORAGE_PATH)
        if (mode == 0) { // vUICC mode
            bindService()
        } else {
            initJniInterface()
        }
    }

    private fun initJniInterface() {
        RetryUtil(looperUtil.looper, Runnable {
            jniInterface?.main(Constant.FILE_STORAGE_PATH)
            jniInterface?.let { LogUtil.init(it) }
            slotMonitor = SlotMonitor(applicationContext)
            slotMonitor!!.startMonitor()
            LocalBroadcastManager.getInstance(this)
                .sendBroadcast(Intent(Constant.ACTION_SERVICE_CONNECTED))
        }, 0 /* maxRetries */, 0 /* delayMillis */).retry(false)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (dispatcherService != null) {
            monitorReady = MONITOR_NOT_READY
        }
        stopService()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver)
    }

    fun networkUpdateState(state: Int) {
        jniInterface?.networkUpdateState(state)
    }

    fun getImei(): String {
        return jniInterface?.getImei().toString()
    }

    fun getEId(eId: ByteArray, eIdLength: IntArray): Int {
        return jniInterface?.getEId(eId, eIdLength)!!
    }

    fun getProfiles(profile: ByteArray, profileLength: IntArray): Int {
        return jniInterface?.getProfiles(profile, profileLength)!!
    }

    fun deleteProfile(iccid: String): Int {
        return jniInterface?.deleteProfile(iccid)!!
    }

    fun enableProfile(iccid: String): Int {
        return jniInterface?.enableProfile(iccid)!!
    }

    fun disableProfile(iccid: String): Int {
        return jniInterface?.disableProfile(iccid)!!
    }

    override fun onBind(intent: Intent?): IBinder {
        return MyBinder()
    }

    inner class MyBinder : Binder() {
        fun getService(): AgentService {
            return this@AgentService
        }
    }

    fun bindService(): Boolean {
        var result = false
        return try {
            val uimIntent = Intent()
            uimIntent.component = ComponentName(
                Constant.MONITOR_PACKAGE_NAME, Constant.MONITOR_SERVICE_NAME
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(uimIntent)
                Log.e(TAG, "startForegroundService")
                startForeground(1, Notification())
            } else {
                startService(uimIntent)
                Log.e(TAG, "startService")
            }
            result = bindService(uimIntent, serviceConnection, Context.BIND_AUTO_CREATE)!!
            Log.e(TAG, "result:$result")
            result
        } catch (e: Exception) {
            Log.e(TAG, "Bind monitor catch exception:", e)
            result
        }
    }

    private fun stopService(): Boolean {
        var result = true
        if (dispatcherService != null) {
            val uimIntent = Intent()
            uimIntent.component = ComponentName(
                Constant.MONITOR_PACKAGE_NAME, Constant.MONITOR_SERVICE_NAME
            )
            unbindService(serviceConnection)
            result = stopService(uimIntent)
        }
        LogUtil.i(TAG, String.format("Stop monitor result: %b", result))
        return result
    }

    private fun checkMonitorInstalled(): Boolean {
        var packageInfo: PackageInfo?
        try {
            packageInfo = packageManager.getPackageInfo(Constant.MONITOR_PACKAGE_NAME, 0)
        } catch (e: PackageManager.NameNotFoundException) {
            packageInfo = null
            e.printStackTrace()
        }
        return packageInfo != null
    }

}
