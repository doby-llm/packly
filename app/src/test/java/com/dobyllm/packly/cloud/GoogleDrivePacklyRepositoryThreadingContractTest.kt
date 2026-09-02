package com.dobyllm.packly.cloud

import java.nio.file.Files
import java.nio.file.Path
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GoogleDrivePacklyRepositoryThreadingContractTest {
    @Test
    fun driveHttpOperationsStayOnIoAfterMainThreadAuthorization() {
        val source = source("app/src/main/java/com/dobyllm/packly/cloud/GoogleDrivePacklyRepository.kt")
        val accessTokenBoundary = source
            .substringAfter("private suspend fun <T> withAccessToken")
            .substringBefore("private fun <T> authBlocked")

        assertTrue(source.contains("import kotlinx.coroutines.Dispatchers"))
        assertTrue(source.contains("import kotlinx.coroutines.withContext"))
        assertTrue(accessTokenBoundary.contains("authorizationClient.authorize("))
        assertTrue(accessTokenBoundary.contains("return withContext(Dispatchers.IO) { operation(accessToken) }"))
        assertFalse(accessTokenBoundary.contains("return operation(accessToken)"))

        listOf(
            "api.findSnapshot(accessToken)",
            "api.downloadSnapshot(accessToken, file.id)",
            "api.upsertSnapshot(accessToken, fileResult.value?.id, snapshot)",
            "api.deleteSnapshot(accessToken, file.id)",
        ).forEach { operation ->
            assertTrue("Expected Drive operation to remain behind the access-token boundary: $operation", source.contains(operation))
        }
    }

    private fun source(relativePath: String): String {
        val candidates = listOf(
            Path.of(relativePath),
            Path.of("..").resolve(relativePath),
        )
        return Files.readString(candidates.first(Files::exists))
    }
}
