package com.example.leaguepro

import com.google.firebase.auth.FirebaseUser
import com.google.firebase.ktx.Firebase
import java.time.LocalDate
import java.util.Date

class League {

    var name: String? = null
    var address: String? = null
    var level: Number? = null
    var description: String? = null
    var entryfee: String? = null
    var prize: String? = null
    var restrictions: String? = null
    var playingPeriod: String?=null
    var leagueManager: String? = null


    constructor(name: String?, address: String?, level: Number?,description: String?,entryfee: String?,prize: String?,restrictions: String?,playingPeriod: String?,leagueManager: String?) {
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
        null, null, 0, null, null, null, null, null, null
    )

    // Metodo copy per consentire la copia immutabile dell'oggetto
    fun copy(
        leagueName: String? = this.name,
        leagueAddress: String? = this.address,
        leagueLevel: Number? = this.level,
        leagueDescription: String? = this.description,
        leagueEntryFee: String? = this.entryfee,
        leaguePrize: String? = this.prize,
        leagueRestrictions: String? = this.restrictions,
        leaguePlayingPeriod: String? = this.playingPeriod,
        uid: String? = this.leagueManager
    ): League {
        return League(
            leagueName,
            leagueAddress,
            leagueLevel,
            leagueDescription,
            leagueEntryFee,
            leaguePrize,
            leagueRestrictions,
            leaguePlayingPeriod,
            uid
        )
    }

}