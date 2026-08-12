package com.mychangli.app.feature.common.compose_ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FunctionCatalogTest {

    @Test
    fun `campus study and life tools are present while exclusions stay absent`() {
        val primaryIds = primaryFunctionShortcuts().map(FunctionShortcut::id)
        val learningIds = learningFunctionShortcuts().map(FunctionShortcut::id)
        val lifeIds = lifeFunctionShortcuts().map(FunctionShortcut::id)

        assertFalse("map" in primaryIds)
        assertTrue(listOf("schedule", "grade", "classroom", "homework", "exam").all(primaryIds::contains))
        assertTrue(listOf("schedule", "grade", "classroom", "homework", "exam", "document", "cet", "mandarin").all(learningIds::contains))
        assertTrue(listOf("electric", "calendar", "lost_found").all(lifeIds::contains))
        assertTrue(sportsFunctionShortcuts().isEmpty())
    }

    @Test
    fun `lost found remains reachable`() {
        val lifeIds = lifeFunctionShortcuts().map(FunctionShortcut::id)

        assertTrue("lost_found" in lifeIds)
    }
}
