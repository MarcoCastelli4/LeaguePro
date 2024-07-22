package com.example.leaguepro

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

data class Team(
    val id: String? = null,                // Un identificatore unico per la squadra
    val name: String? = null,              // Nome della squadra
    val team_manager: String? = null,
    val players: ArrayList<Player>? = null,
    val points: Int? = 0,                  // Punti totali
    val wins: Int? = 0,                    // Vittorie
    val draws: Int? = 0,                   // Pareggi
    val losses: Int? = 0,                  // Sconfitte
    val goalsFor: Int? = 0,                // Goal fatti
    val goalsAgainst: Int? = 0,            // Goal subiti
    val leagues: Map<String, Boolean>? = null // Leagues where the team is registered
) {
    fun getTeamNameById(teamId: String, callback: (String?) -> Unit) {
        val teamRef = FirebaseDatabase.getInstance().reference.child("teams").child(teamId)

        teamRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val team = snapshot.getValue(Team::class.java)
                callback(team?.name)
            }
            override fun onCancelled(error: DatabaseError) {
                callback(null)
            }
        })
    }


    override fun toString(): String {
        return "$name - Punti: $points, Vittorie: $wins, Pareggi: $draws, Sconfitte: $losses, Goal Fatti: $goalsFor, Goal Subiti: $goalsAgainst"
    }
}
