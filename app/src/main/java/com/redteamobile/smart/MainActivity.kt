package com.redteamobile.smart

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.IBinder
import android.text.TextUtils
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.redteamobile.smart.agent.AgentService
import com.redteamobile.smart.agent.AgentService.MyBinder
import com.redteamobile.smart.agent.recycler.ProfileAdapter
import com.redteamobile.smart.agent.recycler.ProfileModel
import com.redteamobile.smart.agent.util.SharePrefSetting
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONObject

class MainActivity : AppCompatActivity(), View.OnClickListener {

    private var agentService: AgentService? = null
    private var profile = ByteArray(1024)
    private var profileLength: IntArray = IntArray(1)

    private var eId = ByteArray(32)
    private var eIdLength: IntArray = IntArray(1)
    private var profileList = ArrayList<ProfileModel>()
    private var isNeedCheck = true
    private val PERMISSON_REQUESTCODE = 110
    private val LOCATION_CODE = 1315
    private val needPermissions = arrayOf<String>(Manifest.permission.READ_PHONE_STATE)

    private val broadcastReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action
            if (Constant.ACTION_NETWORK_STATE_CHANGED == action) {
                val networkState = intent?.getIntExtra(Constant.TAG_NETWORK_STATE, 0)
                if (networkState == Constant.DSI_STATE_CALL_CONNECTED) {
                    showData()
                    parseProfiles()
                }
            } else if (Constant.ACTION_SERVICE_CONNECTED == action) {
                showData()
                parseProfiles()
            }
        }

    }

    private val serviceConnection = object : ServiceConnection {

        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            agentService = (service as MyBinder).getService()
            val adapter = profileRecycler.adapter as ProfileAdapter
            adapter.setAgentService(agentService!!)
        }

        override fun onServiceDisconnected(name: ComponentName) {
        }
    }

    fun checkPermissions(permissions: Array<String>) {
        val needRequestPermissonList = findDeniedPermissions(permissions)
        if (null != needRequestPermissonList && needRequestPermissonList.size > 0) {
            ActivityCompat.requestPermissions(this,
                needRequestPermissonList.toTypedArray(), PERMISSON_REQUESTCODE)
        }
    }

    /**
     * 获取权限集中需要申请权限的列表
     * */
    fun findDeniedPermissions(permissions: Array<String>): List<String> {
        var needRequestPermissonList = ArrayList<String>()
        for (perm in permissions) {
            if (ContextCompat.checkSelfPermission(this,
                    perm) != PackageManager.PERMISSION_GRANTED || ActivityCompat.shouldShowRequestPermissionRationale(
                    this, perm)) {
                needRequestPermissonList.add(perm)
            }
        }
        return needRequestPermissonList
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, paramArrayOfInt: IntArray) {
        when (requestCode) {
            PERMISSON_REQUESTCODE -> if (!verifyPermissions(paramArrayOfInt)) {
                isNeedCheck = false
            }
            LOCATION_CODE -> {
                if (paramArrayOfInt.size > 0 && paramArrayOfInt[0] == PackageManager.PERMISSION_GRANTED) {
                    // 权限被用户同意。

                } else {
                    // 权限被用户拒绝了。
                    Toast.makeText(this@MainActivity, "定位权限被禁止，相关地图功能无法使用！", Toast.LENGTH_LONG).show()
                }

            }
        }

    }

    /**
     * 检测是否说有的权限都已经授权
     */
    fun verifyPermissions(grantResults: IntArray): Boolean {
        for (result in grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        var linearLayoutManager = LinearLayoutManager(this)
        linearLayoutManager.orientation = LinearLayoutManager.VERTICAL
        profileRecycler.layoutManager = linearLayoutManager
        profileRecycler.adapter = ProfileAdapter(profileList)

        var intentFilter = IntentFilter()
        intentFilter.addAction(Constant.ACTION_NETWORK_STATE_CHANGED)
        intentFilter.addAction(Constant.ACTION_SERVICE_CONNECTED)
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, intentFilter)

        enable_next_operationalTv.setOnClickListener(this)

        val intent = Intent(this, AgentService::class.java)
        startService(intent)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        if (isNeedCheck) {
            checkPermissions(needPermissions);
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(serviceConnection)
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver)
    }


    fun showData() {
        if (agentService == null) {
            return
        }

        agentService?.getEId(eId, eIdLength)
        var eid = String(eId)
        if (eid == null || eid.isEmpty()) {
            return
        }
        eidTv.text = eid
        imeiTv.text = agentService?.getImei()
        appVersionTv.text = BuildConfig.VERSION_NAME
    }

    private fun parseProfiles() {
        if (agentService == null) {
            return
        }
        profileList.clear()
        agentService?.getProfiles(profile, profileLength)
        var jsonArray = ByteArray(profileLength[0])
        System.arraycopy(profile, 0, jsonArray, 0, profileLength[0])
        val json = String(jsonArray)

        if (json == null || json.isEmpty()) {
            return
        }
        var jsonObject = JSONObject(json)
        val profiles = jsonObject.getJSONArray("profiles")
        for (i in 0 until profiles.length()) {
            var profile = profiles.getJSONObject(i)
            profileList.add(
                ProfileModel(
                    profile.getString("iccid"),
                    profile.getInt("type"),
                    profile.getInt("state")
                )
            )
        }
        if (profileList.size > 0) {
//            profileRecycler.adapter = agentService?.let { ProfileAdapter(profileList, it) }
            profileRecycler.adapter?.notifyDataSetChanged()
            var isContains = false
            for (profile in profileList) {
                if (TextUtils.equals(profile.iccid, SharePrefSetting.getCurrentIccId())) {
                    isContains = true
                    break
                }
            }
            var list = ArrayList<ProfileModel>()
            for (profile in profileList) {
                if (profile.state == 1) {
                    currentIccid.text = profile.iccid
                    if (!isContains) {
                        SharePrefSetting.putCurrentIccId(profile.iccid)
                    }
                }

                if (profile.type == 2) {
                    list.add(profile)
                }
            }

        }
    }

    override fun onClick(v: View?) {
        if (v != null) {
            if (profileList.size <= 0) {
                return
            }
            when (v.id) {
                startService.id and enable_next_operationalTv.id -> {
                    for (profile in profileList) {
                        if (profile.type == 2) {
                            if (profile.state == 0) {
                                if (agentService == null) {
                                    return
                                }
                                agentService!!.enableProfile(profile.iccid)
                            }
                        }
                    }
                }
                stopService.id -> {
                    for (profile in profileList) {
                        if (profile.type == 2) {
                            if (profile.state == 1) {
                                if (agentService == null) {
                                    return
                                }
                                agentService!!.disableProfile(profile.iccid)
                            }
                        }
                    }
                }
                enable_provisioningTv.id -> {
                    for (profile in profileList) {
                        if (profile.type == 1) {
                            if (agentService == null) {
                                return
                            }
                            agentService!!.enableProfile(profile.iccid)
                        }
                    }
                }
                qa_finishTv.id -> {
                    for (profile in profileList) {
                        if (profile.type == 2) {
                            if (agentService == null) {
                                return
                            }
                            agentService!!.deleteProfile(profile.iccid)
                        }
                    }
                }
            }

        }
    }

}
