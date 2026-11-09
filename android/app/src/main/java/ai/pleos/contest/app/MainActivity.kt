package ai.pleos.contest.app

import ai.pleos.contest.app.ui.AppRoot
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent

/** Back: 첫 화면이면 앱을 끝내지 않고 moveTaskToBack(true) (Pleos 심사 필수 규칙). 안쪽 화면은 Compose BackHandler가 먼저 받는다. */
class MainActivity : ComponentActivity() {

	private val main = Handler(Looper.getMainLooper())
	private lateinit var progress: Progress
	private lateinit var voice: Voice
	private lateinit var coach: Coach
	private lateinit var car: CarWatcher
	private lateinit var drive: DriveState

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
			override fun handleOnBackPressed() {
				moveTaskToBack(true)
			}
		})

		progress = Progress(this)
		voice = Voice(this)
		coach = Coach(progress, voice)
		car = CarWatcher(this, coach::onValue)
		drive = DriveState(this, coach::onDriveRestriction)

		voice.initialize(progress.voice.type)
		car.initialize()
		drive.start()
		// Vehicle.initialize()는 연결 완료 콜백이 없어서, 조금 기다린 뒤 구독을 시작한다(결정기록 D11).
		main.postDelayed({ startWatching(1) }, FIRST_START_DELAY_MS)
		// 기어를 끝내 못 읽어도(차량 서비스 문제) 정차로 보고 소개는 한다
		main.postDelayed({ coach.maybeIntro() }, FIRST_START_DELAY_MS + INTRO_FALLBACK_MS)
		debugChoose(intent)

		setContent { AppRoot(coach) }
	}

	/**
	 * 차량 서비스가 늦게 붙으면 첫 구독이 조용히 실패한다(ERROR_SERVICE_CONNECTION_FAILURE).
	 * 값이 하나도 안 들어오면 몇 초 뒤 다시 구독한다(최대 3번).
	 */
	private fun startWatching(attempt: Int) {
		car.start()
		coach.onWatchStarted()
		if (attempt < MAX_WATCH_ATTEMPTS) {
			main.postDelayed({ if (!coach.connected.value) startWatching(attempt + 1) }, RETRY_DELAY_MS)
		}
	}

	override fun onResume() {
		super.onResume()
		coach.refreshDay()
	}

	override fun onNewIntent(intent: android.content.Intent) {
		super.onNewIntent(intent)
		debugChoose(intent)
	}

	/**
	 * 시험·촬영용(디버그 빌드만, tools/test-missions.py): adb shell am start ... [--ez reset true] [--es mission <id> [--ei prefill n]]
	 * [--es page words|settings|home|icons|garage] [--ez graduate true] [--ei milestone km] [--ez yesterday true] [--es wear BOX,SAGE]
	 * wear는 실제와 같이 포인트로 사서 단다.
	 */
	private fun debugChoose(intent: android.content.Intent?) {
		if (!BuildConfig.DEBUG) return
		if (intent?.getBooleanExtra("reset", false) == true) coach.resetAll()
		intent?.getStringExtra("page")?.let { coach.debugPage.value = it }
		if (intent?.getBooleanExtra("graduate", false) == true) coach.debugGraduate()
		intent?.getIntExtra("milestone", 0)?.takeIf { it > 0 }?.let { coach.debugMilestone(it) }
		intent?.getStringExtra("mission")?.let { id ->
			Missions.byId(id)?.let { coach.choose(it) }
			intent.getIntExtra("prefill", 0).takeIf { it > 0 }?.let { coach.debugPrefill(it) }
		}
		if (intent?.getBooleanExtra("yesterday", false) == true) coach.debugAssignedYesterday()
		intent?.getStringExtra("wear")?.split(',')?.mapNotNull { Item.byName(it.trim()) }?.forEach { coach.wear(it) }
	}

	override fun onDestroy() {
		super.onDestroy()
		main.removeCallbacksAndMessages(null)
		coach.close()
		car.stop()
		drive.stop()
		voice.release()
		releaseCarWhenIdle(System.currentTimeMillis())
	}

	/** 비동기 조회가 끝나기 전에 release하면 안 된다(문서) → 남은 조회가 0이거나 3초가 지나면 release */
	private fun releaseCarWhenIdle(startedAt: Long) {
		if (car.pending == 0 || System.currentTimeMillis() - startedAt > RELEASE_TIMEOUT_MS) {
			car.release()
		} else {
			Handler(Looper.getMainLooper()).postDelayed({ releaseCarWhenIdle(startedAt) }, 200)
		}
	}

	companion object {
		private const val FIRST_START_DELAY_MS = 1500L
		private const val RELEASE_TIMEOUT_MS = 3000L
		private const val INTRO_FALLBACK_MS = 3000L
		private const val RETRY_DELAY_MS = 5000L
		private const val MAX_WATCH_ATTEMPTS = 3
	}
}
