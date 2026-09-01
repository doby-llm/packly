package com.dobyllm.packly.cloud

import com.dobyllm.packly.core.model.PacklyAppDocument
import com.dobyllm.packly.core.model.PacklySessionState
import com.dobyllm.packly.data.json.PacklyJson
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PacklyCloudSnapshotTest {
    @Test
    fun snapshotExcludesTransientSessionAndLocalSyncControls() {
        val local = PacklyAppDocument(
            session = PacklySessionState(lastOpenedTripId = "trip-local-only"),
        )

        val snapshot = local.toCloudSnapshot("2026-09-01T00:00:00Z", revision = 4L)
        val json = PacklyJson.format.encodeToString(snapshot)
        val restored = snapshot.toAppDocument(local)

        assertTrue(json.contains("\"document\""))
        assertTrue(json.contains("\"control\""))
        assertFalse(json.contains("\"session\""))
        assertFalse(json.contains("\"cloudSyncMetadata\""))
        assertEquals("trip-local-only", restored.session.lastOpenedTripId)
        assertEquals(4L, snapshot.syncMetadata().revision)
    }

    @Test
    fun inMemoryDeleteIsVerifiedAndKeepsLocalDocumentUntouched() = runBlocking {
        val local = PacklyAppDocument()
        val snapshot = local.toCloudSnapshot("2026-09-01T00:00:00Z", revision = 2L)
        val drive = InMemoryDrivePacklyRepository(snapshot)

        val result = drive.deleteRemoteSnapshot()

        assertTrue(result is DriveSyncResult.Success)
        assertNull((drive.fetchSnapshot() as DriveSyncResult.Success).value)
        assertEquals(snapshot.document.items, local.items)
    }
}
