package com.example.leaguepro

import android.app.AlertDialog
import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.RecyclerView
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
        holder.tvStage.text = "League match"
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
    //mostra dialogo per modificare risultato match
    private fun showEditDialog(context: Context, match: Match) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.edit_match, null)
        val etTeam1Score = dialogView.findViewById<EditText>(R.id.et_team1_score)
        val etTeam2Score = dialogView.findViewById<EditText>(R.id.et_team2_score)
        val team1name = dialogView.findViewById<TextView>(R.id.tv_team1_name)
        val team2name = dialogView.findViewById<TextView>(R.id.tv_team2_name)
        val llTeam1Scorers = dialogView.findViewById<LinearLayout>(R.id.ll_team1_scorers)
        val llTeam2Scorers = dialogView.findViewById<LinearLayout>(R.id.ll_team2_scorers)
        val saveButton = dialogView.findViewById<Button>(R.id.save_button)
        val cancelButton = dialogView.findViewById<Button>(R.id.cancel_button)

        val database = FirebaseDatabase.getInstance()
        val matchesRef = database.getReference("matches").child(leagueId).child(match.id!!)
        // Pre-fill current teams and scores
        team1name.text = match.team1?.name
        team2name.text = match.team2?.name
        etTeam1Score.setText(match.result1?.toString())
        etTeam2Score.setText(match.result2?.toString())

        // Function to create a row for a scorer
        fun createScorerRow(scorerList: MutableList<String>, llScorers: LinearLayout, team: String) {
            llScorers.removeAllViews()
            for (i in scorerList.indices) {
                val scorer = scorerList[i]
                val row = LayoutInflater.from(context).inflate(R.layout.scorer_row, llScorers, false)
                val scorerTextView = row.findViewById<TextView>(R.id.scorer_text)
                scorerTextView.text = scorer
                scorerTextView.setOnClickListener {
                    showScorerSelectionDialog(context, scorerList, i, team,llScorers)
                }
                llScorers.addView(row)
            }
        }

        // Update scorer rows when score changes
        fun updateScorerRows() {
            val team1Score = etTeam1Score.text.toString().toIntOrNull() ?: 0
            val team2Score = etTeam2Score.text.toString().toIntOrNull() ?: 0

            // Imposto tante righe quanti sono i goal della squadra
            val team1Scorers = mutableListOf<String>().apply {
                repeat(team1Score) { add("Select player") }
            }
            val team2Scorers = mutableListOf<String>().apply {
                repeat(team2Score) { add("Select player") }
            }

            createScorerRow(team1Scorers, llTeam1Scorers, match.team1!!.id!!)
            createScorerRow(team2Scorers, llTeam2Scorers, match.team2!!.id!!)
        }

        // Set up text change listeners to update scorer rows
        etTeam1Score.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateScorerRows()
            }
        })

        etTeam2Score.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateScorerRows()
            }
        })

        val dialog = AlertDialog.Builder(context)
            .setTitle("Edit Match Result")
            .setView(dialogView)
            .create()

        saveButton.setOnClickListener {
            val team1Score = etTeam1Score.text.toString().toIntOrNull()
            val team2Score = etTeam2Score.text.toString().toIntOrNull()

            if (team1Score != null && team2Score != null) {
                // Prima di aggiornare i risultati, rimuovi l'effetto dei risultati precedenti
                if (match.result1 != null && match.result2 != null) {
                    updateLeagueTableAfterMatch(match, leagueId, isReverting = true)
                }

                val matchUpdates = mapOf(
                    "result1" to team1Score,
                    "result2" to team2Score
                )

                matchesRef.updateChildren(matchUpdates)
                    .addOnSuccessListener {
                        Toast.makeText(
                            context,
                            "Results updated successfully!",
                            Toast.LENGTH_SHORT
                        ).show()
                        // Update the local match object
                        match.result1 = team1Score
                        match.result2 = team2Score
                        notifyItemChanged(matchList.indexOf(match)) // Refresh item
                        updateLeagueTableAfterMatch(match, leagueId, false)
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "Failed to update results.", Toast.LENGTH_SHORT)
                            .show()
                    }
            }

            dialog.dismiss()
        }

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    // Function to show a dialog for selecting or modifying a scorer
    private fun showScorerSelectionDialog(context: Context, scorerList: MutableList<String>, position: Int, teamId: String,llScorers:LinearLayout) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.select_scorer, null)
        val spinner = dialogView.findViewById<Spinner>(R.id.spinner_scorers)

        getPlayersForTeam(teamId) { players ->
            if (players.isEmpty()) {
                Toast.makeText(context, "No players found.", Toast.LENGTH_SHORT).show()
            }
            val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, players)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner.adapter = adapter

            // Seleziona il giocatore attualmente scelto nella posizione
            val currentPlayer = scorerList.getOrNull(position)
            spinner.setSelection(adapter.getPosition(currentPlayer ?: ""))
        }

        val dialog = AlertDialog.Builder(context)
            .setTitle("Select Scorer")
            .setView(dialogView)
            .setPositiveButton("OK") { _, _ ->
                val selectedPlayer = spinner.selectedItem as? String ?: return@setPositiveButton
                scorerList[position] = selectedPlayer
                (llScorers.getChildAt(position) as? TextView)?.text = selectedPlayer
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
    }

    // get players of match's teams
    private fun getPlayersForTeam(teamId: String, callback: (List<String>) -> Unit) {
        val database = FirebaseDatabase.getInstance()
        val teamRef = database.getReference("teams").child(teamId).child("players")

        teamRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val playersList = mutableListOf<String>()

                for (playerSnapshot in snapshot.children) {
                    val playerName = playerSnapshot.child("name").getValue(String::class.java)
                    if (playerName != null) {
                        playersList.add(playerName)
                    }
                }
                callback(playersList) // Passa la lista di giocatori al callback
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Failed to read players", error.toException())
                callback(emptyList()) // Restituisci una lista vuota in caso di errore
            }
        })
    }

    private fun updateLeagueTableAfterMatch(match: Match, leagueId: String, isReverting: Boolean) {
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

        // Se stiamo aggiornando match già inserito, togli i punti invece di aggiungerli
        updateTeamStatsInLeague(team1Id, leagueId, if (isReverting) -points1 else points1, matchResult1, matchResult2, isReverting)
        updateTeamStatsInLeague(team2Id, leagueId, if (isReverting) -points2 else points2, matchResult2, matchResult1, isReverting)
    }

    private fun updateTeamStatsInLeague(teamId: String, leagueId: String, points: Int, goalsFor: Int, goalsAgainst: Int, isReverting: Boolean) {
        val teamRef = mDbRef.child("teams").child(teamId).child("tournaments").child(leagueId)

        teamRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(mutableData: MutableData): Transaction.Result {
                val currentStats = mutableData.getValue(TournamentStats::class.java) ?: TournamentStats()

                val updatedStats = currentStats.copy(
                    points = (currentStats.points ?: 0) + points,
                    wins = (currentStats.wins ?: 0) + if (points == 3 && !isReverting) 1 else if (points == -3 && isReverting) -1 else 0,
                    draws = (currentStats.draws ?: 0) + if (points == 1 && !isReverting) 1 else if (points == -1 && isReverting) -1 else 0,
                    losses = (currentStats.losses ?: 0) + if (points == 0 && !isReverting) 1 else if (points == 0 && isReverting) -1 else 0,
                    goalsFor = (currentStats.goalsFor ?: 0) + if (isReverting) -goalsFor else goalsFor,
                    goalsAgainst = (currentStats.goalsAgainst ?: 0) + if (isReverting) -goalsAgainst else goalsAgainst
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
