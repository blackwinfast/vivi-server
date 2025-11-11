package com.example.repositories

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {
    private var connectedUrl: String? = null
    private var dataSource: HikariDataSource? = null

    // If true, DatabaseFactory will verify required tables exist after running migrations.
    // Set this to true in staging/production via the composition root before connecting.
    var verifyOnConnect: Boolean = false

    /**
     * Connect to the given JDBC URL and create schema if not already created.
     * Uses HikariCP to keep a persistent connection pool so in-memory SQLite
     * databases (mode=memory) survive across transactions during tests.
     */
    fun connect(jdbcUrl: String, driver: String = "org.sqlite.JDBC") {
        if (connectedUrl == jdbcUrl && dataSource != null) return

        // close existing pool if different
        dataSource?.close()

        val config = HikariConfig().apply {
            this.jdbcUrl = jdbcUrl
            this.driverClassName = driver
            this.maximumPoolSize = 3
            this.isAutoCommit = false
            // keepalive to avoid immediate shutdown of in-memory DB
            this.connectionTimeout = 30000
        }

        dataSource = HikariDataSource(config)
        Database.connect(dataSource!!)

        transaction {
            SchemaUtils.create(AnniversariesTable, EmotionMemoriesTable)
        }

        // Run optional SQL migrations from resources/db/migration if present (idempotent)
        val stream = DatabaseFactory::class.java.classLoader.getResourceAsStream("db/migration/V1__init.sql")
        if (stream != null) {
            val sql = stream.bufferedReader().use { it.readText() }
            if (sql.isNotBlank()) {
                transaction {
                    // execute SQL (use exec to run multiple statements if present)
                    exec(sql)
                }
            }
        }

        // Optionally verify that required tables exist after migration. If not, fail fast so staging
        // or production deployments don't start with an incomplete schema.
        if (verifyOnConnect) {
            transaction {
                val required = listOf(AnniversariesTable.tableName, EmotionMemoriesTable.tableName)
                val missing = mutableListOf<String>()
                required.forEach { tbl ->
                    var found = false
                    // Use a proper exec per table to check presence
                    exec("SELECT name FROM sqlite_master WHERE type='table' AND name='${'$'}tbl'") { rs ->
                        if (rs.next()) found = true
                    }
                    if (!found) missing.add(tbl)
                }

                if (missing.isNotEmpty()) {
                    error("Missing tables after migration: ${missing.joinToString(", ")}. Migrations may have failed or database URL is incorrect: $jdbcUrl")
                }
            }
        }

        connectedUrl = jdbcUrl
    }
}
