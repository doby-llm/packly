package com.dobyllm.packly.cloud

import com.dobyllm.packly.core.model.PACKLY_CLOUD_SYNC_SCHEMA_VERSION
import com.dobyllm.packly.core.model.PACKLY_DRIVE_APP_PACKAGE
import com.dobyllm.packly.core.model.PACKLY_DRIVE_APPDATA_ROOT
import com.dobyllm.packly.core.model.PACKLY_DRIVE_SNAPSHOT_NAME
import com.dobyllm.packly.core.model.PacklyCloudSyncDisabledReason

interface DrivePacklyRepository {
    val target: DriveSyncTarget
    suspend fun fetchManifest(): DriveSyncResult<DriveManifest>
    suspend fun fetchSnapshot(): DriveSyncResult<PacklyCloudSnapshot?>
    suspend fun upsertSnapshot(snapshot: PacklyCloudSnapshot): DriveSyncResult<DriveManifest>
}

data class DriveSyncTarget(
    val accountBinding: DriveAccountBinding = DriveAccountBinding.UserAuthorizedGoogleAccount,
    val storageSpace: DriveStorageSpace = DriveStorageSpace.AppDataFolder,
)

enum class DriveAccountBinding { UserAuthorizedGoogleAccount }
enum class DriveStorageSpace { AppDataFolder }

sealed interface DriveSyncResult<out T> {
    data class Success<T>(val value: T) : DriveSyncResult<T>
    data class Blocked(val reason: PacklyCloudSyncDisabledReason, val message: String) : DriveSyncResult<Nothing>
    data class Failure(val throwable: Throwable, val retryable: Boolean = true) : DriveSyncResult<Nothing>
}

data class DriveManifest(
    val syncSchemaVersion: Int = PACKLY_CLOUD_SYNC_SCHEMA_VERSION,
    val appPackage: String = PACKLY_DRIVE_APP_PACKAGE,
    val rootPath: String = PACKLY_DRIVE_APPDATA_ROOT,
    val snapshotName: String = PACKLY_DRIVE_SNAPSHOT_NAME,
    val driveFileId: String? = null,
    val updatedAt: String? = null,
    val revision: Long = 0L,
    val target: DriveSyncTarget = DriveSyncTarget(),
) {
    companion object {
        fun fromSnapshot(snapshot: PacklyCloudSnapshot, driveFileId: String? = null): DriveManifest = DriveManifest(
            syncSchemaVersion = snapshot.metadata.syncSchemaVersion,
            appPackage = snapshot.metadata.appPackage,
            rootPath = snapshot.metadata.rootPath,
            snapshotName = snapshot.metadata.snapshotName,
            driveFileId = driveFileId,
            updatedAt = snapshot.metadata.generatedAt,
            revision = snapshot.metadata.revision,
        )
    }
}

class NotConfiguredDrivePacklyRepository(
    private val reason: PacklyCloudSyncDisabledReason = PacklyCloudSyncDisabledReason.GoogleCloudSetupRequired,
    override val target: DriveSyncTarget = DriveSyncTarget(),
) : DrivePacklyRepository {
    override suspend fun fetchManifest(): DriveSyncResult<DriveManifest> = notConfigured()
    override suspend fun fetchSnapshot(): DriveSyncResult<PacklyCloudSnapshot?> = notConfigured()
    override suspend fun upsertSnapshot(snapshot: PacklyCloudSnapshot): DriveSyncResult<DriveManifest> = notConfigured()

    private fun <T> notConfigured(): DriveSyncResult<T> = DriveSyncResult.Blocked(
        reason = reason,
        message = "Google Drive sync is not configured. Add the Android OAuth client ID and enable the Drive appDataFolder scope first.",
    )
}

class InMemoryDrivePacklyRepository(
    initialSnapshot: PacklyCloudSnapshot? = null,
    override val target: DriveSyncTarget = DriveSyncTarget(),
) : DrivePacklyRepository {
    private var snapshot: PacklyCloudSnapshot? = initialSnapshot

    override suspend fun fetchManifest(): DriveSyncResult<DriveManifest> = DriveSyncResult.Success(
        snapshot?.let { DriveManifest.fromSnapshot(it) } ?: DriveManifest(target = target),
    )

    override suspend fun fetchSnapshot(): DriveSyncResult<PacklyCloudSnapshot?> = DriveSyncResult.Success(snapshot)

    override suspend fun upsertSnapshot(snapshot: PacklyCloudSnapshot): DriveSyncResult<DriveManifest> {
        this.snapshot = snapshot
        return DriveSyncResult.Success(DriveManifest.fromSnapshot(snapshot))
    }
}
