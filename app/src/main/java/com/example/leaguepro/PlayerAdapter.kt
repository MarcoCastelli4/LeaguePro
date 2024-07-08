package com.example.leaguepro

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference

class PlayerAdapter(
    val context: Context,
    val playerList: ArrayList<Player>,
    val dbRef: DatabaseReference,
    val mAuth: FirebaseAuth
): RecyclerView.Adapter<PlayerAdapter.PlayerViewHolder>() {

    class PlayerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val player_name: TextView = itemView.findViewById(R.id.player_name)
        val player_role: TextView = itemView.findViewById(R.id.player_role)
        val player_birthday: TextView = itemView.findViewById(R.id.player_birthday)
        //val team_name: TextView = itemView.findViewById(R.id.team_name)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayerViewHolder {
        val view: View = LayoutInflater.from(context).inflate(R.layout.card_player_layout, parent, false)
        return PlayerAdapter.PlayerViewHolder(view)
    }

    override fun getItemCount(): Int {
        return playerList.size
    }

    override fun onBindViewHolder(holder: PlayerViewHolder, position: Int) {
        val currentPlayer = playerList[position]
        holder.player_name.text = currentPlayer.name
        holder.player_role.text = currentPlayer.role
        holder.player_birthday.text = currentPlayer.birthday
    }

}