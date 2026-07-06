package com.dobyllm.packly.cloud

import android.content.Context
import com.dobyllm.packly.core.model.PACKLY_DRIVE_SNAPSHOT_NAME
import com.dobyllm.packly.core.model.PacklyCloudSyncDisabledReason
import com.dobyllm.packly.data.json.PacklyJson
import com.google.android.gms.auth.api.identity.AuthorizationClient
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.decodeFromJsonElement
import org.json.JSONArray
import org.json.JSONObject

private const val DRIVE_APPDATA_SCOPE = "https://www.googleapis.com/auth/drive.appdata"
private const val DRIVE_API = "https://www.googleapis.com/drive/v3"
private const val DRIVE_UPLOAD_API = "https://www.googleapis.com/upload/drive/v3"
private const val BOUNDARY = "PacklyDriveBoundary"

class GoogleDrivePacklyRepository(
    private val authorizationClient: AuthorizationClient,
    private val driveScope: Scope = Scope(DRIVE_APPDATA_SCOPE),
    private val api: DriveSnapshotApi = HttpDriveSnapshotApi(),
    override val target: DriveSyncTarget = DriveSyncTarget(),
) : DrivePacklyRepository {
    override suspend fun fetchManifest(): DriveSyncResult<DriveManifest> = withAccessToken { accessToken ->
        when (val result = api.findSnapshot(accessToken)) {
            is DriveApiResult.Success -> DriveSyncResult.Success(
                result.value?.toManifest() ?: DriveManifest(target = target),
            )
            is DriveApiResult.Unauthorized -> authBlocked()
            is DriveApiResult.Failure -> DriveSyncResult.Failure(result.throwable, result.retryable)
        }
    }

    override suspend fun fetchSnapshot(): DriveSyncResult<PacklyCloudSnapshot?> = withAccessToken { accessToken ->
        when (val fileResult = api.findSnapshot(accessToken)) {
            is DriveApiResult.Success -> {
                val file = fileResult.value ?: return@withAccessToken DriveSyncResult.Success(null)
                when (val download = api.downloadSnapshot(accessToken, file.id)) {
                    is DriveApiResult.Success -> DriveSyncResult.Success(download.value)
                    is DriveApiResult.Unauthorized -> authBlocked()
                    is DriveApiResult.Failure -> DriveSyncResult.Failure(download.throwable, download.retryable)
                }
            }
            is DriveApiResult.Unauthorized -> authBlocked()
            is DriveApiResult.Failure -> DriveSyncResult.Failure(fileResult.throwable, fileResult.retryable)
        }
    }

    override suspend fun upsertSnapshot(snapshot: PacklyCloudSnapshot): DriveSyncResult<DriveManifest> = withAccessToken { accessToken ->
        when (val fileResult = api.findSnapshot(accessToken)) {
            is DriveApiResult.Success -> when (val upload = api.upsertSnapshot(accessToken, fileResult.value?.id, snapshot)) {
                is DriveApiResult.Success -> DriveSyncResult.Success(upload.value.toManifest(snapshot))
                is DriveApiResult.Unauthorized -> authBlocked()
                is DriveApiResult.Failure -> DriveSyncResult.Failure(upload.throwable, upload.retryable)
            }
            is DriveApiResult.Unauthorized -> authBlocked()
            is DriveApiResult.Failure -> DriveSyncResult.Failure(fileResult.throwable, fileResult.retryable)
        }
    }

    private suspend fun <T> withAccessToken(operation: suspend (String) -> DriveSyncResult<T>): DriveSyncResult<T> {
        val authResult = runCatching {
            authorizationClient.authorize(
                AuthorizationRequest.builder()
                    .setRequestedScopes(listOf(driveScope))
                    .build(),
            ).await()
        }.getOrElse { throwable -> return DriveSyncResult.Failure(throwable) }

        if (authResult.hasResolution()) return authBlocked()
        val grantedDriveScope = authResult.grantedScopes.any { it == driveScope.scopeUri }
        val accessToken = authResult.accessToken
        if (!grantedDriveScope || accessToken.isNullOrBlank()) return authBlocked()
        return operation(accessToken)
    }

    private fun <T> authBlocked(): DriveSyncResult<T> = DriveSyncResult.Blocked(
        reason = PacklyCloudSyncDisabledReason.AuthorizationRequired,
        message = "Google Drive authorization requires user interaction before Packly can access appDataFolder.",
    )

    companion object {
        fun create(context: Context): GoogleDrivePacklyRepository = GoogleDrivePacklyRepository(
            authorizationClient = Identity.getAuthorizationClient(context),
        )
    }
}

private interface DriveSnapshotApi {
    suspend fun findSnapshot(accessToken: String): DriveApiResult<DriveFile?>
    suspend fun downloadSnapshot(accessToken: String, fileId: String): DriveApiResult<PacklyCloudSnapshot>
    suspend fun upsertSnapshot(accessToken: String, existingFileId: String?, snapshot: PacklyCloudSnapshot): DriveApiResult<DriveFile>
}

private class HttpDriveSnapshotApi : DriveSnapshotApi {
    override suspend fun findSnapshot(accessToken: String): DriveApiResult<DriveFile?> = runDriveRequest {
        val query = "name='${PACKLY_DRIVE_SNAPSHOT_NAME}' and 'appDataFolder' in parents and trashed=false"
        val url = "$DRIVE_API/files?spaces=appDataFolder&fields=files(id,name,modifiedTime,appProperties)&q=${query.urlEncoded()}"
        val json = URL(url).authorizedConnection(accessToken).readTextResponse()
        val files = JSONObject(json).optJSONArray("files") ?: JSONArray()
        if (files.length() == 0) null else files.getJSONObject(0).toDriveFile()
    }

    override suspend fun downloadSnapshot(accessToken: String, fileId: String): DriveApiResult<PacklyCloudSnapshot> = runDriveRequest {
        val json = URL("$DRIVE_API/files/${fileId.urlEncoded()}?alt=media").authorizedConnection(accessToken).readTextResponse()
        val element = PacklyJson.format.parseToJsonElement(json)
        PacklyJson.format.decodeFromJsonElement(PacklyCloudSnapshot.serializer(), element)
    }

    override suspend fun upsertSnapshot(accessToken: String, existingFileId: String?, snapshot: PacklyCloudSnapshot): DriveApiResult<DriveFile> = runDriveRequest {
        val metadata = JSONObject()
            .put("name", PACKLY_DRIVE_SNAPSHOT_NAME)
            .put("appProperties", JSONObject().put("packlyRevision", snapshot.metadata.revision.toString()))
        if (existingFileId == null) metadata.put("parents", JSONArray().put("appDataFolder"))
        val body = buildMultipartBody(metadata, PacklyJson.format.encodeToString(snapshot))
        val url = if (existingFileId == null) {
            "$DRIVE_UPLOAD_API/files?uploadType=multipart&fields=id,name,modifiedTime,appProperties"
        } else {
            "$DRIVE_UPLOAD_API/files/${existingFileId.urlEncoded()}?uploadType=multipart&fields=id,name,modifiedTime,appProperties"
        }
        URL(url).authorizedConnection(accessToken).apply {
            requestMethod = if (existingFileId == null) "POST" else "PATCH"
            doOutput = true
            setRequestProperty("Content-Type", "multipart/related; boundary=$BOUNDARY")
            outputStream.use { it.write(body.encodeToByteArray()) }
        }.readTextResponse().let { JSONObject(it).toDriveFile() }
    }

    private fun buildMultipartBody(metadata: JSONObject, snapshotJson: String): String = buildString {
        append("--$BOUNDARY\r\n")
        append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
        append(metadata.toString())
        append("\r\n--$BOUNDARY\r\n")
        append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
        append(snapshotJson)
        append("\r\n--$BOUNDARY--\r\n")
    }
}

private sealed interface DriveApiResult<out T> {
    data class Success<T>(val value: T) : DriveApiResult<T>
    data object Unauthorized : DriveApiResult<Nothing>
    data class Failure(val throwable: Throwable, val retryable: Boolean = true) : DriveApiResult<Nothing>
}

private data class DriveFile(
    val id: String,
    val name: String,
    val modifiedTime: String? = null,
    val revision: Long = 0L,
) {
    fun toManifest(snapshot: PacklyCloudSnapshot? = null): DriveManifest = snapshot?.let {
        DriveManifest.fromSnapshot(it, driveFileId = id)
    } ?: DriveManifest(driveFileId = id, updatedAt = modifiedTime, revision = revision)
}

private fun JSONObject.toDriveFile(): DriveFile = DriveFile(
    id = getString("id"),
    name = optString("name"),
    modifiedTime = optString("modifiedTime", null),
    revision = optJSONObject("appProperties")?.optString("packlyRevision")?.toLongOrNull() ?: 0L,
)

private inline fun <T> runDriveRequest(block: () -> T): DriveApiResult<T> = try {
    DriveApiResult.Success(block())
} catch (throwable: DriveHttpException) {
    if (throwable.statusCode == HttpURLConnection.HTTP_UNAUTHORIZED || throwable.statusCode == HttpURLConnection.HTTP_FORBIDDEN) {
        DriveApiResult.Unauthorized
    } else {
        DriveApiResult.Failure(throwable, retryable = throwable.statusCode >= 500)
    }
} catch (throwable: Throwable) {
    DriveApiResult.Failure(throwable)
}

private fun URL.authorizedConnection(accessToken: String): HttpURLConnection = (openConnection() as HttpURLConnection).apply {
    setRequestProperty("Authorization", "Bearer $accessToken")
    setRequestProperty("Accept", "application/json")
    connectTimeout = 15_000
    readTimeout = 30_000
}

private fun HttpURLConnection.readTextResponse(): String {
    val body = try {
        inputStream.use { it.readBytes().decodeToString() }
    } catch (_: IOException) {
        errorStream?.use { it.readBytes().decodeToString() }.orEmpty()
    }
    if (responseCode !in 200..299) throw DriveHttpException(responseCode, body.take(500))
    return body
}

private class DriveHttpException(val statusCode: Int, message: String) : IOException("Drive API returned HTTP $statusCode: $message")

private fun String.urlEncoded(): String = URLEncoder.encode(this, "UTF-8")

private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
    addOnCompleteListener { task ->
        if (task.isSuccessful) {
            continuation.resume(task.result)
        } else {
            continuation.cancel(task.exception ?: IllegalStateException("Google Play Services task failed."))
        }
    }
}
