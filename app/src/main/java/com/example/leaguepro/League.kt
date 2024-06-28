package com.example.leaguepro

import java.time.LocalDate
import java.util.Date

class League {

    var name: String? = null
    var place: String? = null
    var level: String? = null
    var description: String? = null
    var entry: String? = null
    var prize: String? = null
    var restrictions: String? = null
    var startDate: LocalDate?=null
    var endDate: LocalDate?=null
    var lastDate: LocalDate?=null
    var leagueManager: String? = null


    constructor(name: String?, place: String?, level: String?,description: String?,entry: String?,prize: String?,restrictions: String?,startDate: LocalDate?, endDate: LocalDate?,lastDate: LocalDate?,leagueManager: String?) {
        this.name = name
        this.place=place
        this.level=level
        this.description=description
        this.entry=entry
        this.restrictions=restrictions
        this.startDate=startDate
        this.endDate=endDate
        this.lastDate=lastDate
        this.leagueManager=leagueManager
        this.prize=prize
    }

}