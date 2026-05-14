plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.cloudmusicdemo"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.cloudmusicdemo"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    dependencies {
        implementation("com.squareup.retrofit2:retrofit:2.9.0")
        // Gson 解析器
        implementation("com.squareup.retrofit2:converter-gson:2.9.0")
        // OkHttp 网络客户端
        implementation("com.squareup.okhttp3:okhttp:4.10.0")
        // Glide 图片加载库
        implementation("com.squareup.okhttp3:logging-interceptor:4.10.0")
        implementation("com.github.bumptech.glide:glide:4.16.0")
        annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")
        // RecyclerView
        implementation("androidx.recyclerview:recyclerview:1.3.2")
        implementation(libs.appcompat)
        implementation(libs.material)
        implementation(libs.activity)
        implementation(libs.constraintlayout)
        testImplementation(libs.junit)
        androidTestImplementation(libs.ext.junit)
        androidTestImplementation(libs.espresso.core)
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
    }
}

