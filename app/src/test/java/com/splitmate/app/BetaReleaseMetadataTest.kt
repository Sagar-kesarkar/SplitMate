package com.splitmate.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BetaReleaseMetadataTest {
    @Test
    fun betaIdentityAndExportFilenameAreStable() {
        assertEquals("com.splitmate.app", BuildConfig.APPLICATION_ID)
        assertEquals("1.1.0-beta.1", BuildConfig.VERSION_NAME)
        assertEquals(2, BuildConfig.VERSION_CODE)
        assertTrue(BuildConfig.VERSION_CODE > 1)
        assertEquals(
            "SplitMate-beta-v1.1.0-beta.1.apk",
            "SplitMate-beta-v${BuildConfig.VERSION_NAME}.apk"
        )
    }
}
