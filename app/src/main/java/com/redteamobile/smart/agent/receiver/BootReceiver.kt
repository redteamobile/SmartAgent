package com.redteamobile.smart.agent.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.text.TextUtils
import android.util.Log
import com.redteamobile.smart.agent.AgentService

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (TextUtils.equals(Intent.ACTION_BOOT_COMPLETED, intent?.action)) {
            val agent = Intent(context, AgentService::class.java)
            agent.putExtra("action", Intent.ACTION_BOOT_COMPLETED)
            context?.startService(agent)
        }
    }
}
