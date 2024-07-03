package com.example.leaguepro

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DatabaseReference

// LeagueAdapter è collegato al layout league_item
class LeagueAdapter(
    val context: Context,
    val leagueList: ArrayList<League>,
    val dbRef: DatabaseReference
) : RecyclerView.Adapter<LeagueAdapter.LeagueViewHolder>() {

    class LeagueViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textName: TextView = itemView.findViewById(R.id.league_name)
        val textPrize: TextView = itemView.findViewById(R.id.league_prize)
        val floatRating: RatingBar = itemView.findViewById(R.id.league_level)
        val binButton: ImageView = itemView.findViewById(R.id.bin)
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
        holder.floatRating.rating = currentLeague.level!!

        holder.binButton.setOnClickListener {
            showConfirmationDialog(currentLeague, position)
        }
    }

    private fun showConfirmationDialog(league: League, position: Int) {
        val builder = AlertDialog.Builder(context, R.style.CustomAlertDialog)
        builder.setTitle("Delete confirm")
        builder.setMessage("Are you sure to delete ${league.name} and all info connected?")

        builder.setPositiveButton("Delete") { dialog, _ ->
            removeLeagueFromDatabase(league, position)
            dialog.dismiss()
        }

        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }

        builder.create().show()
    }

    private fun removeLeagueFromDatabase(league: League, position: Int) {
        // Usa leagueId per trovare e rimuovere la league dal database
        dbRef.child("leagues").child(league.uid!!).removeValue().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // Rimuovi l'elemento dalla lista locale
                leagueList.remove(league)
                // Notifica l'adapter della rimozione
                 notifyDataSetChanged()
                // Notifica l'adapter del cambiamento della dimensione della lista
                // notifyItemRangeChanged(position, leagueList.size)
                Toast.makeText(context, "League deleted!", Toast.LENGTH_SHORT).show()
                // Opzionalmente, naviga al MyLeagueFragment o esegui altre azioni
            } else {
                Toast.makeText(context, "Error in league deletion", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
