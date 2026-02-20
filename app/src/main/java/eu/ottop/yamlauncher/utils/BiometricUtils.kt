package eu.ottop.yamlauncher.utils

import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import eu.ottop.yamlauncher.R
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties

class BiometricUtils(private val activity: FragmentActivity) {
    private lateinit var callbackSettings: CallbackSettings
    private val logger = Logger.getInstance(activity)

    // Alias for the key used to protect access to biometric-protected settings
    private val settingsKeyAlias = "YamLauncherSettingsKey"

    interface CallbackSettings {
        fun onAuthenticationSucceeded()
        fun onAuthenticationFailed()
        fun onAuthenticationError(errorCode: Int, errorMessage: CharSequence?)
    }

    private fun generateSecretKeyIfNeeded() {
        try {
            val keyStore = KeyStore.getInstance("AndroidKeyStore")
            keyStore.load(null)
            val existingKey = keyStore.getKey(settingsKeyAlias, null)
            if (existingKey != null) {
                return
            }
            val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                settingsKeyAlias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                .setUserAuthenticationRequired(true)
                .setInvalidatedByBiometricEnrollment(true)
                .build()
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                "AndroidKeyStore"
            )
            keyGenerator.init(keyGenParameterSpec)
            keyGenerator.generateKey()
            logger.i("BiometricUtils", "Generated new keystore key for biometric settings")
        } catch (e: Exception) {
            logger.e("BiometricUtils", "Failed to generate keystore key: ${e.message}")
        }
    }

    private fun getSecretKey(): SecretKey? {
        return try {
            val keyStore = KeyStore.getInstance("AndroidKeyStore")
            keyStore.load(null)
            keyStore.getKey(settingsKeyAlias, null) as? SecretKey
        } catch (e: Exception) {
            logger.e("BiometricUtils", "Failed to obtain keystore key: ${e.message}")
            null
        }
    }

    private fun getCipher(): Cipher? {
        return try {
            Cipher.getInstance(
                "${KeyProperties.KEY_ALGORITHM_AES}/" +
                    "${KeyProperties.BLOCK_MODE_CBC}/" +
                    KeyProperties.ENCRYPTION_PADDING_PKCS7
            )
        } catch (e: Exception) {
            logger.e("BiometricUtils", "Failed to get Cipher instance: ${e.message}")
            null
        }
    }

    fun startBiometricSettingsAuth(callbackApp: CallbackSettings) {
        this.callbackSettings = callbackApp

        // Ensure a keystore-backed key exists for protecting settings access
        generateSecretKeyIfNeeded()
        val secretKey = getSecretKey()
        val cipher = getCipher()

        if (secretKey == null || cipher == null) {
            logger.e("BiometricUtils", "Unable to start biometric auth due to missing crypto components")
            callbackSettings.onAuthenticationError(
                BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL,
                "Biometric cryptographic components not available"
            )
            return
        }

        try {
            // Initialize cipher; we simply perform an encryption in onAuthenticationSucceeded
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        } catch (e: Exception) {
            logger.e("BiometricUtils", "Failed to initialize cipher: ${e.message}")
            callbackSettings.onAuthenticationError(
                BiometricPrompt.ERROR_UNABLE_TO_PROCESS,
                "Unable to initialize biometric cipher"
            )
            return
        }

        val authenticationCallback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                logger.i("BiometricUtils", "Biometric authentication succeeded")
                try {
                    val cryptoObject = result.cryptoObject
                    val authCipher = cryptoObject?.cipher
                    if (authCipher == null) {
                        logger.e("BiometricUtils", "No cipher available from biometric result")
                        callbackSettings.onAuthenticationError(
                            BiometricPrompt.ERROR_UNABLE_TO_PROCESS,
                            "Biometric cipher not available"
                        )
                        return
                    }
                    // Perform a cryptographic operation that depends on the keystore key.
                    // We encrypt a fixed value; if this fails, authentication is treated as failed.
                    val testData = "settings_auth".toByteArray(Charsets.UTF_8)
                    authCipher.doFinal(testData)
                    callbackSettings.onAuthenticationSucceeded()
                } catch (e: Exception) {
                    logger.e("BiometricUtils", "Cryptographic operation failed after biometric auth: ${e.message}")
                    callbackSettings.onAuthenticationError(
                        BiometricPrompt.ERROR_UNABLE_TO_PROCESS,
                        "Biometric cryptographic operation failed"
                    )
                }
            }

            override fun onAuthenticationFailed() {
                logger.w("BiometricUtils", "Biometric authentication failed")
                callbackSettings.onAuthenticationFailed()
            }

            override fun onAuthenticationError(errorCode: Int, errorMessage: CharSequence) {
                logger.e("BiometricUtils", "Biometric authentication error: $errorMessage (code: $errorCode)")
                callbackSettings.onAuthenticationError(errorCode, errorMessage)
            }
        }

        val executor = ContextCompat.getMainExecutor(activity)
        val biometricPrompt = BiometricPrompt(activity, executor, authenticationCallback)

        val authenticators = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        } else {
            BiometricManager.Authenticators.BIOMETRIC_STRONG
        }
        val canAuthenticate =
            BiometricManager.from(activity).canAuthenticate(authenticators)

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(activity.getString(R.string.text_biometric_login))
            .setSubtitle(activity.getString(R.string.text_biometric_login_sub))
            .setAllowedAuthenticators(authenticators)
            .setConfirmationRequired(false)
            .build()

        if (canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS) {
            logger.i("BiometricUtils", "Starting biometric authentication")
            biometricPrompt.authenticate(
                BiometricPrompt.CryptoObject(cipher),
                promptInfo
            )
        } else {
            logger.w("BiometricUtils", "Biometric authentication not available: $canAuthenticate")
        }
    }
}
