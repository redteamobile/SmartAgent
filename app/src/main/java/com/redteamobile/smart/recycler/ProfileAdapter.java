package com.redteamobile.smart.recycler;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.redteamobile.smart.R;
import com.redteamobile.smart.agent.AgentService;

import java.util.List;

public class ProfileAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private AgentService agentService;
    private List profileList;

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_profile, parent, false);
        return new ProfileHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof ProfileHolder) {
            ProfileModel model = (ProfileModel) profileList.get(position);
            ((ProfileHolder) holder).bind(model);
        }
    }

    @Override
    public int getItemCount() {
        return profileList.size();
    }

    public ProfileAdapter(List profileList) {
        super();
        this.profileList = profileList;
    }

    public void setAgentService(AgentService agentService) {
        this.agentService = agentService;
    }

    public final class ProfileHolder extends RecyclerView.ViewHolder {

        public void bind(final ProfileModel profileModel) {
            View view = this.itemView;
            boolean var3 = false;
            boolean var4 = false;
            TextView tv_vitemIccid = (TextView) view.findViewById(R.id.itemIccid);
            tv_vitemIccid.setText((CharSequence) profileModel.getIccid());
            TextView tv_itemEnableBtn = (TextView) view.findViewById(R.id.itemEnableBtn);
            if (profileModel.getState() == 0) {
                tv_itemEnableBtn.setText((CharSequence) "启用");
            } else {
                tv_itemEnableBtn.setText((CharSequence) "禁用");
            }

            if (profileModel.getType() == 0) {
            }

            tv_itemEnableBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (agentService != null) {
                        if (profileModel.getState() == 0) {
                            profileModel.setState(1);
                            agentService.enableProfile(profileModel.getIccid());
                        } else {
                            profileModel.setState(0);
                            agentService.disableProfile(profileModel.getIccid());
                        }
                        notifyDataSetChanged();
                    }
                }
            });
        }

        public ProfileHolder(View itemView) {
            super(itemView);
        }

    }
}