package ai.pleos.contest.app

import androidx.annotation.DrawableRes

/**
 * 내 차 꾸미기(결정기록 D29). 주차 중 화면에서만 보인다.
 * 포인트는 차가 확인한 기능에서만 나온다: 새로 익힌 기능 하나에 100P, 18개를 다 익히면(종점) 300P.
 * 연속 기록·순위·뽑기·잃는 것은 없다. 산 것은 언제든 바꿔 달 수 있다.
 */
enum class Slot(val label: String) { ROOF("지붕"), COLOR("차 색"), WINDOW("창 안") }

/** bodyTint는 먹선이 보이도록 중간 밝기만 고른다(낮 기준) */
enum class Item(
	val slot: Slot,
	val label: String,
	val price: Int,
	val bodyTint: Long? = null,
	@DrawableRes val fill: Int? = null,
	@DrawableRes val line: Int? = null,
) {
	RACK(Slot.ROOF, "루프랙", 200, line = R.drawable.roof_rack_line),
	BOX(Slot.ROOF, "루프박스", 400, fill = R.drawable.roof_box_fill, line = R.drawable.roof_box_line),
	BIKE(Slot.ROOF, "자전거 캐리어", 400, line = R.drawable.roof_bike_line),
	TENT(Slot.ROOF, "루프탑 텐트", 600, fill = R.drawable.roof_tent_fill, line = R.drawable.roof_tent_line),
	SAND(Slot.COLOR, "모래", 100, 0xFFDCCFB6),
	SAGE(Slot.COLOR, "숲", 100, 0xFF9FB3A2),
	SKY(Slot.COLOR, "하늘", 100, 0xFFAFC4D8),
	BRICK(Slot.COLOR, "벽돌", 100, 0xFFD39B7E),
	AMBER(Slot.COLOR, "호박", 100, 0xFFF6C453),
	FRESHENER(Slot.WINDOW, "룸미러 방향제", 150, fill = R.drawable.win_freshener_fill, line = R.drawable.win_freshener_line),
	DOLL(Slot.WINDOW, "대시보드 인형", 150, fill = R.drawable.win_doll_fill, line = R.drawable.win_doll_line),
	DOG(Slot.WINDOW, "창가 강아지", 150, fill = R.drawable.win_dog_fill, line = R.drawable.win_dog_line),
	;

	companion object {
		fun of(slot: Slot) = entries.filter { it.slot == slot }
		fun byName(name: String?) = entries.firstOrNull { it.name == name }
	}
}

object Points {
	const val PER_LEARNED = 100
	const val TERMINUS = 300

	/** 번 포인트 - 쓴 포인트. 번 것은 익힌 기록에서 매번 다시 센다(따로 저장하지 않아 어긋날 일이 없다) */
	fun balance(learned: Int, allDone: Boolean, owned: Set<Item>): Int =
		(learned * PER_LEARNED + (if (allDone) TERMINUS else 0) - owned.sumOf { it.price }).coerceAtLeast(0)

	fun gained(count: Int, total: Int): Int = PER_LEARNED + if (count >= total) TERMINUS else 0
}
