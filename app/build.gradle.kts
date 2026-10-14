import java.io.FileInputStream
import java.util.Properties
import java.security.MessageDigest

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

// 历史兼容接口只允许 Debug 显式配置。Release 统一指向同域禁用哨兵，
// 并由客户端在网络栈前拦截，避免访问不存在的旧接口。
// 值需以 "/" 结尾（Retrofit BaseUrl 要求）。
val legacyDisabledBaseUrl = "https://api.mychangliapp.cn/api/v1/legacy-disabled/"
val planetApiBaseUrlDebug: String = localProperties.getProperty("planet.apiBaseUrl.debug")
    ?: System.getenv("PLANET_API_BASE_URL_DEBUG")
    ?: "https://planet-api-debug.invalid/v1/"
val planetApiBaseUrlRelease = legacyDisabledBaseUrl
val adsEnabled: Boolean = (
    localProperties.getProperty("ads.enabled")
        ?: System.getenv("ADS_ENABLED")
        ?: "false"
    ).trim().equals("true", ignoreCase = true)
val communityApiBaseUrlDebug: String = localProperties.getProperty("community.apiBaseUrl")
    ?: System.getenv("MY_CHANGLI_COMMUNITY_API_BASE_URL")
    ?: "https://community-api.invalid/app/"
val communityCommentsBaseUrlDebug: String = localProperties.getProperty("community.commentsBaseUrl")
    ?: System.getenv("MY_CHANGLI_COMMUNITY_COMMENTS_BASE_URL")
    ?: "https://community-api.invalid/app/fresh_news/"
val communityUserBaseUrlDebug: String = localProperties.getProperty("community.userBaseUrl")
    ?: System.getenv("MY_CHANGLI_COMMUNITY_USER_BASE_URL")
    ?: "https://community-api.invalid/users/"
val toolsApiBaseUrlDebug: String = localProperties.getProperty("tools.apiBaseUrl")
    ?: System.getenv("MY_CHANGLI_TOOLS_API_BASE_URL")
    ?: "https://tools-api.invalid/app/tools/"
val legacyToolsEnabledDebug = !toolsApiBaseUrlDebug.contains(".invalid", ignoreCase = true)
val campusToolsApiBaseUrlDebug: String =
    localProperties.getProperty("campusTools.apiBaseUrl.debug")
        ?: System.getenv("MY_CHANGLI_CAMPUS_TOOLS_API_BASE_URL_DEBUG")
        ?: "http://127.0.0.1:8787/api/v1/"
val campusToolsApiBaseUrlRelease = "https://api.mychangliapp.cn/api/v1/"
// An explicit local acceptance build uses adb reverse; never falls back to production.
val candidateApiBaseUrl = providers.gradleProperty("candidateApiBaseUrl").orNull
    ?: "http://127.0.0.1:8787/api/v1/"
check(candidateApiBaseUrl in setOf("http://127.0.0.1:8787/api/v1/", "https://candidate.mychangliapp.cn/api/v1/")) {
    "Candidate endpoint must be the isolated local or dedicated HTTPS service"
}

tasks.register("verifyReleaseEndpointSafety") {
    group = "verification"
    description = "Fails when a Release endpoint can reconnect to inherited production services."
    notCompatibleWithConfigurationCache("Release endpoint verification uses script-local validation.")
    inputs.property(
        "releaseEndpoints",
        listOf(
            planetApiBaseUrlRelease,
            campusToolsApiBaseUrlRelease,
            legacyDisabledBaseUrl,
        ).joinToString("\n"),
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
            "Release endpoints must use HTTPS"
        }
        check(releaseEndpoints.none { endpoint ->
            endpoint.contains(".invalid", ignoreCase = true) ||
                endpoint.contains("localhost", ignoreCase = true) ||
                endpoint.contains("127.0.0.1")
        }) {
            "Release endpoints must not contain placeholder or loopback hosts"
        }
        check(campusToolsApiBaseUrlRelease == "https://api.mychangliapp.cn/api/v1/") {
            "Release Campus Tools endpoint must use the accepted production API"
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
        // V1.0.4/36 is the immutable production baseline. V1.0.5/37 adds
        // announcement delivery, consent-flow, branding and widget-preview work.
        versionCode = 41
        versionName = "1.0.7"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            // Explicit local instrumentation override; never accepted for a release task.
            val testAbi = providers.gradleProperty("identityTestAbi").orNull
            check(testAbi == null || gradle.startParameter.taskNames.none {
                it.contains("release", ignoreCase = true) || it.contains("bundle", ignoreCase = true)
            }) { "identityTestAbi is only for isolated Debug instrumentation" }
            abiFilters.add(testAbi ?: "arm64-v8a")
        }

        // 兜底值：保证任何未显式覆盖的 variant 都拿得到 BaseUrl。
        // debug / release / benchmark 会在各自 buildType 内再覆盖为对应环境值。
        buildConfigField(
            "String",
            "PLANET_API_BASE_URL",
            "\"$planetApiBaseUrlRelease\""
        )
        buildConfigField("boolean", "ADS_ENABLED", adsEnabled.toString())
        buildConfigField("String", "COMMUNITY_API_BASE_URL", "\"$legacyDisabledBaseUrl\"")
        buildConfigField("String", "COMMUNITY_COMMENTS_BASE_URL", "\"$legacyDisabledBaseUrl\"")
        buildConfigField("String", "COMMUNITY_USER_BASE_URL", "\"$legacyDisabledBaseUrl\"")
        buildConfigField("String", "TOOLS_API_BASE_URL", "\"$legacyDisabledBaseUrl\"")
        buildConfigField(
            "String",
            "CAMPUS_TOOLS_API_BASE_URL",
            "\"$campusToolsApiBaseUrlRelease\""
        )
        buildConfigField("boolean", "RELEASE_USER_SURFACE", "false")
        buildConfigField("boolean", "LEGACY_SERVICES_ENABLED", "false")
        // New automatic consent expansion is still restricted to isolated acceptance builds.
        buildConfigField("boolean", "AUTOMATIC_SYNC_POLICY_ENABLED", "false")
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            buildConfigField("boolean", "AUTOMATIC_SYNC_POLICY_ENABLED", "true")
            buildConfigField(
                "String",
                "PLANET_API_BASE_URL",
                "\"$planetApiBaseUrlDebug\""
            )
            buildConfigField(
                "String",
                "CAMPUS_TOOLS_API_BASE_URL",
                "\"$campusToolsApiBaseUrlDebug\""
            )
            buildConfigField("String", "COMMUNITY_API_BASE_URL", "\"$communityApiBaseUrlDebug\"")
            buildConfigField("String", "COMMUNITY_COMMENTS_BASE_URL", "\"$communityCommentsBaseUrlDebug\"")
            buildConfigField("String", "COMMUNITY_USER_BASE_URL", "\"$communityUserBaseUrlDebug\"")
            buildConfigField("String", "TOOLS_API_BASE_URL", "\"$toolsApiBaseUrlDebug\"")
            buildConfigField("boolean", "LEGACY_SERVICES_ENABLED", legacyToolsEnabledDebug.toString())
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            buildConfigField("boolean", "ADS_ENABLED", "false")
            buildConfigField("boolean", "RELEASE_USER_SURFACE", "true")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (hasReleaseSigningConfig) {
                signingConfig = signingConfigs.getByName("release")
            }
            buildConfigField(
                "String",
                "PLANET_API_BASE_URL",
                "\"$planetApiBaseUrlRelease\""
            )
            buildConfigField(
                "String",
                "CAMPUS_TOOLS_API_BASE_URL",
                "\"$campusToolsApiBaseUrlRelease\""
            )
            buildConfigField("boolean", "LEGACY_SERVICES_ENABLED", "false")
        }

        create("candidate") {
            initWith(getByName("release"))
            buildConfigField("boolean", "AUTOMATIC_SYNC_POLICY_ENABLED", "true")
            applicationIdSuffix = ".candidate"
            versionNameSuffix = "-test.20261003.1"
            matchingFallbacks += listOf("release")
            buildConfigField("String", "CAMPUS_TOOLS_API_BASE_URL", "\"$candidateApiBaseUrl\"")
        }

        create("benchmark") {
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
            buildConfigField("boolean", "ADS_ENABLED", "false")
            buildConfigField("boolean", "RELEASE_USER_SURFACE", "true")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // benchmark 贴近线上环境，走 release 域名
            buildConfigField(
                "String",
                "PLANET_API_BASE_URL",
                "\"$planetApiBaseUrlRelease\""
            )
            buildConfigField(
                "String",
                "CAMPUS_TOOLS_API_BASE_URL",
                "\"$campusToolsApiBaseUrlRelease\""
            )
            buildConfigField("boolean", "LEGACY_SERVICES_ENABLED", "false")
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
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

val validateReleaseSigning by tasks.registering {
    inputs.property("releaseSigningConfigured", hasReleaseSigningConfig)
    doLast {
        check(inputs.properties["releaseSigningConfigured"] == true) {
            "Release signing is not configured. Set MY_CHANGLI_SIGNING_CONFIG to a complete local properties file outside the repository."
        }
    }
}

val verifyAuthorizedCsustDataGet by tasks.registering {
    group = "verification"
    description = "Verifies the rightsholder-authorized CSUSTDataGet security build before use."
    val artifact = layout.projectDirectory.file(
        "libs/CSUSTDataGet-1.1.4-no-sensitive-logs.aar"
    )
    inputs.file(artifact)
    doLast {
        val file = artifact.asFile
        check(file.isFile) {
            "Authorized CSUSTDataGet artifact is missing: ${file.absolutePath}"
        }
        val digest = MessageDigest.getInstance("SHA-256")
        val actual = file.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
            digest.digest().joinToString("") { byte -> "%02X".format(byte) }
        }
        check(actual == "9B03523FA04E9C8B3F5CAAAD49DC282B17EB2AC82E69A49E89764B21ABF71C29") {
            "Authorized CSUSTDataGet artifact SHA-256 mismatch"
        }
    }
}

tasks.named("preBuild").configure {
    dependsOn(verifyAuthorizedCsustDataGet)
}

tasks.matching { it.name == "preReleaseBuild" }.configureEach {
    dependsOn("verifyReleaseEndpointSafety")
}

tasks.matching { task ->
    task.name == "assembleCandidate" || task.name == "installCandidate" ||
    task.name == "assembleRelease" ||
        task.name == "bundleRelease" ||
        task.name == "installRelease"
}.configureEach {
    dependsOn(validateReleaseSigning)
}

androidComponents {
    beforeVariants(selector().withBuildType("release")) { variantBuilder ->
        (variantBuilder as com.android.build.api.variant.HasHostTestsBuilder)
            .hostTests[com.android.build.api.variant.HostTestBuilder.UNIT_TEST_TYPE]
            ?.enable = true
    }
    onVariants { variant ->
        variant.outputs.forEach { output ->
            val versionName = output.versionName.orNull ?: ""
            (output as? com.android.build.api.variant.impl.VariantOutputImpl)
                ?.outputFileName?.set("MyChangli_${variant.name}_v${versionName}.apk")
        }
    }
}
dependencies {
    implementation(libs.androidx.browser)
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
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
    implementation(libs.okhttp.urlconnection)
    //Retrofit
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.converter.scalars)
    // Gson
    implementation(libs.gson)
    //TimetableView
    implementation(libs.timetableview)
    //Room
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)
    // 图片裁剪库
    implementation(libs.ucrop)
    // PhotoView

    // SubsamplingScaleImageView
    implementation(libs.subsampling.scale.image.view)
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
    //lottie
    implementation(libs.lottie)
    // 缺省页
    implementation(libs.statelayout)
    // jsoup
    implementation(libs.jsoup)
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
    androidTestImplementation(libs.androidx.junit)
    testImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // workmanager
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.work.rxjava2)
    implementation(project(":CP_Common"))
    //CodeLocator
    debugImplementation(libs.codelocator.core)
    debugImplementation(libs.codelocator.lancet.all)
    // Coil for image loading
    implementation(libs.coil.compose)
    //csustDataGet
    // Local 1.1.4 security build: authentication and request behavior are unchanged;
    // dependency-internal logs that could expose credentials/cookies are disabled.
    implementation(files("libs/CSUSTDataGet-1.1.4-no-sensitive-logs.aar"))
//    implementation(libs.csustdataget)
//    implementation(files("libs/csust_spider-release.aar"))
    implementation(libs.androidx.constraintlayout.compose)
    implementation(libs.haze)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)

    // Campus map remains excluded from V1: no Lite3DMap/AMap dependency.
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
