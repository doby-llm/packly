package com.dobyllm.packly.cloud

import com.dobyllm.packly.core.model.PACKLY_CLOUD_SYNC_SCHEMA_VERSION
import com.dobyllm.packly.core.model.PACKLY_DRIVE_APP_PACKAGE
import com.dobyllm.packly.core.model.PACKLY_DRIVE_APPDATA_ROOT
import com.dobyllm.packly.core.model.PACKLY_DRIVE_SNAPSHOT_NAME
import com.dobyllm.packly.core.model.PacklyAppDocument
import com.dobyllm.packly.core.model.PacklyCloudSyncMetadata
import kotlinx.serialization.Serializable

@Serializable
data class PacklyCloudSnapshot(
    val metadata: PacklyCloudSnapshotMetadata = PacklyCloudSnapshotMetadata(),
    val document: PacklyAppDocument = PacklyAppDocument(),
)

@Serializable
data class PacklyCloudSnapshotMetadata(
    val syncSchemaVersion: Int = PACKLY_CLOUD_SYNC_SCHEMA_VERSION,
    val appPackage: String = PACKLY_DRIVE_APP_PACKAGE,
    val rootPath: String = PACKLY_DRIVE_APPDATA_ROOT,
    val snapshotName: String = PACKLY_DRIVE_SNAPSHOT_NAME,
    val generatedAt: String? = null,
    val revision: Long = 0L,
    val deviceId: String? = null,
)

fun PacklyAppDocument.toCloudSnapshot(generatedAt: String): PacklyCloudSnapshot = PacklyCloudSnapshot(
    metadata = PacklyCloudSnapshotMetadata(
        generatedAt = generatedAt,
        revision = cloudSyncMetadata.revision,
        deviceId = cloudSyncMetadata.lastModifiedDeviceId,
    ),
    document = this,
)

fun PacklyCloudSnapshot.syncMetadata(): PacklyCloudSyncMetadata = document.cloudSyncMetadata.copy(
    syncSchemaVersion = metadata.syncSchemaVersion,
    revision = metadata.revision.coerceAtLeast(document.cloudSyncMetadata.revision),
    lastModifiedDeviceId = metadata.deviceId ?: document.cloudSyncMetadata.lastModifiedDeviceId,
)
