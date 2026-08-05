package com.mychangli.app.core.account

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountInputRulesTest {
    @Test
    fun phoneMustMatchSupportedMobileShape() {
        assertTrue(AccountInputRules.isValidPhone("18812345678"))
        assertFalse(AccountInputRules.isValidPhone("12812345678"))
        assertFalse(AccountInputRules.isValidPhone("1881234567"))
    }

    @Test
    fun registrationRequiresNicknameButLoginDoesNot() {
        assertFalse(
            AccountInputRules.canSubmit("18812345678", "safe-pass", "", registering = true)
        )
        assertTrue(
            AccountInputRules.canSubmit("18812345678", "safe-pass", "", registering = false)
        )
    }
}
