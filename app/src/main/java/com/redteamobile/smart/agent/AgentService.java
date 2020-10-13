package com.redteamobile.smart.agent;

import android.app.Notification;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.redteamobile.monitor.IDispatcherService;
import com.redteamobile.smart.Constant;
import com.redteamobile.smart.JniInterface;
import com.redteamobile.smart.util.AssetManager;
import com.redteamobile.smart.util.LooperUtil;
import com.redteamobile.smart.util.RetryUtil;

import java.util.concurrent.CountDownLatch;

import static com.redteamobile.smart.Constant.ACTION_BOOTSTRAP_READY;
import static com.redteamobile.smart.Constant.ACTION_NETWORK_STATE_CHANGED;

public class AgentService extends Service {

    private static final String TAG = "AgentService";
    private IDispatcherService dispatcherService = null;
    private JniInterface jniInterface;
    private SlotMonitor slotMonitor;
    private boolean isInitAgent = false;
    private final LooperUtil looperUtil = new LooperUtil();

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ACTION_NETWORK_STATE_CHANGED.equals(intent.getAction())) {
                int networkState = intent.getIntExtra(Constant.TAG_NETWORK_STATE, 0);
                networkUpdateState(networkState);
            } else if (ACTION_BOOTSTRAP_READY.equals(intent.getAction())) {
                initAgent();
            }
        }
    };

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            dispatcherService = IDispatcherService.Stub.asInterface(service);
            monitorReady.countDown();
            if (dispatcherService != null) {
                jniInterface.setService(dispatcherService);
            }
            if (!isInitAgent) {
                isInitAgent = true;
                initJniInterface();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            dispatcherService = null;
            monitorReady = MONITOR_NOT_READY;
            if (checkMonitorInstalled()) {
                bindService();
            } else {
                Toast.makeText(
                        AgentService.this,
                        "Monitor uninstalled, please restart.",
                        Toast.LENGTH_LONG
                ).show();
                new RetryUtil(looperUtil.getLooper(), new Runnable() {
                    @Override
                    public void run() {
                        android.os.Process.killProcess(android.os.Process.myPid());
                        System.exit(0);
                    }
                }, 0 /* maxRetries */, 3000L /* delayMillis */).retry(false);
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        new AssetManager(this).preloadedToCache();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_NETWORK_STATE_CHANGED);
        intentFilter.addAction(ACTION_BOOTSTRAP_READY);
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(broadcastReceiver, intentFilter);
    }

    private void initAgent() {
        jniInterface = new JniInterface(this);
        int uiccMode = jniInterface.getUiccMode(Constant.FILE_STORAGE_PATH);
        if (uiccMode != 1) {
            bindService();
        } else {
            initJniInterface();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (isInitAgent) {
            LocalBroadcastManager.getInstance(AgentService.this)
                    .sendBroadcast(new Intent(Constant.ACTION_SERVICE_CONNECTED));
        }
        return super.onStartCommand(intent, flags, startId);

    }

    private void initJniInterface() {
        new RetryUtil(looperUtil.getLooper(), new Runnable() {
            @Override
            public void run() {
                jniInterface.main(Constant.FILE_STORAGE_PATH);
                slotMonitor = new SlotMonitor(getApplicationContext());
                slotMonitor.startMonitor();
                LocalBroadcastManager.getInstance(AgentService.this)
                        .sendBroadcast(new Intent(Constant.ACTION_SERVICE_CONNECTED));
            }
        }, 0 /* maxRetries */, 0 /* delayMillis */).retry(false);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (dispatcherService != null) {
            monitorReady = MONITOR_NOT_READY;
        }
        stopService();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
    }

    private void networkUpdateState(int state) {
        jniInterface.networkUpdateState(state);
    }

    public String getImei() {
        return jniInterface.getImei();
    }

    public int getEId(byte[] eId, int[] eIdLength) {
        return jniInterface.getEId(eId, eIdLength);
    }

    public final int getProfiles(byte[] profile, int[] profileLength) {
        return jniInterface.getProfiles(profile, profileLength);
    }

    public int deleteProfile(String iccid) {
        return jniInterface.deleteProfile(iccid);
    }

    public int enableProfile(String iccid) {
        return jniInterface.enableProfile(iccid);
    }

    public int disableProfile(String iccid) {
        return jniInterface.disableProfile(iccid);
    }

    private void setEuiccMode() {
        jniInterface.setUiccMode(1);
    }

    private void setVuiccMode() {
        jniInterface.setUiccMode(0);
    }

    public class MyBinder extends Binder {

        public AgentService getService() {
            return AgentService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new AgentService.MyBinder();
    }


    private boolean bindService() {
        boolean result = false;
        Intent intent = new Intent();
        intent.setComponent(
                new ComponentName(Constant.MONITOR_PACKAGE_NAME, Constant.MONITOR_SERVICE_NAME));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
            Log.e(TAG, "startForegroundService");
            startForeground(1, new Notification());
        } else {
            startService(intent);
            Log.e(TAG, "startService");
        }
        result = bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        return result;
    }

    private boolean stopService() {
        boolean result = true;
        if (dispatcherService != null) {
            Intent intent = new Intent();
            intent.setComponent(
                    new ComponentName(Constant.MONITOR_PACKAGE_NAME, Constant.MONITOR_SERVICE_NAME));
            unbindService(serviceConnection);
            result = stopService(intent);
        }
        return result;
    }

    private boolean checkMonitorInstalled() {
        try {
            PackageInfo packageInfo = getPackageManager()
                    .getPackageInfo(Constant.MONITOR_PACKAGE_NAME, 0);
            return packageInfo != null;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return false;
    }

    private static final CountDownLatch MONITOR_NOT_READY = new CountDownLatch(1);
    public static CountDownLatch monitorReady;

    static {
        monitorReady = MONITOR_NOT_READY;
    }
}
