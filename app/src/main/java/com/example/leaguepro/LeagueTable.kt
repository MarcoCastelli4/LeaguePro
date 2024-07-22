package com.example.leaguepro

data class LeagueTable(
    val id: String? = null,              // Nullable ID della lega
    val name: String? = null,            // Nullable Nome della lega
    val teams: MutableList<Team> = mutableListOf() // Lista di squadre inizialmente vuota
) {

    // Metodi della classe
    fun addOrUpdateTeam(team: Team) {
        teams.removeAll { it.name == team.name }
        teams.add(team)
        sortTeams()
    }

    fun removeTeamByName(teamName: String) {
        teams.removeAll { it.name == teamName }
    }

    fun getSortedTeams(): List<Team> {
        return teams.sortedWith(
            compareByDescending<Team> { it.points }
                .thenByDescending { it.wins }
                .thenBy { it.goalsFor }
        )
    }

    private fun sortTeams() {
        teams.sortWith(
            compareByDescending<Team> { it.points }
                .thenByDescending { it.wins }
                .thenBy { it.goalsFor }
        )
    }

    fun toDisplayString(): String {
        return getSortedTeams().joinToString(separator = "\n") { it.toString() }
    }
}
