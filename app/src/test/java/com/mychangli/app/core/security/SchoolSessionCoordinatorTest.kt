package com.mychangli.app.core.security

import com.mychangli.app.core.release.V1ReleasePolicy
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The historical coordinator tests become V1 school-credential boundary regressions. */
class SchoolSessionCoordinatorTest {
    @Test
    fun `school integration cannot be remotely or locally opened in V1`() {
        assertTrue(V1ReleasePolicy.isHardClosed("school_official_api"))
        assertTrue(V1ReleasePolicy.isHardClosed("school_credentials"))
        assertTrue(V1ReleasePolicy.isHardClosed("campus_map"))
    }

    @Test
    fun `school credential fields are rejected from own backend boundary`() {
        listOf(
            "student_id",
            "student_password",
            "school_password",
            "captcha",
            "school_cookie",
            "school_token",
        ).forEach { assertFalse(V1ReleasePolicy.maySendCredentialToOwnBackend(it)) }

        assertTrue(V1ReleasePolicy.maySendCredentialToOwnBackend("nickname"))
        assertTrue(V1ReleasePolicy.maySendCredentialToOwnBackend("installation_id"))
    }
}
