package com.appdist.infrastructure

import com.appdist.TestDatabase
import com.appdist.infrastructure.database.tables.UsersTable
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.test.Test
import kotlin.test.assertEquals

class DatabaseTest {
    @Test
    fun `tables created successfully`() {
        TestDatabase.init()
        transaction {
            val count = UsersTable.selectAll().count()
            assertEquals(0L, count)
        }
    }
}
