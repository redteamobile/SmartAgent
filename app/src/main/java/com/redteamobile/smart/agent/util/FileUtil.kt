package com.redteamobile.smart.agent.util

import android.content.Context
import com.redteamobile.smart.Constant
import java.io.File

class FileUtil {

    companion object {
        private val TAG = "FileUtil"

        fun getFilesDir(context: Context): File {
            var cacheDir: File? = File(Constant.FILE_STORAGE_PATH)
            if (cacheDir == null) {
                cacheDir = File(Constant.FILE_STORAGE_PATH)
            }
            return cacheDir
        }
    }
}
