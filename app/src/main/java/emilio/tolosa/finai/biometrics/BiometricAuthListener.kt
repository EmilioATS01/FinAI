package emilio.tolosa.finai.biometrics

/**
 * Interfaz
 */
interface BiometricAuthListener {
    fun onBiometricAuthenticationSuccess(result: androidx.biometric.BiometricPrompt.AuthenticationResult)
    fun onBiometricAuthenticationError(errorCode: Int, errorMessage: String)
}