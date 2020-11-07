package com.redteamobile.smart.agent;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.redteamobile.monitor.IDispatcherService;
import com.redteamobile.smart.Constant;
import com.redteamobile.smart.JniInterface;
import com.redteamobile.smart.R;
import com.redteamobile.smart.util.AssetManager;
import com.redteamobile.smart.util.FileUtil;
import com.redteamobile.smart.util.LogUtil;
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
    private final LooperUtil looperUtil = new LooperUtil();
    private String storagePath;
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ACTION_NETWORK_STATE_CHANGED.equals(intent.getAction())) {
                int networkState = intent.getIntExtra(Constant.TAG_NETWORK_STATE, Constant.DSI_STATE_CALL_IDLE);
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
            initJniInterface();
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
        storagePath = FileUtil.getAppPath(this);
        jniInterface.init(storagePath);
        int uiccMode = getUiccMode();
        LogUtil.e(TAG, "uiccMode: " + uiccMode);
        if (uiccMode == Constant.EUICC_MODE) {
            initJniInterface();
        } else if (uiccMode == Constant.VUICC_MODE) {
            bindService();
        }
    }

    private void initJniInterface() {
        new RetryUtil(looperUtil.getLooper(), new Runnable() {
            @Override
            public void run() {
                jniInterface.main();
                slotMonitor = new SlotMonitor(getApplicationContext());
                slotMonitor.startMonitor();
                LocalBroadcastManager.getInstance(AgentService.this)
                        .sendBroadcast(new Intent(Constant.ACTION_NOTIFY_STATE));
            }
        }, 0 /* maxRetries */, 0 /* delayMillis */).retry(false);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (dispatcherService != null) {
            monitorReady = MONITOR_NOT_READY;
        }
        if (slotMonitor != null) {
            slotMonitor.stopMonitor();
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

    public int disableProfile(final String iccid) {
        Log.e(TAG, "disableProfile: " + iccid);
        return jniInterface.disableProfile(iccid);
    }

    public void setVuiccMode() {
        jniInterface.setUiccMode(Constant.VUICC_MODE);
    }

    public void setEuiccMode() {
        jniInterface.setUiccMode(Constant.EUICC_MODE);
    }

    public int getUiccMode() {
        if (!TextUtils.isEmpty(storagePath)) {
            return jniInterface.getUiccMode(storagePath);
        }
        return Constant.UNKNOWN_MODE;
    }

    public class MyBinder extends Binder {
        public AgentService getService() {
            return AgentService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new MyBinder();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LocalBroadcastManager.getInstance(AgentService.this)
                .sendBroadcast(new Intent(Constant.ACTION_NOTIFY_STATE));
        return super.onStartCommand(intent, flags, startId);
    }

    private boolean bindService() {
        boolean result;
        Intent intent = new Intent();
        intent.setComponent(
                new ComponentName(Constant.MONITOR_PACKAGE_NAME, Constant.MONITOR_SERVICE_NAME));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
            startOForeground();
        } else {
            startService(intent);
        }
        result = bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        return result;
    }

    //Android O 启动需要前台服务
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void startOForeground() {
        String NOTIFICATION_CHANNEL_ID = "com.redteamobile.smart";
        String channelName = "Smart Background Service";
        NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_NONE);
        chan.setLightColor(Color.RED);
        chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.createNotificationChannel(chan);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
        Notification notification = notificationBuilder.setOngoing(true)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("App is running in background")
                .setPriority(NotificationManager.IMPORTANCE_MIN)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();
        startForeground(2, notification);
    }

    private void stopService() {
        if (dispatcherService != null) {
            Intent intent = new Intent();
            intent.setComponent(
                    new ComponentName(Constant.MONITOR_PACKAGE_NAME, Constant.MONITOR_SERVICE_NAME));
            unbindService(serviceConnection);
            stopService(intent);
        }
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
