import groovy.json.JsonSlurper
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

val googleServicesFile = file("google-services.json")
if (googleServicesFile.exists()) {
    pluginManager.apply("com.google.gms.google-services")
    pluginManager.apply("com.google.firebase.crashlytics")
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use(::load)
}

fun resolveWebClientId(): String {
    System.getenv("GOOGLE_WEB_CLIENT_ID")?.takeIf(String::isNotBlank)?.let { return it }
    localProperties.getProperty("GOOGLE_WEB_CLIENT_ID")?.takeIf(String::isNotBlank)?.let { return it }
    if (!googleServicesFile.exists()) return ""
    return runCatching {
        val root = JsonSlurper().parse(googleServicesFile) as Map<*, *>
        val clients = root["client"] as? List<*> ?: emptyList<Any>()
        clients.asSequence()
            .mapNotNull { (it as? Map<*, *>)?.get("oauth_client") as? List<*> }
            .flatten()
            .mapNotNull { it as? Map<*, *> }
            .firstOrNull { it["client_type"].toString() == "3" }
            ?.get("client_id")?.toString().orEmpty()
    }.getOrDefault("")
}

fun quoted(value: String) = "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
fun secret(name: String): String? = System.getenv(name) ?: localProperties.getProperty(name)
val releaseStoreFile = secret("RELEASE_STORE_FILE")
val canSignRelease = listOf(
    releaseStoreFile,
    secret("RELEASE_STORE_PASSWORD"),
    secret("RELEASE_KEY_ALIAS"),
    secret("RELEASE_KEY_PASSWORD"),
).all { !it.isNullOrBlank() }

android {
    namespace = "com.piyush.livetranslate"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.piyush.livetranslate"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", quoted(resolveWebClientId()))
    }

    signingConfigs {
        if (canSignRelease) {
            create("release") {
                storeFile = rootProject.file(requireNotNull(releaseStoreFile))
                storePassword = secret("RELEASE_STORE_PASSWORD")
                keyAlias = secret("RELEASE_KEY_ALIAS")
                keyPassword = secret("RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            versionNameSuffix = "-debug"
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            if (canSignRelease) signingConfig = signingConfigs.getByName("release")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures { compose = true; buildConfig = true }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    packaging { resources.excludes += setOf("/META-INF/{AL2.0,LGPL2.1}") }
}

kotlin { jvmToolchain(17) }

ksp { arg("dagger.fastInit", "enabled") }

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:common"))
    implementation(project(":core:database"))
    implementation(project(":core:ui"))
    implementation(project(":domain"))
    implementation(project(":data:auth"))
    implementation(project(":data:translation"))
    implementation(project(":feature:auth"))
    implementation(project(":feature:home"))
    implementation(project(":feature:overlay"))
    implementation(project(":feature:history"))
    implementation(project(":feature:settings"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.splashscreen)
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.hilt.work)
    implementation(libs.hilt.android)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.storage)
    ksp(libs.hilt.compiler)
    ksp(libs.androidx.hilt.compiler)
    debugImplementation(libs.androidx.compose.ui.tooling)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.espresso.core)
}
