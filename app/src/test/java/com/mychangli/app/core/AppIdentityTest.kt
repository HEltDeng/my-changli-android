package com.mychangli.app.core

import com.mychangli.app.BuildConfig
import org.junit.Assert.assertEquals
import org.junit.Test

class AppIdentityTest {

    @Test
    fun buildUsesVariantAppropriateApplicationId() {
        val expected = if (BuildConfig.DEBUG) {
            "com.mychangli.app.debug"
        } else {
            "com.mychangli.app"
        }
        assertEquals(expected, BuildConfig.APPLICATION_ID)
    }
}
