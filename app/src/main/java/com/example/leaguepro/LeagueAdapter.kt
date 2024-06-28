package com.example.leaguepro

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView


// LeagueAdapter è collegato al layout league_item
class LeagueAdapter(val context: Context, val leagueList: ArrayList<League>): RecyclerView.Adapter<LeagueAdapter.LeagueViewHolder>() {

    class LeagueViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val textName: TextView = itemView.findViewById(R.id.league_name)
        val textPrize: TextView = itemView.findViewById(R.id.league_prize)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LeagueViewHolder {
        val view: View = LayoutInflater.from(context).inflate(R.layout.card_league_layout, parent, false)
        return LeagueViewHolder(view)
    }

    override fun getItemCount(): Int {
        return leagueList.size
    }

    override fun onBindViewHolder(holder: LeagueViewHolder, position: Int) {
        val currentLeague = leagueList[position]
        holder.textName.text = currentLeague.name
        holder.textPrize.text = currentLeague.prize

    }
}
