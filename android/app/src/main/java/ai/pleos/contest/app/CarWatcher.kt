package ai.pleos.contest.app

import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.area.SeatArea
import ai.pleos.playground.vehicle.api.model.area.WindowArea
import ai.pleos.playground.vehicle.constant.area.SeatValue
import ai.pleos.playground.vehicle.constant.area.TurnSignalValue
import ai.pleos.playground.vehicle.constant.area.WindowValue
import ai.pleos.playground.vehicle.constant.control.LightState
import ai.pleos.playground.vehicle.constant.control.VehicleGear
import ai.pleos.playground.vehicle.constant.control.WiperState
import ai.pleos.playground.vehicle.constant.unit.TemperatureUnit
import ai.pleos.playground.vehicle.listener.*
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

/** 쓰기(set) API는 하나도 부르지 않는다. 차를 바꾸지 않고 사용자가 바꾼 것을 알아채기만 하는 앱이다. */
class CarWatcher(context: Context, private val onValue: (Signal, Any?, Boolean) -> Unit) {

	private val vehicle = Vehicle(context.applicationContext)
	private val main = Handler(Looper.getMainLooper())
	private val unregisters = CopyOnWriteArrayList<() -> Unit>()

	@Volatile private var initialized = false

	/** start()/stop()마다 올린다. 지원 영역 응답이 늦게 와도 지난 세대면 등록하지 않는다(늦은 등록·중복 등록 방지). */
	@Volatile private var generation = 0

	/** 진행 중인 비동기 조회 수. 0일 때만 release한다(SDK 문서). 콜백은 다른 스레드에서 온다. */
	private val pendingCount = AtomicInteger(0)
	val pending: Int get() = pendingCount.get()

	fun initialize() {
		try {
			vehicle.initialize()
			initialized = true
		} catch (t: Throwable) {
			Log.w(TAG, "Vehicle.initialize 실패", t)
		}
	}

	/** initialize 뒤 서비스 연결 시간을 조금 준 다음 부른다(결정기록 D11). */
	fun start() {
		if (!initialized) return
		stop()
		generation++
		safe("hvac") { hvac() }
		safe("seat") { seat() }
		safe("wheel") { wheel() }
		safe("light") { light() }
		safe("turn") { turnSignal() }
		safe("wiper") { wiper() }
		safe("drive") { drive() }
	}

	fun stop() {
		generation++
		unregisters.forEach { runCatching { it() } }
		unregisters.clear()
	}

	fun release() {
		if (!initialized) return
		runCatching { vehicle.release() }.onFailure { Log.w(TAG, "release 실패", it) }
		initialized = false
	}

	private fun safe(name: String, block: () -> Unit) {
		try { block() } catch (t: Throwable) { Log.w(TAG, "$name 등록 실패", t) }
	}

	private fun post(s: Signal, v: Any?, fromCallback: Boolean) {
		main.post { onValue(s, v, fromCallback) }
	}

	private fun <T> get(s: Signal, call: (ok: (T?) -> Unit, fail: (Exception) -> Unit) -> Unit) {
		pendingCount.incrementAndGet()
		try {
			call({ v -> pendingCount.decrementAndGet(); post(s, v, false) }, { e -> pendingCount.decrementAndGet(); Log.i(TAG, "get $s: ${e.message}") })
		} catch (t: Throwable) {
			pendingCount.decrementAndGet()
			Log.i(TAG, "get $s 예외: ${t.message}")
		}
	}

	private fun reg(s: Signal, register: () -> Unit, unregister: () -> Unit) {
		try {
			register()
			unregisters += unregister
		} catch (t: Throwable) {
			Log.i(TAG, "reg $s 예외: ${t.message}")
		}
	}

	/** VehicleListener는 sealed라 직접 구현할 수 없어서, 같은 시그니처의 onFailed를 물려준다(Probe와 같은 방식). */
	private abstract inner class L(val s: Signal) {
		fun onFailed(e: Exception) { Log.i(TAG, "cb $s: ${e.message}") }
		fun on(v: Any?) = post(s, v, true)
	}

	/**
	 * 기능마다 차가 받는 영역이 다르다(예: 공조 오토는 앞좌석 묶음, 에어컨은 전 좌석 묶음, 온도는 좌석마다).
	 * 그래서 먼저 지원 영역(capability)을 묻고, 운전석이 들어 있는 영역 → 첫 영역 → 기본값 순으로 고른다.
	 * 운전석 영역(0x1)을 그냥 넣으면 "area ID not supported"로 조용히 실패한다(2026-10-27 확인).
	 */
	private fun withArea(
		s: Signal,
		fallback: SeatArea,
		check: (ok: (Any?, Any?) -> Unit, fail: (Exception) -> Unit) -> Unit,
		use: (SeatArea) -> Unit,
	) {
		val pick: (Any?) -> SeatArea = { x ->
			val found = when (x) {
				is Map<*, *> -> x.keys.filterIsInstance<SeatArea>()
				is Collection<*> -> x.filterIsInstance<SeatArea>()
				else -> emptyList()
			}
			found.firstOrNull { it.values?.contains(SeatValue.ROW_1_LEFT) == true } ?: found.firstOrNull() ?: fallback
		}
		val gen = generation
		val useIfCurrent: (SeatArea) -> Unit = { a -> if (gen == generation && initialized) safe(s.name) { use(a) } }
		pendingCount.incrementAndGet()
		try {
			check(
				{ _, x -> pendingCount.decrementAndGet(); val a = pick(x); main.post { useIfCurrent(a) } },
				{ e -> pendingCount.decrementAndGet(); Log.i(TAG, "cap $s: ${e.message}"); main.post { useIfCurrent(fallback) } },
			)
		} catch (t: Throwable) {
			pendingCount.decrementAndGet()
			Log.i(TAG, "cap $s 예외: ${t.message}")
			useIfCurrent(fallback)
		}
	}

	private fun hvac() {
		val api = vehicle.getHVAC()
		mapOf(Signal.FRONT_DEFROST to FRONT_GLASS, Signal.REAR_DEFROST to REAR_GLASS).forEach { (s, a) ->
			get<Boolean>(s) { ok, f -> api.getHvacDefrosterState(a, ok, f) }
			val l = object : L(s), HvacDefrosterStateListener { override fun onHvacDefrosterStateUpdated(isOn: Boolean?) = on(isOn) }
			reg(s, { api.registerHvacDefrosterState(a, l) }) { api.unregisterHvacDefrosterState(a, l) }
		}
		Signal.HVAC_AUTO.let { s ->
			withArea(s, DRIVER, { ok, f -> api.checkHvacAutoCapabilities(ok, f) }) { a: SeatArea ->
				get<Boolean>(s) { ok, f -> api.getHvacAutoState(a, ok, f) }
				val l = object : L(s), HvacAutoStateListener { override fun onHvacAutoStateUpdated(isOn: Boolean?) = on(isOn) }
				reg(s, { api.registerHvacAutoState(a, l) }) { api.unregisterHvacAutoState(a, l) }
			}
		}
		Signal.HVAC_AC.let { s ->
			withArea(s, DRIVER, { ok, f -> api.checkHvacAcCapabilities(ok, f) }) { a: SeatArea ->
				get<Boolean>(s) { ok, f -> api.getHvacAcState(a, ok, f) }
				val l = object : L(s), HvacAcStateListener { override fun onHvacAcStateUpdated(isOn: Boolean?) = on(isOn) }
				reg(s, { api.registerHvacAcState(a, l) }) { api.unregisterHvacAcState(a, l) }
			}
		}
		Signal.RECIRC.let { s ->
			withArea(s, DRIVER, { ok, f -> api.checkHvacRecirculationCapabilities(ok, f) }) { a: SeatArea ->
				get<Boolean>(s) { ok, f -> api.getHvacRecirculationState(a, ok, f) }
				val l = object : L(s), HvacRecirculationStateListener { override fun onHvacAutoRecirculationStateUpdated(isOn: Boolean?) = on(isOn) }
				reg(s, { api.registerHvacRecirculationState(a, l) }) { api.unregisterHvacRecirculationState(a, l) }
			}
		}
		Signal.AIR_CLEANER.let { s ->
			withArea(s, DRIVER, { ok, f -> api.checkHvacAirCleanerCapabilities(ok, f) }) { a: SeatArea ->
				get<Boolean>(s) { ok, f -> api.getHvacAirCleanerState(a, ok, f) }
				val l = object : L(s), HvacAirCleanerStateListener { override fun onHvacAirCleanerStateUpdated(isOn: Boolean?) = on(isOn) }
				reg(s, { api.registerHvacAirCleanerState(a, l) }) { api.unregisterHvacAirCleanerState(a, l) }
			}
		}
		Signal.FAN_SPEED.let { s ->
			withArea(s, DRIVER, { ok, f -> api.checkHvacFanSpeedCapabilities(ok, f) }) { a: SeatArea ->
				get<Int>(s) { ok, f -> api.getHvacFanSpeed(a, ok, f) }
				val l = object : L(s), HvacFanSpeedListener { override fun onHvacFanSpeedUpdated(speed: Int?) = on(speed) }
				reg(s, { api.registerHvacFanSpeed(a, l) }) { api.unregisterHvacFanSpeed(a, l) }
			}
		}
		mapOf(Signal.TEMP_DRIVER to DRIVER, Signal.TEMP_PASSENGER to PASSENGER).forEach { (s, a) ->
			get<Float>(s) { ok, f -> api.getHvacTemperature(a, TemperatureUnit.CELSIUS, ok, f) }
			val l = object : L(s), HvacTemperatureListener { override fun onHvacTemperatureUpdated(temperature: Float?) = on(temperature) }
			reg(s, { api.registerHvacTemperature(a, l) }) { api.unregisterHvacTemperature(a, l) }
		}
	}

	private fun seat() {
		val api = vehicle.getSeat()
		// 에뮬레이터에서는 시트 온도 하나에 열선(양수)·통풍(음수)이 같이 온다. 실차는 통풍이 따로 올 수 있어 둘 다 듣는다.
		Signal.SEAT_HEAT_DRIVER.let { s ->
			get<Int>(s) { ok, f -> api.getHvacSeatTemperature(DRIVER, ok, f) }
			val l = object : L(s), HvacSeatTemperatureListener { override fun onHvacSeatTemperatureUpdated(level: Int?) = on(level) }
			reg(s, { api.registerHvacSeatTemperature(DRIVER, l) }) { api.unregisterHvacSeatTemperature(DRIVER, l) }
		}
		Signal.SEAT_VENT_DRIVER.let { s ->
			get<Int>(s) { ok, f -> api.getHvacSeatVentilationState(DRIVER, ok, f) }
			val l = object : L(s), HvacSeatVentilationListener { override fun onHvacSeatVentilationUpdated(level: Int?) = on(level) }
			reg(s, { api.registerHvacSeatVentilation(DRIVER, l) }) { api.unregisterHvacSeatVentilation(DRIVER, l) }
		}
	}

	private fun wheel() {
		val api = vehicle.getSteeringWheel()
		val s = Signal.WHEEL_HEAT
		get<Int>(s) { ok, f -> api.getHvacSteeringWheelHeatState(ok, f) }
		val l = object : L(s), HvacSteeringWheelHeatStateListener { override fun onHvacSteeringWheelHeatStateUpdated(level: Int?) = on(level) }
		reg(s, { api.registerHvacSteeringWheelHeatState(l) }) { api.unregisterHvacSteeringWheelHeatState(l) }
	}

	private fun light() {
		val api = vehicle.getLight()
		Signal.AMBIENT_COLOR.let { s ->
			withArea(s, DRIVER, { ok, f -> api.checkAmbientLightBrightnessColorCapabilities(ok, f) }) { a: SeatArea ->
				get<Int>(s) { ok, f -> api.getAmbientLightBrightnessColor(a, ok, f) }
				val l = object : L(s), AmbientLightBrightnessColorListener { override fun onBrightnessColorUpdated(color: Int?) = on(color) }
				reg(s, { api.registerAmbientLightBrightnessColorListener(a, l) }) { api.unregisterAmbientLightBrightnessColorListener(a, l) }
			}
		}
		Signal.READING_LIGHT.let { s ->
			get<LightState>(s) { ok, f -> api.getCurrentReadingLightState(DRIVER, ok, f) }
			val l = object : L(s), LightStateListener { override fun onLightStateUpdated(state: LightState?) = on(state) }
			reg(s, { api.registerReadingLightState(DRIVER, l) }) { api.unregisterReadingLightState(DRIVER, l) }
		}
		Signal.HIGH_BEAM.let { s ->
			get<LightState>(s) { ok, f -> api.getCurrentHighBeamLightState(ok, f) }
			val l = object : L(s), LightStateListener { override fun onLightStateUpdated(state: LightState?) = on(state) }
			reg(s, { api.registerHighBeamLightState(l) }) { api.unregisterHighBeamLightState(l) }
		}
	}

	private fun turnSignal() {
		val api = vehicle.getTurnSignal()
		val s = Signal.TURN_SIGNAL
		get<TurnSignalValue>(s) { ok, f -> api.getCurrentTurnSignalState(ok, f) }
		val l = object : L(s), TurnSignalStateListener { override fun onTurnSignalStateUpdated(state: TurnSignalValue?) = on(state) }
		reg(s, { api.registerTurnSignalState(l) }) { api.unregisterTurnSignalState(l) }
	}

	private fun wiper() {
		val api = vehicle.getWiper()
		val s = Signal.WIPER_FRONT
		get<WiperState>(s) { ok, f -> api.getCurrentWiperState(FRONT_GLASS, ok, f) }
		val l = object : L(s), WiperStateListener { override fun onWiperStateUpdated(state: WiperState?) = on(state) }
		reg(s, { api.registerWiperState(FRONT_GLASS, l) }) { api.unregisterWiperState(FRONT_GLASS, l) }
	}

	@Suppress("DEPRECATION")
	private fun drive() {
		Signal.GEAR.let { s ->
			val api = vehicle.getGear()
			get<VehicleGear>(s) { ok, f -> api.getCurrentGearSelectionState(ok, f) }
			val l = object : L(s), GearSelectionListener { override fun onSelected(gear: VehicleGear?) = on(gear) }
			reg(s, { api.registerGearSelection(l) }) { api.unregisterGearSelection(l) }
		}
		Signal.CRUISE_TARGET.let { s ->
			val api = vehicle.getDrivingMode()
			get<Float>(s) { ok, f -> api.getCruiseControlTargetSpeed(ok, f) }
			val l = object : L(s), CruiseControlTargetSpeedListener { override fun onCruiseControlTargetSpeedUpdated(speed: Float?) = on(speed) }
			reg(s, { api.registerCruiseControlTargetSpeed(l) }) { api.unregisterCruiseControlTargetSpeed(l) }
		}
		Signal.ODOMETER.let { s ->
			val api = vehicle.getOdometer()
			get<Float>(s) { ok, f -> api.getCurrentDistanceDriven(ok, f) }
			val l = object : L(s), DistanceDrivenListener { override fun onDistanceDrivenUpdated(distance: Float?) = on(distance) }
			reg(s, { api.registerDistanceDriven(l) }) { api.unregisterDistanceDriven(l) }
		}
	}

	companion object {
		private const val TAG = "CarWatcher"
		private val DRIVER = SeatArea(SeatValue.ROW_1_LEFT)
		private val PASSENGER = SeatArea(SeatValue.ROW_1_RIGHT)
		private val FRONT_GLASS = WindowArea(WindowValue.FRONT_WINDSHIELD)
		private val REAR_GLASS = WindowArea(WindowValue.REAR_WINDSHIELD)

		fun isOn(v: Any?): Boolean? = when (v) {
			null -> null
			is Boolean -> v
			is LightState -> v == LightState.ON
			is WiperState -> v == WiperState.ON
			is TurnSignalValue -> v != TurnSignalValue.OFF
			is Number -> v.toFloat() > 0f
			else -> null
		}
	}
}
