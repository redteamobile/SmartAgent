package com.redteamobile.smart;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.redteamobile.smart.agent.AgentService;
import com.redteamobile.smart.recycler.ProfileAdapter;
import com.redteamobile.smart.recycler.ProfileModel;
import com.redteamobile.smart.util.SharePrefSetting;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity implements View.OnClickListener {

    private static final String TAG = "MainActivity";
    private RecyclerView profileRecycler;
    private TextView enableNextTv;
    private TextView eidTv;
    private TextView imeiTv;
    private TextView appVersionTv;
    private TextView currentIccidTv;
    private TextView euiccMode;
    private TextView vuiccMode;
    private TextView enableProTv;
    private TextView finishTv;

    private AgentService agentService;
    private final byte[] eId = new byte[32];
    private final int[] eIdLength = new int[1];
    private List<ProfileModel> profileList = new ArrayList();
    private final int PERMISSON_REQUESTCODE = 110;
    private String[] needPermissions = {Manifest.permission.READ_PHONE_STATE};
    private int currentIccidIndex = -1;
    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Constant.ACTION_NOTIFY_STATE.equals(intent.getAction())) {
                showData();
                parseProfiles();
            }
        }
    };

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            agentService = ((AgentService.MyBinder) service).getService();
            ProfileAdapter adapter = (ProfileAdapter) profileRecycler.getAdapter();
            if (adapter != null) {
                adapter.setAgentService(agentService);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkPermmission();
        profileRecycler = findViewById(R.id.profileRecycler);
        enableNextTv = findViewById(R.id.enable_next_operationalTv);
        eidTv = findViewById(R.id.eidTv);
        imeiTv = findViewById(R.id.imeiTv);
        appVersionTv = findViewById(R.id.appVersionTv);
        currentIccidTv = findViewById(R.id.currentIccid);
        euiccMode = findViewById(R.id.euiccMode);
        vuiccMode = findViewById(R.id.vuiccMode);
        enableProTv = findViewById(R.id.enable_provisioningTv);
        finishTv = findViewById(R.id.deleteAllTv);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        profileRecycler.setLayoutManager(linearLayoutManager);
        profileRecycler.setAdapter(new ProfileAdapter(profileList));

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constant.ACTION_NOTIFY_STATE);
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, intentFilter);

        enableNextTv.setOnClickListener(this);
        euiccMode.setOnClickListener(this);
        vuiccMode.setOnClickListener(this);
        enableProTv.setOnClickListener(this);
        finishTv.setOnClickListener(this);

        Intent intent = new Intent(this, AgentService.class);
        startService(intent);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(serviceConnection);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
    }

    private void checkPermmission() {
        ActivityCompat.requestPermissions(this,
                needPermissions, PERMISSON_REQUESTCODE);
    }

    private void updateModeUI() {
        euiccMode.setEnabled(agentService.getUiccMode() != Constant.EUICC_MODE);
        vuiccMode.setEnabled(agentService.getUiccMode() != Constant.VUICC_MODE);
    }

    private void showData() {
        if (agentService == null) {
            return;
        }
        updateModeUI();
        agentService.getEId(eId, eIdLength);
        String eid = new String(eId);
        if (TextUtils.isEmpty(eid)) {
            return;
        }
        eidTv.setText(eid);
        imeiTv.setText(agentService.getImei());
        appVersionTv.setText(BuildConfig.VERSION_NAME);
    }

    private byte[] profile = new byte[1024];
    private int[] profileLength = new int[1024];

    private void parseProfiles() {
        if (agentService == null) {
            Log.e(TAG, getString(R.string.agent_service_empty));
            return;
        }
        profileList.clear();
        agentService.getProfiles(profile, profileLength);
        byte[] jsonArray = new byte[this.profileLength[0]];
        System.arraycopy(this.profile, 0, jsonArray, 0, this.profileLength[0]);
        String json = new String(jsonArray, StandardCharsets.UTF_8);
        if (TextUtils.isEmpty(json)) {
            return;
        }
        try {
            JSONObject jsonObject = new JSONObject(json);
            JSONArray profiles = jsonObject.getJSONArray("profiles");
            for (int i = 0; i < profiles.length(); i++) {
                JSONObject jsonProfile = profiles.getJSONObject(i);
                profileList.add(new ProfileModel(jsonProfile.getString("iccid"), jsonProfile.getInt("type"),
                        jsonProfile.getInt("state")));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if (profileList.size() > 0) {
            profileRecycler.getAdapter().notifyDataSetChanged();
            boolean isContains = false;
            for (int i = 0; i < profileList.size(); i++) {
                ProfileModel model = (ProfileModel) profileList.get(i);
                if (TextUtils.equals(model.getIccid(), SharePrefSetting.getCurrentIccId())) {
                    isContains = true;
                    break;
                }
            }
            ArrayList list = new ArrayList<ProfileModel>();
            for (int i = 0; i < profileList.size(); i++) {
                ProfileModel model = (ProfileModel) profileList.get(i);
                if (model.getState() == 1) {
                    currentIccidIndex = i;
                    currentIccidTv.setText(model.getIccid());
                    if (!isContains) {
                        SharePrefSetting.putCurrentIccId(model.getIccid());
                    }
                }
                if (model.getType() == 2) {
                    list.add(model);
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View v) {
        if (profileList.size() <= 0) {
            showData();
            parseProfiles();
            return;
        }
        if (agentService == null) {
            return;
        }
        switch (v.getId()) {
            case R.id.euiccMode:
                // 需要插入EUICC卡
                agentService.setUiccMode(Constant.EUICC_MODE);
                updateModeUI();
                Toast.makeText(MainActivity.this, getString(R.string.reboot_work), Toast.LENGTH_SHORT).show();
                break;
            case R.id.vuiccMode:
                agentService.setUiccMode(Constant.VUICC_MODE);
                updateModeUI();
                Toast.makeText(MainActivity.this,  getString(R.string.reboot_work), Toast.LENGTH_SHORT).show();
                break;
            case R.id.enable_next_operationalTv:
                if (profileList.size() > 1) {
                    currentIccidIndex = (++currentIccidIndex >= profileList.size()) ? 1 : currentIccidIndex;
                }
                agentService.enableProfile(profileList.get(currentIccidIndex).getIccid());

                break;
            case R.id.enable_provisioningTv:
                for (int i = 0; i < profileList.size(); i++) {
                    ProfileModel profileModel = profileList.get(i);
                    if (profileModel.getType() == 1) {
                        if (agentService == null) {
                            return;
                        }
                        agentService.enableProfile(profileModel.getIccid());
                    }
                }
                break;
            case R.id.deleteAllTv:
                for (int i = 0; i < profileList.size(); i++) {
                    ProfileModel profileModel = profileList.get(i);
                    if (profileModel.getType() == 2) {
                        if (agentService == null) {
                            return;
                        }
                        Log.e(TAG, "onClick: " + profileModel.getIccid());
                        agentService.deleteProfile(profileModel.getIccid());
                    }
                }
                break;
        }
    }


}

