package ai.pleos.contest.app

import android.car.Car
import android.car.drivingstate.CarUxRestrictions
import android.car.drivingstate.CarUxRestrictionsManager
import android.content.Context
import android.util.Log

/**
 * 주행 규제 상태(AAOS 표준 CarUxRestrictionsManager). 권한이 필요 없다.
 * "DO 필요"가 켜지면 주행 중 화면 규칙을 지켜야 한다. 기어(Vehicle SDK)와 함께 Coach가 주행 여부를 정한다.
 */
class DriveState(context: Context, private val onChange: (Boolean) -> Unit) {

	private val app = context.applicationContext
	private var car: Car? = null
	private var ux: CarUxRestrictionsManager? = null

	fun start() {
		try {
			val c = Car.createCar(app) ?: return
			car = c
			val m = c.getCarManager(Car.CAR_UX_RESTRICTION_SERVICE) as? CarUxRestrictionsManager ?: return
			ux = m
			m.currentCarUxRestrictions?.let { onChange(restricted(it)) }
			m.registerListener { r -> onChange(restricted(r)) }
		} catch (t: Throwable) {
			Log.w(TAG, "주행 규제 구독 실패", t)
		}
	}

	fun stop() {
		runCatching { ux?.unregisterListener() }
		runCatching { car?.disconnect() }
		ux = null
		car = null
	}

	private fun restricted(r: CarUxRestrictions) = r.isRequiresDistractionOptimization

	companion object {
		private const val TAG = "DriveState"
	}
}
