package com.redteamobile.smart.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.redteamobile.smart.agent.AgentService;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {

        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.i("TAG","Agent Start------");
            Intent startAgentServiceIntent = new Intent(context, AgentService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(startAgentServiceIntent);
            } else {
                context.startService(startAgentServiceIntent);
            }
        }
    }
}
