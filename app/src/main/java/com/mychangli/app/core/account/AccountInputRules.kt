package com.mychangli.app.core.account

object AccountInputRules {
    private val mainlandMobile = Regex("^1[3-9]\\d{9}$")

    fun isValidPhone(value: String): Boolean = mainlandMobile.matches(value.trim())

    fun isValidPassword(value: String): Boolean = value.length in 8..200

    fun canSubmit(
        phone: String,
        password: String,
        nickname: String,
        registering: Boolean,
    ): Boolean = isValidPhone(phone) &&
        isValidPassword(password) &&
        (!registering || nickname.trim().isNotEmpty())
}
