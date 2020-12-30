package com.redteamobile.smart.cellular;

public interface TelephonySetting {

    String getImei();

    boolean getDataEnabled(int subId);

    void setDataEnabled(int subId, boolean enable);

    boolean getDataRoamingEnabled(int subId);

    void setDataRoamingEnabled(int subId, boolean enable);

    boolean setNonRoamingForOperator(String mccMnc);

    boolean setOperatorBrandName(String name);

    int getSubId(int slotId);

    String getImsi(int subId);

    String initIccid();

    boolean isSimReady(int slotId);

    int getDefaultDataSubId();

    void setDefaultDataSubId(int subId);

    void setDisplayName(int subId, String name);

    String getModel();

    String getNetworkType();

    boolean configureApn(String apn, String mccMnc);

    boolean clearApn();

}
