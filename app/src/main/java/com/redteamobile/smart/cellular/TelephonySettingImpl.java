package com.redteamobile.smart.cellular;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import com.redteamobile.smart.util.LogUtil;
import com.redteamobile.smart.util.SharePrefSetting;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static android.telephony.TelephonyManager.NETWORK_TYPE_1xRTT;
import static android.telephony.TelephonyManager.NETWORK_TYPE_CDMA;
import static android.telephony.TelephonyManager.NETWORK_TYPE_EDGE;
import static android.telephony.TelephonyManager.NETWORK_TYPE_EVDO_A;
import static android.telephony.TelephonyManager.NETWORK_TYPE_GPRS;
import static android.telephony.TelephonyManager.NETWORK_TYPE_GSM;
import static android.telephony.TelephonyManager.NETWORK_TYPE_IDEN;
import static android.telephony.TelephonyManager.NETWORK_TYPE_IWLAN;
import static android.telephony.TelephonyManager.NETWORK_TYPE_LTE;
import static android.telephony.TelephonyManager.NETWORK_TYPE_TD_SCDMA;

public class TelephonySettingImpl implements TelephonySetting {

    private static final String TAG = "TelephonySettingImpl";
    private static final String APN_NAME = "Redtea Mobile";
    private static final Uri APN_URI = Uri.parse("content://telephony/carriers");
    private static final Uri PREFERRED_APN_URI = Uri.parse("content://telephony/carriers/preferapn");

    private TelephonyManager telephonyManager;
    private SubscriptionManager subscriptionManager;
    private ConnectivityManager connectivityManager;
    private ContentResolver resolver;
    private boolean isMultiSim;

    public TelephonySettingImpl(Context context) {
        telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        connectivityManager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        subscriptionManager = SubscriptionManager.from(context);
        resolver = context.getContentResolver();
        isMultiSim = false;

        try {
            Method method = TelephonyManager.class.getDeclaredMethod("isMultiSimEnabled");
            isMultiSim = (boolean) method.invoke(telephonyManager);
            LogUtil.d(TAG, "isMultiSimEnabled:" + isMultiSim);
        } catch (NoSuchMethodException e) {
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }

    }

    @Override
    public String getImei() {
        String imei = "";
        try {
            Method getImei = TelephonyManager.class.getDeclaredMethod("getImei", Integer.TYPE);
            imei = (String) getImei.invoke(this.telephonyManager, 0);
        } catch (Exception e) {
            Log.e(TAG, "Reflection exception" + e.getMessage());
        }
        return imei;
    }


    @Override
    public boolean getDataEnabled(int subId) {
        boolean result = false;
        try {
            Method method = TelephonyManager.class.getDeclaredMethod("getDataEnabled", Integer.TYPE);
            result = (boolean) method.invoke(this.telephonyManager, subId);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
        return result;
    }

    @Override
    public void setDataEnabled(int subId, boolean enable) {
        try {
            Method method = TelephonyManager.class
                    .getDeclaredMethod("setDataEnabled", Integer.TYPE, Boolean.TYPE);
            method.invoke(telephonyManager, subId, enable);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean getDataRoamingEnabled(int subId) {
        boolean result = false;
        if (!isMultiSim) {
            result = Settings.Global.getInt(this.resolver, "data_roaming", 0) != 0;
        } else {
            try {
                Method method = TelephonyManager.class
                        .getDeclaredMethod("getIntWithSubId", ContentResolver.class, String.class,
                                Integer.TYPE);
                result = (boolean) method
                        .invoke(this.telephonyManager, this.resolver, "data_roaming", subId);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    @Override
    public void setDataRoamingEnabled(int subId, boolean enable) {
        int roaming = enable ? 1 : 0;
        try {
            Settings.Global.putInt(this.resolver,
                    isMultiSim ? Settings.Global.DATA_ROAMING + subId : Settings.Global.DATA_ROAMING,
                    roaming);
        } catch (SecurityException e) {
            e.printStackTrace();
        }

        try {
            @SuppressLint("DiscouragedPrivateApi") Method method = SubscriptionManager.class
                    .getDeclaredMethod("setDataRoaming", Integer.TYPE, Integer.TYPE);
            method.invoke(this.subscriptionManager, roaming, subId);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean setNonRoamingForOperator(String mccMnc) {
        //TODO not implemented
        return false;
    }

    @Override
    public boolean setOperatorBrandName(String name) {
        return telephonyManager.setOperatorBrandOverride(name);
    }

    @Override
    public int getSubId(int slotId) {
        try {
            Method method = SubscriptionManager.class.getDeclaredMethod("getSubId", Integer.TYPE);
            int[] subIdArray = (int[]) method.invoke(null, slotId);
            if (subIdArray != null && subIdArray.length > 0) {
                return subIdArray[0];
            }
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
        return -1;
    }

    @Override
    public String getImsi(int subId) {
        String imsi = null;
        try {
            Method method = TelephonyManager.class.getDeclaredMethod("getSubscriberId", Integer.TYPE);
            imsi = (String) method.invoke(this.telephonyManager, subId);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
        return imsi;
    }

    @Override
    public String initIccid() {
        String iccId = telephonyManager.getSimSerialNumber();
            SubscriptionInfo info = subscriptionManager.getActiveSubscriptionInfoForSimSlotIndex(0);
            if (info != null) {
                // 在插入卡片的时候多数情况会不对
                iccId = info.getIccId();
            }
        if (!TextUtils.isEmpty(iccId) && !TextUtils.equals(iccId, SharePrefSetting.getCurrentIccId())) {
            SharePrefSetting.putCurrentIccId(iccId);
        }
        return iccId;
    }

    @Override
    public boolean isSimReady(int slotId) {
        int simState = 0;
        try {
            Method method = TelephonyManager.class.getDeclaredMethod("getSimState", Integer.TYPE);
            simState = (int) method.invoke(this.telephonyManager, slotId);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return simState == 5;
    }

    @Override
    public int getDefaultDataSubId() {
        int subId = -1;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            subId = SubscriptionManager.getDefaultDataSubscriptionId();
            Log.e(TAG, String.format("getDefaultDataSubId() returns %d", subId));
            return subId;
        }
        try {
            Method method = SubscriptionManager.class.getDeclaredMethod("getDefaultDataSubId");
            subId = (int) method.invoke(this.subscriptionManager);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return subId;
    }

    @Override
    public void setDefaultDataSubId(int subId) {
        if (subId < 0) {
            return;
        }
        try {
            Method method = SubscriptionManager.class
                    .getDeclaredMethod("setDefaultDataSubId", Integer.TYPE);
            method.invoke(this.subscriptionManager, subId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setDisplayName(int subId, String name) {
        try {
            @SuppressLint("SoonBlockedPrivateApi") Method method = SubscriptionManager.class
                    .getDeclaredMethod("setDisplayName", String.class, Integer.TYPE);
            method.invoke(this.subscriptionManager, name, subId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getModel() {
        return Build.BRAND;
    }

    @Override
    public String getNetworkType() {
        String netType = "UNKNOWN";
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                netType = "WIFI";
            } else if (networkInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
                int subtype = networkInfo.getSubtype();
                switch (subtype) {
                    case NETWORK_TYPE_GSM:
                    case NETWORK_TYPE_GPRS:
                    case NETWORK_TYPE_CDMA:
                    case NETWORK_TYPE_EDGE:
                    case NETWORK_TYPE_1xRTT:
                    case NETWORK_TYPE_IDEN:
                        netType = "2G";
                        break;
                    case NETWORK_TYPE_TD_SCDMA:
                    case NETWORK_TYPE_EVDO_A:
                    case TelephonyManager.NETWORK_TYPE_UMTS:
                    case TelephonyManager.NETWORK_TYPE_EVDO_0:
                    case TelephonyManager.NETWORK_TYPE_HSDPA:
                    case TelephonyManager.NETWORK_TYPE_HSUPA:
                    case TelephonyManager.NETWORK_TYPE_HSPA:
                    case TelephonyManager.NETWORK_TYPE_EVDO_B:
                    case TelephonyManager.NETWORK_TYPE_EHRPD:
                    case TelephonyManager.NETWORK_TYPE_HSPAP:
                        netType = "3G";
                        break;
                    case NETWORK_TYPE_IWLAN:
                    case NETWORK_TYPE_LTE:
                        netType = "4G";
                        break;
                    default:
                        String subtypeName = networkInfo.getSubtypeName();
                        if (subtypeName.equalsIgnoreCase("TD-SCDMA")
                                || subtypeName.equalsIgnoreCase("WCDMA")
                                || subtypeName.equalsIgnoreCase("CDMA2000")) {
                            netType = "3G";
                        }
                        break;
                }

            }
        }
        return netType;
    }

    @Override
    public boolean configureApn(String apn, String mccMnc) {
        int subId = getSubId(0);
        if (subId < 0) {
            return false;
        }
        if (TextUtils.isEmpty(mccMnc) || TextUtils.isEmpty(apn)) {
            return false;
        }
        int apnId = lookupApn(apn, mccMnc);
        if (apnId < 0) {
            apnId = insertApn(apn, mccMnc);
        }

        return apnId >= 0 && setPreferredApn(subId, apnId);
    }

    @Override
    public boolean clearApn() {
        int delete = resolver.delete(APN_URI, "name = ?", new String[]{APN_NAME});
        return delete >= 0;
    }

    private int lookupApn(String apn, String mccMnc) {
        Cursor cursor = resolver.query(APN_URI, null, "name = ? AND apn = ? AND numeric = ?",
                new String[]{APN_NAME, apn, mccMnc}, null);
        if (cursor == null) {
            return -1;
        } else {
            int id = -1;
            if (cursor.moveToFirst()) {
                id = cursor.getShort(cursor.getColumnIndex("_id"));
            }
            cursor.close();
            return id;
        }
    }

    private int insertApn(String apn, String mccMnc) {
        ContentValues values = new ContentValues();
        values.put("name", APN_NAME);
        values.put("apn", apn);
        values.put("numeric", mccMnc);
        values.put("mcc", mccMnc.substring(0, 3));
        values.put("mnc", mccMnc.substring(3));
        values.put("type", "default");
        Uri insert = resolver.insert(APN_URI, values);

        if (insert == null) {
            return -1;
        } else {
            Cursor cursor = resolver.query(insert, null, null, null, null);
            int id = -1;
            if (cursor.moveToFirst()) {
                id = cursor.getShort(cursor.getColumnIndex("_id"));
            }
            cursor.close();
            return id;
        }
    }

    private boolean setPreferredApn(int subId, int apnId) {
        ContentValues values = new ContentValues();
        values.put("apn_id", apnId);
        Uri uri = this.isMultiSim ? Uri.withAppendedPath(PREFERRED_APN_URI, "subId/" + subId)
                : PREFERRED_APN_URI;
        int update = resolver.update(uri, values, null, null);
        return update > 0;
    }
}
