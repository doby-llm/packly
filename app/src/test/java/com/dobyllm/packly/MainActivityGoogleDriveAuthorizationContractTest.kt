package com.dobyllm.packly

import java.nio.file.Files
import java.nio.file.Path
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MainActivityGoogleDriveAuthorizationContractTest {
    @Test
    fun driveAuthorizationCallbackParsesNonNullDataBeforeConsideringResultOk() {
        val callback = driveAuthorizationCallbackSource()
        val parserCall = "authorizationClient.getAuthorizationResultFromIntent(data)"

        assertTrue(callback.contains("val data = result.data"))
        assertTrue(callback.contains(parserCall))
        assertFalse(
            "Parser invocation must not be hidden behind a non-RESULT_OK guard.",
            callback.substringBefore(parserCall).contains("result.resultCode != RESULT_OK"),
        )
        assertFalse(
            "Parser invocation must not be hidden behind a RESULT_OK-only guard.",
            callback.substringBefore(parserCall).contains("result.resultCode == RESULT_OK"),
        )
    }

    @Test
    fun driveAuthorizationCallbackKeepsNullCancellationAndMissingDataExplicit() {
        val callback = driveAuthorizationCallbackSource()

        assertTrue(callback.contains("data == null && result.resultCode == RESULT_CANCELED"))
        assertTrue(callback.contains("packlyViewModel.onGoogleDriveAuthorizationCancelled()"))
        assertTrue(callback.contains("if (data == null)"))
        assertTrue(callback.contains("packlyViewModel.onGoogleDriveAuthorizationDataMissing()"))
    }

    @Test
    fun driveAuthorizationCallbackKeepsScopeAndTokenChecksBeforeAuthorizedState() {
        val persistBlock = source("app/src/main/java/com/dobyllm/packly/MainActivity.kt")
            .substringAfter("private fun persistDriveAuthorizationResult")
            .substringBefore("\n    }\n}")
        val scopeCheck = "!granted -> packlyViewModel.onGoogleDriveAuthorizationScopeDenied()"
        val tokenCheck = "!tokenPresent -> packlyViewModel.onGoogleDriveAuthorizationBlankToken()"
        val authorizedState = "else -> packlyViewModel.onGoogleDriveAuthorized()"

        assertTrue(persistBlock.contains("val granted = result.grantedScopes.any { it == PACKLY_DRIVE_SCOPE }"))
        assertTrue(persistBlock.contains("val tokenPresent = !result.accessToken.isNullOrBlank()"))
        assertTrue(persistBlock.indexOf(scopeCheck) in 0 until persistBlock.indexOf(authorizedState))
        assertTrue(persistBlock.indexOf(tokenCheck) in 0 until persistBlock.indexOf(authorizedState))
    }

    private fun driveAuthorizationCallbackSource(): String = source("app/src/main/java/com/dobyllm/packly/MainActivity.kt")
        .substringAfter("private val driveAuthorizationLauncher")
        .substringBefore("override fun onCreate")

    private fun source(relativePath: String): String {
        val candidates = listOf(
            Path.of(relativePath),
            Path.of("..").resolve(relativePath),
        )
        return candidates.first(Files::exists).toFile().readText(Charsets.UTF_8)
    }
}
