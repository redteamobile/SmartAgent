package com.redteamobile.smart.util;

import android.content.Context;

import com.redteamobile.smart.Constant;

import java.io.File;

public class FileUtil {

    public static File getFilesDir(Context context) {
        File cacheDir = new File(Constant.FILE_STORAGE_PATH);
        return cacheDir;
    }
}
