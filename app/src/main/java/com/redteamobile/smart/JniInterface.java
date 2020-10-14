package com.redteamobile.smart;

import android.content.Context;
import android.os.RemoteException;

import com.redteamobile.library.code.service.LibraryService;
import com.redteamobile.monitor.IDispatcherService;
import com.redteamobile.smart.agent.AgentService;
import com.redteamobile.smart.cellular.TelephonySetting;
import com.redteamobile.smart.cellular.TelephonySettingImpl;
import com.redteamobile.smart.util.SharePrefSetting;

import java.util.concurrent.TimeUnit;

public class JniInterface {

    private static final String TAG = "JniInterface";
    private TelephonySetting telephonySetting;
    private LibraryService libraryService;
    private IDispatcherService dispatcherService;

    public JniInterface(Context context) {
        System.loadLibrary("agent_jni");
        this.telephonySetting = new TelephonySettingImpl(context);
        libraryService = new LibraryService(context);
    }

    public void setService(IDispatcherService dispatcherService) {
        this.dispatcherService = dispatcherService;
    }

    public final native int logPrintString(int level, String log);

    public final native int main(String path);

    public final native int networkUpdateState(int networkState);

    public final native int getUiccMode(String path);

    public final native int setUiccMode(int mode);

    public final native int getEId(byte[] eId, int[] eIdLength);

    public final native int getProfiles(byte[] profile, int[] profileLength);

    public final native int deleteProfile(String iccid);

    public final native int enableProfile(String iccid);

    public final native int disableProfile(String iccid);

    public int getMcc() {
        return SharePrefSetting.getMcc();
    }

    public int getMnc() {
        return SharePrefSetting.getMnc();
    }

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

    public int openChannel() {
        return libraryService.openChannel();
    }

    public int closeChannel(int channelId) {
        return libraryService.closeChannel(channelId) ? 0 : 1;
    }

    public final byte[] transmitApdu(byte[] data, int channelId) {
        return libraryService.transmit(data, channelId);
    }

    public final byte[] commandApdu(byte[] apdu) {
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

    public String getCurrentIccid() {
        telephonySetting.initIccid();
        return SharePrefSetting.getCurrentIccId();
    }

    public String getCurrentImsi() {
        return telephonySetting.getImsi(0);
    }

    public int getSignalDbm() {
        return SharePrefSetting.getSignalDbm();
    }

    public int getSignalLevel() {
        return SharePrefSetting.getSignalLevel();
    }

    public String getNetworkType() {
        return telephonySetting.getNetworkType();
    }

    public int setApn(String apn, String mccMnc) {
        return telephonySetting.configureApn(apn, mccMnc) ? 0 : -1;
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
}
