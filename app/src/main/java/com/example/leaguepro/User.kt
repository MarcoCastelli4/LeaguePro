package com.example.leaguepro

class User {
    var userType: String?=null
    var name: String? = null
    var email: String? = null
    var uid: String? = null


    constructor() {}

    constructor(userType:String?, name: String?, email: String?, uid: String?) {
        this.name = name
        this.email = email
        this.uid = uid
        this.userType=userType
    }
}
