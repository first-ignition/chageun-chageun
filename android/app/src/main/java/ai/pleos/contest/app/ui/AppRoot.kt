package ai.pleos.contest.app.ui

import ai.pleos.contest.app.Coach
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

private enum class Page { Home, Words, Settings, Icons, Garage }

/** 뒤로 가기: 안쪽 화면이면 첫 화면으로, 첫 화면이면 MainActivity가 moveTaskToBack. */
@Composable
fun AppRoot(coach: Coach) {
	var page by remember { mutableStateOf(Page.Home) }
	val driving = coach.driving.value
	val moment = coach.moment.value

	LaunchedEffect(driving) { if (driving) page = Page.Home }
	LaunchedEffect(coach.debugPage.value) {
		Page.entries.firstOrNull { it.name.equals(coach.debugPage.value, ignoreCase = true) }?.let { page = it }
		coach.debugPage.value = null
	}

	BackHandler(enabled = !driving && (moment != null || page != Page.Home)) {
		if (moment != null) coach.dismissMoment() else page = Page.Home
	}

	AppTheme {
		FitToScreen {
			when {
				driving -> DrivingScreen(coach)
				moment != null -> MomentScreen(coach, moment, openGarage = { coach.dismissMoment(); page = Page.Garage })
				page == Page.Words -> WordsScreen(coach) { page = Page.Home }
				page == Page.Settings -> SettingsScreen(coach) { page = Page.Home }
				page == Page.Icons -> IconSheet()
				page == Page.Garage -> GarageScreen(coach) { page = Page.Home }
				else -> HomeScreen(coach, openWords = { page = Page.Words }, openSettings = { page = Page.Settings }, openGarage = { page = Page.Garage })
			}
		}
	}
}

/**
 * 화면이 작으면(12.9·14.6인치, 반쪽 창) 글자·간격·버튼을 같은 비율로 줄인다.
 * 기준: 17인치 앱 영역 높이 약 1,200dp, 좁은 화면 기준 폭 820dp.
 * 최소 85%까지만 줄여 버튼은 64dp 이상(Google 정차 화면 기준), 가장 작은 글자(24sp)도 20sp 이상을 지킨다.
 * 넘치는 내용은 화면마다 스크롤로 받는다.
 */
@Composable
private fun FitToScreen(content: @Composable () -> Unit) {
	// 창 크기는 설정값(반쪽 창이면 반쪽 크기)으로 본다. 키보드가 창을 줄여도 글자가 출렁이지 않게.
	val cfg = LocalConfiguration.current
	val scale = minOf(1f, cfg.screenHeightDp / FIT_HEIGHT_DP, cfg.screenWidthDp / FIT_WIDTH_DP).coerceAtLeast(MIN_SCALE)
	val d = LocalDensity.current
	Box(Modifier.fillMaxSize()) {
		// 시스템 글자 배율이 1보다 작아도 우리 글자는 줄이지 않는다(가장 작은 글자 20sp 이상 유지)
		CompositionLocalProvider(LocalDensity provides Density(d.density * scale, d.fontScale.coerceAtLeast(1f))) { content() }
	}
}

private const val FIT_HEIGHT_DP = 1150f
private const val FIT_WIDTH_DP = 820f
private const val MIN_SCALE = 0.85f
