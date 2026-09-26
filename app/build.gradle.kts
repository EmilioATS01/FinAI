import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.ksp)
}

val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) load(f.inputStream())
}

android {
    namespace = "emilio.tolosa.finai"
    compileSdk = 37

    defaultConfig {
        applicationId = "emilio.tolosa.finai"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "OPENAI_KEY", "\"${localProps["OPENAI_KEY"] ?: ""}\"")
        buildConfigField("String", "EXCHANGE_KEY", "\"${localProps["EXCHANGE_KEY"] ?: ""}\"")
        buildConfigField("String", "BELVO_ID", "\"${localProps["BELVO_ID"] ?: ""}\"")
        buildConfigField("String", "BELVO_SECRET", "\"${localProps["BELVO_SECRET"] ?: ""}\"")
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)

    // Room: base de datos local
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // DataStore: preferencias/sesión
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // ViewModel + corrutinas en el ciclo de vida
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.fragment:fragment-ktx:1.8.5")

    // Retrofit: llamadas HTTP a las APIs
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Implementacion de biometricos
    implementation("androidx.biometric:biometric:1.1.0")


    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}