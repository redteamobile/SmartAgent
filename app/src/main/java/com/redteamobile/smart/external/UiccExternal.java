package com.redteamobile.smart.external;

/**
 * External interface
 */
public interface UiccExternal {

    public int networkUpdateState(int networkState);

    public int getUiccMode();

    public int setUiccMode(int mode);

    public int getEId(byte[] eId, int[] eIdLength);

    public int getProfiles(byte[] profile, int[] profileLength);

    public int deleteProfile(String iccid);

    public int enableProfile(String iccid);

    public int disableProfile(String iccid);

    public String getImei();
}
