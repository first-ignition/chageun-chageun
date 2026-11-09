package ai.pleos.contest.app.ui

import ai.pleos.contest.app.CarWatcher
import ai.pleos.contest.app.Coach
import ai.pleos.contest.app.Signal
import ai.pleos.contest.app.label
import ai.pleos.contest.app.rang
import androidx.compose.ui.text.font.FontWeight
import ai.pleos.contest.app.Mission
import ai.pleos.contest.app.Missions
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(coach: Coach, openWords: () -> Unit, openSettings: () -> Unit, openGarage: () -> Unit) {
	val p = LocalPalette.current
	BoxWithConstraints(Modifier.fillMaxSize().background(p.bg)) {
		val wide = maxWidth >= Width.wide
		Column(Modifier.fillMaxSize().padding(horizontal = gutter(maxWidth), vertical = 24.dp)) {
			TopBar(openWords, openGarage, openSettings)
			Spacer(Modifier.height(16.dp))
			if (wide) {
				// 두 단의 윗선을 맞춘다: 머리말 두 줄이 같은 높이(남은 높이의 15%)에서 시작하고, 세로선도 거기서 시작
				BoxWithConstraints(Modifier.fillMaxSize()) {
					val top = maxHeight * 0.15f
					Row(Modifier.fillMaxSize()) {
						Column(Modifier.weight(0.62f).fillMaxHeight().verticalScroll(rememberScrollState()).padding(top = top, end = 64.dp, bottom = 24.dp)) {
							TodayBody(coach, openWords, compact = false)
						}
						Box(Modifier.padding(top = top, bottom = 48.dp).width(1.dp).fillMaxHeight().background(p.line))
						Column(
							Modifier.weight(0.38f).fillMaxHeight().clickable(role = Role.Button, onClick = openWords)
								.verticalScroll(rememberScrollState()).padding(top = top, start = 64.dp, bottom = 24.dp),
						) { Progress(coach, openGarage) }
					}
				}
			} else {
				Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(vertical = 16.dp)) {
					TodayBody(coach, openWords, compact = true, showButton = false)
					Spacer(Modifier.height(40.dp))
					Box(Modifier.fillMaxWidth().height(1.dp).background(p.line))
					Spacer(Modifier.height(32.dp))
					MiniCar(coach, openGarage, Modifier.widthIn(max = 320.dp))
					Spacer(Modifier.height(24.dp))
					Box(Modifier.clickable(role = Role.Button, onClick = openWords)) { ProgressCompact(coach) }
				}
				if (coach.todayPending) {
					Spacer(Modifier.height(16.dp))
					BigButton("다시 듣기", { coach.speakIntro() }, Modifier.fillMaxWidth(), icon = { SpeakerIcon(p.onPrimary) })
				}
			}
		}
	}
}

/** 차 연결 상태는 첫 화면 "차가 보고 있어요" 줄이 맡는다. */
@Composable
private fun TopBar(openWords: () -> Unit, openGarage: () -> Unit, openSettings: () -> Unit) {
	Row(Modifier.fillMaxWidth().height(96.dp), verticalAlignment = Alignment.CenterVertically) {
		Txt("차근차근", Type.titleL.copy(fontWeight = FontWeight.Bold))
		Spacer(Modifier.weight(1f))
		// 글자 메뉴는 누르는 칸(좌우 24dp 여백)만큼 오른쪽으로 내보내 글자 끝이 본문 오른쪽 선에 맞게
		Row(Modifier.offset(x = 24.dp)) {
			BigButton("단어장", openWords, kind = BtnKind.Text, modifier = Modifier.widthIn(min = 120.dp))
			BigButton("내 차", openGarage, kind = BtnKind.Text, modifier = Modifier.widthIn(min = 120.dp))
			BigButton("설정", openSettings, kind = BtnKind.Text, modifier = Modifier.widthIn(min = 120.dp))
		}
	}
}

@Composable
private fun TodayBody(coach: Coach, openWords: () -> Unit, compact: Boolean, showButton: Boolean = true) {
	val m = coach.today.value
	when {
		coach.allDone -> Graduated(coach, openWords)
		coach.todayDone -> TodayDone(coach, m)
		else -> TodayMission(coach, m, compact, showButton)
	}
}

@Composable
private fun TodayMission(coach: Coach, m: Mission, compact: Boolean, showButton: Boolean) {
	val p = LocalPalette.current
	// 계절 이름에 이미 "·"가 있어 가는 세로선으로 나눈다
	Row(verticalAlignment = Alignment.CenterVertically) {
		Txt(if (m.byVoice != null) "오늘 ${coach.carName.value}에게 할 말" else "오늘 손으로 익히기", Type.label, p.sub)
		Box(Modifier.padding(horizontal = 16.dp).size(width = 1.dp, height = 20.dp).background(p.muted))
		Txt(m.season, Type.label, p.sub)
	}
	Spacer(Modifier.height(20.dp))
	if (m.byVoice != null) Bubble(m.byVoice, style = if (compact) Type.bubbleS else Type.bubbleL)
	else Txt(m.name, if (compact) Type.headlineS else Type.headlineM)
	Spacer(Modifier.height(if (compact) 28.dp else 40.dp))
	// 외울 필요가 없다는 걸 먼저 말한다 (리서치 C3: 리 오토·샤오펑 "보이는 것을 그대로 말하기")
	if (m.byVoice != null) {
		Txt("외울 필요 없어요. 글레오를 부르고 보이는 그대로 말하세요.", Type.bodyL)
		Spacer(Modifier.height(8.dp))
		Txt("버튼으로 해도 돼요 · ${m.byHand}", Type.bodyM, p.sub)
	} else {
		Txt(m.byHand, Type.bodyL)
	}
	Spacer(Modifier.height(if (compact) 28.dp else 40.dp))
	Watching(coach, m)
	if (showButton) {
		Spacer(Modifier.height(36.dp))
		BigButton("다시 듣기", { coach.speakIntro() }, Modifier.widthIn(min = 280.dp), icon = { SpeakerIcon(p.onPrimary) })
		Spacer(Modifier.height(16.dp))
		Txt(if (m.byVoice != null) "운전 중에는 말로만 하세요." else "운전 중이라면 안전할 때만 하세요.", Type.label, p.sub)
	}
}

/** 심사·사용자 모두에게 "차 데이터로 확인한다"를 보여 주는 줄 */
@Composable
private fun Watching(coach: Coach, m: Mission) {
	val p = LocalPalette.current
	val s = m.goal.signal
	val v = coach.watched.value
	val src = coach.watchedSignal.value
	val ventOn: Boolean? = (v as? Number)?.toInt()?.let { if (src == Signal.SEAT_HEAT_DRIVER) it < 0 else it > 0 }
	val valueGoal = m.goal.byValue
	val alreadyOn = !valueGoal && (if (s == Signal.SEAT_VENT_DRIVER) ventOn == true else CarWatcher.isOn(v) == true)
	Row(Modifier.fillMaxWidth().heightIn(min = 64.dp), verticalAlignment = Alignment.CenterVertically) {
		ControlIcon(s, on = if (valueGoal) null else alreadyOn, size = 64.dp)
		Spacer(Modifier.width(20.dp))
		Column(Modifier.weight(1f)) {
			Txt(if (coach.connected.value) "차가 보고 있어요" else "차와 연결하는 중", Type.label, p.sub)
			val now = if (s == Signal.SEAT_VENT_DRIVER && ventOn != null) (if (ventOn) "켜짐" else "꺼짐") else watchedText(m, v)
			// 이미 켜져 있으면 같은 줄에서 바로 할 일을 말한다(줄을 하나 더 늘리지 않게)
			// 연결 전에는 "확인 중"을 두 번 말하지 않게 기능 이름만
			Txt(
				when {
					!coach.connected.value -> s.label
					alreadyOn -> "${s.label} · 이미 켜짐, 껐다가 다시 켜 보세요"
					else -> "${s.label} · 지금 $now"
				},
				Type.bodyL.copy(fontWeight = FontWeight.SemiBold),
			)
		}
	}
}

private fun watchedText(m: Mission, v: Any?): String {
	val s = m.goal.signal
	// 통풍이 시트 온도(음수)로 오는 경우
	if (s == Signal.SEAT_VENT_DRIVER && v is Number) return v.toInt().let { if (it == 0) "꺼짐" else "켜짐 ${kotlin.math.abs(it)}단" }
	return Coach.valueText(s, v)
}

/** 오늘 것을 마친 첫 화면: 말풍선은 그대로 두어 앱의 얼굴을 지키고, 다음 말을 바로 이어서 */
@Composable
private fun TodayDone(coach: Coach, m: Mission) {
	val p = LocalPalette.current
	Txt("오늘은 다 했어요", Type.label, p.sub)
	Spacer(Modifier.height(20.dp))
	// 끝낸 말풍선: 오른쪽 위에 체크 도장을 겹쳐 "진행 중"과 구별
	if (m.byVoice != null) Box {
		Bubble(m.byVoice, Modifier.padding(top = 12.dp, end = 12.dp), style = Type.bubble)
		CheckBadge(48.dp, p.text, p.bg, Modifier.align(Alignment.TopEnd))
	} else Txt(m.name, Type.headlineS)
	Spacer(Modifier.height(28.dp))
	Txt("이제 이 말은 아세요. 내일 또 하나 알려 드릴게요.", Type.bodyL, p.sub)
	val next = coach.next
	if (next != null) {
		Spacer(Modifier.height(56.dp))
		Txt("다음에 배울 말", Type.label, p.sub)
		Spacer(Modifier.height(8.dp))
		Txt(next.headline, Type.titleM)
		Spacer(Modifier.height(28.dp))
		BigButton("하나 더 배우기", { coach.learnNext() })
	}
}

@Composable
private fun Graduated(coach: Coach, openWords: () -> Unit) {
	val p = LocalPalette.current
	StationStamp(Missions.all.size, terminus = true)
	Spacer(Modifier.height(32.dp))
	Txt("${coach.carName.value.rang} 말하는 법 ${Missions.all.size}가지, 모두 익혔어요", Type.headlineS)
	Spacer(Modifier.height(12.dp))
	Txt("이제 이 차는 말로 다루실 수 있어요. 헷갈리면 단어장에서 다시 보세요.", Type.bodyL, p.sub)
	Spacer(Modifier.height(32.dp))
	BigButton("단어장 보기", openWords)
}

/** 같은 진도를 여러 번 보여 주지 않는다: 숫자 하나 + 노선 한 줄 + 이번 주 */
@Composable
private fun Progress(coach: Coach, openGarage: () -> Unit) {
	val p = LocalPalette.current
	val total = Missions.all.size
	val n = coach.learned
	Txt("${coach.carName.value.rang} 말하기", Type.label, p.sub)
	Spacer(Modifier.height(8.dp))
	Row(verticalAlignment = Alignment.Bottom) {
		Txt("$n", Type.count)
		Spacer(Modifier.width(12.dp))
		Txt("/ ${total}역", Type.titleL, p.sub, Modifier.padding(bottom = 12.dp))
	}
	// 두 단 윗선(머리말)은 그대로 두고, 내 차는 노선 바로 위에 올린다(노선 위를 달리는 내 차)
	Spacer(Modifier.height(24.dp))
	MiniCar(coach, openGarage, Modifier.widthIn(max = 400.dp))
	Spacer(Modifier.height(8.dp))
	RouteLine(n, total, todayPending = coach.todayPending, dot = 16.dp)
	Spacer(Modifier.height(28.dp))
	Txt(weekText(coach), Type.bodyM, p.sub)
	coach.next?.takeIf { !coach.todayDone }?.let { nx ->
		Spacer(Modifier.height(40.dp))
		Txt("다음 역", Type.label, p.sub)
		Spacer(Modifier.height(4.dp))
		Txt(nx.headline, Type.titleS, maxLines = 2)
	}
}

@Composable
private fun MiniCar(coach: Coach, openGarage: () -> Unit, modifier: Modifier) {
	val p = LocalPalette.current
	Column(modifier.fillMaxWidth().clickable(role = Role.Button, onClick = openGarage)) {
		Txt("내 차 꾸미기 · %,d P".format(coach.points), Type.label, p.sub)
		Spacer(Modifier.height(12.dp))
		CarPicture(coach.equipped.value, coach.carName.value, beginner = !coach.allDone, modifier = Modifier.fillMaxWidth(), tags = false)
	}
}

@Composable
private fun ProgressCompact(coach: Coach) {
	val p = LocalPalette.current
	val total = Missions.all.size
	val n = coach.learned
	Column(Modifier.fillMaxWidth()) {
		Row(verticalAlignment = Alignment.Bottom) {
			Txt("${coach.carName.value.rang} 말하기", Type.label, p.sub, Modifier.weight(1f).padding(bottom = 6.dp))
			Txt("$n", Type.headlineM.copy(fontFeatureSettings = "tnum"))
			Txt(" / ${total}역", Type.titleS, p.sub, Modifier.padding(bottom = 8.dp))
		}
		Spacer(Modifier.height(16.dp))
		RouteLine(n, total, todayPending = coach.todayPending, dot = 12.dp)
		Spacer(Modifier.height(16.dp))
		Txt(weekText(coach), Type.bodyM, p.sub)
	}
}

private fun weekText(coach: Coach): String {
	val today = java.time.LocalDate.now().toEpochDay()
	val days = coach.done.values.toSet().count { it in (today - 6)..today }
	return if (days == 0) "하루 하나씩, ${Missions.all.size}일이면 편하게 말해요." else "최근 7일 중 ${days}일 익혔어요."
}
