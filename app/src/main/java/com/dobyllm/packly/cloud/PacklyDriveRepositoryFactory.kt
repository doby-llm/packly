package com.dobyllm.packly.cloud

import android.content.Context
import com.dobyllm.packly.BuildConfig
import com.dobyllm.packly.core.model.PacklyCloudSyncDisabledReason

object PacklyDriveRepositoryFactory {
    fun create(context: Context): DrivePacklyRepository {
        if (!BuildConfig.PACKLY_DRIVE_SYNC_ENABLED) {
            return NotConfiguredDrivePacklyRepository(PacklyCloudSyncDisabledReason.GoogleCloudSetupRequired)
        }
        if (BuildConfig.PACKLY_GOOGLE_ANDROID_CLIENT_ID.isBlank()) {
            return NotConfiguredDrivePacklyRepository(PacklyCloudSyncDisabledReason.OAuthClientMissing)
        }
        return GoogleDrivePacklyRepository.create(context.applicationContext)
    }
}
