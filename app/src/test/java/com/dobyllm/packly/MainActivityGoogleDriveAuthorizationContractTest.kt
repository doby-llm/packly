package com.dobyllm.packly

import java.nio.file.Files
import java.nio.file.Path
import org.junit.Assert.assertEquals
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
        val parserCall = "authorizationClient.getAuthorizationResultFromIntent(data)"
        val cancellationCall = "packlyViewModel.onGoogleDriveAuthorizationCancelled()"

        assertTrue(callback.contains("data == null && result.resultCode == RESULT_CANCELED"))
        assertTrue(callback.contains(cancellationCall))
        assertTrue(callback.indexOf(cancellationCall) in 0 until callback.indexOf(parserCall))
        assertTrue(callback.contains("if (data == null)"))
        assertTrue(callback.contains("packlyViewModel.onGoogleDriveAuthorizationDataMissing()"))
    }

    @Test
    fun driveAuthorizationFailuresUseDistinctSafeStatusCodes() {
        val activitySource = source("app/src/main/java/com/dobyllm/packly/MainActivity.kt")
        val viewModelSource = source("app/src/main/java/com/dobyllm/packly/PacklyAppViewModel.kt")
        val callback = driveAuthorizationCallbackSource()
        val authorizeBlock = activitySource
            .substringAfter("authorizationClient.authorize(request)")
            .substringBefore("private fun persistDriveAuthorizationResult")

        assertTrue(activitySource.contains("import com.google.android.gms.common.api.ApiException"))
        assertFalse(activitySource.contains("import com.google.android.gms.common.api.CommonStatusCodes"))
        assertEquals(2, activitySource.lines().count { it.contains("(error as? ApiException)?.statusCode") })
        assertTrue(callback.contains("packlyViewModel.onGoogleDriveAuthorizationParserFailed(statusCode)"))
        assertFalse(callback.contains("CommonStatusCodes"))
        assertFalse(callback.contains("packlyViewModel.onGoogleDriveAuthorizationCancelled(statusCode)"))
        assertTrue(authorizeBlock.contains("packlyViewModel.onGoogleDriveAuthorizationRequestFailed(statusCode)"))
        assertFalse(callback.contains("error.message"))
        assertFalse(callback.contains("error.toString()"))
        assertFalse(authorizeBlock.contains("error.message"))
        assertFalse(authorizeBlock.contains("error.toString()"))

        assertTrue(viewModelSource.contains("fun onGoogleDriveAuthorizationParserFailed(statusCode: Int? = null)"))
        assertTrue(viewModelSource.contains("fun onGoogleDriveAuthorizationRequestFailed(statusCode: Int? = null)"))
        assertTrue(viewModelSource.contains("recordGoogleDriveAuthorizationFailure(\"authorization_parser_failed\", statusCode)"))
        assertTrue(viewModelSource.contains("recordGoogleDriveAuthorizationFailure(\"authorization_request_failed\", statusCode)"))
        assertTrue(viewModelSource.contains("\"\${baseCode}_status_\$it\""))
        assertTrue(viewModelSource.contains("lastError = code"))
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
