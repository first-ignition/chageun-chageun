package ai.pleos.contest.app

import android.content.Context
import java.time.LocalDate

enum class Tone(val label: String, val sample: String) {
	TEACHER("친절한 선생님", "천천히, 존댓말로"),
	FRIEND("옆자리 친구", "편하게, 다정하게"),
	CALM("차분한 안내", "짧고 정확하게"),
}

/**
 * 목소리. type은 TTS setSpeakerType에 넘기는 화자 이름.
 * 에뮬레이터 TTS 서비스(AmbientAiService) 안의 한국어 화자 4명. 성별은 음높이로 확인(결정기록 D18).
 * seoyoon은 글레오가 쓰는 기본 목소리다.
 */
enum class VoiceKind(val label: String, val sub: String, val type: String) {
	SEOYOON("서윤", "여성 · 기본 목소리", "seoyoon"),
	HAEUN("하은", "여성 · 밝은 목소리", "haeun"),
	SANGHYEOK("상혁", "남성 · 또렷한 목소리", "sanghyeok"),
	EUNWOO("은우", "남성 · 낮은 목소리", "eunwoo"),
}

/** 기기 안에만 저장한다. 서버·계정 없음(개인정보를 밖으로 보내지 않는다). */
class Progress(context: Context) {

	private val prefs = context.applicationContext.getSharedPreferences("progress", Context.MODE_PRIVATE)

	fun doneDays(): Map<String, Long> =
		prefs.getStringSet(KEY_DONE, emptySet()).orEmpty().mapNotNull { e ->
			val (id, day) = e.split('@').takeIf { it.size == 2 } ?: return@mapNotNull null
			day.toLongOrNull()?.let { id to it }
		}.toMap()

	fun markDone(id: String, day: LocalDate) {
		val set = prefs.getStringSet(KEY_DONE, emptySet()).orEmpty().filterNot { it.startsWith("$id@") }.toMutableSet()
		set += "$id@${day.toEpochDay()}"
		prefs.edit().putStringSet(KEY_DONE, set).apply()
	}

	var todayId: String?
		get() = prefs.getString(KEY_TODAY, null)
		set(v) = prefs.edit().putString(KEY_TODAY, v).apply()

	var todayAssignedDay: Long
		get() = prefs.getLong(KEY_TODAY_DAY, -1)
		set(v) = prefs.edit().putLong(KEY_TODAY_DAY, v).apply()

	var tone: Tone
		get() = runCatching { Tone.valueOf(prefs.getString(KEY_TONE, null)!!) }.getOrDefault(Tone.TEACHER)
		set(v) = prefs.edit().putString(KEY_TONE, v.name).apply()

	var voice: VoiceKind
		get() = runCatching { VoiceKind.valueOf(prefs.getString(KEY_VOICE, null)!!) }.getOrDefault(VoiceKind.SEOYOON)
		set(v) = prefs.edit().putString(KEY_VOICE, v.name).apply()

	var carName: String
		get() = prefs.getString(KEY_CAR, null)?.takeIf { it.isNotBlank() } ?: DEFAULT_CAR_NAME
		set(v) = prefs.edit().putString(KEY_CAR, v.trim().take(MAX_CAR_NAME)).apply()

	var soundOn: Boolean
		get() = prefs.getBoolean(KEY_SOUND, true)
		set(v) = prefs.edit().putBoolean(KEY_SOUND, v).apply()

	var celebratedKm: Set<Int>
		get() = prefs.getStringSet(KEY_KM, emptySet()).orEmpty().mapNotNull { it.toIntOrNull() }.toSet()
		set(v) = prefs.edit().putStringSet(KEY_KM, v.map { it.toString() }.toSet()).apply()

	var owned: Set<Item>
		get() = prefs.getStringSet(KEY_OWNED, emptySet()).orEmpty().mapNotNull { Item.byName(it) }.toSet()
		set(v) = prefs.edit().putStringSet(KEY_OWNED, v.map { it.name }.toSet()).apply()

	var equipped: Map<Slot, Item>
		get() = Slot.entries.mapNotNull { s -> Item.byName(prefs.getString("$KEY_EQUIP${s.name}", null))?.let { s to it } }.toMap()
		set(v) = prefs.edit().apply { Slot.entries.forEach { s -> putString("$KEY_EQUIP${s.name}", v[s]?.name) } }.apply()

	fun reset() = prefs.edit().clear().apply()

	companion object {
		private const val KEY_DONE = "done"
		private const val KEY_TODAY = "today"
		private const val KEY_TODAY_DAY = "today_day"
		private const val KEY_TONE = "tone"
		private const val KEY_VOICE = "voice"
		private const val KEY_SOUND = "sound"
		private const val KEY_KM = "km"
		private const val KEY_CAR = "car_name"
		private const val KEY_OWNED = "owned"
		private const val KEY_EQUIP = "equip_"
		const val DEFAULT_CAR_NAME = "내 차"
		const val MAX_CAR_NAME = 8
	}
}
