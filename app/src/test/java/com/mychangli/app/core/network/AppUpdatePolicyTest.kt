package com.mychangli.app.core.network

import com.mychangli.app.core.network.campustools.RemoteAppVersion
import com.mychangli.app.core.update.VersionCheckResult
import com.mychangli.app.core.update.VersionPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdatePolicyTest {
    private fun version(
        latest: Int,
        minimum: Int = 1,
        force: Boolean = false,
    ) = RemoteAppVersion(
        latest_version_code = latest,
        minimum_version_code = minimum,
        latest_version_name = "1.0.$latest",
        force_update = force,
    )

    @Test
    fun currentOrOlderServerVersionIsLatest() {
        assertEquals(
            VersionCheckResult.Latest,
            VersionPolicy.evaluate(10, updateAvailable = false, latest = version(10)),
        )
        assertEquals(
            VersionCheckResult.Latest,
            VersionPolicy.evaluate(10, updateAvailable = true, latest = version(9)),
        )
    }

    @Test
    fun newerVersionIsAvailableWithoutForcingNormalUpgrade() {
        val result = VersionPolicy.evaluate(10, true, version(11)) as VersionCheckResult.Available
        assertEquals(11, result.version.latest_version_code)
        assertFalse(result.mandatory)
    }

    @Test
    fun forceFlagOrMinimumVersionMarksLocalDemonstrationMandatory() {
        assertTrue((VersionPolicy.evaluate(10, true, version(11, force = true)) as VersionCheckResult.Available).mandatory)
        assertTrue((VersionPolicy.evaluate(10, true, version(11, minimum = 11)) as VersionCheckResult.Available).mandatory)
    }
}
