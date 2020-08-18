package com.redteamobile.smart.agent.util

import com.redteamobile.smart.JniInterface

class LogUtil {

    companion object {

        private val TAG = LogUtil::class.java.simpleName
        private const val LOG_NONE = 0
        private const val LOG_ERR = 1
        private const val LOG_WARN = 2
        private const val LOG_DBG = 3
        private const val LOG_INFO = 4
        private const val LOG_ALL = 5
        private var jniInterface: JniInterface? = null

        fun init(jniInterface: JniInterface) {
            this.jniInterface = jniInterface
        }

        @JvmStatic
        fun v(tag: String, msg: String) {
            jniInterface?.logPrintString(LOG_ALL, " [$tag] $msg\n")
        }

        @JvmStatic
        fun d(tag: String, msg: String) {
            jniInterface?.logPrintString(LOG_DBG, " [$tag] $msg\n")
        }

        @JvmStatic
        fun i(tag: String, msg: String) {
            jniInterface?.logPrintString(LOG_INFO, " [$tag] $msg\n")
        }

        @JvmStatic
        fun w(tag: String, msg: String) {
            jniInterface?.logPrintString(LOG_WARN, " [$tag] $msg\n")
        }

        @JvmStatic
        fun e(tag: String, msg: String) {
            jniInterface?.logPrintString(LOG_ERR, " [$tag] $msg\n")
        }

        @JvmStatic
        fun e(tag: String, msg: String, ex: Throwable) {
            jniInterface?.logPrintString(LOG_ERR, " [$tag] $msg\n$ex")
        }
    }
}
