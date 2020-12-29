package com.redteamobile.smart;

import android.content.Context;

import com.redteamobile.smart.util.SharePrefSetting;

/**
 * 包含native方法声明以及C调用的java方法；
 */
public class Jni {

    private static final String TAG = Jni.class.getSimpleName();

    private UiccManger uiccManger;

    public Jni(Context context, UiccManger uiccManger) {
        this.uiccManger = uiccManger;
        System.loadLibrary("agent_jni");
    }

    public final native int logPrintString(int level, String log);

    public final native int init(String path);

    public final native int main();

    public final native int networkUpdateState(int networkState);

    public final native int getUiccMode(String path);

    public final native int setUiccMode(int mode);

    public final native int getEId(byte[] eId, int[] eIdLength);

    public final native int getProfiles(byte[] profile, int[] profileLength);

    public final native int deleteProfile(String iccid);

    public final native int enableProfile(String iccid);

    public final native int disableProfile(String iccid);

    public final native int stopUicc();
    public final native int startUicc();

    public int getMcc() {
        return SharePrefSetting.getMcc();
    }

    public int getMnc() {
        return SharePrefSetting.getMnc();
    }

    public String getImei() {
        return uiccManger.getImei();
    }

    public String getModel() {
        return uiccManger.getModel();
    }

    public String getMonitorVersion() {
        return uiccManger.getMonitorVersion();
    }

    public void notifyCardState() {
        uiccManger.notifyCardChange();
    }

    public int openChannel() {
        return uiccManger.openChannel();
    }

    public int closeChannel(int channelId) {
        return uiccManger.closeChannel(channelId);
    }

    public final byte[] transmitApdu(byte[] data, int channelId) {
        return uiccManger.transmitApdu(data, channelId);
    }

    public final byte[] commandApdu(byte[] apdu) {
        return uiccManger.commandApdu(apdu);
    }

    public String getCurrentIccid() {
        return uiccManger.getCurIccid();
    }

    public String getCurrentImsi() {
        return uiccManger.getCurImsi();
    }

    public int getSignalDbm() {
        return uiccManger.getSignalDbm();
    }

    public int getSignalLevel() {
        return uiccManger.getSignalLevel();

    }

    public String getNetworkType() {
        return uiccManger.getNetworkType();
    }

    public int setApn(String apn, String mccMnc) {
        return uiccManger.setApn(apn, mccMnc);
    }
}
