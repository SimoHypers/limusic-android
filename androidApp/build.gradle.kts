import java.util.Properties

// The app's signing key lives outside the repo. local.properties names its properties file as
// limusic.signing (storeFile, storePassword, keyAlias, keyPassword). Without it, debug builds use
// the default debug key and release builds are unsigned.
val limusicSigning: Properties? =
    rootProject
        .file("local.properties")
        .takeIf { it.exists() }
        ?.let { f -> Properties().apply { f.inputStream().use { load(it) } } }
        ?.getProperty("limusic.signing")
        ?.let { File(it) }
        ?.takeIf { it.exists() }
        ?.let { f -> Properties().apply { f.inputStream().use { load(it) } } }

// version-name in libs.versions.toml is the only version. The code follows from it, so an RC always
// ranks below its release and Android never sees a downgrade: 1.2.3 -> 1020399, 1.2.3-rc.4 -> 1020304.
// Ceiling: minor and patch below 100, rc below 99.
fun versionCodeOf(name: String): Int {
    val m =
        Regex("""(\d+)\.(\d+)\.(\d+)(?:-rc\.(\d+))?""").matchEntire(name)
            ?: error("version-name must be X.Y.Z or X.Y.Z-rc.N, got $name")
    val (major, minor, patch, rc) = m.destructured
    require(minor.toInt() < 100 && patch.toInt() < 100 && (rc.toIntOrNull() ?: 0) < 99) { "version-name $name is past the versionCode ceiling" }
    return major.toInt() * 1_000_000 + minor.toInt() * 10_000 + patch.toInt() * 100 + (rc.toIntOrNull() ?: 99)
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

android {
    val abis = arrayOf("armeabi-v7a", "arm64-v8a", "x86_64")

    namespace = "com.maxrave.simpmusic"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.limusic.android"
        minSdk = 26
        targetSdk = 36
        versionName =
            libs.versions.version.name
                .get()
        versionCode = versionCodeOf(versionName!!)
        vectorDrawables.useSupportLibrary = true
        multiDexEnabled = true

        @Suppress("UnstableApiUsage")
        androidResources {
            localeFilters +=
                listOf(
                    "en",
                    "vi",
                    "it",
                    "de",
                    "ru",
                    "tr",
                    "fi",
                    "pl",
                    "pt",
                    "fr",
                    "es",
                    "zh-rCN",
                    "id",
                    "in",
                    "ar",
                    "ja",
                    "zh-rTW",
                    "uk",
                    "iw",
                    "az",
                    "hi",
                    "th",
                    "nl",
                    "ko",
                    "ca",
                    "fa",
                    "bg",
                    "sv",
                    "hr",
                )
        }
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters.add("x86_64")
            abiFilters.add("armeabi-v7a")
            abiFilters.add("arm64-v8a")
        }
    }

    signingConfigs {
        limusicSigning?.let { props ->
            create("limusic") {
                storeFile = file(props.getProperty("storeFile"))
                storePassword = props.getProperty("storePassword")
                keyAlias = props.getProperty("keyAlias")
                keyPassword = props.getProperty("keyPassword")
            }
        }
    }

    bundle {
        language {
            enableSplit = false
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("limusic")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            splits {
                abi {
                    isEnable = true
                    reset()
                    isUniversalApk = true
                    include(*abis)
                }
            }
        }
        debug {
            isMinifyEnabled = false
            // The id stays com.limusic.android in every build type: it is the installed app.
            signingConfig = signingConfigs.findByName("limusic") ?: signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    // enable view binding
    buildFeatures {
        viewBinding = true
        compose = true
        buildConfig = true
    }
    packaging {
        jniLibs.useLegacyPackaging = true
        jniLibs.excludes +=
            listOf(
                "META-INF/META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/license.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/notice.txt",
                "META-INF/ASL2.0",
                "META-INF/asm-license.txt",
                "META-INF/notice",
                "META-INF/*.kotlin_module",
            )
        resources {
            excludes += "META-INF/versions/9/OSGI-INF/MANIFEST.MF"
            // kuromoji ships the SAME META-INF/CONTRIBUTORS.md and META-INF/LICENSE.md in both
            // kuromoji-ipadic and the kuromoji-core it depends on, and mergeDebugJavaResource
            // refuses to pick between two files at one path. Excluded by pattern rather than by
            // name: dropping only CONTRIBUTORS.md moves the failure to LICENSE.md on the next
            // build. Nothing here is code — these are documentation files.
            excludes += "META-INF/*.md"
            // kuromoji's ipadic dictionary is ~13 MB of .bin resources — the whole reason the APK
            // grew by 14.4 MB. Android fetches the same eight files on demand instead (see
            // KuromojiDictionary in lyricsService androidMain), so the classpath copies are
            // dropped here. Desktop is untouched: its jars keep the resources and kuromoji's
            // default classpath resolver keeps working there.
            excludes += "com/atilika/kuromoji/ipadic/*.bin"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugaring)
    val debugImplementation = "debugImplementation"
    debugImplementation(libs.ui.tooling)
    implementation(libs.activity.compose)

    // Custom Activity On Crash
    implementation(libs.customactivityoncrash)

    // Easy Permissions
    implementation(libs.easypermissions)

    // Legacy Support
    implementation(libs.legacy.support.v4)
    // Coroutines
    implementation(libs.coroutines.android)


    implementation(projects.composeApp)
    implementation(projects.data)

    implementation(projects.crashlyticsEmpty)
}
