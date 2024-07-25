package com.example.leaguepro

class Match() {

    var id: String? = null
    var team1: Team? = null
    var team2: Team? = null
    var date: String? = null
    var time: String? = null
    var result1: Int? = null
    var result2: Int? = null

    constructor(id: String?, team1: Team?, team2: Team?, date: String?, time: String?, result1: Int?, result2: Int?) : this() {
        this.id = id
        this.team1 = team1
        this.team2 = team2
        this.date = date
        this.time = time
        this.result1 = result1
        this.result2 = result2
    }
}
