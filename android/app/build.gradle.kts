// 공모전 본 앱. 공통 설정(SDK 수준, Compose, android.car, 서명)은 루트 build.gradle.kts에서 들어온다.
plugins {
	alias(libs.plugins.android.application)
	alias(libs.plugins.kotlin.android)
	alias(libs.plugins.kotlin.compose)
}

android {
	namespace = "ai.pleos.contest.app"
	defaultConfig {
		applicationId = "ai.pleos.contest.app"
		versionCode = 1
		versionName = "0.1.0"
	}
}

dependencies {
	implementation(libs.androidx.core.ktx)
	implementation(libs.androidx.activity.compose)
	implementation(libs.androidx.lifecycle.runtime.ktx)
	implementation(platform(libs.androidx.compose.bom))
	implementation(libs.bundles.compose)
	// 차량 상태 읽기 + 음성 안내만 쓴다 (위치·내비·음성 인식은 쓰지 않음)
	implementation(libs.pleos.vehicle)
	implementation(libs.pleos.tts)
}
