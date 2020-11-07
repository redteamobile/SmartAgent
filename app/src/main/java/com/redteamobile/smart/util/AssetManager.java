package com.redteamobile.smart.util;

import android.content.Context;
import android.content.Intent;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.redteamobile.smart.Constant;

import java.io.File;

public class AssetManager {

    private static final String DEFAULT_ASSETS = "assets.der";
    private final String ASSETS = "rt_share_profile.der";
    private String cachePath;
    private String cacheFile;
    private Context context;

    public AssetManager(Context context) {
        super();
        cachePath = FileUtil.getAppPath(context) + "/";
        cacheFile = cachePath + ASSETS;
        this.context = context;
    }

    public void preloadedToCache() {
        File path = new File(this.cachePath);
        if (!path.exists()) {
            path.mkdirs();
        }
        File file = new File(this.cacheFile);
        if (file.exists()) {
            LocalBroadcastManager.getInstance(context)
                    .sendBroadcast(new Intent(Constant.ACTION_BOOTSTRAP_READY));
        }
        new AssetUtil(context).copyFileInThread(DEFAULT_ASSETS, ASSETS, cachePath);
    }
}
