pluginManagement {
	repositories {
		google()
		mavenCentral()
		gradlePluginPortal()
	}
}

dependencyResolutionManagement {
	repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
	repositories {
		google()
		mavenCentral()
		// Pleos Connect SDK 공개 저장소 (인증 불필요)
		maven {
			url = uri("https://nexus-playground.pleos.ai/repository/maven-releases/")
			content { includeGroupByRegex("ai\\.pleos\\..*") }
		}
	}
}

rootProject.name = "PleosContest"

// 모듈 목록: probe = 차량 데이터 점검 앱, app = 공모전 본 앱
include(":app")
