package com.hmx.harness.tools.mcp

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Authenticated encryption for the MCP server list.
 *
 * Both confidentiality (env values and static headers may carry tokens) and
 * integrity are provided by AES-GCM with a non-exportable AndroidKeyStore key.
 * Any failure is thrown to the caller so configuration loading fails closed;
 * this object never returns plaintext from an unverifiable blob.
 */
internal object McpConfigCrypto {

    private const val KEY_ALIAS = "mcp_config_encryption"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val IV_BYTES = 12
    private val MAGIC = byteArrayOf(0x48, 0x4d, 0x43, 0x50, 0x31) // "HMCP1"

    private val keyStore: KeyStore by lazy { KeyStore.getInstance("AndroidKeyStore").apply { load(null) } }

    fun isEncrypted(data: ByteArray): Boolean =
        data.size > MAGIC.size + IV_BYTES && data.copyOfRange(0, MAGIC.size).contentEquals(MAGIC)

    private fun key(): SecretKey {
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    fun encrypt(plain: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        return MAGIC + cipher.iv + cipher.doFinal(plain)
    }

    fun decrypt(blob: ByteArray): ByteArray {
        require(isEncrypted(blob)) { "Unsupported MCP configuration format." }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, blob, MAGIC.size, IV_BYTES))
        return cipher.doFinal(blob, MAGIC.size + IV_BYTES, blob.size - MAGIC.size - IV_BYTES)
    }
}
