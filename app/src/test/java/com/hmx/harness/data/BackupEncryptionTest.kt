package com.hmx.harness.data

import org.junit.Assert.*
import org.junit.Test

class BackupEncryptionTest {
    private fun password() = "a long unique backup password".toCharArray()

    @Test fun roundTrip() {
        val plain = "private-api-key-and-settings".toByteArray()
        val encrypted = BackupEncryption.encrypt(plain, password())
        assertArrayEquals(plain, BackupEncryption.decrypt(encrypted, password()))
        assertFalse(encrypted.toString(Charsets.ISO_8859_1).contains("private-api-key"))
    }

    @Test fun randomSaltAndNonce() {
        val plain = "same data".toByteArray()
        assertFalse(BackupEncryption.encrypt(plain, password()).contentEquals(BackupEncryption.encrypt(plain, password())))
    }

    @Test fun wrongPasswordIsRejected() {
        val encrypted = BackupEncryption.encrypt(byteArrayOf(1, 2), password())
        assertThrows(IllegalArgumentException::class.java) { BackupEncryption.decrypt(encrypted, "wrong password here".toCharArray()) }
    }

    @Test fun tamperingIsRejected() {
        val encrypted = BackupEncryption.encrypt(byteArrayOf(1, 2), password())
        for (position in listOf(0, 5, 21, 33, encrypted.lastIndex)) {
            val altered = encrypted.copyOf()
            altered[position] = (altered[position].toInt() xor 1).toByte()
            assertThrows(IllegalArgumentException::class.java) { BackupEncryption.decrypt(altered, password()) }
        }
    }

    @Test fun truncationAndOversizeAreRejected() {
        assertThrows(IllegalArgumentException::class.java) { BackupEncryption.decrypt(ByteArray(20), password()) }
        assertThrows(IllegalArgumentException::class.java) { BackupEncryption.decrypt(ByteArray(BackupEncryption.MAX_BYTES + 1), password()) }
        assertThrows(IllegalArgumentException::class.java) { BackupEncryption.encrypt(ByteArray(BackupEncryption.MAX_BYTES), password()) }
    }

    @Test fun shortPasswordIsRejectedForNewBackups() {
        for (short in listOf("", "12345", "12345678901")) {
            assertThrows(IllegalArgumentException::class.java) { BackupEncryption.encrypt(byteArrayOf(1), short.toCharArray()) }
        }
    }

    @Test fun twelveCharacterPasswordRoundTrip() {
        val secret = "exactly-12!".toCharArray()
        val encrypted = BackupEncryption.encrypt(byteArrayOf(1, 2, 3), secret)
        assertArrayEquals(byteArrayOf(1, 2, 3), BackupEncryption.decrypt(encrypted, secret))
    }

    /** Reproduces the legacy format so old exports remain restorable. */
    private fun legacyEncrypt(plain: ByteArray, password: CharArray): ByteArray {
        val random = java.security.SecureRandom()
        val salt = ByteArray(16).also(random::nextBytes)
        val nonce = ByteArray(12).also(random::nextBytes)
        val header = byteArrayOf(72, 65, 72, 66, 1) + salt + nonce
        val spec = javax.crypto.spec.PBEKeySpec(password, salt, 600_000, 256)
        val key = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        val cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(javax.crypto.Cipher.ENCRYPT_MODE,
            javax.crypto.spec.SecretKeySpec(key, "AES"), javax.crypto.spec.GCMParameterSpec(128, nonce))
        cipher.updateAAD(header)
        return header + cipher.doFinal(plain)
    }

    @Test fun legacySixCharacterBackupStillDecrypts() {
        val legacy = legacyEncrypt("old settings".toByteArray(), "abc123".toCharArray())
        assertArrayEquals("old settings".toByteArray(), BackupEncryption.decrypt(legacy, "abc123".toCharArray()))
        assertThrows(IllegalArgumentException::class.java) {
            BackupEncryption.decrypt(legacy, "12345".toCharArray())
        }
    }
}
