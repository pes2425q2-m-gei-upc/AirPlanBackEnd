package org.example.database

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

object MissatgesTable : Table("missatges") {
    val usernameSender = varchar("username_sender", 100)
    val usernameReceiver = varchar("username_reciever", 100)
    val dataEnviament = datetime("data_enviament")
    val missatge = text("missatge")

    override val primaryKey = PrimaryKey(usernameSender, usernameReceiver, dataEnviament)
}