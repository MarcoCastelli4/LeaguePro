package com.example.leaguepro
data class Team(
    val id: String? = null,
    val name: String? = null,
    val team_manager: String? = null,
    val players: List<Player>? = listOf(),
    val tournaments: Map<String, TournamentStats>? = mapOf() // Modificato
)
data class TournamentStats (
    val points: Int? = 0,
    val wins: Int? = 0,
    val draws: Int? = 0,
    val losses: Int? = 0,
    val goalsFor: Int? = 0,
    val goalsAgainst: Int? = 0
)
