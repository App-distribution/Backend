package com.appdist.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.appdist.core.database.dao.BuildDao
import com.appdist.core.database.entity.BuildEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BuildDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: BuildDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.buildDao()
    }

    @After
    fun tearDown() { db.close() }

    @Test
    fun `upsert and query builds for project`() = runTest {
        val build = buildEntity(id = "b1", projectId = "p1")
        dao.upsertBuilds(listOf(build))

        dao.getBuildsForProject("p1").test {
            val items = awaitItem()
            assert(items.size == 1)
            assert(items[0].id == "b1")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `deleteStale removes old entries`() = runTest {
        val old = buildEntity(id = "old", projectId = "p1", cachedAt = 1000L)
        val fresh = buildEntity(id = "fresh", projectId = "p1", cachedAt = System.currentTimeMillis())
        dao.upsertBuilds(listOf(old, fresh))
        dao.deleteStale(olderThan = System.currentTimeMillis() - 60_000)

        dao.getBuildsForProject("p1").test {
            val items = awaitItem()
            assert(items.size == 1)
            assert(items[0].id == "fresh")
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun buildEntity(id: String, projectId: String, cachedAt: Long = System.currentTimeMillis()) =
        BuildEntity(
            id = id, projectId = projectId, versionName = "1.0.0", versionCode = 1,
            channel = "alpha", environment = "qa", changelog = null, fileSize = 1024,
            checksumSha256 = "abc", status = "active", isLatestInChannel = true,
            uploadDate = System.currentTimeMillis(), uploaderName = "dev", cachedAt = cachedAt
        )
}
