import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.baselineprofile)
    id("kotlin-parcelize")
}
configurations.all {
    exclude(group = "org.jetbrains.kotlin", module = "kotlin-android-extensions-runtime")
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        FileInputStream(file).use { load(it) }
    }
}

val releaseSigningProperties = Properties()
val releaseSigningConfigFile = System.getenv("MY_CHANGLI_SIGNING_CONFIG")
    ?.trim()
    ?.takeIf(String::isNotEmpty)
    ?.let(::file)
releaseSigningConfigFile
    ?.takeIf { it.isFile }
    ?.inputStream()
    ?.use(releaseSigningProperties::load)

val releaseSigningKeys = listOf(
    "storeFile",
    "storePassword",
    "keyAlias",
    "keyPassword",
)
val hasReleaseSigningConfig = releaseSigningConfigFile?.isFile == true &&
    releaseSigningKeys.all { key -> releaseSigningProperties.getProperty(key).orEmpty().isNotBlank() }

val adsEnabled: Boolean = (
    localProperties.getProperty("ads.enabled")
        ?: System.getenv("ADS_ENABLED")
        ?: "false"
    ).trim().equals("true", ignoreCase = true)
val campusToolsApiBaseUrlDebug: String =
    localProperties.getProperty("campusTools.apiBaseUrl.debug")
        ?: System.getenv("MY_CHANGLI_CAMPUS_TOOLS_API_BASE_URL_DEBUG")
        ?: "http://127.0.0.1:8787/api/v1/"
val campusToolsApiBaseUrlRelease: String =
    localProperties.getProperty("campusTools.apiBaseUrl.release")
        ?: System.getenv("MY_CHANGLI_CAMPUS_TOOLS_API_BASE_URL_RELEASE")
        ?: "https://campus-tools.invalid/api/v1/"

// Closed school, map, sports, payment, and legacy community implementations are removed only
// from this V1 branch. The complete code remains on main and the protected baseline tag.

tasks.register("verifyReleaseEndpointSafety") {
    group = "verification"
    description = "Fails when a Release endpoint can reconnect to inherited production services."
    inputs.property(
        "releaseEndpoints",
        campusToolsApiBaseUrlRelease,
    )
    doLast {
        val releaseEndpoints = inputs.properties
            .getValue("releaseEndpoints")
            .toString()
            .lineSequence()
            .filter(String::isNotBlank)
            .toList()
        val forbiddenHosts = listOf(
            "api.planet.zhelearn.com",
            "api-dev.planet.zhelearn.com",
        )
        check(releaseEndpoints.none { endpoint ->
            forbiddenHosts.any { forbidden -> endpoint.contains(forbidden, ignoreCase = true) }
        }) { "Release endpoints must not use inherited Planet services" }
        check(releaseEndpoints.all { it.startsWith("https://") }) {
            "Release endpoints must use HTTPS or an HTTPS .invalid placeholder"
        }
    }
}

android {
    namespace = "com.mychangli.app"
    compileSdk = 36
    compileSdkMinor = 1
    buildFeatures {
        buildConfig = true
        compose = true
    }
    
    signingConfigs {
        if (hasReleaseSigningConfig) {
            create("release") {
                storeFile = file(releaseSigningProperties.getProperty("storeFile"))
                storePassword = releaseSigningProperties.getProperty("storePassword")
                keyAlias = releaseSigningProperties.getProperty("keyAlias")
                keyPassword = releaseSigningProperties.getProperty("keyPassword")
            }
        }
    }
    
    defaultConfig {
        applicationId = "com.mychangli.app"
        minSdk = 24
        targetSdk = 36
        versionCode = 32
        versionName = "1.0.0"

        ndk {
            // 设置支持的SO库架构
            abiFilters.add("arm64-v8a")
        }

        buildConfigField("boolean", "ADS_ENABLED", adsEnabled.toString())
        buildConfigField(
            "String",
            "CAMPUS_TOOLS_API_BASE_URL",
            "\"$campusToolsApiBaseUrlRelease\""
        )
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            buildConfigField(
                "String",
                "CAMPUS_TOOLS_API_BASE_URL",
                "\"$campusToolsApiBaseUrlDebug\""
            )
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            buildConfigField("boolean", "ADS_ENABLED", "false")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (hasReleaseSigningConfig) {
                signingConfig = signingConfigs.getByName("release")
            }
            buildConfigField(
                "String",
                "CAMPUS_TOOLS_API_BASE_URL",
                "\"$campusToolsApiBaseUrlRelease\""
            )
        }

        create("benchmark") {
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
            buildConfigField("boolean", "ADS_ENABLED", "false")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // benchmark 贴近线上环境，走 release 域名
            buildConfigField(
                "String",
                "CAMPUS_TOOLS_API_BASE_URL",
                "\"$campusToolsApiBaseUrlRelease\""
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
    viewBinding {
        enable = true
    }
    packaging {
        resources {
            // 排除重复的文件
            excludes += "mozilla/public-suffix-list.txt"
        }
    }
    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
    buildFeatures {
        viewBinding = true
    }
}

val verifyV1ForbiddenBinariesAbsent by tasks.registering {
    group = "verification"
    description = "Fails if an unverified local Android binary is present in the V1 worktree."
    val forbidden = listOf(
        layout.projectDirectory.file("libs/CSUSTDataGet-1.1.4-no-sensitive-logs.aar"),
        layout.projectDirectory.file("libs/Lite3DMap.jar"),
    )
    inputs.files(forbidden)
    doLast {
        val present = forbidden.map { it.asFile }.filter(File::exists)
        check(present.isEmpty()) {
            "V1 must build without unverified local binaries: ${present.joinToString()}"
        }
    }
}

tasks.named("preBuild").configure {
    dependsOn(verifyV1ForbiddenBinariesAbsent)
}

val validateReleaseSigning by tasks.registering {
    inputs.property("releaseSigningConfigured", hasReleaseSigningConfig)
    doLast {
        check(inputs.properties["releaseSigningConfigured"] == true) {
            "Release signing is not configured. Set MY_CHANGLI_SIGNING_CONFIG to a complete local properties file outside the repository."
        }
    }
}

tasks.matching { task ->
    task.name == "assembleRelease" ||
        task.name == "bundleRelease" ||
        task.name == "installRelease"
}.configureEach {
    dependsOn(validateReleaseSigning)
}

androidComponents {
    onVariants { variant ->
        variant.outputs.forEach { output ->
            val versionName = output.versionName.orNull ?: ""
            (output as? com.android.build.api.variant.impl.VariantOutputImpl)
                ?.outputFileName?.set("MyChangli_${variant.name}_v${versionName}.apk")
        }
    }
}
dependencies {
    implementation(libs.androidx.profileinstaller)
    implementation(libs.androidx.compose.ui.graphics)
    "baselineProfile"(project(":baselineprofile"))
    // leakcanary
    debugImplementation(libs.leakcanary.android)
    // Material Design
    //RxJava
    implementation(libs.rxjava)
    implementation(libs.rxandroid)
    implementation(libs.androidx.room.rxjava3)
    //Glide
    implementation(libs.glide)
    ksp(libs.glide.ksp)
    //MMKV
    implementation(libs.mmkv)
    // OkHttp
    implementation(libs.okhttp)
    implementation(libs.logging.interceptor)
    //Retrofit
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    // Gson
    implementation(libs.gson)
    //TimetableView
    implementation(libs.timetableview)
    //Room
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)
    // PhotoView
    implementation(libs.photoview)
    // ViewModel
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    // Activity KTX for viewModels()
    implementation(libs.androidx.activity.ktx)
    // Fragment KTX 提供了 viewModels() 扩展函数
    implementation(libs.androidx.fragment.ktx)
    //滚轮
    implementation(libs.common)
    implementation(libs.wheelpicker)
    //FlexboxLayout
    implementation(libs.flexbox)
    implementation(libs.statelayout)
    //lottie
    implementation(libs.lottie)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.process.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    testImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation(project(":CP_Common"))
    //CodeLocator
    debugImplementation(libs.codelocator.core)
    debugImplementation(libs.codelocator.lancet.all)
    // Coil for image loading
    implementation(libs.coil.compose)
    implementation(libs.androidx.constraintlayout.compose)
    implementation(libs.haze)
    implementation(libs.androidx.navigation3.runtime)
}
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

// Gradle 9 writes the forked test worker classpath through an argument file. On Windows that
// argument file is decoded with the system code page, so a project path containing Chinese
// characters becomes unreadable to the worker even though test discovery succeeds. Stage only
// the compiled test classes in an ASCII-safe temporary directory; production outputs stay put.
val stagedDebugUnitTestClasses = layout.dir(
    providers.systemProperty("java.io.tmpdir").map { temporaryRoot ->
        file("$temporaryRoot/my-changli/debug-unit-test-classes")
    }
)
val stageDebugUnitTestClasses by tasks.registering(Sync::class) {
    dependsOn(
        "compileDebugUnitTestKotlin",
        "compileDebugUnitTestJavaWithJavac",
        "bundleDebugClassesToRuntimeJar",
        ":CP_Common:bundleLibRuntimeToJarDebug"
    )
    from(layout.buildDirectory.dir(
        "intermediates/built_in_kotlinc/debugUnitTest/compileDebugUnitTestKotlin/classes"
    ))
    from(layout.buildDirectory.dir(
        "intermediates/javac/debugUnitTest/compileDebugUnitTestJavaWithJavac/classes"
    ))
    from(layout.buildDirectory.file(
        "intermediates/runtime_app_classes_jar/debug/bundleDebugClassesToRuntimeJar/classes.jar"
    )) {
        into("runtime")
        rename { "app-runtime.jar" }
    }
    from(project(":CP_Common").layout.buildDirectory.file(
        "intermediates/runtime_library_classes_jar/debug/bundleLibRuntimeToJarDebug/classes.jar"
    )) {
        into("runtime")
        rename { "cp-common-runtime.jar" }
    }
    into(stagedDebugUnitTestClasses)
}

afterEvaluate {
    tasks.withType<org.gradle.api.tasks.testing.Test>().configureEach {
        if (name == "testDebugUnitTest") {
            dependsOn(stageDebugUnitTestClasses)
            classpath = files(
                stagedDebugUnitTestClasses,
                stagedDebugUnitTestClasses.map { it.file("runtime/app-runtime.jar") },
                stagedDebugUnitTestClasses.map { it.file("runtime/cp-common-runtime.jar") },
                classpath
            )
        }
    }
}
