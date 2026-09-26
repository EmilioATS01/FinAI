package emilio.tolosa.finai.biometrics

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat


object BiometricUtil {
    val TAG = this.javaClass.simpleName

    /**
     * Comprueba si el dispositivo tiene soporte biomÃ©trico
     */
    private fun hasBiometricCapability(context: Context): Int {
        val biometricManager = BiometricManager.from(context)
        return biometricManager.canAuthenticate()
    }

    /**
     * Comprueba si la autenticaciÃ³n biomÃ©trica (por ejemplo, huella dactilar) estÃ¡ configurada en el dispositivo
     */
    fun isBiometricReady(context: Context) = hasBiometricCapability(context) == BiometricManager.BIOMETRIC_SUCCESS

    /**
     * Prepara el cuadro de diÃ¡logo PromptInfo con la configuraciÃ³n proporcionada
     */
    private fun setBiometricPromptInfo(
        title: String,
        subtitle: String,
        description: String,
        allowDeviceCredential: Boolean
    ): BiometricPrompt.PromptInfo {
        val builder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setDescription(description)

        builder.apply {
            if (allowDeviceCredential) setDeviceCredentialAllowed(true)
            else setNegativeButtonText("Cancel")
        }

        return builder.build()
    }

    /**
     * Inicializa BiometricPrompt con los caller y callback handlers
     */
    private fun initBiometricPrompt(
        activity: AppCompatActivity,
        listener: BiometricAuthListener
    ): BiometricPrompt {
        // Attach calling Activity
        val executor = ContextCompat.getMainExecutor(activity)

        // Attach callback handlers
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                Log.e(TAG, "$errorCode :: $errString")
                listener.onBiometricAuthenticationError(errorCode, errString.toString())
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                Log.w(TAG, "Authentication failed for an unknown reason")
                listener.onBiometricAuthenticationError(0, "Authentication failed for an unknown reason")
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                listener.onBiometricAuthenticationSuccess(result)
            }
        }

        return BiometricPrompt(activity, executor, callback)
    }

    /**
     * Muestra un aviso biomÃ©trico con las configuraciones proporcionadas
     */
    fun showBiometricPrompt(
        title: String = "Biometric Authentication",
        subtitle: String = "Enter biometric credentials to login.",
        description: String = "Input your Fingerprint or FaceID!",
        activity: AppCompatActivity,
        listener: BiometricAuthListener,
        cryptoObject: BiometricPrompt.CryptoObject? = null,
        allowDeviceCredential: Boolean = false
    ) {
        // Prepare BiometricPrompt Dialog
        val promptInfo = setBiometricPromptInfo(
            title,
            subtitle,
            description,
            allowDeviceCredential
        )

        // Attach with caller and callback handler
        val biometricPrompt = initBiometricPrompt(activity, listener)

        // Authenticate with a CryptoObject if provided, otherwise default authentication
        biometricPrompt.apply {
            if (cryptoObject == null) authenticate(promptInfo)
            else authenticate(promptInfo, cryptoObject)
        }
    }

    /**
     * Navega a la pantalla de ConfiguraciÃ³n del dispositivo ConfiguraciÃ³n biomÃ©trica
     */
    fun lunchBiometricSettings(context: Context) {
        ActivityCompat.startActivity(
            context,
            Intent(Settings.ACTION_SECURITY_SETTINGS),
            null
        )
    }

}