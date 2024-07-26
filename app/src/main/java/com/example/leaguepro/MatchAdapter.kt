package com.example.leaguepro

import android.app.AlertDialog
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.leaguepro.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener

class MatchAdapter(
    private val matchList: List<Match>,
    private val leagueOwnerId: String,
    private val leagueId: String,
    private val mDbRef: DatabaseReference
) : RecyclerView.Adapter<MatchAdapter.MatchViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MatchViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.card_match_layout, parent, false)
        return MatchViewHolder(view)
    }

    override fun onBindViewHolder(holder: MatchViewHolder, position: Int) {
        val match = matchList[position]
        holder.tvStage.text = "League match" // Update as necessary
        holder.tvMatchTime.text = "${match.date} ${match.time}"
        holder.tvTeam1.text = match.team1?.name
        holder.tvTeam1Score.text = match.result1?.toString()
        holder.tvTeam2.text = match.team2?.name
        holder.tvTeam2Score.text = match.result2?.toString()
        holder.tvStatus.visibility = if (match.result1 != null && match.result2 != null) View.VISIBLE else View.GONE
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == leagueOwnerId) {
            holder.btnEdit.visibility = View.VISIBLE
            holder.btnEdit.setOnClickListener {
                showEditDialog(holder.itemView.context, match)
            }
        } else {
            holder.btnEdit.visibility = View.GONE
        }
    }
    private fun showEditDialog(context: Context, match: Match) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.edit_match, null)
        val etTeam1Score = dialogView.findViewById<EditText>(R.id.et_team1_score)
        val etTeam2Score = dialogView.findViewById<EditText>(R.id.et_team2_score)

        // Pre-fill current scores
        etTeam1Score.setText(match.result1?.toString())
        etTeam2Score.setText(match.result2?.toString())

        val database = FirebaseDatabase.getInstance()
        val matchesRef = database.getReference("matches").child(leagueId).child(match.id!!) // Navigate to the correct node

        AlertDialog.Builder(context)
            .setTitle("Edit Match Result")
            .setView(dialogView)
            .setPositiveButton("Save") { dialog, _ ->
                val team1Score = etTeam1Score.text.toString().toIntOrNull()
                val team2Score = etTeam2Score.text.toString().toIntOrNull()

                if (team1Score != null && team2Score != null) {
                    val matchUpdates = mapOf(
                        "result1" to team1Score,
                        "result2" to team2Score
                    )

                    matchesRef.updateChildren(matchUpdates)
                        .addOnSuccessListener {
                            Toast.makeText(context, "Results updated successfully!", Toast.LENGTH_SHORT).show()
                            // Update the local match object
                            match.result1 = team1Score
                            match.result2 = team2Score
                            notifyItemChanged(matchList.indexOf(match)) // Refresh item
                            updateLeagueTableAfterMatch(match,leagueId)
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, "Failed to update results.", Toast.LENGTH_SHORT).show()
                        }
                }

                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .create()
            .show()
    }

    private fun updateLeagueTableAfterMatch(match: Match, leagueId: String) {
        val team1Id = match.team1?.id ?: return
        val team2Id = match.team2?.id ?: return

        val matchResult1 = match.result1 ?: return
        val matchResult2 = match.result2 ?: return

        // Calcola i punti
        val points1 = when {
            matchResult1 == matchResult2 -> 1 // Pareggio
            matchResult1 > matchResult2 -> 3 // Vittoria per team1
            else -> 0 // Vittoria per team2
        }

        val points2 = when {
            matchResult1 == matchResult2 -> 1 // Pareggio
            matchResult1 < matchResult2 -> 3 // Vittoria per team2
            else -> 0 // Vittoria per team1
        }

        // Aggiorna nel database
        updateTeamStatsInLeague(team1Id, leagueId, points1, matchResult1, matchResult2)
        updateTeamStatsInLeague(team2Id, leagueId, points2, matchResult2, matchResult1)
    }

    private fun updateTeamStatsInLeague(teamId: String, leagueId: String, points: Int, goalsFor: Int, goalsAgainst: Int) {
        val teamRef = mDbRef.child("teams").child(teamId).child("tournaments").child(leagueId)

        teamRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(mutableData: MutableData): Transaction.Result {
                val currentStats = mutableData.getValue(TournamentStats::class.java) ?: TournamentStats()

                val updatedStats = currentStats.copy(
                    points = (currentStats.points ?: 0) + points,
                    wins = (currentStats.wins ?: 0) + if (points == 3) 1 else 0,
                    draws = (currentStats.draws ?: 0) + if (points == 1) 1 else 0,
                    losses = (currentStats.losses ?: 0) + if (points == 0) 1 else 0,
                    goalsFor = (currentStats.goalsFor ?: 0) + goalsFor,
                    goalsAgainst = (currentStats.goalsAgainst ?: 0) + goalsAgainst
                )

                mutableData.value = updatedStats
                return Transaction.success(mutableData)
            }

            override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                if (error != null) {
                    Log.e("DatabaseUpdate", "Failed to update team $teamId in league $leagueId: ${error.message}")
                } else {
                    Log.d("DatabaseUpdate", "Team $teamId in league $leagueId updated successfully")
                }
            }
        })
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
        val btnEdit: ImageView = itemView.findViewById(R.id.iv_edit)
    }
}
