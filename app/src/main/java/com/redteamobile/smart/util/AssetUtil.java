package com.redteamobile.smart.util;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.common.hash.Hashing;
import com.google.common.hash.HashingInputStream;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import com.redteamobile.smart.Constant;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class AssetUtil {

    private static final String TAG = "AssetUtil";

    private AssetManager assetManager;
    private Context context;

    public AssetUtil(Context context) {
        assetManager = context.getAssets();
        context = context;
    }

    public void copyFileInThread(
            final String assetFileName, final String fileName, final String dir) {
        Log.i(TAG, "copyFileOnThread() ");
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    copyFile(assetFileName, fileName, dir);
                    if (!verifyPlmnMd5(assetFileName, fileName, dir)) {
                        Log.i(TAG, "Md5 misMatch");
                        copyFileInThread(assetFileName, fileName, dir);
                    } else {
                        Log.i(TAG, "Md5 is match! ");
                        LocalBroadcastManager.getInstance(context)
                                .sendBroadcast(new Intent(Constant.ACTION_BOOTSTRAP_READY));
                    }
                } catch (IOException e) {
                    Log.e(TAG, "copyFile() ", e);
                }
            }
        }).start();
    }

    public void copyFile(String assetFileName, String fileName, String dir) throws IOException {
        Log.i(TAG, String.format("copyFile(fileName: %s) start", fileName));
        String path = dir + fileName;
        InputStream is = null;
        OutputStream os = null;
        try {
            File dbFile = new File(path);
            if (dbFile.exists()) {
                dbFile.delete();
            }

            File f = new File(dir);
            if (!f.exists()) {
                f.mkdir();
            }
            is = assetManager.open(assetFileName);
            os = new FileOutputStream(path);
            byte[] buffer = new byte[1024];
            int length = 0;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
        } finally {
            if (os != null) {
                os.flush();
                os.close();
            }
            if (is != null) {
                is.close();
            }
        }
        Log.i(TAG, String.format("copyFile(fileName: %s) end", fileName));
    }

    public boolean verifyPlmnMd5(String assetFileName, String fileName, String dir) {
        try {
            String path = dir + fileName;
            InputStream is = assetManager.open(assetFileName);
            HashingInputStream his = new HashingInputStream(Hashing.md5(), is);
            ByteStreams.copy(his, ByteStreams.nullOutputStream());
            String sourceMd5 = his.hash().toString();
            return sourceMd5.equals(Files.hash(new File(path), Hashing.md5()).toString());
        } catch (IOException e) {
            Log.e(TAG, "copyFile() ", e);
            return false;
        }
    }

}
