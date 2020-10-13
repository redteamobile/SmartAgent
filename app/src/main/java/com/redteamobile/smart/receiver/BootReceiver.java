package com.redteamobile.smart.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.redteamobile.smart.agent.AgentService;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Intent startAgentServiceIntent = new Intent(context, AgentService.class);
            context.startService(startAgentServiceIntent);
        }
    }
}
