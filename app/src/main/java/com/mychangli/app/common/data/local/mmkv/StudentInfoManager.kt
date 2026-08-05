package com.mychangli.app.common.data.local.mmkv

import com.mychangli.app.core.security.CredentialCipher
import com.tencent.mmkv.MMKV

object StudentInfoManager {
    private val mmkv by lazy {
        MMKV.mmkvWithID(CACHE_ID).also { target ->
            migrateFromLegacyIfNeeded(target)
        }
    }

    private const val CACHE_ID = "stu_info_cache"
    private const val LEGACY_CACHE_ID = "import_cache"

    private const val KEY_STUDENT_ID = "student_id"
    private const val KEY_PASSWORD = "student_password"
    private const val KEY_PASSWORD_ENCRYPTED = "student_password_encrypted"

    internal class Credentials(
        val studentId: String,
        val studentPassword: String
    )

    private fun migrateFromLegacyIfNeeded(target: MMKV) {
        val currentPlainPassword = target.getString(KEY_PASSWORD, "") ?: ""
        val currentEncryptedPassword = target.getString(KEY_PASSWORD_ENCRYPTED, "") ?: ""
        if (currentPlainPassword.isNotEmpty()) {
            if (currentEncryptedPassword.isNotEmpty()) {
                target.removeValueForKey(KEY_PASSWORD)
            } else {
                writeEncryptedPassword(target, currentPlainPassword)
            }
        }

        val legacy = MMKV.mmkvWithID(LEGACY_CACHE_ID)
        val legacyStudentId = legacy.getString(KEY_STUDENT_ID, "") ?: ""
        val legacyPassword = legacy.getString(KEY_PASSWORD, "") ?: ""
        if (target.getString(KEY_STUDENT_ID, "").isNullOrEmpty() && legacyStudentId.isNotEmpty()) {
            target.putString(KEY_STUDENT_ID, legacyStudentId)
        }
        val hasCurrentPassword =
            !target.getString(KEY_PASSWORD_ENCRYPTED, "").isNullOrEmpty() ||
                !target.getString(KEY_PASSWORD, "").isNullOrEmpty()
        if (!hasCurrentPassword && legacyPassword.isNotEmpty()) {
            writeEncryptedPassword(target, legacyPassword)
        }
        if (legacyPassword.isNotEmpty()) {
            // Never retain the legacy plaintext copy. If encryption was unavailable, the app
            // deliberately falls back to an incomplete binding and asks the user to bind again.
            legacy.removeValueForKey(KEY_PASSWORD)
        }
        if (!target.getString(KEY_STUDENT_ID, "").isNullOrEmpty()) {
            legacy.removeValueForKey(KEY_STUDENT_ID)
        }
    }

    @get:Synchronized
    var studentId: String
        get() = mmkv.getString(KEY_STUDENT_ID, "") ?: ""
        private set(value) {
            mmkv.putString(KEY_STUDENT_ID, value)
        }

    @get:Synchronized
    var studentPassword: String
        get() {
            val encrypted = mmkv.getString(KEY_PASSWORD_ENCRYPTED, "") ?: ""
            if (encrypted.isEmpty()) {
                val plain = mmkv.getString(KEY_PASSWORD, "") ?: ""
                if (plain.isEmpty()) return ""
                return if (writeEncryptedPassword(mmkv, plain)) plain else ""
            }
            return runCatching { CredentialCipher.decrypt(encrypted) }
                .getOrDefault("")
        }
        private set(value) {
            writeEncryptedPassword(mmkv, value)
        }

    /** A binding is complete only when both persisted values can be read back successfully. */
    val hasCompleteBinding: Boolean
        get() = credentialsOrNull() != null

    @Synchronized
    internal fun credentialsOrNull(): Credentials? {
        val id = studentId.trim()
        if (id.isEmpty()) return null
        val password = studentPassword
        if (password.isEmpty()) return null
        return Credentials(id, password)
    }

    /**
     * Persists the two credential fields as one logical operation.
     *
     * MMKV does not provide transactions, so the encrypted payload is prepared first and the
     * previous values are restored if either write or the read-back verification fails.
     */
    @Synchronized
    fun saveCredentials(studentId: String, studentPassword: String): Boolean {
        val normalizedId = studentId.trim()
        if (normalizedId.isEmpty() || studentPassword.isEmpty()) return false

        val encryptedPassword = runCatching {
            CredentialCipher.encrypt(studentPassword)
        }.getOrNull()?.takeIf { it.isNotEmpty() } ?: return false

        val previousId = RawValue.read(mmkv, KEY_STUDENT_ID)
        val previousPlainPassword = RawValue.read(mmkv, KEY_PASSWORD)
        val previousEncryptedPassword = RawValue.read(mmkv, KEY_PASSWORD_ENCRYPTED)

        val passwordWritten = mmkv.encode(KEY_PASSWORD_ENCRYPTED, encryptedPassword)
        val idWritten = passwordWritten && mmkv.encode(KEY_STUDENT_ID, normalizedId)
        val verified = idWritten && runCatching {
            mmkv.decodeString(KEY_STUDENT_ID, "") == normalizedId &&
                CredentialCipher.decrypt(
                    mmkv.decodeString(KEY_PASSWORD_ENCRYPTED, "").orEmpty()
                ) == studentPassword
        }.getOrDefault(false)

        if (verified) {
            mmkv.removeValueForKey(KEY_PASSWORD)
            if (!mmkv.containsKey(KEY_PASSWORD)) return true
        }

        val idRestored = previousId.restore(mmkv, KEY_STUDENT_ID)
        val plainPasswordRestored = previousPlainPassword.restore(mmkv, KEY_PASSWORD)
        val encryptedPasswordRestored = previousEncryptedPassword.restore(
            mmkv,
            KEY_PASSWORD_ENCRYPTED
        )
        if (!idRestored || !plainPasswordRestored || !encryptedPasswordRestored) {
            // A partial credential pair must never be treated as a completed binding.
            mmkv.removeValuesForKeys(
                arrayOf(KEY_STUDENT_ID, KEY_PASSWORD, KEY_PASSWORD_ENCRYPTED)
            )
        }
        return false
    }

    private fun writeEncryptedPassword(target: MMKV, value: String): Boolean {
        if (value.isEmpty()) {
            target.removeValueForKey(KEY_PASSWORD)
            target.removeValueForKey(KEY_PASSWORD_ENCRYPTED)
            return true
        }
        val encrypted = runCatching { CredentialCipher.encrypt(value) }.getOrNull()
            ?: return false
        if (!target.encode(KEY_PASSWORD_ENCRYPTED, encrypted)) return false
        target.removeValueForKey(KEY_PASSWORD)
        return true
    }

    private data class RawValue(
        val exists: Boolean,
        val value: String
    ) {
        fun restore(target: MMKV, key: String): Boolean {
            return if (exists) {
                target.encode(key, value)
            } else {
                target.removeValueForKey(key)
                !target.containsKey(key)
            }
        }

        companion object {
            fun read(target: MMKV, key: String): RawValue = RawValue(
                exists = target.containsKey(key),
                value = target.decodeString(key, "").orEmpty()
            )
        }
    }

    @Synchronized
    fun clear() {
        studentId = ""
        studentPassword = ""
    }
}
