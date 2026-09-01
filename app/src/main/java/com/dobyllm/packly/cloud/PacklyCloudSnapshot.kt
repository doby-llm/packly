package com.dobyllm.packly.cloud

import com.dobyllm.packly.core.model.CurrentSchemaVersion
import com.dobyllm.packly.core.model.PACKLY_CLOUD_SYNC_SCHEMA_VERSION
import com.dobyllm.packly.core.model.PACKLY_DRIVE_APP_PACKAGE
import com.dobyllm.packly.core.model.PACKLY_DRIVE_APPDATA_ROOT
import com.dobyllm.packly.core.model.PACKLY_DRIVE_SNAPSHOT_NAME
import com.dobyllm.packly.core.model.PacklyAppDocument
import com.dobyllm.packly.core.model.PacklyCategory
import com.dobyllm.packly.core.model.PacklyCloudSyncMetadata
import com.dobyllm.packly.core.model.PacklyCloudSyncSettings
import com.dobyllm.packly.core.model.PacklyItem
import com.dobyllm.packly.core.model.PacklyList
import com.dobyllm.packly.core.model.PacklySettings
import com.dobyllm.packly.core.model.PacklyTrip
import kotlinx.serialization.Serializable

/**
 * User data is deliberately separate from [PacklyCloudControlEnvelope]. Session state,
 * account details, access tokens, dirty/outbox state, and device IDs never enter this object.
 */
@Serializable
data class PacklyCloudSnapshot(
    val metadata: PacklyCloudSnapshotMetadata = PacklyCloudSnapshotMetadata(),
    val document: PacklyUserDataSnapshot = PacklyUserDataSnapshot(),
    val control: PacklyCloudControlEnvelope = PacklyCloudControlEnvelope(),
)

@Serializable
data class PacklyUserDataSnapshot(
    val schemaVersion: Int = CurrentSchemaVersion,
    val items: List<PacklyItem> = emptyList(),
    val lists: List<PacklyList> = emptyList(),
    val trips: List<PacklyTrip> = emptyList(),
    val categories: List<PacklyCategory> = emptyList(),
    val settings: PacklyUserSettingsSnapshot = PacklyUserSettingsSnapshot(),
)

@Serializable
data class PacklyUserSettingsSnapshot(
    val themeMode: com.dobyllm.packly.core.model.ThemeMode = com.dobyllm.packly.core.model.ThemeMode.Light,
    val dynamicColorEnabled: Boolean = false,
    val selectedPaletteKey: String = "packly_default",
    val firstLaunchCompleted: Boolean = false,
    val languagePreference: com.dobyllm.packly.core.model.LanguagePreference = com.dobyllm.packly.core.model.LanguagePreference.System,
) {
    fun toLocalSettings(existing: PacklySettings): PacklySettings = PacklySettings(
        themeMode = themeMode,
        dynamicColorEnabled = dynamicColorEnabled,
        selectedPaletteKey = selectedPaletteKey,
        firstLaunchCompleted = firstLaunchCompleted,
        languagePreference = languagePreference,
        cloudSync = existing.cloudSync,
    )

    companion object {
        fun from(settings: PacklySettings): PacklyUserSettingsSnapshot = PacklyUserSettingsSnapshot(
            themeMode = settings.themeMode,
            dynamicColorEnabled = settings.dynamicColorEnabled,
            selectedPaletteKey = settings.selectedPaletteKey,
            firstLaunchCompleted = settings.firstLaunchCompleted,
            languagePreference = settings.languagePreference,
        )
    }
}

@Serializable
data class PacklyCloudControlEnvelope(
    val revision: Long = 0L,
    val lastModifiedDeviceId: String? = null,
    val lastModifiedAt: String? = null,
    val tombstones: List<com.dobyllm.packly.core.model.PacklyCloudTombstone> = emptyList(),
)

@Serializable
data class PacklyCloudSnapshotMetadata(
    val syncSchemaVersion: Int = PACKLY_CLOUD_SYNC_SCHEMA_VERSION,
    val appPackage: String = PACKLY_DRIVE_APP_PACKAGE,
    val rootPath: String = PACKLY_DRIVE_APPDATA_ROOT,
    val snapshotName: String = PACKLY_DRIVE_SNAPSHOT_NAME,
    val generatedAt: String? = null,
    // Kept in the non-user envelope for compatible manifest inspection and old snapshots.
    val revision: Long = 0L,
)

fun PacklyAppDocument.toCloudSnapshot(
    generatedAt: String,
    revision: Long = cloudSyncMetadata.revision,
): PacklyCloudSnapshot {
    val effectiveRevision = revision.coerceAtLeast(cloudSyncMetadata.revision)
    return PacklyCloudSnapshot(
        metadata = PacklyCloudSnapshotMetadata(
            generatedAt = generatedAt,
            revision = effectiveRevision,
        ),
        document = PacklyUserDataSnapshot(
            schemaVersion = schemaVersion,
            items = items,
            lists = lists,
            trips = trips,
            categories = categories,
            settings = PacklyUserSettingsSnapshot.from(settings),
        ),
        control = PacklyCloudControlEnvelope(
            revision = effectiveRevision,
            lastModifiedDeviceId = cloudSyncMetadata.lastModifiedDeviceId,
            lastModifiedAt = cloudSyncMetadata.lastModifiedAt,
            tombstones = cloudSyncMetadata.tombstones,
        ),
    )
}

fun PacklyCloudSnapshot.syncMetadata(): PacklyCloudSyncMetadata {
    val revision = maxOf(metadata.revision, control.revision)
    return PacklyCloudSyncMetadata(
        syncSchemaVersion = metadata.syncSchemaVersion,
        revision = revision,
        lastModifiedDeviceId = control.lastModifiedDeviceId,
        lastModifiedAt = control.lastModifiedAt,
        tombstones = control.tombstones,
    )
}

fun PacklyCloudSnapshot.toAppDocument(existing: PacklyAppDocument = PacklyAppDocument()): PacklyAppDocument =
    PacklyAppDocument(
        schemaVersion = document.schemaVersion,
        items = document.items,
        lists = document.lists,
        trips = document.trips,
        categories = document.categories,
        settings = document.settings.toLocalSettings(existing.settings),
        // A cloud import must not replace transient UI session state.
        session = existing.session,
        cloudSyncMetadata = existing.cloudSyncMetadata.copy(
            syncSchemaVersion = metadata.syncSchemaVersion,
            revision = syncMetadata().revision,
            lastModifiedDeviceId = syncMetadata().lastModifiedDeviceId,
            lastModifiedAt = syncMetadata().lastModifiedAt,
            tombstones = syncMetadata().tombstones,
        ),
    )

fun PacklyCloudSnapshot.isSupported(): Boolean =
    metadata.appPackage == PACKLY_DRIVE_APP_PACKAGE &&
        metadata.rootPath == PACKLY_DRIVE_APPDATA_ROOT &&
        metadata.snapshotName == PACKLY_DRIVE_SNAPSHOT_NAME &&
        metadata.syncSchemaVersion <= PACKLY_CLOUD_SYNC_SCHEMA_VERSION &&
        document.schemaVersion <= CurrentSchemaVersion
