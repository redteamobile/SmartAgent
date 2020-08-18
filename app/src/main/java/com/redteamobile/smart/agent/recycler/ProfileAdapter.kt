package com.redteamobile.smart.agent.recycler

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.redteamobile.smart.R
import com.redteamobile.smart.agent.AgentService
import kotlinx.android.synthetic.main.item_profile.view.*

class ProfileAdapter(private var profileList: List<ProfileModel>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var agentService: AgentService? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return ProfileHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_profile, parent, false)
        )
    }

    override fun getItemCount(): Int = profileList.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ProfileHolder) {
            val any = profileList[position]
            if (any is ProfileModel) {
                holder.bind(any)
            }
        }
    }

    fun setAgentService(agentService: AgentService) {
        this.agentService = agentService
    }

    inner class ProfileHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bind(profileModel: ProfileModel) {
            with(itemView) {
                itemIccid.text = profileModel.iccid
                if (profileModel.state == 0) {
                    itemEnableBtn.text = "启用"
                } else {
                    itemEnableBtn.text = "禁用"
                }
                if (profileModel.type == 0) {

                }
                itemEnableBtn.setOnClickListener {
                    if (agentService != null) {
                        if (profileModel.state == 0) {
                            profileModel.state = 1
                            agentService!!.enableProfile(profileModel.iccid)
                        } else {
                            profileModel.state = 0
                            agentService!!.disableProfile(profileModel.iccid)
                        }
                        notifyDataSetChanged()
                    }
                }
            }
        }
    }

}
