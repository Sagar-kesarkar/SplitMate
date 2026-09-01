
import java.time.LocalDate

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
}

val betaVersion = "1.1.0-beta.1"
val betaVersionCode = 2

android {
  namespace = "com.splitmate.app"
  compileSdk = 36

  defaultConfig {
    applicationId = "com.splitmate.app"
    minSdk = 26
    targetSdk = 36
    versionCode = betaVersionCode
    versionName = betaVersion

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
    debug { }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  kotlinOptions {
    jvmTarget = "17"
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
  dependenciesInfo {
    includeInApk = false
    includeInBundle = true
  }
}

tasks.register("exportBetaApk") {
  group = "distribution"
  description = "Tests, builds, and exports the installable SplitMate internal beta APK."
  dependsOn("testDebugUnitTest", "assembleDebug")

  doLast {
    val sourceApk = layout.buildDirectory.file("outputs/apk/debug/app-debug.apk").get().asFile
    require(sourceApk.isFile) { "Debug APK was not produced: ${sourceApk.absolutePath}" }

    val releasesDirectory = rootProject.layout.projectDirectory.dir("releases").asFile
    val archiveDirectory = releasesDirectory.resolve("archive")
    archiveDirectory.mkdirs()

    val latestApk = releasesDirectory.resolve("SplitMate-beta-latest.apk")
    val versionedApk = archiveDirectory.resolve("SplitMate-beta-v$betaVersion.apk")
    sourceApk.copyTo(latestApk, overwrite = true)
    sourceApk.copyTo(versionedApk, overwrite = true)

    releasesDirectory.resolve("README.txt").writeText(
      """
      SplitMate Beta internal build

      Version: $betaVersion (code $betaVersionCode)
      APK: SplitMate-beta-v$betaVersion.apk
      Build date: ${LocalDate.now()}
      Signing: Android debug/internal-beta signing key (not release-signed)

      Install by opening the APK on an Android device and allowing your file-sharing app
      to install unknown apps when Android asks. For an attached developer device, use:
      adb install -r SplitMate-beta-latest.apk

      The APK can be shared through WhatsApp, Google Drive, email, USB, or another file
      transfer method. Long-term public updates require a stable private release keystore.
      Every future update must retain application ID com.splitmate.app and use the same
      signing key as the installed build.
      """.trimIndent() + System.lineSeparator()
    )

    logger.lifecycle("Exported ${latestApk.absolutePath}")
    logger.lifecycle("Archived ${versionedApk.absolutePath}")
  }
}

dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.material)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
}
