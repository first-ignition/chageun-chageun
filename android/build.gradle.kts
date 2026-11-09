import com.android.build.api.dsl.ApplicationExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

plugins {
	alias(libs.plugins.android.application) apply false
	alias(libs.plugins.kotlin.android) apply false
	alias(libs.plugins.kotlin.compose) apply false
}

// ── 모든 앱 모듈(:probe, 나중의 :app) 공통 설정 ──────────────────────────
// 각 모듈의 build.gradle.kts에는 namespace·applicationId·의존성만 적는다.
subprojects {
	plugins.withId("com.android.application") {
		extensions.configure<ApplicationExtension> {
			compileSdk = libs.versions.compileSdk.get().toInt()
			defaultConfig {
				minSdk = libs.versions.minSdk.get().toInt()
				targetSdk = libs.versions.targetSdk.get().toInt()
			}
			// AAOS 표준 Car API(CarUxRestrictionsManager, CarPropertyManager)
			useLibrary("android.car")
			compileOptions {
				sourceCompatibility = JavaVersion.VERSION_17
				targetCompatibility = JavaVersion.VERSION_17
			}
			buildFeatures {
				compose = true
				buildConfig = true
			}
			buildTypes {
				getByName("release") {
					isMinifyEnabled = false
					proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
				}
			}
			packaging {
				resources.excludes += setOf("/META-INF/{AL2.0,LGPL2.1}", "/META-INF/*.kotlin_module")
			}
			// 릴리스 서명: 키는 저장소 밖(~/.pleos-keys/)에 두고 경로·비밀번호는 환경변수로 받는다.
			// (docs/05_개발/개발_가이드.md "릴리스 APK 만들기")
			val storePath = System.getenv("PLEOS_KEYSTORE")
			if (!storePath.isNullOrBlank()) {
				signingConfigs {
					create("release") {
						storeFile = file(storePath)
						storePassword = System.getenv("PLEOS_KEYSTORE_PASSWORD")
						keyAlias = System.getenv("PLEOS_KEY_ALIAS")
						keyPassword = System.getenv("PLEOS_KEY_PASSWORD")
					}
				}
				buildTypes.getByName("release").signingConfig = signingConfigs.getByName("release")
			}
		}
	}
	plugins.withId("org.jetbrains.kotlin.android") {
		extensions.configure<KotlinAndroidProjectExtension> {
			compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
		}
	}
}
