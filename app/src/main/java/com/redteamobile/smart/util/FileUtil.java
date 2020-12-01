package com.redteamobile.smart.util;

import android.content.Context;
import android.util.Log;

public class FileUtil {

    /**
     * @param context
     * @return 存放文件地方，
     */
    public static String getAppPath(Context context) {
        Log.e("TAG", "getAppPath: "+ context.getFilesDir().getParent() );
        return context.getFilesDir().getParent();
    }
}
