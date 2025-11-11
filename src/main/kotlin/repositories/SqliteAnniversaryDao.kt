package com.example.repositories

import com.example.services.Anniversary
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

object AnniversariesTable : Table("anniversaries") {
    val id = varchar("id", 200)
    val userId = varchar("user_id", 200)
    val title = varchar("title", 500)
    val date = varchar("date", 32)
    val createdBy = varchar("created_by", 16)
    val description = varchar("description", 1000).nullable()
    val recurring = bool("recurring")
    // primaryKey intentionally omitted to avoid depending on the PrimaryKey helper symbol.
    // Exposed will still work with the single-column id; explicit primaryKey can be
    // added later if desired.
}

class SqliteAnniversaryDao(private val jdbcUrl: String = "jdbc:sqlite:data/dev.db") : AnniversaryDao {
    init {
        DatabaseFactory.connect(jdbcUrl)
    }

    override fun save(anniversary: Anniversary) {
        transaction {
            try {
                AnniversariesTable.insert {
                    it[id] = anniversary.id
                    it[userId] = anniversary.userId
                    it[title] = anniversary.title
                    it[date] = anniversary.date.toString()
                    it[createdBy] = anniversary.createdBy.name
                    it[description] = anniversary.description
                    it[recurring] = anniversary.recurring
                }
            } catch (e: ExposedSQLException) {
                println("insert failed: ${'$'}{e::class} ${'$'}{e.message}")
                throw e
            }
        }
    }

    override fun listByUser(userIdStr: String): List<Anniversary> {
        return transaction {
            AnniversariesTable.selectAll()
                .asSequence()
                // sort in-memory to avoid depending on query.orderBy overloads
                .filter { row -> row.get(AnniversariesTable.userId) == userIdStr }
                .sortedByDescending { row -> row.get(AnniversariesTable.id) }
                .map { row ->
                    Anniversary(
                        id = row.get(AnniversariesTable.id),
                        userId = row.get(AnniversariesTable.userId),
                        title = row.get(AnniversariesTable.title),
                        date = java.time.LocalDate.parse(row.get(AnniversariesTable.date)),
                        createdBy = com.example.services.CreatorType.valueOf(row.get(AnniversariesTable.createdBy)),
                        description = row.get(AnniversariesTable.description),
                        recurring = row.get(AnniversariesTable.recurring),
                    )
                }.toList()
        }
    }

    override fun findById(idStr: String): Anniversary? {
        return transaction {
            val maybe = AnniversariesTable.selectAll()
                .asSequence()
                .filter { row -> row.get(AnniversariesTable.id) == idStr }
                .map { row ->
                    Anniversary(
                        id = row.get(AnniversariesTable.id),
                        userId = row.get(AnniversariesTable.userId),
                        title = row.get(AnniversariesTable.title),
                        date = java.time.LocalDate.parse(row.get(AnniversariesTable.date)),
                        createdBy = com.example.services.CreatorType.valueOf(row.get(AnniversariesTable.createdBy)),
                        description = row.get(AnniversariesTable.description),
                        recurring = row.get(AnniversariesTable.recurring),
                    )
                }.firstOrNull()
            maybe
        }
    }

    override fun update(anniversary: Anniversary): Boolean {
        return transaction {
            val updated = AnniversariesTable.update({ AnniversariesTable.id eq anniversary.id }) { stmt ->
                stmt[AnniversariesTable.title] = anniversary.title
                stmt[AnniversariesTable.date] = anniversary.date.toString()
                stmt[AnniversariesTable.createdBy] = anniversary.createdBy.name
                stmt[AnniversariesTable.description] = anniversary.description
                stmt[AnniversariesTable.recurring] = anniversary.recurring
            }
            updated > 0
        }
    }

    override fun delete(id: String): Boolean {
        return transaction {
            val removed = AnniversariesTable.deleteWhere { AnniversariesTable.id eq id }
            removed > 0
        }
    }
}
