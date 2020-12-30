package com.redteamobile.smart.recycler;

public class ProfileModel {

    private String iccid;
    private int type;
    private int state;

    public ProfileModel(String iccid, int type, int state) {
        this.iccid = iccid;
        this.type = type;
        this.state = state;
    }

    public String getIccid() {
        return iccid;
    }

    public void setIccid(String iccid) {
        this.iccid = iccid;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }
}
