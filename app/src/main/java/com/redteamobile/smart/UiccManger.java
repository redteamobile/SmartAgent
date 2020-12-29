package com.redteamobile.smart;

import android.content.Context;
import android.content.Intent;
import android.os.RemoteException;
import android.text.TextUtils;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.redteamobile.library.code.service.LibraryService;
import com.redteamobile.monitor.IDispatcherService;
import com.redteamobile.smart.agent.AgentService;
import com.redteamobile.smart.cellular.TelephonySetting;
import com.redteamobile.smart.cellular.TelephonySettingImpl;
import com.redteamobile.smart.external.UiccExternal;
import com.redteamobile.smart.util.SharePrefSetting;

import java.util.concurrent.TimeUnit;

/**
 * 管理euicc和vuicc功能类,包括获取系统信息等
 */
public class UiccManger implements UiccExternal {
    private static final String TAG = UiccManger.class.getSimpleName();
    private static UiccManger instance;
    private Jni jni;
    private String storagePath;
    private TelephonySetting telephonySetting;
    private LibraryService libraryService;
    private IDispatcherService dispatcherService;
    private Context context;

    private UiccManger(Context context) {
        jni = new Jni(context.getApplicationContext(), this);
        this.telephonySetting = new TelephonySettingImpl(context);
        libraryService = new LibraryService(context);
        this.context = context;
    }

    public static UiccManger getInstance(Context context) {
        if (instance == null) {
            instance = new UiccManger(context);
        }
        return instance;
    }

    public void setService(IDispatcherService dispatcherService) {
        this.dispatcherService = dispatcherService;
    }

    public final int init(String path) {
        storagePath = path;
        return jni.init(path);
    }

    public final int main() {
        return jni.main();
    }

    public final int logPrintString(int level, String log) {
        return jni.logPrintString(level, log);
    }

    @Override
    public final int networkUpdateState(int networkState) {
        return jni.networkUpdateState(networkState);
    }

    @Override
    public final int getUiccMode() {
        if (!TextUtils.isEmpty(storagePath)) {
            return jni.getUiccMode(storagePath);
        } else {
            return Constant.UNKNOWN_MODE;
        }
    }

    @Override
    public final int setUiccMode(int mode) {
        return jni.setUiccMode(mode);
    }

    @Override
    public final int getEId(byte[] eId, int[] eIdLength) {
        return jni.getEId(eId, eIdLength);
    }

    @Override
    public final int getProfiles(byte[] profile, int[] profileLength) {
        return jni.getProfiles(profile, profileLength);
    }

    @Override
    public final int deleteProfile(String iccid) {
        return deleteProfile(iccid);
    }

    @Override
    public final int enableProfile(String iccid) {
        return jni.enableProfile(iccid);
    }

    @Override
    public final int disableProfile(String iccid) {
        return jni.disableProfile(iccid);
    }

    @Override
    public int closeUicc() {
        return jni.stopUicc();
    }

    @Override
    public int insertUicc() {
        return jni.startUicc();
    }

    @Override
    public String getImei() {
        return telephonySetting.getImei();
    }

    public String getModel() {
        return telephonySetting.getModel();
    }

    public String getMonitorVersion() {
        if (dispatcherService == null) {
            return "";
        }
        try {
            return dispatcherService.getVersionName();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return "";
    }

    public void notifyCardChange() {
        LocalBroadcastManager.getInstance(context)
                .sendBroadcast(new Intent(Constant.ACTION_NOTIFY_STATE));
    }

    public int openChannel() {
        return libraryService.openChannel();
    }

    public int closeChannel(int channelId) {
        return libraryService.closeChannel(channelId) ? 0 : 1;
    }

    public byte[] transmitApdu(byte[] data, int channelId) {
        return libraryService.transmit(data, channelId);
    }

    private boolean checkMonitorServiceReady() {
        boolean result;
        try {
            result = dispatcherService != null && AgentService.monitorReady.await(5000L, TimeUnit.MILLISECONDS);
        } catch (InterruptedException var3) {
            result = false;
        }

        return result;
    }

    public byte[] commandApdu(byte[] apdu) {
        if (!checkMonitorServiceReady()) {
            return new byte[0];
        } else {
            try {
                byte[] commandApdu = dispatcherService.commandApdu(apdu);
                return commandApdu;
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    public String getCurIccid() {
        // 注意：如果获取系统的iccid不准确，那么返回null，交给native agent自己去获取当前使用的card，但实际上我们应该信任系统
//        telephonySetting.initIccid();
        return null;
    }

    public String getCurImsi() {
        return telephonySetting.getImsi(0);
    }

    public int getSignalDbm() {
        return SharePrefSetting.getSignalDbm();
    }

    public int getSignalLevel() {
        return SharePrefSetting.getSignalLevel();
    }

    public int setApn(String apn, String mccMnc) {
        return telephonySetting.configureApn(apn, mccMnc) ? 0 : -1;
    }

    public String getNetworkType() {
        return telephonySetting.getNetworkType();
    }

}
