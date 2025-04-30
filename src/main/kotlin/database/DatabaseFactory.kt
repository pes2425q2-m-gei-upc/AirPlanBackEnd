package org.example.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.SchemaUtils
import java.util.concurrent.TimeUnit
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.TransactionManager
import java.sql.Connection

object DatabaseFactory {
    fun init() {
        // en cas de execucció local "jdbc:postgresql://nattech.fib.upc.edu:40351/midb"
        // en cas de execucció al servidor "jdbc:postgresql://172.16.4.35:8081/midb"
        val url = "jdbc:postgresql://nattech.fib.upc.edu:40351/midb"
        val user = "airplan"
        val password = "airplan1234"

        // Configuración del pool de conexiones con HikariCP
        val config = HikariConfig()
        config.driverClassName = "org.postgresql.Driver"
        config.jdbcUrl = url
        config.username = user
        config.password = password
        config.maximumPoolSize = 10
        config.minimumIdle = 2
        config.idleTimeout = TimeUnit.MINUTES.toMillis(10)
        config.maxLifetime = TimeUnit.MINUTES.toMillis(30)
        config.connectionTimeout = TimeUnit.SECONDS.toMillis(30)

        // IMPORTANTE: Eliminamos la configuración de autoCommit y connectionTestQuery
        // para evitar conflictos con Exposed

        // Propiedades PostgreSQL específicas para mayor estabilidad
        config.addDataSourceProperty("tcpKeepAlive", "true")
        config.addDataSourceProperty("socketTimeout", "30")
        config.addDataSourceProperty("connectTimeout", "10")
        config.addDataSourceProperty("loginTimeout", "10")

        val dataSource = HikariDataSource(config)
        val database = Database.connect(dataSource)

        // Configuramos el nivel de aislamiento de transacción de manera global
        // para evitar que cambie durante las transacciones
        TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_READ_COMMITTED

        // Verificar la conexión a la base de datos al inicio
        try {
            transaction {
                exec("SELECT 1") // Comprobar que la conexión funciona
                println("✅ Conexión a la base de datos establecida correctamente")

                // Crear tablas si no existen
                SchemaUtils.create(ActivitatTable) // Crea la tabla si no existe
                SchemaUtils.create(UsuarioTable) // Crea la tabla si no existe
                SchemaUtils.create(ValoracioTable) // Crea la tabla si no existe
                SchemaUtils.create(UserBlockTable) // Crea la tabla de bloqueos si no existe


                println("✅ Esquema de la base de datos verificado")
            }
        } catch (e: Exception) {
            println("❌ Error al conectar a la base de datos: ${e.message}")
            e.printStackTrace()
        }
    }
}