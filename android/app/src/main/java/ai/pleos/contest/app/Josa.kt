package ai.pleos.contest.app

/**
 * 이름 뒤에 붙는 조사. 받침이 있으면 "이랑/을", 없으면 "랑/를".
 * 예: 내 차랑, 붕붕이랑, 별님이랑 / 와이퍼를, 상향등을. 숫자·영문은 읽는 소리로 본다(GV80 → 팔 → 받침 있음).
 */
private fun String.hasBatchim(): Boolean {
	val c = trimEnd().lastOrNull() ?: return false
	return when (c) {
		in '가'..'힣' -> (c - '가') % 28 != 0
		in '0'..'9' -> c in "013678" // 영·일·삼·육·칠·팔
		in 'A'..'Z', in 'a'..'z' -> c.uppercaseChar() in "LMNR" // 엘·엠·엔·알
		else -> false
	}
}

/** "내 차" → "내 차랑", "별님" → "별님이랑" */
val String.rang: String get() = this + if (hasBatchim()) "이랑" else "랑"

/** "와이퍼" → "와이퍼를", "상향등" → "상향등을" */
val String.eul: String get() = this + if (hasBatchim()) "을" else "를"
