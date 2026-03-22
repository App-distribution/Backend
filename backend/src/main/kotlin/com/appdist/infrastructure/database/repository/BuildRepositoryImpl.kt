package com.appdist.infrastructure.database.repository

import com.appdist.domain.model.*
import com.appdist.domain.repository.BuildRepository
import com.appdist.infrastructure.database.tables.BuildsTable
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.UUID

class BuildRepositoryImpl : BuildRepository {
    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    override suspend fun create(build: Build): Build = dbQuery {
        BuildsTable.insert { row ->
            row[id] = build.id
            row[projectId] = build.projectId
            row[versionName] = build.versionName
            row[versionCode] = build.versionCode
            row[buildNumber] = build.buildNumber
            row[flavor] = build.flavor
            row[buildType] = build.buildType
            row[environment] = build.environment.name
            row[channel] = build.channel.name
            row[branch] = build.branch
            row[commitHash] = build.commitHash
            row[uploaderId] = build.uploaderId
            row[uploadDate] = build.uploadDate
            row[changelog] = build.changelog
            row[fileSizeBytes] = build.fileSizeBytes
            row[checksumSha256] = build.checksumSha256
            row[minSdk] = build.minSdk
            row[targetSdk] = build.targetSdk
            row[certFingerprint] = build.certFingerprint
            row[abis] = Json.encodeToString(build.abis)
            row[storageKey] = build.storageKey
            row[status] = build.status.name
            row[expiryDate] = build.expiryDate
            row[isLatestInChannel] = build.isLatestInChannel
        }
        build
    }

    override suspend fun findById(id: UUID): Build? = dbQuery {
        BuildsTable.selectAll().where { BuildsTable.id eq id }.singleOrNull()?.toBuild()
    }

    override suspend fun listByProject(
        projectId: UUID, channel: ReleaseChannel?, search: String?, page: Int, limit: Int
    ): List<Build> = dbQuery {
        BuildsTable.selectAll()
            .where {
                var condition = BuildsTable.projectId eq projectId
                if (channel != null) condition = condition and (BuildsTable.channel eq channel.name)
                if (search != null) condition = condition and (
                    (BuildsTable.versionName like "%$search%") or
                    (BuildsTable.branch like "%$search%")
                )
                condition
            }
            .orderBy(BuildsTable.uploadDate, SortOrder.DESC)
            .limit(limit).offset(page.toLong() * limit)
            .map { it.toBuild() }
    }

    override suspend fun listRecent(workspaceId: UUID, limit: Int): List<Build> = dbQuery {
        (BuildsTable innerJoin com.appdist.infrastructure.database.tables.ProjectsTable)
            .selectAll()
            .where { com.appdist.infrastructure.database.tables.ProjectsTable.workspaceId eq workspaceId }
            .orderBy(BuildsTable.uploadDate, SortOrder.DESC)
            .limit(limit)
            .map { it.toBuild() }
    }

    override suspend fun update(buildId: UUID, changelog: String?, status: BuildStatus): Build = dbQuery {
        BuildsTable.update({ BuildsTable.id eq buildId }) {
            if (changelog != null) it[BuildsTable.changelog] = changelog
            it[BuildsTable.status] = status.name
        }
        BuildsTable.selectAll().where { BuildsTable.id eq buildId }.single().toBuild()
    }

    override suspend fun delete(buildId: UUID) = dbQuery {
        BuildsTable.deleteWhere { BuildsTable.id eq buildId }
        Unit
    }

    override suspend fun findByChecksum(checksum: String): Build? = dbQuery {
        BuildsTable.selectAll().where { BuildsTable.checksumSha256 eq checksum }.singleOrNull()?.toBuild()
    }

    override suspend fun unsetLatestInChannel(projectId: UUID, channel: ReleaseChannel) = dbQuery {
        BuildsTable.update({
            (BuildsTable.projectId eq projectId) and (BuildsTable.channel eq channel.name)
        }) { it[isLatestInChannel] = false }
        Unit
    }

    private fun ResultRow.toBuild() = Build(
        id = this[BuildsTable.id],
        projectId = this[BuildsTable.projectId],
        versionName = this[BuildsTable.versionName],
        versionCode = this[BuildsTable.versionCode],
        buildNumber = this[BuildsTable.buildNumber],
        flavor = this[BuildsTable.flavor],
        buildType = this[BuildsTable.buildType],
        environment = BuildEnvironment.valueOf(this[BuildsTable.environment]),
        channel = ReleaseChannel.valueOf(this[BuildsTable.channel]),
        branch = this[BuildsTable.branch],
        commitHash = this[BuildsTable.commitHash],
        uploaderId = this[BuildsTable.uploaderId],
        uploadDate = this[BuildsTable.uploadDate],
        changelog = this[BuildsTable.changelog],
        fileSizeBytes = this[BuildsTable.fileSizeBytes],
        checksumSha256 = this[BuildsTable.checksumSha256],
        minSdk = this[BuildsTable.minSdk],
        targetSdk = this[BuildsTable.targetSdk],
        certFingerprint = this[BuildsTable.certFingerprint],
        abis = runCatching { Json.decodeFromString<List<String>>(this[BuildsTable.abis]) }.getOrDefault(emptyList()),
        storageKey = this[BuildsTable.storageKey],
        status = BuildStatus.valueOf(this[BuildsTable.status]),
        expiryDate = this[BuildsTable.expiryDate],
        isLatestInChannel = this[BuildsTable.isLatestInChannel],
    )
}
