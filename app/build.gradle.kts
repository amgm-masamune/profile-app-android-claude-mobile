plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// versionCode: エポックからの経過「分」。CIでもローカルでも同じ式なので単調増加し、ダウングレードしない。
val appVersionCode: Int = (System.currentTimeMillis() / 60000L).toInt()
// versionName: CIでは GITHUB_RUN_NUMBER、ローカルでは "local"
val appVersionName: String = "0.1." + (System.getenv("GITHUB_RUN_NUMBER") ?: "local")

android {
    namespace = "com.amgm.personallog"
    compileSdk = 35

    defaultConfig {
        // debug/release で同一(applicationIdSuffix は付けない)
        applicationId = "com.amgm.personallog"
        minSdk = 28
        targetSdk = 35
        versionCode = appVersionCode
        versionName = appVersionName
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // 更新エラー対策: debug 署名鍵をリポジトリ内の固定ファイルに固定する。
    // CIは毎回新しいランナーで ~/.android/debug.keystore を作り直すため、固定しないと
    // ビルドごとに署名が変わり「上書き更新できない」(INSTALL_FAILED_UPDATE_INCOMPATIBLE)になる。
    signingConfigs {
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
        }
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
    }
}

ksp {
    // DBスキーマJSONを app/schemas に出力(コミット対象)。version を上げたら Migration を必ず追加する。
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.datastore.preferences)

    testImplementation(libs.junit)
}
