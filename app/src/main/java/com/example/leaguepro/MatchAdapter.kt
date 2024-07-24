package com.example.leaguepro

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.leaguepro.R

class MatchAdapter(private val matchList: List<Match>) : RecyclerView.Adapter<MatchAdapter.MatchViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MatchViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.card_match_layout, parent, false)
        return MatchViewHolder(view)
    }

    override fun onBindViewHolder(holder: MatchViewHolder, position: Int) {
        val match = matchList[position]
        holder.tvStage.text = "Group stage: Stage A" // Update as necessary
        holder.tvMatchTime.text = "${match.date} ${match.time}"
        holder.tvTeam1.text = match.team1?.name
        holder.tvTeam1Score.text = match.result?.first?.toString()
        holder.tvTeam2.text = match.team2?.name
        holder.tvTeam2Score.text = match.result?.second?.toString()
        holder.tvStatus.text = "FINE" // Update as necessary
        holder.ivEdit.setOnClickListener {
            // handle edit action
        }
    }

    override fun getItemCount(): Int = matchList.size

    class MatchViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvStage: TextView = itemView.findViewById(R.id.tv_stage)
        val tvMatchTime: TextView = itemView.findViewById(R.id.tv_match_time)
        val tvTeam1: TextView = itemView.findViewById(R.id.tv_team1)
        val tvTeam1Score: TextView = itemView.findViewById(R.id.tv_team1_score)
        val tvTeam2: TextView = itemView.findViewById(R.id.tv_team2)
        val tvTeam2Score: TextView = itemView.findViewById(R.id.tv_team2_score)
        val tvStatus: TextView = itemView.findViewById(R.id.tv_status)
        val ivEdit: ImageView = itemView.findViewById(R.id.iv_edit)
    }
}
