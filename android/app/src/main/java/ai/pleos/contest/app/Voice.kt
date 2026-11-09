package ai.pleos.contest.app

import ai.pleos.playground.tts.TextToSpeech
import ai.pleos.playground.tts.listener.EventListener
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import ai.pleos.playground.tts.constant.ErrorType as TtsErrorType
import ai.pleos.playground.tts.constant.Mode as TtsMode
import ai.pleos.playground.tts.listener.OnInitializeListener as TtsInitListener

/**
 * 음성 안내 (Pleos TextToSpeech, ON_DEVICE — 앱 등록·네트워크 불필요, 결정기록 D8).
 * 순서(문서): 생성 → initialize → onSuccess 뒤 리스너 등록 → speak → stop → 리스너 해제 → release
 * 준비 전에 들어온 문장은 하나만 기억했다가 준비되면 읽는다.
 */
class Voice(context: Context) {

	private val app = context.applicationContext
	private var tts: TextToSpeech? = null
	private var listenerAdded = false
	private var waiting: String? = null
	private var speaker: String? = null
	/** SDK 콜백은 다른 스레드에서 온다. 상태는 메인 스레드에서만 만진다. */
	private val main = Handler(Looper.getMainLooper())

	private var ready = false

	private val events = object : EventListener {
		override fun onStart() {}
		override fun onReady() {}
		override fun onDone() {}
		override fun onStop() {}
		override fun onError(errorType: TtsErrorType) {
			Log.w(TAG, "TTS 오류: ${errorType.name}")
		}
		override fun onUpdatedRms(rms: Double) {}
	}

	fun initialize(speakerType: String?) {
		if (tts != null) return
		speaker = speakerType
		try {
			val t = TextToSpeech(app, TtsMode.ON_DEVICE)
			tts = t
			t.initialize(object : TtsInitListener {
				override fun onSuccess() = main.post {
					if (tts !== t) return@post // 그 사이 release됨
					runCatching { t.addEventListener(events); listenerAdded = true }
					applySpeaker()
					ready = true
					waiting?.let { waiting = null; speak(it) }
				}.let {}
				override fun onFailure(errorType: TtsErrorType) {
					Log.w(TAG, "TTS 초기화 실패: ${errorType.name}")
				}
			})
		} catch (t: Throwable) {
			Log.w(TAG, "TTS 생성 실패", t)
		}
	}

	/**
	 * 화자 이름(seoyoon·haeun·sanghyeok·eunwoo). 문서에 없는 값이라 에뮬레이터 TTS 서비스에서 찾았다(결정기록 D18).
	 * 없는 이름을 넣으면 소리 없이 AUDIO_ERROR가 난다.
	 */
	fun setSpeaker(type: String) {
		speaker = type
		if (ready) applySpeaker()
	}

	private fun applySpeaker() {
		val t = tts ?: return
		val type = speaker
		if (type.isNullOrBlank()) return
		runCatching { t.setSpeakerType(type) }.onFailure { Log.i(TAG, "setSpeakerType($type) 실패: ${it.message}") }
	}

	/** 앞 문장을 끊고 새 문장을 읽는다 */
	fun speak(text: String) {
		val t = tts
		if (t == null || !ready) {
			waiting = text
			return
		}
		runCatching { t.speak(text, true) }.onFailure { Log.w(TAG, "speak 실패", it) }
	}

	fun stop() {
		waiting = null
		runCatching { tts?.stop() }
	}

	fun release() {
		tts?.let { t ->
			runCatching { t.stop() }
			if (listenerAdded) runCatching { t.removeEventListener(events) }
			runCatching { t.release() }
		}
		tts = null
		listenerAdded = false
		ready = false
	}

	companion object {
		private const val TAG = "Voice"
	}
}
