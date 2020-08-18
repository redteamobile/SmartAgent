package com.redteamobile.smart.agent.util

import android.content.Context
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.redteamobile.smart.Constant
import java.io.File

class AssetManager {

    private val TAG: String = AssetManager::class.java.simpleName

    private val DEFAULT_ASSETS = "B200518021540844291_assets.der"
    private val ASSETS = "rt_share_profile.der"

    private var cachePath: String? = null
    private var cacheFile: String? = null
    private var context: Context

    constructor(context: Context) {
        this.context = context
        cachePath = FileUtil.getFilesDir(context).absolutePath.toString() + "/"
        cacheFile = cachePath + ASSETS
    }

    // todo 路径需要验证
    fun preloadedToCache() {
        LogUtil.i(TAG, "preloadedToCache(): $cachePath")
        val path = File(cachePath)
        if (!path.exists()) {
            path.mkdirs()
        }
        val file = File(cacheFile)
        if (file.exists()) {
            context?.let { LocalBroadcastManager.getInstance(it).sendBroadcast(Intent(Constant.ACTION_BOOTSTRAP_READY)) }
        }
        AssetUtil(context).copyFileInThread(DEFAULT_ASSETS, ASSETS, cachePath)
    }
}
