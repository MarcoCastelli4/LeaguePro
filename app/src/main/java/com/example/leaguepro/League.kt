package com.example.leaguepro

import com.google.firebase.auth.FirebaseUser
import com.google.firebase.ktx.Firebase
import java.time.LocalDate
import java.util.Date

class League {

    var uid: String?=null
    var name: String? = null
    var address: String? = null
    var level: Float? = null
    var description: String? = null
    var entryfee: String? = null
    var prize: String? = null
    var restrictions: String? = null
    var playingPeriod: String?=null
    var leagueManager: String? = null


    constructor(uid: String?,name: String?, address: String?, level: Float?,description: String?,entryfee: String?,prize: String?,restrictions: String?,playingPeriod: String?,leagueManager: String?) {
        this.uid=uid
        this.name = name
        this.address=address
        this.level=level
        this.description=description
        this.entryfee=entryfee
        this.restrictions=restrictions
        this.playingPeriod=playingPeriod
        this.leagueManager=leagueManager
        this.prize=prize
    }

    // Costruttore vuoto richiesto da Firebase
    constructor() : this(
        null,null, null, 0.0f, null, null, null, null, null, null
    )



}