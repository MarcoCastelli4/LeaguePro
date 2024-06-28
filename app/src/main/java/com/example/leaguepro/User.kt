package com.example.leaguepro

class User {
    var userType: String?=null
    var fullname: String? = null
    var email: String? = null
    var uid: String? = null


    constructor() {}

    constructor(userType:String?, fullname: String?, email: String?, uid: String?) {
        this.fullname = fullname
        this.email = email
        this.uid = uid
        this.userType=userType
    }
}
