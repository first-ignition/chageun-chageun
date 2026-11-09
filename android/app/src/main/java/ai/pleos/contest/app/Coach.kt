package ai.pleos.contest.app

import ai.pleos.playground.vehicle.constant.control.VehicleGear
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import java.time.LocalDate
import kotlin.math.abs

sealed interface Moment {
	data class Learned(val mission: Mission, val count: Int, val evidence: String) : Moment
	data class Milestone(val km: Int) : Moment
}

/** 성공 판정은 차 신호로만 한다. 말로 했는지 손으로 했는지는 구분하지 않는다. */
class Coach(private val progress: Progress, private val voice: Voice) {

	val driving = mutableStateOf(false)
	val today = mutableStateOf(Missions.all.first())
	val done = mutableStateMapOf<String, Long>()
	val caughtWhileDriving = mutableStateOf<Moment.Learned?>(null)
	val moment = mutableStateOf<Moment?>(null)
	/** 시험·화면 촬영용(디버그 빌드만, MainActivity --es page) */
	val debugPage = mutableStateOf<String?>(null)
	val connected = mutableStateOf(false)
	val tone = mutableStateOf(progress.tone)
	val voiceKind = mutableStateOf(progress.voice)
	val soundOn = mutableStateOf(progress.soundOn)
	val carName = mutableStateOf(progress.carName)
	val owned = mutableStateOf(progress.owned)
	val equipped = mutableStateOf(progress.equipped)
	val watched = mutableStateOf<Any?>(null)
	val watchedSignal = mutableStateOf<Signal?>(null)

	private val values = HashMap<Signal, Any?>()
	private val firstSeenAt = HashMap<Signal, Long>()
	private var gearDriving = false
	private var uxRestricted = false
	private var introSpokenFor: String? = null
	/** 구독을 시작한 시각. 직후에 쏟아지는 초기값은 "바꿨다"로 세지 않는다. */
	private var watchStartedAt = Long.MAX_VALUE
	private val milestoneQueue = ArrayDeque<Int>()
	private var gearKnown = false
	private val main = Handler(Looper.getMainLooper())

	init {
		// 이름이 바뀌어 없어진 미션 기록은 세지 않는다
		done.putAll(progress.doneDays().filterKeys { Missions.byId(it) != null })
		today.value = pickToday()
	}

	fun refreshDay() {
		rollDay()
		// 기어를 아직 모르면 기다린다(첫 기어 값이 오면 maybeIntro가 불린다)
		if (gearKnown) maybeIntro()
	}

	private fun rollDay() {
		val before = today.value.id
		today.value = pickToday()
		if (today.value.id != before) introSpokenFor = null
	}

	val todayDone: Boolean get() = done.containsKey(today.value.id)
	val allDone: Boolean get() = Missions.all.all { done.containsKey(it.id) }
	val todayPending: Boolean get() = !todayDone && !allDone
	val learned: Int get() = Missions.all.count { done.containsKey(it.id) }
	val next: Mission? get() = Missions.all.firstOrNull { !done.containsKey(it.id) && it.id != today.value.id }

	/**
	 * 오늘의 미션: 저장된 것이 아직 안 끝났으면 그대로, 끝났어도 같은 날이면 그대로(오늘 몫 완료 화면),
	 * 날이 바뀌었으면 다음 미션.
	 */
	private fun pickToday(): Mission {
		val todayDay = LocalDate.now().toEpochDay()
		val saved = Missions.byId(progress.todayId)
		if (saved != null && (!done.containsKey(saved.id) || progress.todayAssignedDay == todayDay || done[saved.id] == todayDay)) return saved
		val m = Missions.all.firstOrNull { !done.containsKey(it.id) } ?: Missions.all.last()
		assign(m)
		return m
	}

	private fun assign(m: Mission) {
		progress.todayId = m.id
		progress.todayAssignedDay = LocalDate.now().toEpochDay()
		today.value = m
		caughtWhileDriving.value = null
		watched.value = values[watchSignal(m)]
		watchedSignal.value = watchSignal(m)
	}

	/** 통풍은 에뮬레이터에서 시트 온도(음수)로 온다 */
	private fun watchSignal(m: Mission): Signal {
		if (m.goal.signal != Signal.SEAT_VENT_DRIVER) return m.goal.signal
		val vent = (values[Signal.SEAT_VENT_DRIVER] as? Number)?.toInt()
		return if (vent == null || vent == 0) Signal.SEAT_HEAT_DRIVER else Signal.SEAT_VENT_DRIVER
	}

	fun choose(m: Mission) {
		assign(m)
		introSpokenFor = null
		speakIntro()
	}

	fun learnNext() {
		next?.let { choose(it) }
	}

	fun speakIntro() {
		if (driving.value) return
		introSpokenFor = today.value.id
		say(Talk.intro(today.value, tone.value, carName.value))
	}

	fun maybeIntro() {
		if (driving.value || todayDone || moment.value != null || introSpokenFor == today.value.id) return
		speakIntro()
	}

	fun dismissMoment() {
		moment.value = null
		main.removeCallbacks(delayedPraise)
		voice.stop()
		milestoneQueue.removeFirstOrNull()?.let { showMilestone(it) }
	}

	fun setTone(t: Tone) {
		tone.value = t
		progress.tone = t
		say(Talk.intro(today.value, t, carName.value))
	}

	fun setVoice(v: VoiceKind) {
		voiceKind.value = v
		progress.voice = v
		voice.setSpeaker(v.type)
		setSound(true)
		say(Talk.intro(today.value, tone.value, carName.value))
	}

	fun setCarName(name: String) {
		progress.carName = name
		carName.value = progress.carName
	}

	val points: Int get() = Points.balance(learned, allDone, owned.value)

	fun wear(item: Item): Boolean {
		if (item !in owned.value) {
			if (points < item.price) return false
			owned.value = owned.value + item
			progress.owned = owned.value
		}
		val now = equipped.value
		equipped.value = if (now[item.slot] == item) now - item.slot else now + (item.slot to item)
		progress.equipped = equipped.value
		return true
	}

	fun setSound(on: Boolean) {
		soundOn.value = on
		progress.soundOn = on
		if (!on) voice.stop()
	}

	fun resetAll() {
		progress.reset()
		done.clear()
		moment.value = null
		caughtWhileDriving.value = null
		milestoneQueue.clear()
		main.removeCallbacks(delayedPraise)
		tone.value = progress.tone
		voiceKind.value = progress.voice
		voice.setSpeaker(progress.voice.type)
		soundOn.value = progress.soundOn
		carName.value = progress.carName
		owned.value = progress.owned
		equipped.value = progress.equipped
		introSpokenFor = null
		val first = Missions.all.first()
		assign(first)
		// 이미 지난 이정표는 다시 축하하지 않도록 지금 거리로 기준을 다시 잡는다
		(values[Signal.ODOMETER] as? Number)?.let { initMilestones(it.toFloat()) }
	}

	/** 시험·화면 촬영용(디버그 빌드만): 18개 모두 익힌 상태로 */
	fun debugGraduate() {
		val today0 = LocalDate.now()
		Missions.all.forEachIndexed { i, m -> record(m, today0.minusDays((Missions.all.size - i).toLong())) }
	}

	/** 시험·화면 촬영용(디버그 빌드만): 주행거리 소식 카드 바로 띄우기 */
	fun debugMilestone(km: Int) { moment.value = Moment.Milestone(km) }

	fun debugAssignedYesterday() { progress.todayAssignedDay -= 1 }

	/**
	 * 시연용(디버그 빌드만): 말로 하는 미션 중 뒤쪽 n개를 지난 n일 동안 하나씩 익힌 것으로 채운다.
	 * 3분 시연에서 "하루 하나씩 쌓인다"를 보여 주려고. 뒤쪽에서 채워야 "하나 더 배우기"가 목록 순서대로 이어진다.
	 */
	fun debugPrefill(n: Int) {
		val today0 = LocalDate.now()
		Missions.all.filter { it.byVoice != null && it.id != today.value.id }.takeLast(n).forEachIndexed { i, m ->
			record(m, today0.minusDays((n - i).toLong()))
		}
	}

	private fun record(m: Mission, day: LocalDate) {
		progress.markDone(m.id, day)
		done[m.id] = day.toEpochDay()
	}

	fun close() {
		main.removeCallbacksAndMessages(null)
	}

	private fun say(text: String) {
		if (!soundOn.value || driving.value) return
		voice.speak(text)
	}

	fun onWatchStarted() {
		watchStartedAt = SystemClock.elapsedRealtime()
	}

	fun onDriveRestriction(restricted: Boolean) {
		uxRestricted = restricted
		updateDriving()
	}

	/** CarWatcher가 메인 스레드에서 부른다 */
	fun onValue(signal: Signal, v: Any?, fromCallback: Boolean) {
		if (v == null) return
		val known = values.containsKey(signal)
		if (!known) firstSeenAt[signal] = SystemClock.elapsedRealtime()
		val prev = values[signal]
		values[signal] = v
		connected.value = true
		if (signal == watchSignal(today.value)) {
			watched.value = v
			watchedSignal.value = signal
		}
		Log.d(TAG, "$signal ${if (fromCallback) "cb" else "get"} $prev -> $v")

		when (signal) {
			Signal.GEAR -> {
				gearDriving = v != VehicleGear.GEAR_PARK && v != VehicleGear.GEAR_UNKNOWN
				updateDriving()
				if (!gearKnown) {
					gearKnown = true
					maybeIntro()
				}
			}
			Signal.ODOMETER -> onOdometer((v as? Number)?.toFloat() ?: return)
			else -> {}
		}
		if (known && reached(today.value, signal, prev, v)) onReached(today.value)
	}

	private fun updateDriving() {
		val now = gearDriving || uxRestricted
		if (now == driving.value) return
		driving.value = now
		if (now) {
			main.removeCallbacks(delayedPraise)
			voice.stop()
			moment.value = null
		} else {
			val caught = caughtWhileDriving.value
			caughtWhileDriving.value = null
			rollDay()
			when {
				caught != null -> celebrate(caught)
				milestoneQueue.isNotEmpty() -> showMilestone(milestoneQueue.removeFirst())
				else -> maybeIntro()
			}
		}
	}

	private fun reached(m: Mission, signal: Signal, prev: Any?, v: Any?): Boolean {
		if (done.containsKey(m.id)) return false
		if (SystemClock.elapsedRealtime() - watchStartedAt < SETTLE_MS) return false
		if (!plausible(signal, prev)) return false
		// 처음 값이 들어온 직후 0에서 바뀐 것은 실제 조작인지 기본값을 채운 것인지 가릴 수 없어 세지 않는다.
		// (꺼진 크루즈·멈춘 바람처럼 0은 정상값이라, 시간이 지난 뒤의 0→값은 센다)
		if (m.goal.byValue && (prev as? Number)?.toFloat() == 0f &&
			SystemClock.elapsedRealtime() - (firstSeenAt[signal] ?: 0L) < ZERO_GUARD_MS) return false
		val g = m.goal
		// 에뮬레이터는 통풍을 시트 온도의 음수로 보낸다(가능한것_지도 2차 재측정)
		if (g.signal == Signal.SEAT_VENT_DRIVER && signal == Signal.SEAT_HEAT_DRIVER) {
			val a = (prev as? Number)?.toInt() ?: return false
			val b = (v as? Number)?.toInt() ?: return false
			return a >= 0 && b < 0
		}
		if (g.signal != signal) return false
		val a = (prev as? Number)?.toFloat()
		val b = (v as? Number)?.toFloat()
		return when (g) {
			is Goal.TurnsOn -> CarWatcher.isOn(prev) == false && CarWatcher.isOn(v) == true
			is Goal.Changes -> !same(prev, v)
			is Goal.Lowers -> a != null && b != null && b < a - 0.05f
			is Goal.Raises -> a != null && b != null && b > a + 0.05f
		}
	}

	/**
	 * 말이 안 되는 이전 값은 기준으로 쓰지 않는다. 에뮬레이터는 Virtual Console이 붙는 순간
	 * 온도를 0.0 → 22.0으로 보내는데, 이걸 "온도를 바꿨다"로 세면 안 된다.
	 */
	private fun plausible(signal: Signal, prev: Any?): Boolean = when (signal) {
		Signal.TEMP_DRIVER, Signal.TEMP_PASSENGER -> (prev as? Number)?.toFloat()?.let { it in 14f..34f } ?: false
		else -> true
	}

	private fun same(a: Any?, b: Any?): Boolean =
		if (a is Number && b is Number) abs(a.toFloat() - b.toFloat()) < 0.05f else a == b

	private fun onReached(m: Mission) {
		Log.i(TAG, "REACHED ${m.id} driving=${driving.value}")
		record(m, LocalDate.now())
		val card = Moment.Learned(m, done.size, evidenceNow(m, driving.value))
		if (driving.value) caughtWhileDriving.value = card else celebrate(card)
	}

	private fun evidenceNow(m: Mission, whileDriving: Boolean): String {
		val s = watchSignal(m)
		val t = java.time.LocalTime.now()
		val time = "${if (t.hour < 12) "오전" else "오후"} ${(t.hour + 11) % 12 + 1}:${"%02d".format(t.minute)}"
		return "${s.label} ${valueText(s, values[s])} · ${if (whileDriving) "주행 중 " else ""}$time"
	}

	/**
	 * 카드는 바로, 칭찬 말은 조금 뒤에. 글레오에게 말로 시켰다면 그 순간 글레오가 대답하는 중이라
	 * 바로 말하면 겹치거나 오디오 포커스를 못 받는다.
	 */
	private val delayedPraise = Runnable {
		val now = moment.value as? Moment.Learned ?: return@Runnable
		say(Talk.praise(now.mission, now.count, tone.value))
	}

	private fun celebrate(card: Moment.Learned) {
		moment.value = card
		main.removeCallbacks(delayedPraise)
		main.postDelayed(delayedPraise, PRAISE_DELAY_MS)
	}

	private fun onOdometer(km: Float) {
		// 0이나 연결 직후 값은 믿지 않는다(가짜 0 → 실제 값으로 튀면 이정표가 한꺼번에 터진다)
		if (km <= 0f || SystemClock.elapsedRealtime() - watchStartedAt < SETTLE_MS) return
		val celebrated = progress.celebratedKm
		if (INIT !in celebrated) {
			// 처음 읽은 거리보다 앞선 이정표는 조용히 지난 것으로 둔다
			initMilestones(km)
			return
		}
		val crossed = Milestones.kms.filter { km >= it && it !in celebrated }
		if (crossed.isEmpty()) return
		progress.celebratedKm = celebrated + crossed
		val top = crossed.max()
		if (driving.value || moment.value != null) milestoneQueue.addLast(top) else showMilestone(top)
	}

	private fun showMilestone(km: Int) {
		moment.value = Moment.Milestone(km)
		say(Talk.milestone(km, tone.value))
	}

	private fun initMilestones(km: Float) {
		progress.celebratedKm = setOf(INIT) + Milestones.kms.filter { km >= it }
	}

	companion object {
		fun valueText(s: Signal, v: Any?): String {
			if (v == null) return "확인 중"
			val n = v as? Number
			return when (s) {
				Signal.TEMP_DRIVER, Signal.TEMP_PASSENGER -> n?.let { "${"%.1f".format(it.toFloat())}도" }
				Signal.FAN_SPEED -> n?.let { "${it.toInt()}단" }
				Signal.CRUISE_TARGET -> n?.let { "%.0f".format(it.toFloat()) }
				Signal.AMBIENT_COLOR -> n?.let { "#%06X".format(it.toInt() and 0xFFFFFF) }
				Signal.SEAT_HEAT_DRIVER -> n?.toInt()?.let { if (it > 0) "열선 ${it}단" else if (it < 0) "통풍 ${-it}단" else "꺼짐" }
				Signal.WHEEL_HEAT, Signal.SEAT_VENT_DRIVER -> n?.toInt()?.let { if (it > 0) "켜짐 ${it}단" else "꺼짐" }
				else -> when (CarWatcher.isOn(v)) { true -> "켜짐"; false -> "꺼짐"; null -> null }
			} ?: v.toString()
		}

		private const val TAG = "Coach"
		private const val INIT = 0
		private const val SETTLE_MS = 2000L
		private const val PRAISE_DELAY_MS = 2500L
		private const val ZERO_GUARD_MS = 10_000L
	}
}
