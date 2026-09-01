package com.dobyllm.packly.cloud

import com.dobyllm.packly.core.model.CategoryId
import com.dobyllm.packly.core.model.InstantString
import com.dobyllm.packly.core.model.ItemId
import com.dobyllm.packly.core.model.ListId
import com.dobyllm.packly.core.model.PacklyAppDocument
import com.dobyllm.packly.core.model.PacklyCategory
import com.dobyllm.packly.core.model.PacklyCloudSyncConnectionStatus
import com.dobyllm.packly.core.model.PacklyCloudSyncMetadata
import com.dobyllm.packly.core.model.PacklyCloudSyncOperation
import com.dobyllm.packly.core.model.PacklyCloudSyncOperationType
import com.dobyllm.packly.core.model.PacklyCloudTombstone
import com.dobyllm.packly.core.model.PacklyItem
import com.dobyllm.packly.core.model.PacklyList
import com.dobyllm.packly.core.model.PacklyTrip
import com.dobyllm.packly.core.model.TripId
import com.dobyllm.packly.core.time.PacklyClock
import com.dobyllm.packly.data.repository.DataStorePacklyRepository
import java.util.UUID
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class PacklyCloudSyncCoordinator(
    private val repository: DataStorePacklyRepository,
    private val driveRepository: DrivePacklyRepository,
    private val deviceIdProvider: CloudSyncDeviceIdProvider,
) {
    private val syncMutex = Mutex()

    suspend fun syncNow() = syncMutex.withLock {
        syncNowLocked()
    }

    private suspend fun syncNowLocked() {
        val deviceId = deviceIdProvider.getOrCreateDeviceId()
        repository.updateCloudSyncStatus(PacklyCloudSyncConnectionStatus.Syncing, lastError = null)
        when (val remote = driveRepository.fetchSnapshot()) {
            is DriveSyncResult.Blocked -> repository.updateCloudSyncStatus(
                status = PacklyCloudSyncConnectionStatus.NotConfigured,
                disabledReason = remote.reason,
                lastError = remote.message,
            )
            is DriveSyncResult.Failure -> repository.updateCloudSyncStatus(
                status = PacklyCloudSyncConnectionStatus.Offline,
                lastError = remote.throwable.message ?: remote.throwable::class.java.simpleName,
            )
            is DriveSyncResult.Success -> reconcile(remote.value, deviceId)
        }
    }

    suspend fun deleteRemoteBackup() = syncMutex.withLock {
        when (val result = driveRepository.deleteRemoteSnapshot()) {
            is DriveSyncResult.Success -> repository.updateSettings { settings ->
                settings.copy(
                    cloudSync = settings.cloudSync.copy(
                        enabled = false,
                        status = PacklyCloudSyncConnectionStatus.Disconnected,
                        disabledReason = com.dobyllm.packly.core.model.PacklyCloudSyncDisabledReason.UserNotConnected,
                        lastError = null,
                    ),
                )
            }
            is DriveSyncResult.Blocked -> repository.updateCloudSyncStatus(
                status = PacklyCloudSyncConnectionStatus.NeedsAttention,
                disabledReason = result.reason,
                lastError = result.message,
            )
            is DriveSyncResult.Failure -> repository.updateCloudSyncStatus(
                status = if (result.retryable) PacklyCloudSyncConnectionStatus.Offline else PacklyCloudSyncConnectionStatus.NeedsAttention,
                lastError = result.throwable.message ?: result.throwable::class.java.simpleName,
            )
        }
    }

    private suspend fun reconcile(remote: PacklyCloudSnapshot?, deviceId: String) {
        val local = repository.currentDocument()
        if (remote == null) {
            upload(local.preparedForUpload(deviceId), deviceId)
            return
        }

        if (!remote.isSupported()) {
            repository.updateCloudSyncStatus(
                status = PacklyCloudSyncConnectionStatus.NeedsAttention,
                lastError = "remote_data_unsupported",
            )
            return
        }
        val remoteDocument = remote.toAppDocument(local)
        if (!local.hasUserContent()) {
            val imported = remoteDocument.copy(
                cloudSyncMetadata = remoteDocument.cloudSyncMetadata.copy(
                    tombstones = mergeTombstones(
                        local.cloudSyncMetadata.tombstones,
                        remoteDocument.cloudSyncMetadata.tombstones,
                    ),
                ),
            ).markImported(remote.control.revision, deviceId)
            repository.replaceFromCloud(imported)
            repository.updateCloudSyncStatus(
                PacklyCloudSyncConnectionStatus.Synced,
                lastSyncedAt = PacklyClock.now(),
                lastError = null,
            )
            return
        }

        val merged = mergeDocuments(local, remoteDocument, deviceId)

        if (merged.hasSameUserDataAs(remoteDocument)) {
            repository.replaceFromCloud(merged.markImported(remote.control.revision, deviceId))
            repository.updateCloudSyncStatus(PacklyCloudSyncConnectionStatus.Synced, lastSyncedAt = PacklyClock.now(), lastError = null)
            return
        }

        upload(merged.preparedForUpload(deviceId), deviceId)
    }

    private suspend fun upload(document: PacklyAppDocument, deviceId: String) {
        val now = PacklyClock.now()
        val snapshot = document.toCloudSnapshot(generatedAt = now)
        when (val uploaded = driveRepository.upsertSnapshot(snapshot)) {
            is DriveSyncResult.Success -> repository.replaceFromCloud(
                document.copy(
                    cloudSyncMetadata = document.cloudSyncMetadata.copy(
                        dirty = false,
                        outbox = emptyList(),
                        lastUploadedRevision = uploaded.value.revision,
                        lastImportedCloudRevision = uploaded.value.revision,
                        lastModifiedDeviceId = deviceId,
                    ),
                    settings = document.settings.copy(
                        cloudSync = document.settings.cloudSync.copy(
                            enabled = true,
                            status = PacklyCloudSyncConnectionStatus.Synced,
                            lastSyncedAt = now,
                            lastError = null,
                        ),
                    ),
                ),
            )
            is DriveSyncResult.Blocked -> repository.updateCloudSyncStatus(
                status = PacklyCloudSyncConnectionStatus.NotConfigured,
                disabledReason = uploaded.reason,
                lastError = uploaded.message,
            )
            is DriveSyncResult.Failure -> repository.updateCloudSyncStatus(
                status = PacklyCloudSyncConnectionStatus.Offline,
                lastError = uploaded.throwable.message ?: uploaded.throwable::class.java.simpleName,
            )
        }
    }
}

fun PacklyAppDocument.markCloudDirty(deviceId: String, now: InstantString): PacklyAppDocument {
    val nextRevision = cloudSyncMetadata.revision + 1
    return copy(
        cloudSyncMetadata = cloudSyncMetadata.copy(
            revision = nextRevision,
            lastModifiedDeviceId = deviceId,
            lastModifiedAt = now,
            dirty = true,
            outbox = listOf(
                PacklyCloudSyncOperation(
                    id = UUID.randomUUID().toString(),
                    type = PacklyCloudSyncOperationType.UpsertSnapshot,
                    revision = nextRevision,
                    createdAt = now,
                ),
            ),
            tombstones = cloudSyncMetadata.tombstones + deletedEntityTombstones(nextRevision, deviceId, now),
        ),
    )
}

private fun PacklyAppDocument.preparedForUpload(deviceId: String): PacklyAppDocument {
    val now = PacklyClock.now()
    val nextRevision = cloudSyncMetadata.revision + 1
    val metadata = if (cloudSyncMetadata.dirty && cloudSyncMetadata.outbox.isNotEmpty()) {
        cloudSyncMetadata
    } else cloudSyncMetadata.copy(
        revision = nextRevision,
        dirty = true,
        outbox = listOf(
            PacklyCloudSyncOperation(
                id = UUID.randomUUID().toString(),
                type = PacklyCloudSyncOperationType.UpsertSnapshot,
                revision = nextRevision,
                createdAt = now,
            ),
        ),
    )
    return copy(
        cloudSyncMetadata = metadata.copy(lastModifiedDeviceId = deviceId, lastModifiedAt = now),
    )
}

private fun PacklyAppDocument.markImported(remoteRevision: Long, deviceId: String): PacklyAppDocument = copy(
    cloudSyncMetadata = cloudSyncMetadata.copy(
        revision = maxOf(cloudSyncMetadata.revision, remoteRevision),
        dirty = false,
        outbox = emptyList(),
        lastImportedCloudRevision = remoteRevision,
        lastModifiedDeviceId = cloudSyncMetadata.lastModifiedDeviceId ?: deviceId,
    ),
)

private fun PacklyAppDocument.hasUserContent(): Boolean =
    items.any { !it.isSeed } || lists.any { !it.isSeed } || trips.isNotEmpty() || categories.any { !it.isSeed }

private fun PacklyAppDocument.hasSameUserDataAs(other: PacklyAppDocument): Boolean =
    copy(
        cloudSyncMetadata = other.cloudSyncMetadata,
        settings = settings.copy(cloudSync = other.settings.cloudSync),
    ) == other

private fun mergeDocuments(local: PacklyAppDocument, remote: PacklyAppDocument, deviceId: String): PacklyAppDocument {
    val localRevisionWins = local.cloudSyncMetadata.revision >= remote.cloudSyncMetadata.revision
    val merged = (if (localRevisionWins) local else remote).copy(
        items = mergeById(local.items, remote.items, PacklyItem::id, PacklyItem::updatedAt),
        lists = mergeById(local.lists, remote.lists, PacklyList::id, PacklyList::updatedAt),
        trips = mergeById(local.trips, remote.trips, PacklyTrip::id, PacklyTrip::updatedAt),
        categories = mergeCategories(local.categories, remote.categories),
        settings = if (localRevisionWins) local.settings else remote.settings,
    )
    val changedFromRemote = !merged.hasSameUserDataAs(remote)
    val nextRevision = maxOf(local.cloudSyncMetadata.revision, remote.cloudSyncMetadata.revision) + if (changedFromRemote) 1 else 0
    return merged.copy(
        // Cloud control state is local-only. Never replace the local outbox with
        // the remote envelope while merging user data.
        cloudSyncMetadata = local.cloudSyncMetadata.copy(
            revision = nextRevision,
            dirty = changedFromRemote,
            lastModifiedDeviceId = if (changedFromRemote) deviceId else local.cloudSyncMetadata.lastModifiedDeviceId,
            tombstones = mergeTombstones(local.cloudSyncMetadata.tombstones, remote.cloudSyncMetadata.tombstones),
        ),
    )
}

private fun mergeTombstones(
    local: List<PacklyCloudTombstone>,
    remote: List<PacklyCloudTombstone>,
): List<PacklyCloudTombstone> = (local + remote)
    .distinctBy { "${it.entityType}:${it.entityId}:${it.revision}" }

private fun <T, K> mergeById(
    local: List<T>,
    remote: List<T>,
    id: (T) -> K,
    updatedAt: (T) -> String,
): List<T> {
    val remoteById = remote.associateBy(id)
    val localById = local.associateBy(id)
    return (localById.keys + remoteById.keys).mapNotNull { key ->
        val localValue = localById[key]
        val remoteValue = remoteById[key]
        when {
            localValue == null -> remoteValue
            remoteValue == null -> localValue
            updatedAt(localValue) >= updatedAt(remoteValue) -> localValue
            else -> remoteValue
        }
    }.sortedBy { id(it).toString() }
}

private fun mergeCategories(local: List<PacklyCategory>, remote: List<PacklyCategory>): List<PacklyCategory> =
    mergeById(local, remote, PacklyCategory::id, PacklyCategory::updatedAt)
        .sortedWith(compareBy(PacklyCategory::sortOrder, PacklyCategory::id))

private fun PacklyAppDocument.deletedEntityTombstones(revision: Long, deviceId: String, now: InstantString): List<PacklyCloudTombstone> {
    val existing = cloudSyncMetadata.tombstones.map { it.entityType to it.entityId }.toSet()
    fun tombstone(entityType: String, entityId: String) = PacklyCloudTombstone(entityType, entityId, now, revision, deviceId)
    val itemTombstones = items.filter { it.isArchived }.mapNotNull { tombstoneIfMissing("item", it.id, existing, ::tombstone) }
    val listTombstones = lists.filter { it.isArchived }.mapNotNull { tombstoneIfMissing("list", it.id, existing, ::tombstone) }
    val tripTombstones = trips.filter { it.status.name == "Archived" }.mapNotNull { tombstoneIfMissing("trip", it.id, existing, ::tombstone) }
    return itemTombstones + listTombstones + tripTombstones
}

private fun tombstoneIfMissing(
    entityType: String,
    entityId: String,
    existing: Set<Pair<String, String>>,
    create: (String, String) -> PacklyCloudTombstone,
): PacklyCloudTombstone? = if (entityType to entityId in existing) null else create(entityType, entityId)
