package com.redteamobile.smart.util;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class NetPingUtil extends AsyncTask<String, String, String> {
    private static final String TAG = NetPingUtil.class.getSimpleName();

    @Override
    protected String doInBackground(String... params) {
        Log.e(TAG, "doInBackground: " + params[0]);
        String s = Ping(params[0]);
        Log.i("ping", s);
        return s;
    }

    private String Ping(String str) {
        String resault = "";
        Process p;
        try {
//ping -c 3 -w 100  中  ，-c 是指ping的次数 3是指ping 3次 ，-w 100  以秒为单位指定超时间隔，是指超时时间为100秒
            p = Runtime.getRuntime().exec("ping -c 10 -w 10 " + str);
            int status = p.waitFor();
            InputStream input = p.getInputStream();
            BufferedReader in = new BufferedReader(new InputStreamReader(input));
            StringBuffer buffer = new StringBuffer();
            String line = "";
            while ((line = in.readLine()) != null) {
                buffer.append(line);
            }
            Log.i("TAG", "Ping: " + buffer.toString());
            if (status == 0) {
                resault = handleResult(buffer.toString());
            } else {
                resault = "0,10,0";
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return resault;
    }

    /**
     * min/avg/max/mdev=29.129/41.882/99.380/19.371ms
     *
     * @param returnMsg
     * @return
     */
    private String handleResult(String returnMsg) {
        final String receivedMark = "received,";
        final String lossMark = "%packetloss";
        final String mdevMark = "mdev=";
        final String splitMark = ",";
        String statisticsStr = returnMsg.substring(returnMsg.indexOf("statistics")).replace(" ", "");
        Log.e(TAG, "statisticsStr=:" + statisticsStr);
        String rtt = statisticsStr.substring(statisticsStr.indexOf(mdevMark) + mdevMark.length()).replace("ms", "");
        Log.e(TAG, "rtt=:" + rtt);
        String[] rttArr = rtt.split("/");
        String delay = rttArr[1];
        String mdev = rttArr[3];

        int receivedIndex = statisticsStr.indexOf(receivedMark);
        int packetIndex = statisticsStr.indexOf(lossMark);
        String loss = statisticsStr.substring(receivedIndex + receivedMark.length(), packetIndex);
        Log.e(TAG, "loss: " + loss);
        String resultStr = delay + splitMark + loss + splitMark + mdev;
        return resultStr;
    }

}
