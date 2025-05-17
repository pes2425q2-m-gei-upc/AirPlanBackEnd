package org.example.database

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.javatime.timestamp

object ReportTable : Table("reports") {
    val id = integer("id").autoIncrement()
    val reportedUsername = varchar("usuari_reportat", 100).references(
        UsuarioTable.username,
        onDelete = ReferenceOption.CASCADE,
        onUpdate = ReferenceOption.CASCADE
    )
    val reporterUsername = varchar("usuari_reportador", 100).references(
        UsuarioTable.username,
        onDelete = ReferenceOption.CASCADE,
        onUpdate = ReferenceOption.CASCADE
    )
    val reason = text("motiu")
    val timestamp = timestamp("data_hora").defaultExpression(CurrentTimestamp())

    override val primaryKey = PrimaryKey(id, name = "PK_Report")

    /*init {
        uniqueIndex("uq_parella_report", usuariReportador, usuariReportat)
        check("chk_diferents_usuaris") { usuariReportador neq usuariReportat }
    }*/
}