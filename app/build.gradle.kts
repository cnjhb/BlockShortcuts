import java.io.File
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "asia.cnjhb.blockshortcuts"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "asia.cnjhb.blockshortcuts"
        minSdk = 33
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            // 优先使用 local.properties / 环境变量里的发布密钥；缺省回退到 debug 密钥，保证本地能直接出包
            val props = Properties()
            val local = rootProject.file("local.properties")
            if (local.exists()) {
                local.inputStream().use { props.load(it) }
            }
            val store = props.getProperty("RELEASE_STORE_FILE") ?: System.getenv("BS_STORE_FILE")
            val alias = props.getProperty("RELEASE_KEY_ALIAS") ?: System.getenv("BS_KEY_ALIAS")
            if (store != null && alias != null) {
                storeFile = file(store)
                storePassword = props.getProperty("RELEASE_STORE_PASSWORD") ?: System.getenv("BS_STORE_PASSWORD")
                keyAlias = alias
                keyPassword = props.getProperty("RELEASE_KEY_PASSWORD") ?: System.getenv("BS_KEY_PASSWORD")
            } else {
                val debugKeystore = File(System.getProperty("user.home"), ".android/debug.keystore")
                storeFile = debugKeystore
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    compileOnly(libs.xposed.api)
    implementation(libs.androidx.core)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}