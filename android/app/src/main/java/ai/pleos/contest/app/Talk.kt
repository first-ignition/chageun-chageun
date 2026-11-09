package ai.pleos.contest.app

/** 규칙: 한 번에 두세 문장, 전문 용어 대신 생활 말, 주행 중에는 말하지 않는다(Coach가 막는다). */
object Talk {

	fun intro(m: Mission, tone: Tone, car: String): String {
		val v = m.byVoice
		return when (tone) {
			Tone.TEACHER -> if (v != null)
				"오늘 ${car}에게 할 말은, \"$v\"예요. 글레오를 부른 다음 이렇게 말해 보세요. 버튼으로 하셔도 괜찮아요."
			else
				"오늘은 ${m.name.eul} 해 볼게요. ${m.byHand}. 해 보시면 차가 알아채요."
			Tone.FRIEND -> if (v != null)
				"오늘은 이거 해 보자. \"$v\". 글레오 부르고 그대로 말하면 돼."
			else
				"오늘은 ${m.name}! ${m.byHand}. 해 보면 내가 알아챌게."
			Tone.CALM -> if (v != null)
				"오늘의 문장. \"$v\". 글레오를 부른 뒤 말씀하세요."
			else
				"오늘의 기능. ${m.name}. ${m.byHand}."
		}
	}

	fun praise(m: Mission, count: Int, tone: Tone): String = when (tone) {
		// 확인은 짧게 한 번(리서치 C7). 몇 개째인지는 카드가 보여 준다.
		Tone.TEACHER -> "잘하셨어요! ${m.praise}"
		// 친구 말투는 반말로만 (m.praise는 존댓말이라 섞지 않는다)
		Tone.FRIEND -> if (count <= 1) "오, 했네! 첫 번째 성공이야." else "오, 했네! 벌써 ${count}개째야."
		Tone.CALM -> "확인했습니다. ${count}번째 완료."
	}

	fun milestone(km: Int, tone: Tone): String {
		val tip = Milestones.tip(km)
		return when (tone) {
			Tone.TEACHER -> "축하드려요, 벌써 ${"%,d".format(km)}킬로미터를 달렸어요. $tip"
			Tone.FRIEND -> "와, ${"%,d".format(km)}킬로 찍었다! $tip"
			Tone.CALM -> "누적 ${"%,d".format(km)}킬로미터. $tip"
		}
	}
}

/**
 * 신차 관리 상식: 누적 주행거리 이정표. 근거는 투싼 HEV 취급설명서(06_콘텐츠목록 ② K5·K6·K7·K9).
 * 신형 NX5 매뉴얼이 나오면 숫자를 다시 확인한다.
 */
object Milestones {
	val kms = listOf(1000, 6000, 10000)

	fun tip(km: Int): String = when (km) {
		1000 -> "길들이기 기간이 끝났어요. 이제 평소처럼 편하게 운전하셔도 돼요."
		6000 -> "이쯤부터 연비가 안정돼요. 첫 달 연비가 낮았어도 걱정하지 마세요."
		10000 -> "엔진오일 교체 주기를 확인할 때예요. 서비스 예약을 잡아 보세요."
		else -> ""
	}

	fun title(km: Int): String = when (km) {
		1000 -> "길들이기 끝"
		6000 -> "연비가 자리 잡는 때"
		10000 -> "엔진오일 확인"
		else -> "${"%,d".format(km)} km"
	}
}
