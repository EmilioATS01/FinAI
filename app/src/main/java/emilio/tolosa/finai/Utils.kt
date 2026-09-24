package emilio.tolosa.finai

import java.security.MessageDigest
import java.text.NumberFormat
import java.util.Locale

fun String.sha256(): String =
    MessageDigest.getInstance("SHA-256")
        .digest(toByteArray())
        .joinToString("") { "%02x".format(it) }

fun Double.mx(): String =
    NumberFormat.getCurrencyInstance(
        Locale("es", "MX")
    ).format(this)