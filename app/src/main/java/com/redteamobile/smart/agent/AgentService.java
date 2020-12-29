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
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.redteamobile.monitor.IDispatcherService;
import com.redteamobile.smart.Constant;
import com.redteamobile.smart.R;
import com.redteamobile.smart.UiccManger;
import com.redteamobile.smart.external.UiccExternal;
import com.redteamobile.smart.util.AssetManager;
import com.redteamobile.smart.util.FileUtil;
import com.redteamobile.smart.util.LogUtil;
import com.redteamobile.smart.util.LooperUtil;
import com.redteamobile.smart.util.RetryUtil;

import java.util.concurrent.CountDownLatch;

import static com.redteamobile.smart.Constant.ACTION_BOOTSTRAP_READY;
import static com.redteamobile.smart.Constant.ACTION_NETWORK_STATE_CHANGED;

public class AgentService extends Service implements UiccExternal {

    private static final String TAG = "AgentService";
    private IDispatcherService dispatcherService = null;
    private UiccManger uiccManager;
    private SlotMonitor slotMonitor;
    private final LooperUtil looperUtil = new LooperUtil();
    private String storagePath;
    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
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

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            dispatcherService = IDispatcherService.Stub.asInterface(service);
            monitorReady.countDown();
            if (dispatcherService != null) {
                uiccManager.setService(dispatcherService);
            }
            runAgentMain(true);
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
                        getResources().getString(R.string.monitor_uninstall),
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotification();
        }

        uiccManager = UiccManger.getInstance(this);
        new AssetManager(this).preloadedToCache();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_NETWORK_STATE_CHANGED);
        intentFilter.addAction(ACTION_BOOTSTRAP_READY);
        intentFilter.addAction(Constant.ACTION_NOTIFY_STATE);
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(broadcastReceiver, intentFilter);
    }

    private void initAgent() {
        storagePath = FileUtil.getAppPath(this);
        uiccManager.init(storagePath);
        int uiccMode = getUiccMode();
        LogUtil.e(TAG, "uiccMode: " + uiccMode);
        if (uiccMode == Constant.EUICC_MODE) {
            runAgentMain(false);
        } else if (uiccMode == Constant.VUICC_MODE) {
            bindService();
        }
    }

    // Delay calling jni main methods, etc.
    private void runAgentMain(boolean isVuicc) {
        new RetryUtil(looperUtil.getLooper(), new Runnable() {
            @Override
            public void run() {
                uiccManager.main();
                slotMonitor = new SlotMonitor(getApplicationContext());
                slotMonitor.startMonitor();
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

    @Override
    public int networkUpdateState(int networkState) {
        return uiccManager.networkUpdateState(networkState);
    }

    @Override
    public int getUiccMode() {
        return uiccManager.getUiccMode();
    }

    @Override
    public int setUiccMode(int mode) {
        return uiccManager.setUiccMode(mode);
    }

    @Override
    public int getEId(byte[] eId, int[] eIdLength) {
        return uiccManager.getEId(eId, eIdLength);
    }

    @Override
    public final int getProfiles(byte[] profile, int[] profileLength) {
        return uiccManager.getProfiles(profile, profileLength);
    }

    @Override
    public String getImei() {
        return uiccManager.getImei();
    }

    @Override
    public int deleteProfile(String iccid) {
        return uiccManager.deleteProfile(iccid);
    }

    @Override
    public int enableProfile(String iccid) {
        return uiccManager.enableProfile(iccid);
    }

    @Override
    public int disableProfile(final String iccid) {
        return uiccManager.disableProfile(iccid);
    }

    @Override
    public int closeUicc() {
        return uiccManager.closeUicc();
    }

    @Override
    public int insertUicc() {
        return uiccManager.insertUicc();
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

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        return super.onStartCommand(intent, flags, startId);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void createNotification() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        // 通知渠道的id
        String id = "my_channel_02";
        // 用户可以看到的通知渠道的名字.
        CharSequence name = "Agent";
//         用户可以看到的通知渠道的描述
        String description = "start Agent";
        int importance = NotificationManager.IMPORTANCE_HIGH;
        NotificationChannel mChannel = new NotificationChannel(id, name, importance);
        mChannel.setLightColor(Color.RED);
        mChannel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
//         最后在notificationmanager中创建该通知渠道 //
        notificationManager.createNotificationChannel(mChannel);

        // 为该通知设置一个id
        int notifyID = 2;
        // 通知渠道的id
        String CHANNEL_ID = id;
        // Create a notification and set the notification channel.
        Notification notification = new Notification.Builder(this)
                .setContentTitle("New Message").setContentText("You've received new messages.")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setChannelId(CHANNEL_ID)
                .build();
        startForeground(notifyID, notification);
    }

    private boolean bindService() {
        boolean result;
        Intent intent = new Intent();
        intent.setComponent(
                new ComponentName(Constant.MONITOR_PACKAGE_NAME, Constant.MONITOR_SERVICE_NAME));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
        result = bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        return result;
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
