import java.net.URI

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp") version "2.0.21-1.0.28"
}

val projectPath = projectDir.toPath().toAbsolutePath().normalize()
val apiUrl = providers.gradleProperty("aquaflowApiUrl").orNull
val releaseStore = providers.environmentVariable("AQUAFLOW_RELEASE_KEYSTORE").orNull
val releaseStorePassword = providers.environmentVariable("AQUAFLOW_RELEASE_STORE_PASSWORD").orNull
val releaseAlias = providers.environmentVariable("AQUAFLOW_RELEASE_KEY_ALIAS").orNull
val releaseKeyPassword = providers.environmentVariable("AQUAFLOW_RELEASE_KEY_PASSWORD").orNull
val releaseSigning = listOf(releaseStore, releaseStorePassword, releaseAlias, releaseKeyPassword)
if (releaseSigning.any { it != null }) {
    require(releaseSigning.none { it.isNullOrBlank() }) { "Completa las cuatro variables AQUAFLOW_RELEASE_* para firmar la APK." }
}

fun apiConfig(url: String): String {
    val normalizada = url.trim().trimEnd('/')
    if (normalizada.isBlank()) return "\"\""
    val uri = URI(normalizada)
    require(uri.scheme in listOf("http", "https") && uri.host != null &&
        uri.userInfo == null && uri.query == null && uri.fragment == null) {
        "aquaflowApiUrl debe ser una URL HTTP(S) sin credenciales, consulta ni fragmento."
    }
    return "\"$normalizada/\""
}

if (projectPath.any { it.toString().equals("OneDrive", ignoreCase = true) }) {
    val projectKey = Integer.toHexString(rootProject.projectDir.absolutePath.hashCode())
    layout.buildDirectory.set(
        file("${System.getProperty("user.home")}/.gradle/aquachile-build/$projectKey/app")
    )
}

android {
    namespace = "com.duoc.aquaflow"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.duoc.aquaflow"
        minSdk = 24
        targetSdk = 35
        versionCode = 2
        versionName = "1.0.0-rc.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (releaseStore != null) {
            create("entrega") {
                storeFile = rootProject.file(releaseStore)
                storePassword = releaseStorePassword
                keyAlias = releaseAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        debug {
            buildConfigField("String", "API_URL", apiConfig(apiUrl ?: "http://10.0.2.2:8080"))
        }
        release {
            signingConfig = signingConfigs.findByName("entrega")
            buildConfigField("String", "API_URL", apiConfig(apiUrl?.takeIf { it.startsWith("https://") }.orEmpty()))
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    sourceSets.getByName("androidTest").assets.srcDir("$projectDir/schemas")
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    // Android base (versiones alineadas con Kotlin 2.0.21 y compileSdk 35)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")

    // Compose (fijado por el BOM)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation("androidx.compose.material:material-icons-extended")

    // Base de datos local (Room 2.7.0-alpha11)
    val roomVersion = "2.7.0-alpha11"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    // Pruebas unitarias
    testImplementation(libs.junit)
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    // Las pruebas de JVM no traen org.json (en el teléfono sí): se agrega para probar el codec de Room
    testImplementation("org.json:json:20240303")

    // Pruebas instrumentadas y herramientas de depuración
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
