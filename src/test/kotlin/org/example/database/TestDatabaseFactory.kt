package org.example.database

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.transactions.TransactionManager
import java.sql.Connection
import java.util.concurrent.atomic.AtomicInteger

/**
 * Database factory for tests that creates an isolated H2 in-memory database.
 */
object TestDatabaseFactory {
    private val counter = AtomicInteger()
    
    /**
     * Initialize a test database with a unique name to prevent conflicts.
     * Returns the database instance.
     */
    fun init(): Database {
        // Create a unique database name
        val databaseId = counter.incrementAndGet()
        val databaseName = "test_db_$databaseId"
        
        // Configure H2 with PostgreSQL compatibility
        val jdbcURL = "jdbc:h2:mem:$databaseName;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE"
        val database = Database.connect(
            url = jdbcURL,
            driver = "org.h2.Driver",
            user = "sa",
            password = ""
        )
        
        // Set up the schema
        transaction(database) {
            addLogger(StdOutSqlLogger)
            
            // Drop tables if they exist
            SchemaUtils.drop(UsuarioTable)
            
            // Create tables with the same structure as in production
            SchemaUtils.create(UsuarioTable)
            
            // Set the transaction isolation level to match production settings
            TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_READ_COMMITTED
        }
        
        return database
    }
}