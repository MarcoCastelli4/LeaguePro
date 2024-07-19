package com.example.leaguepro

class Match {
    var team1: Team?=null
    var team2: Team?=null
    var date: String? = null
    var time: String? = null
    var result: Pair<Int, Int>? = null

    constructor(
        team1: Team?,
        team2: Team?,
        date: String?,
        time: String?,
        result: Pair<Int, Int>?
    ) {
        this.team1 = team1
        this.team2 = team2
        this.date = date
        this.time = time
        this.result = result

    }

    override fun toString(): String {
        return "Match(team1=${team1!!.name}, team2=${team2!!.name}, date=$date, time=$time, resultTeam1=${result!!.first}, resultTeam2=${result!!.second})"
    }
}
