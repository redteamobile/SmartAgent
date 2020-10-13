package com.redteamobile.smart;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.redteamobile.smart.agent.AgentService;
import com.redteamobile.smart.recycler.ProfileAdapter;
import com.redteamobile.smart.recycler.ProfileModel;
import com.redteamobile.smart.util.SharePrefSetting;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class MainActivity extends Activity implements View.OnClickListener {

    private static final String TAG = "MainActivity";
    private RecyclerView profileRecycler;
    private TextView enable_next_operationalTv;
    private TextView eidTv;
    private TextView imeiTv;
    private TextView appVersionTv;
    private TextView currentIccidTv;
    private TextView startServiceTv;
    private TextView stopServiceTv;
    private TextView enable_provisioningTv;
    private TextView qa_finishTv;

    private AgentService agentService;
    private byte[] eId = new byte[32];
    private int[] eIdLength = new int[1];
    private ArrayList<ProfileModel> profileList = new ArrayList();
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Constant.ACTION_NETWORK_STATE_CHANGED.equals(intent.getAction())) {
                int networkState = intent.getIntExtra(Constant.TAG_NETWORK_STATE, 0);
                if (networkState == Constant.DSI_STATE_CALL_CONNECTED) {
                    showData();
                    parseProfiles();
                }
            } else if (Constant.ACTION_SERVICE_CONNECTED.equals(intent.getAction())) {
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
            adapter.setAgentService(agentService);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        profileRecycler = findViewById(R.id.profileRecycler);
        enable_next_operationalTv = findViewById(R.id.enable_next_operationalTv);
        eidTv = findViewById(R.id.eidTv);
        imeiTv = findViewById(R.id.imeiTv);
        appVersionTv = findViewById(R.id.appVersionTv);
        currentIccidTv = findViewById(R.id.currentIccid);
        startServiceTv = findViewById(R.id.startService);
        stopServiceTv = findViewById(R.id.stopService);
        enable_provisioningTv = findViewById(R.id.enable_provisioningTv);
        qa_finishTv = findViewById(R.id.qa_finishTv);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        profileRecycler.setLayoutManager(linearLayoutManager);
        profileRecycler.setAdapter(new ProfileAdapter(profileList));

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constant.ACTION_NETWORK_STATE_CHANGED);
        intentFilter.addAction(Constant.ACTION_SERVICE_CONNECTED);
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, intentFilter);

        enable_next_operationalTv.setOnClickListener(this);
        startServiceTv.setOnClickListener(this);
        stopServiceTv.setOnClickListener(this);
        enable_provisioningTv.setOnClickListener(this);
        qa_finishTv.setOnClickListener(this);

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

    private void showData() {
        if (agentService == null) {
            return;
        }
        agentService.getEId(eId, eIdLength);
        String eid = new String(eId);
        if (eid == null || TextUtils.isEmpty(eid)) {
            return;
        }
        eidTv.setText(eid);
        imeiTv.setText(agentService.getImei());
        appVersionTv.setText(BuildConfig.VERSION_NAME);
    }

    private byte[] profile = new byte[1024];
    private int[] profileLength = new int[1024];

    private void parseProfiles() {
        Log.d(TAG, "parseProfiles");
        if (agentService == null) {
            Log.e(TAG, "parseProfiles agentService is null");
            return;
        }
        profileList.clear();
        Log.d(TAG, "parseProfiles before getProfiles");
        agentService.getProfiles(profile, profileLength);
        Log.d(TAG, "parseProfiles before jsonArray");
        byte[] jsonArray = new byte[this.profileLength[0]];
        Log.d(TAG, "parseProfiles before arraycopy");
        System.arraycopy(this.profile, 0, jsonArray, 0, this.profileLength[0]);
        String json = new String(jsonArray, StandardCharsets.UTF_8);
        Log.d(TAG, "parseProfiles json : " + json);
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
    public void onClick(View v) {
        Log.e(TAG, "onClick profileList size :" + profileList.size());
        if (profileList.size() <= 0) {
            showData();
            parseProfiles();
            return;
        }
        switch (v.getId()) {
            case R.id.startService:
            case R.id.enable_next_operationalTv:
                for (int i = 0; i < profileList.size(); i++) {
                    ProfileModel profileModel = profileList.get(i);
                    if (profileModel.getType() == 2) {
                        if (profileModel.getState() == 0) {
                            if (agentService == null) {
                                return;
                            }
                            agentService.enableProfile(profileModel.getIccid());
                        }
                    }
                }
                break;
            case R.id.stopService:
                for (int i = 0; i < profileList.size(); i++) {
                    ProfileModel profileModel = profileList.get(i);
                    if (profileModel.getType() == 2) {
                        if (profileModel.getState() == 1) {
                            if (agentService == null) {
                                return;
                            }
                            agentService.disableProfile(profileModel.getIccid());
                        }
                    }
                }
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
            case R.id.qa_finishTv:
                for (int i = 0; i < profileList.size(); i++) {
                    ProfileModel profileModel = profileList.get(i);
                    if (profileModel.getType() == 2) {
                        if (agentService == null) {
                            return;
                        }
                        agentService.deleteProfile(profileModel.getIccid());
                    }
                }
                break;
        }
    }
}

