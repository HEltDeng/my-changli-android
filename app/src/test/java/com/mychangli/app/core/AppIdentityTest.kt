package com.mychangli.app.core

import com.mychangli.app.BuildConfig
import org.junit.Assert.assertEquals
import org.junit.Test

class AppIdentityTest {

    @Test
    fun debugBuildUsesIndependentApplicationId() {
        assertEquals("com.mychangli.app.debug", BuildConfig.APPLICATION_ID)
    }
}
