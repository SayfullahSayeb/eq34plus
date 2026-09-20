plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
}

android {
  namespace = "com.hoco.eq34"
  compileSdk = 36

  defaultConfig {
    applicationId = "com.hoco.eq34"
    minSdk = 24
    targetSdk = 36
    versionCode = 7
    versionName = "1.0.8"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    create("release") {
      val ksEnv = System.getenv("RELEASE_KEYSTORE")
      if (ksEnv != null) {
        storeFile = file("release.keystore")
        storePassword = System.getenv("RELEASE_KEYSTORE_PASSWORD") ?: ""
        keyAlias = System.getenv("RELEASE_KEY_ALIAS") ?: "eq34plus"
        keyPassword = System.getenv("RELEASE_KEY_PASSWORD") ?: ""
      } else {
        val ksFile = file("release.keystore")
        if (ksFile.exists()) {
          storeFile = ksFile
          storePassword = "eq34plus2024"
          keyAlias = "eq34plus"
          keyPassword = "eq34plus2024"
        }
      }
    }
  }

  buildTypes {
    debug {
      isMinifyEnabled = false
    }
    release {
      isCrunchPngs = false
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      val ksEnv = System.getenv("RELEASE_KEYSTORE")
      if (ksEnv != null || file("release.keystore").exists()) {
        signingConfig = signingConfigs.getByName("release")
      }
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  dependenciesInfo {
    includeInApk = false
    includeInBundle = false
  }
}

dependencies {
  implementation(files("libs/jl_bluetooth_rcsp_V4.2.0_40250-release.aar"))
  implementation(libs.gson)
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)

  debugImplementation(libs.androidx.compose.ui.tooling)
}
