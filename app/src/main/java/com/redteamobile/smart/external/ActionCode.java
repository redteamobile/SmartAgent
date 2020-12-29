package com.redteamobile.smart.external;

public enum ActionCode {
    SUCCESS(0), FAIL(-1);
    private int code;

    ActionCode(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
