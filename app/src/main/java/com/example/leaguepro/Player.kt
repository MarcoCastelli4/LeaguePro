package com.example.leaguepro

class Player {
    var name: String?=null
    var role: String? = null
    var birthday: String? = null
    var uid: String? = null


    constructor() {}

    constructor(name: String?, role: String?,birthday: String?, uid: String?) {
        this.name = name
        this.role = role
        this.birthday = birthday
        this.uid = uid
    }
}
