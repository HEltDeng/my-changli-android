package com.mychangli.app.core.web

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OfficialWebsiteMobileNavigationTest {

    @Test
    fun `webview mobile navigation uses the accepted full panel layout`() {
        listOf(
            "mychangli-webview-mobile-nav-fix",
            "position: fixed !important",
            "z-index: 100 !important",
            "flex-direction: column !important",
            "min-height: 48px !important",
            "align-items: center !important",
            "overflow-y: auto !important",
            "background: var(--white) !important",
            ".site-nav a[aria-current=\"page\"]",
        ).forEach { expected ->
            assertTrue("Missing mobile navigation rule: $expected", MOBILE_NAV_COMPATIBILITY_SCRIPT.contains(expected))
        }

        assertFalse(MOBILE_NAV_COMPATIBILITY_SCRIPT.contains("http://"))
        assertFalse(MOBILE_NAV_COMPATIBILITY_SCRIPT.contains("addJavascriptInterface"))
    }
}
