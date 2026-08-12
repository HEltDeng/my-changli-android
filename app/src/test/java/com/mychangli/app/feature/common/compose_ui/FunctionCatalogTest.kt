package com.mychangli.app.feature.common.compose_ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FunctionCatalogTest {

    @Test
    fun `removed tools are absent from user-facing shortcut groups`() {
        val primaryIds = primaryFunctionShortcuts().map(FunctionShortcut::id)
        val secondaryIds = secondaryFunctionShortcuts().map(FunctionShortcut::id)
        val lifeIds = lifeFunctionShortcuts().map(FunctionShortcut::id)

        assertFalse("map" in primaryIds)
        assertFalse("calendar" in primaryIds)
        assertFalse("electric" in lifeIds)
        assertFalse("sports" in lifeIds)
        assertFalse("account" in secondaryIds)
        assertFalse("account" in lifeIds)
    }

    @Test
    fun `retained life tools remain reachable`() {
        val lifeIds = lifeFunctionShortcuts().map(FunctionShortcut::id)

        assertTrue("lost_found" in lifeIds)
    }
}
