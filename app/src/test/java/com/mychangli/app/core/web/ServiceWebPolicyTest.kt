package com.mychangli.app.core.web

import org.junit.Assert.*
import org.junit.Test

class ServiceWebPolicyTest {
    @Test fun `external browser accepts web addresses but not executable or local schemes`() {
        assertTrue(ServiceWebPolicy.httpUrl("https://cjcx.neea.edu.cn/results"))
        listOf("javascript:alert(1)", "file:///data/private", "content://private", "data:text/plain,x", "https://user:secret@example.com", "https://", "").forEach {
            assertFalse(it, ServiceWebPolicy.httpUrl(it))
        }
    }
    @Test fun `back skips intermediate authentication and identical redirect entries`() {
        val urls = listOf("https://example.com/service", "https://example.com/login", "https://example.com/callback", "https://example.com/result", "https://example.com/result")
        assertEquals(0, ServiceWebPolicy.previousBusinessIndex(urls, 4))
        assertNull(ServiceWebPolicy.previousBusinessIndex(urls, 0))
    }
}
