package com.dobyllm.packly.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class PacklyCloudSyncConnectionStatus {
    Disabled,
    NotConfigured,
    Disconnected,
    Syncing,
    Synced,
    Offline,
    NeedsAttention,
}

@Serializable
enum class PacklyCloudSyncDisabledReason {
    GoogleCloudSetupRequired,
    OAuthClientMissing,
    UserNotConnected,
    AccountChanged,
    UnsafeLocalState,
    AuthorizationRequired,
}

/** Stable, non-user-visible error codes stored locally and mapped through resources in the UI. */
@Serializable
enum class PacklyCloudSyncErrorCode {
    AuthorizationCancelled,
    AuthorizationDataMissing,
    AuthorizationParserFailed,
    AuthorizationScopeDenied,
    AuthorizationBlankToken,
    AuthorizationRequired,
    AccountChanged,
    NetworkUnavailable,
    RemoteServerError,
    RemoteDataInvalid,
    RemoteBackupDeleteFailed,
    RemoteBackupDeleteUnverified,
    GoogleCloudSetupRequired,
    Unknown,
}

@Serializable
enum class PacklyCloudSyncOperationType { UpsertSnapshot, DeleteEntity }

@Serializable
data class PacklyCloudSyncSettings(
    val enabled: Boolean = false,
    val status: PacklyCloudSyncConnectionStatus = PacklyCloudSyncConnectionStatus.NotConfigured,
    val disabledReason: PacklyCloudSyncDisabledReason = PacklyCloudSyncDisabledReason.GoogleCloudSetupRequired,
    // Legacy fields remain local-only for forward-compatible decoding. Cloud snapshots use a separate user envelope.
    val accountEmail: String? = null,
    val accountId: String? = null,
    val lastSyncedAt: InstantString? = null,
    val lastError: String? = null,
)

@Serializable
data class PacklyCloudSyncMetadata(
    val syncSchemaVersion: Int = PACKLY_CLOUD_SYNC_SCHEMA_VERSION,
    val revision: Long = 0L,
    val lastModifiedDeviceId: String? = null,
    val lastModifiedAt: InstantString? = null,
    val dirty: Boolean = false,
    val lastUploadedRevision: Long = 0L,
    val lastImportedCloudRevision: Long = 0L,
    val outbox: List<PacklyCloudSyncOperation> = emptyList(),
    val tombstones: List<PacklyCloudTombstone> = emptyList(),
)

@Serializable
data class PacklyCloudSyncOperation(
    val id: String,
    val type: PacklyCloudSyncOperationType,
    val entityType: String? = null,
    val entityId: String? = null,
    val revision: Long,
    val createdAt: InstantString,
    val attemptCount: Int = 0,
    val nextRetryAt: InstantString? = null,
    val lastError: String? = null,
)

@Serializable
data class PacklyCloudTombstone(
    val entityType: String,
    val entityId: String,
    val deletedAt: InstantString,
    val revision: Long,
    val deviceId: String? = null,
)

const val PACKLY_CLOUD_SYNC_SCHEMA_VERSION = 2
const val PACKLY_DRIVE_APP_PACKAGE = "com.gusanitolabs.packly"
const val PACKLY_DRIVE_APPDATA_ROOT = "appDataFolder"
const val PACKLY_DRIVE_SNAPSHOT_NAME = "packly_snapshot.json"
const val PACKLY_DRIVE_SCOPE = "https://www.googleapis.com/auth/drive.appdata"
