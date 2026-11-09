package ai.pleos.contest.app

/** 모두 에뮬레이터 + Virtual Console에서 값이 바뀌어 도착하는 것을 확인한 신호다(docs/04_아이디어/가능한것_지도.md 2차 재측정). */
enum class Signal {
	FRONT_DEFROST, REAR_DEFROST,
	HVAC_AUTO, HVAC_AC, RECIRC, AIR_CLEANER, FAN_SPEED,
	TEMP_DRIVER, TEMP_PASSENGER,
	WHEEL_HEAT, SEAT_HEAT_DRIVER, SEAT_VENT_DRIVER,
	AMBIENT_COLOR, READING_LIGHT, HIGH_BEAM,
	TURN_SIGNAL, WIPER_FRONT, CRUISE_TARGET,
	GEAR, ODOMETER,
}

val Signal.label: String
	get() = when (this) {
		Signal.FRONT_DEFROST -> "앞 유리 김 서림 제거"
		Signal.REAR_DEFROST -> "뒷유리 열선"
		Signal.HVAC_AUTO -> "에어컨 자동(AUTO)"
		Signal.HVAC_AC -> "에어컨(A/C)"
		Signal.RECIRC -> "내기 순환"
		Signal.AIR_CLEANER -> "공기청정"
		Signal.FAN_SPEED -> "바람 세기"
		Signal.TEMP_DRIVER -> "운전석 온도"
		Signal.TEMP_PASSENGER -> "조수석 온도"
		Signal.WHEEL_HEAT -> "핸들 열선"
		Signal.SEAT_HEAT_DRIVER -> "운전석 시트"
		Signal.SEAT_VENT_DRIVER -> "운전석 통풍"
		Signal.AMBIENT_COLOR -> "무드등 색"
		Signal.READING_LIGHT -> "독서등"
		Signal.HIGH_BEAM -> "상향등"
		Signal.TURN_SIGNAL -> "방향지시등"
		Signal.WIPER_FRONT -> "앞 와이퍼"
		Signal.CRUISE_TARGET -> "크루즈 목표 속도"
		Signal.GEAR -> "기어"
		Signal.ODOMETER -> "누적 주행거리"
	}

/** 미션 성공 조건. 앱을 켠 뒤 "새로" 그 상태가 되어야 성공이다(이미 켜져 있던 건 세지 않는다). */
sealed interface Goal {
	val signal: Signal

	data class TurnsOn(override val signal: Signal) : Goal

	data class Changes(override val signal: Signal) : Goal

	data class Lowers(override val signal: Signal) : Goal

	data class Raises(override val signal: Signal) : Goal

	val byValue: Boolean get() = this !is TurnsOn
}

data class Mission(
	val id: String,
	val name: String,
	/** 글레오에게 하는 한 문장. 말로 못 하면 null */
	val byVoice: String?,
	val byHand: String,
	val why: String,
	val praise: String,
	val goal: Goal,
	val season: String,
) {
	val headline: String get() = byVoice ?: name
}

/**
 * 차의 언어 18개. 순서 = 배우는 순서(하루 하나).
 * 고른 기준(05_공격검토): "있는 줄은 아는데 안 써 봤다" × 이 차에 있음 × 차 신호로 확인됨.
 * 글레오 문장은 예시다. 실제로 알아듣는 표현은 실차·전시장에서 확인해 고친다(결정기록 D17).
 */
object Missions {
	val all: List<Mission> = listOf(
		Mission(
			id = "front_defrost", name = "앞 유리 김 서림 없애기",
			byVoice = "앞 유리 김 서림 없애 줘",
			byHand = "화면 맨 아래 공조 줄의 앞 유리 그림 버튼",
			why = "겨울 아침, 비 오는 날 앞이 뿌옇게 안 보일 때 바로 걷혀요.",
			praise = "이제 김이 서려도 당황하지 않으셔도 돼요.",
			goal = Goal.TurnsOn(Signal.FRONT_DEFROST), season = "겨울·장마",
		),
		Mission(
			id = "hvac_auto", name = "에어컨 자동",
			byVoice = "에어컨 자동으로 해 줘",
			byHand = "아래 온도 숫자를 누르면 나오는 공조 창의 Auto",
			why = "온도만 정해 두면 바람 세기와 방향은 차가 알아서 맞춰요.",
			praise = "이제 바람 세기를 일일이 만지지 않으셔도 돼요.",
			goal = Goal.TurnsOn(Signal.HVAC_AUTO), season = "사계절",
		),
		Mission(
			id = "wheel_heat", name = "핸들 열선",
			byVoice = "핸들 따뜻하게 해 줘",
			byHand = "화면 아래 핸들 모양 버튼",
			why = "겨울 아침 손 시림이 금방 사라져요.",
			praise = "손이 금방 따뜻해질 거예요.",
			goal = Goal.TurnsOn(Signal.WHEEL_HEAT), season = "겨울",
		),
		Mission(
			id = "seat_heat", name = "운전석 열선",
			byVoice = "운전석 열선 켜 줘",
			byHand = "화면 아래 의자 모양 버튼(빨간 물결)",
			why = "히터보다 몸이 빨리 데워져요. 허리가 편해요.",
			praise = "등이 따뜻해지면 히터 온도는 조금 낮춰도 돼요.",
			goal = Goal.TurnsOn(Signal.SEAT_HEAT_DRIVER), season = "겨울",
		),
		Mission(
			id = "temp_driver", name = "온도 말로 바꾸기",
			byVoice = "온도 2도 올려 줘",
			byHand = "화면 아래 왼쪽 온도 숫자 옆 화살표",
			why = "메뉴를 몰라도 \"좀 덥다\" 한마디면 돼요.",
			praise = "온도는 말 한마디로 끝이에요.",
			goal = Goal.Raises(Signal.TEMP_DRIVER), season = "사계절",
		),
		Mission(
			id = "recirc", name = "내기 순환",
			byVoice = "내기 순환 켜 줘",
			byHand = "공조 화면의 차 안 화살표 버튼",
			why = "터널, 매연 많은 길에서 바깥 공기를 막아요. 오래 두면 졸리니 나와서는 꺼 주세요.",
			praise = "터널을 나오면 다시 바깥 공기로 바꿔 주세요.",
			goal = Goal.TurnsOn(Signal.RECIRC), season = "터널·정체",
		),
		Mission(
			id = "rear_defrost", name = "뒷유리 열선",
			byVoice = "뒷유리 열선 켜 줘",
			byHand = "공조 화면의 뒷유리 모양 버튼",
			why = "뒷유리 성에와 김을 녹여 후진할 때 잘 보여요.",
			praise = "후진할 때 뒤가 훨씬 잘 보일 거예요.",
			goal = Goal.TurnsOn(Signal.REAR_DEFROST), season = "겨울·비",
		),
		Mission(
			id = "ambient_color", name = "무드등 색",
			byVoice = "무드등 보라색으로 바꿔 줘",
			byHand = "세워 두고: 차량 설정 > 라이트 > 실내 무드 조명",
			why = "밤길 눈이 편하고, 퇴근길 기분이 바뀌어요.",
			praise = "오늘 기분에 맞는 색이네요.",
			goal = Goal.Changes(Signal.AMBIENT_COLOR), season = "밤·퇴근",
		),
		Mission(
			id = "air_cleaner", name = "공기청정",
			byVoice = "공기청정 켜 줘",
			byHand = "공조 화면의 공기청정 버튼",
			why = "미세먼지 나쁜 날 차 안 공기를 필터로 걸러요.",
			praise = "미세먼지 많은 날엔 이것부터 켜세요.",
			goal = Goal.TurnsOn(Signal.AIR_CLEANER), season = "황사·미세먼지",
		),
		Mission(
			id = "fan_speed", name = "바람 세기",
			byVoice = "바람 약하게 해 줘",
			byHand = "공조 화면의 바람 세기 막대",
			why = "바람 소리가 크면 말소리가 안 들려요. 한마디로 줄이세요.",
			praise = "\"세게\", \"약하게\" 둘 다 됩니다.",
			goal = Goal.Lowers(Signal.FAN_SPEED), season = "사계절",
		),
		Mission(
			id = "seat_vent", name = "운전석 통풍",
			byVoice = "운전석 통풍 켜 줘",
			byHand = "화면 아래 의자 모양 버튼(파란 바람)",
			why = "여름에 등이 땀에 젖지 않아요.",
			praise = "여름 장거리가 훨씬 편해져요.",
			goal = Goal.TurnsOn(Signal.SEAT_VENT_DRIVER), season = "여름",
		),
		Mission(
			id = "temp_passenger", name = "옆자리 온도 따로",
			byVoice = "조수석 온도 올려 줘",
			byHand = "화면 아래 오른쪽 온도 숫자",
			why = "덥다, 춥다로 다툴 일이 줄어요. 자리마다 따로 맞춰요.",
			praise = "이제 옆자리 온도는 따로 맞추시면 돼요.",
			goal = Goal.Changes(Signal.TEMP_PASSENGER), season = "동승",
		),
		Mission(
			id = "hvac_ac", name = "에어컨 켜기",
			byVoice = "에어컨 켜 줘",
			byHand = "공조 화면의 A/C 버튼",
			why = "여름뿐 아니라 비 오는 날 김 서림에도 에어컨이 도움이 돼요.",
			praise = "비 오는 날 김이 서려도 에어컨이 도와줘요.",
			goal = Goal.TurnsOn(Signal.HVAC_AC), season = "여름·비",
		),
		Mission(
			id = "reading_light", name = "독서등",
			byVoice = "독서등 켜 줘",
			byHand = "천장 앞쪽 전구 버튼",
			why = "밤에 물건 찾을 때 그 자리만 밝혀 운전자 눈이 부시지 않아요.",
			praise = "다 쓰셨으면 꺼 두세요. 밤길에 눈부셔요.",
			goal = Goal.TurnsOn(Signal.READING_LIGHT), season = "밤·정차",
		),
		Mission(
			id = "turn_signal", name = "원터치 방향지시등",
			byVoice = null,
			byHand = "레버를 끝까지 말고 살짝만 톡",
			why = "살짝만 쳐도 정해 둔 횟수만큼 깜빡여 차선 바꿀 때 손이 편해요.",
			praise = "살짝만 톡, 그게 전부예요.",
			goal = Goal.TurnsOn(Signal.TURN_SIGNAL), season = "차선 변경",
		),
		Mission(
			id = "wiper", name = "와이퍼",
			byVoice = null,
			byHand = "오른쪽 레버를 아래로 (AUTO는 한 칸)",
			why = "비 올 때 레버 한 칸이면 돼요. AUTO 칸에 두면 비 양에 맞춰 알아서 움직여요.",
			praise = "다음엔 AUTO 칸에 두고 감도 다이얼도 돌려 보세요.",
			goal = Goal.TurnsOn(Signal.WIPER_FRONT), season = "비",
		),
		Mission(
			id = "cruise_speed", name = "크루즈 속도 맞추기",
			byVoice = null,
			byHand = "핸들 왼쪽 크루즈 버튼, 위아래로 속도",
			why = "고속도로에서 발을 떼도 정한 속도로 가요. 앞차와 거리도 맞춰요.",
			praise = "장거리 운전 피로가 확 줄어들 거예요.",
			goal = Goal.Changes(Signal.CRUISE_TARGET), season = "고속도로",
		),
		Mission(
			id = "high_beam", name = "상향등",
			byVoice = null,
			byHand = "왼쪽 레버를 앞으로 밀기",
			why = "가로등 없는 밤길에서 멀리까지 비춰요. 마주 오는 차가 있으면 꺼 주세요.",
			praise = "마주 오는 차가 보이면 꼭 다시 내려 주세요.",
			goal = Goal.TurnsOn(Signal.HIGH_BEAM), season = "밤 국도",
		),
	)

	fun byId(id: String?): Mission? = all.firstOrNull { it.id == id }
}
