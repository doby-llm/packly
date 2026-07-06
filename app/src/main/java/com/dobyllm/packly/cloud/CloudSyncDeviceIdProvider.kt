package com.dobyllm.packly.cloud

import android.content.Context
import java.util.UUID

class CloudSyncDeviceIdProvider(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences("packly_cloud_sync", Context.MODE_PRIVATE)

    fun getOrCreateDeviceId(): String {
        preferences.getString(KEY_DEVICE_ID, null)?.let { return it }
        val deviceId = UUID.randomUUID().toString()
        preferences.edit().putString(KEY_DEVICE_ID, deviceId).apply()
        return deviceId
    }

    private companion object {
        const val KEY_DEVICE_ID = "device_id"
    }
}
