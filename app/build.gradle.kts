import java.util.Properties

val secretsProps = Properties().apply{
    val secretsFile = rootProject.file("secrets.properties")
    if(secretsFile.exists()){
        load(secretsFile.inputStream())
    }
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("kotlin-kapt")
    id("com.google.gms.google-services")
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
}


android {
    namespace = "com.example.walking_hadang"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.walking_hadang"
        minSdk = 25
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "WALK_API_KEY", "\"${secretsProps["WALK_API_KEY"]}\"")
    }

    buildTypes {
        release {
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
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    viewBinding{
        enable=true
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {


    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation("androidx.preference:preference:1.2.1")
    implementation ("com.google.android.material:material:1.12.0")


    implementation("com.squareup.retrofit2:retrofit:2.0.0")
    implementation("com.google.code.gson:gson:2.8.9")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    implementation(platform("com.google.firebase:firebase-bom:34.0.0"))

    implementation("com.google.firebase:firebase-firestore-ktx:24.7.1")
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-messaging-ktx:23.4.1")

    implementation("androidx.multidex:multidex:2.0.1")
    implementation("com.google.android.gms:play-services-auth:21.3.0")

    implementation("androidx.emoji2:emoji2:1.3.0")
    implementation("androidx.emoji2:emoji2-bundled:1.3.0")


    //구글 map API 관련
    // Maps SDK for Android
    implementation("com.google.android.gms:play-services-maps:19.0.0")

    implementation("com.google.android.gms:play-services-location:17.0.0")

    implementation("com.kakao.sdk:v2-all:2.20.1") // 전체 모듈 설치, 2.11.0 버전부터 지원
    implementation("com.kakao.sdk:v2-user:2.20.1") // 카카오 로그인 API 모듈
    implementation("com.kakao.sdk:v2-share:2.20.1") // 카카오톡 공유 API 모듈
    implementation("com.kakao.sdk:v2-talk:2.20.1") // 카카오톡 채널, 카카오톡 소셜, 카카오톡 메시지 API 모듈
    implementation("com.kakao.sdk:v2-friend:2.20.1") // 피커 API 모듈
    implementation("com.kakao.sdk:v2-navi:2.20.1") // 카카오내비 API 모듈
    implementation("com.kakao.sdk:v2-cert:2.20.1") // 카카오톡 인증 서비스 API 모듈\\

    implementation("androidx.core:core-splashscreen:1.0.1")

    //캘린더
    implementation("com.kizitonwose.calendar:view:2.4.0")

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.2")

}

secrets{
    propertiesFileName = "secrets.properties"
    defaultPropertiesFileName = "local.defaults.properties"
}