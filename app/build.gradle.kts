import com.android.tools.build.jetifier.core.type.PackageName

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.snuabar.mycomfy"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.snuabar.mycomfy"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "1.1.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            val defaultPackageName: String? = android.defaultConfig.applicationId
            buildConfigField("String", "defaultPackageName", "\"$defaultPackageName\"")
        }
        debug {
            resValue("string", "app_name", "MyComfy\n(DEBUG)")
            applicationIdSuffix = ".debug"
            // 获取特定 BuildType 的包名
            val defaultPackageName: String? = android.defaultConfig.applicationId
            buildConfigField("String", "defaultPackageName", "\"$defaultPackageName\"")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.documentfile)
    implementation(libs.legacy.support.v4)
    implementation(libs.recyclerview)
    implementation(libs.core.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation(libs.photoview)
    // Retrofit网络库
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.logging.interceptor)
    // 图片加载库
    implementation(libs.glide)
    // google MLKit 实时翻译
    implementation(libs.translate)
}