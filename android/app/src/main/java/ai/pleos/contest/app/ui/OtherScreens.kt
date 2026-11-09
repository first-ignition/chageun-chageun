package ai.pleos.contest.app.ui

import ai.pleos.contest.app.Coach
import ai.pleos.contest.app.Points
import ai.pleos.contest.app.Milestones
import ai.pleos.contest.app.Mission
import ai.pleos.contest.app.Missions
import ai.pleos.contest.app.Moment
import ai.pleos.contest.app.Tone
import ai.pleos.contest.app.VoiceKind
import ai.pleos.contest.app.rang
import ai.pleos.contest.app.Progress
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.LocalDate

/**
 * 주행 중 화면: 정지 그림, 버튼 없음, 소리 없음. 종이 한가운데 오늘의 말 말풍선 하나만(1~2초에 읽히게).
 * 바뀌는 것은 "차가 알아챘다" 한 장면뿐(상태가 바뀔 때만). 화면 위쪽 2/5에 두어 시선을 덜 내리게
 * (리서치 C1 — 화면 하단까지 시선 20° vs 56°, Vi Bilägare 2022).
 */
@Composable
fun DrivingScreen(coach: Coach) {
	val p = LocalPalette.current
	val caught = coach.caughtWhileDriving.value
	BoxWithConstraints(Modifier.fillMaxSize().background(p.bg).padding(horizontal = Size.pad)) {
		val compact = maxWidth < Width.compact
		Column(
			Modifier.fillMaxWidth().padding(top = maxHeight * 0.28f),
			horizontalAlignment = Alignment.CenterHorizontally,
		) {
			when {
				caught != null -> {
					CheckBadge(80.dp, p.success, p.onSuccess)
					Spacer(Modifier.height(28.dp))
					Txt("차가 알아챘어요", Type.headlineL, align = TextAlign.Center)
					Spacer(Modifier.height(12.dp))
					Txt("주차(P)하면 알려 드릴게요", Type.titleM, p.sub, align = TextAlign.Center)
				}
				!coach.todayDone -> {
					val m = coach.today.value
					// 밤에도 채운 말풍선(가장 빨리 읽히는 모양). 밤 호박색은 한 단계 낮춰 눈부시지 않게(Theme.kt)
					if (m.byVoice != null) Bubble(m.byVoice, style = if (compact) Type.bubbleM else Type.bubbleXL, tailCenter = true)
					else Txt(m.name, Type.headlineL, align = TextAlign.Center)
				}
				else -> {
					Txt("운전에 집중하세요", Type.headlineM, align = TextAlign.Center)
					Spacer(Modifier.height(12.dp))
					Txt("오늘의 말은 익혔어요", Type.titleM, p.sub, align = TextAlign.Center)
				}
			}
		}
	}
}

@Composable
fun MomentScreen(coach: Coach, moment: Moment, openGarage: () -> Unit) {
	val p = LocalPalette.current
	Box(Modifier.fillMaxSize().background(p.bg).padding(horizontal = Size.pad, vertical = 32.dp), contentAlignment = Alignment.Center) {
		Column(
			Modifier.widthIn(max = 1100.dp).fillMaxWidth().verticalScroll(rememberScrollState()),
			horizontalAlignment = Alignment.CenterHorizontally,
		) {
			when (moment) {
				is Moment.Learned -> Learned(moment.mission, moment.count, moment.evidence)
				is Moment.Milestone -> MilestoneBody(moment.km, coach)
			}
			Spacer(Modifier.height(64.dp))
			BigButton("좋아요", { coach.dismissMoment() }, Modifier.widthIn(min = 480.dp))
			// 포인트는 칭찬 맨 아래 한 줄로만(도장·칭찬이 먼저). 내 차 꾸미기는 결정기록 D29
			if (moment is Moment.Learned) {
				Spacer(Modifier.height(24.dp))
				val gained = Points.gained(moment.count, Missions.all.size)
				Row(verticalAlignment = Alignment.CenterVertically) {
					Txt("+$gained P", Type.titleS.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, fontFeatureSettings = "tnum"), p.sub)
					Spacer(Modifier.width(8.dp))
					BigButton("내 차 꾸미기", openGarage, kind = BtnKind.Text)
				}
			}
		}
	}
}

@Composable
private fun Learned(m: Mission, count: Int, evidence: String) {
	val p = LocalPalette.current
	StationStamp(count)
	Spacer(Modifier.height(32.dp))
	Txt("해냈어요", Type.headlineM, align = TextAlign.Center)
	Spacer(Modifier.height(24.dp))
	if (m.byVoice != null) Bubble(m.byVoice, style = Type.bubble, tailCenter = true) else Txt(m.name, Type.headlineS)
	Spacer(Modifier.height(32.dp))
	Txt(m.praise, Type.titleM, align = TextAlign.Center)
	Spacer(Modifier.height(16.dp))
	BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
		val narrow = maxWidth < Width.narrow
		Row(verticalAlignment = Alignment.CenterVertically) {
			if (!narrow) {
				ControlGlyph(m.goal.signal, p.sub, 32.dp)
				Spacer(Modifier.width(12.dp))
			}
			Txt("차가 확인했어요 · " + (if (narrow) evidence.substringBefore(" · ") else evidence), Type.bodyM, p.sub, maxLines = 1)
		}
	}
}

@Composable
private fun MilestoneBody(km: Int, coach: Coach) {
	val p = LocalPalette.current
	Stamp("%,d".format(km), "km")
	Spacer(Modifier.height(32.dp))
	Txt(Milestones.title(km), Type.headlineM, align = TextAlign.Center)
	Spacer(Modifier.height(16.dp))
	Txt(Milestones.tip(km), Type.bodyL, p.sub, Modifier.widthIn(max = 960.dp), align = TextAlign.Center)
}

/** 단어장 = 말로 하기 선 · 손으로 익히기 선 두 노선도 (결정기록 D24·D25) */
@Composable
fun WordsScreen(coach: Coach, back: () -> Unit) {
	val p = LocalPalette.current
	var open by remember { mutableStateOf<Mission?>(null) }
	val (byVoice, byHand) = Missions.all.partition { it.byVoice != null }
	val voiceLine = "${coach.carName.value}에게 말로 하기"
	BoxWithConstraints(Modifier.fillMaxSize().background(p.bg)) {
		val wide = maxWidth >= Width.split
		Column(Modifier.fillMaxSize().padding(horizontal = gutter(maxWidth), vertical = 24.dp)) {
			Header("단어장", back)
			Spacer(Modifier.height(16.dp))
			if (wide) {
				Row(Modifier.fillMaxSize()) {
					LineColumn(coach, voiceLine, "글레오를 부르고 이 말을 하면 돼요", byVoice, Modifier.weight(1.6f).padding(end = 48.dp)) { open = it }
					Box(Modifier.padding(vertical = 24.dp).width(1.dp).fillMaxHeight().background(p.line))
					LineColumn(coach, "손으로 익히기", "말 대신 레버·버튼으로", byHand, Modifier.weight(1f).padding(start = 48.dp)) { open = it }
				}
			} else {
				Box {
					Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = 72.dp)) {
						LineColumn(coach, voiceLine, "글레오를 부르고 이 말을 하면 돼요", byVoice, Modifier.fillMaxWidth(), scroll = false) { open = it }
						Spacer(Modifier.height(32.dp))
						Box(Modifier.fillMaxWidth().height(1.dp).background(p.line))
						Spacer(Modifier.height(32.dp))
						LineColumn(coach, "손으로 익히기", "말 대신 레버·버튼으로", byHand, Modifier.fillMaxWidth(), scroll = false) { open = it }
					}
					BottomFade(Modifier.align(Alignment.BottomCenter))
				}
			}
		}
	}
	open?.let { m -> WordDetail(coach, m, close = { open = null }, chosen = { open = null; back() }) }
}

@Composable
private fun LineColumn(
	coach: Coach, title: String, sub: String, stations: List<Mission>,
	modifier: Modifier, scroll: Boolean = true, onOpen: (Mission) -> Unit,
) {
	val p = LocalPalette.current
	val passed = stations.count { coach.done.containsKey(it.id) }
	// 내 여정 순서: 지나온 역(익힌 날 순) → 오늘의 역 → 남은 역(배우는 순서)
	val ordered = stations.filter { coach.done.containsKey(it.id) }.sortedBy { coach.done[it.id] } +
		stations.filter { !coach.done.containsKey(it.id) && it.id == coach.today.value.id } +
		stations.filter { !coach.done.containsKey(it.id) && it.id != coach.today.value.id }
	Column(modifier) {
		Row(verticalAlignment = Alignment.Bottom) {
			Txt(title, Type.titleM, modifier = Modifier.weight(1f, fill = false))
			Spacer(Modifier.width(16.dp))
			Txt("$passed / ${stations.size}역", Type.bodyM.copy(fontFeatureSettings = "tnum"), p.sub, Modifier.padding(bottom = 4.dp))
		}
		Spacer(Modifier.height(4.dp))
		Txt(sub, Type.bodyM, p.sub)
		Spacer(Modifier.height(20.dp))
		val body: @Composable () -> Unit = {
			ordered.forEachIndexed { i, m ->
				Station(
					coach, m,
					first = i == 0, last = i == ordered.lastIndex,
					nextPassed = ordered.getOrNull(i + 1)?.let { coach.done.containsKey(it.id) || it.id == coach.today.value.id } == true,
				) { onOpen(m) }
			}
		}
		if (scroll) Box {
			Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = 64.dp)) { body() }
			BottomFade(Modifier.align(Alignment.BottomCenter))
		} else body()
	}
}

/** 목록 아래 끝을 종이색으로 옅게 — 잘린 줄이 고장처럼 보이지 않게(정지 그림) */
@Composable
private fun BottomFade(modifier: Modifier) {
	val p = LocalPalette.current
	Box(modifier.fillMaxWidth().height(72.dp).background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color.Transparent, p.bg))))
}

private enum class StationState { Passed, Today, Ahead }

@Composable
private fun Station(coach: Coach, m: Mission, first: Boolean, last: Boolean, nextPassed: Boolean, onClick: () -> Unit) {
	val p = LocalPalette.current
	val day = coach.done[m.id]
	val state = when {
		day != null -> StationState.Passed
		coach.today.value.id == m.id -> StationState.Today
		else -> StationState.Ahead
	}
	Row(
		Modifier.fillMaxWidth().heightIn(min = 96.dp).height(IntrinsicSize.Min).clip(RoundedCornerShape(Size.radiusS)).clickable(role = Role.Button, onClick = onClick),
		verticalAlignment = Alignment.Top,
	) {
		Canvas(Modifier.width(56.dp).fillMaxHeight()) {
			val cx = size.width / 2
			// 역 표시는 문장 첫 줄 높이에 (둘째 줄이 있어도 첫 줄에 맞춘다)
			val cy = 26.dp.toPx() + 22.dp.toPx()
			val dash = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 6.dp.toPx()))
			val thick = 6.dp.toPx()
			val thin = 2.dp.toPx()
			val inPassed = state != StationState.Ahead
			if (!first) drawLine(if (inPassed) p.text else p.muted, Offset(cx, 0f), Offset(cx, cy), strokeWidth = if (inPassed) thick else thin, pathEffect = if (inPassed) null else dash)
			val solidBelow = state == StationState.Passed && nextPassed
			if (!last) drawLine(if (solidBelow) p.text else p.muted, Offset(cx, cy), Offset(cx, size.height), strokeWidth = if (solidBelow) thick else thin, pathEffect = if (solidBelow) null else dash)
			when (state) {
				StationState.Passed -> drawCircle(p.text, 13.dp.toPx(), Offset(cx, cy))
				StationState.Today -> {
					drawCircle(p.bubble, 20.dp.toPx(), Offset(cx, cy))
					drawCircle(p.text, 20.dp.toPx(), Offset(cx, cy), style = Stroke(3.dp.toPx()))
				}
				StationState.Ahead -> {
					drawCircle(p.bg, 11.dp.toPx(), Offset(cx, cy))
					drawCircle(p.muted, 11.dp.toPx(), Offset(cx, cy), style = Stroke(thin))
				}
			}
		}
		Spacer(Modifier.width(20.dp))
		Column(Modifier.weight(1f).padding(top = 26.dp, bottom = 22.dp)) {
			Txt(m.headline, Type.titleS, if (state == StationState.Ahead) p.sub else p.text)
			if (state == StationState.Today) {
				Spacer(Modifier.height(2.dp))
				Txt("오늘의 역 · 해 보면 차가 알아채요", Type.bodyS, p.text)
			}
		}
		Spacer(Modifier.width(16.dp))
		Txt(
			when (state) {
				StationState.Passed -> dateText(day!!)
				StationState.Today -> "오늘"
				StationState.Ahead -> m.season
			},
			// 남은 역의 꼬리표는 보통 굵기 — 제목보다 진해 보이지 않게
			if (state == StationState.Today) Type.label else Type.label.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Normal),
			if (state == StationState.Today) p.text else p.sub, Modifier.padding(top = 32.dp),
		)
		Spacer(Modifier.width(8.dp))
	}
}

/** 가운데 뜨는 팝업 대신 오른쪽 판. 움직임 없이 바로 나타난다. */
@Composable
private fun WordDetail(coach: Coach, m: Mission, close: () -> Unit, chosen: () -> Unit) {
	val p = LocalPalette.current
	BoxWithConstraints(Modifier.fillMaxSize().background(Color(0xFF1B1915).copy(alpha = 0.4f)).clickable(onClick = close)) {
		val narrow = maxWidth < Width.split
		val shape = if (narrow) RoundedCornerShape(0.dp) else RoundedCornerShape(topStart = Size.radius, bottomStart = Size.radius)
		Column(
			Modifier.align(Alignment.CenterEnd).fillMaxHeight().fillMaxWidth(if (narrow) 1f else 0.5f)
				.clip(shape).background(p.bg).border(1.dp, p.line, shape)
				.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
				.padding(horizontal = if (narrow) 32.dp else Size.pad, vertical = 24.dp),
		) {
			Row(Modifier.fillMaxWidth().height(96.dp), verticalAlignment = Alignment.CenterVertically) {
				Txt(m.season + (coach.done[m.id]?.let { "  ·  ${dateText(it)} 익힘" } ?: ""), Type.label, p.sub, Modifier.weight(1f))
				CloseButton(close)
			}
			Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
				Spacer(Modifier.height(8.dp))
				if (m.byVoice != null) Bubble(m.byVoice, style = Type.bubbleS) else Txt(m.name, Type.headlineS)
				Spacer(Modifier.height(32.dp))
				if (m.byVoice != null) Detail("말로", "글레오를 부르고 → ${m.byVoice}")
				Detail("손으로", m.byHand)
				Detail("좋은 점", m.why)
				if (coach.today.value.id != m.id && !coach.done.containsKey(m.id)) {
					Spacer(Modifier.height(32.dp))
					BigButton("오늘의 말로 정하기", { coach.choose(m); chosen() })
				}
			}
		}
	}
}

@Composable
private fun CloseButton(onClick: () -> Unit) {
	val p = LocalPalette.current
	Box(Modifier.size(Size.touch).clip(RoundedCornerShape(Size.radius)).clickable(role = Role.Button, onClickLabel = "닫기", onClick = onClick), contentAlignment = Alignment.Center) {
		Canvas(Modifier.size(28.dp)) {
			val w = size.width
			drawLine(p.text, Offset(w * 0.1f, w * 0.1f), Offset(w * 0.9f, w * 0.9f), 3.dp.toPx(), StrokeCap.Round)
			drawLine(p.text, Offset(w * 0.9f, w * 0.1f), Offset(w * 0.1f, w * 0.9f), 3.dp.toPx(), StrokeCap.Round)
		}
	}
}

@Composable
private fun Detail(head: String, body: String) {
	val p = LocalPalette.current
	Row(Modifier.padding(vertical = 12.dp)) {
		Txt(head, Type.label, p.sub, Modifier.width(120.dp).padding(top = 6.dp))
		Txt(body, Type.bodyL, modifier = Modifier.weight(1f, fill = false).widthIn(max = 560.dp))
	}
}

private fun dateText(epochDay: Long): String {
	val d = LocalDate.ofEpochDay(epochDay)
	return "${d.monthValue}월 ${d.dayOfMonth}일"
}

@Composable
fun SettingsScreen(coach: Coach, back: () -> Unit) {
	val p = LocalPalette.current
	var confirmReset by remember { mutableStateOf(false) }
	BoxWithConstraints(Modifier.fillMaxSize().background(p.bg)) {
		val gutter = gutter(maxWidth)
		val twoCol = maxWidth >= Width.wide
		val tone: @Composable (Boolean) -> Unit = { first ->
			Section(first) {
				Column {
					SectionTitle("말투")
					Choices(Tone.entries.map { t -> ChoiceItem(t.label, t.sample, coach.tone.value == t) { coach.setTone(t) } })
				}
			}
		}
		val voice: @Composable () -> Unit = {
			Section {
				Column {
					SectionTitle("목소리")
					Choices(VoiceKind.entries.map { v ->
						ChoiceItem(v.label, v.sub, coach.soundOn.value && coach.voiceKind.value == v) { coach.setVoice(v) }
					} + ChoiceItem("소리 끄기", "화면으로만", !coach.soundOn.value) { coach.setSound(false) })
				}
			}
		}
		val about: @Composable () -> Unit = {
			Section {
				Column {
					SectionTitle("이 앱이 하는 일")
					Txt("차를 직접 바꾸지 않아요. 내가 바꾼 것을 알아채기만 해요.", Type.bodyL)
					Spacer(Modifier.height(4.dp))
					Txt("기록은 이 차 안에만 남고, 밖으로 보내지 않아요.", Type.bodyL)
				}
			}
		}
		val reset: @Composable () -> Unit = {
			Section {
				Column {
					SectionTitle("처음부터 다시")
					Txt(if (confirmReset) "익힌 말이 모두 지워져요. 정말 처음부터 할까요?" else "익힌 말과 설정을 지우고 첫날로 돌아가요.", Type.bodyL)
					Spacer(Modifier.height(20.dp))
					Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
						if (confirmReset) {
							BigButton("아니요", { confirmReset = false }, kind = BtnKind.Quiet)
							BigButton("네, 처음부터", { coach.resetAll(); confirmReset = false; back() })
						} else {
							BigButton("처음부터", { confirmReset = true }, kind = BtnKind.Secondary)
						}
					}
				}
			}
		}
		Column(Modifier.fillMaxSize().padding(horizontal = gutter, vertical = 24.dp)) {
			Header("설정", back)
			Spacer(Modifier.height(8.dp))
			if (twoCol) {
				Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(64.dp)) {
					Column(Modifier.weight(1f).widthIn(max = 760.dp).verticalScroll(rememberScrollState())) { CarNameCard(coach); about(); reset() }
					Column(Modifier.weight(1f).widthIn(max = 760.dp).verticalScroll(rememberScrollState())) { tone(true); voice() }
				}
			} else {
				Column(Modifier.widthIn(max = 960.dp).verticalScroll(rememberScrollState())) { CarNameCard(coach); tone(false); voice(); about(); reset() }
			}
		}
	}
}

@Composable
internal fun SectionTitle(title: String, note: String? = null) {
	val p = LocalPalette.current
	Row(verticalAlignment = Alignment.CenterVertically) {
		Txt(title, Type.label.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold), p.sub)
		if (note != null) {
			Box(Modifier.padding(horizontal = 14.dp).size(width = 1.dp, height = 18.dp).background(p.muted))
			Txt(note, Type.label, p.sub)
		}
	}
	Spacer(Modifier.height(8.dp))
}

/** 정차 중에만 보이는 화면이라 키보드를 써도 된다 */
@Composable
private fun CarNameCard(coach: Coach) {
	val p = LocalPalette.current
	var typing by remember { mutableStateOf(false) }
	var text by remember { mutableStateOf("") }
	val current = coach.carName.value
	Section(first = true) {
		Column {
			SectionTitle("내 차 이름", "첫 화면 이름: ${current.rang} 말하기")
			Choices((CAR_NAMES + if (current !in CAR_NAMES) listOf(current) else emptyList()).map { n ->
				ChoiceItem(n, if (n == Progress.DEFAULT_CAR_NAME) "기본" else "", current == n && !typing) { typing = false; coach.setCarName(n) }
			} + ChoiceItem("직접 쓰기", "${Progress.MAX_CAR_NAME}자까지", typing) { typing = true; text = "" })
			if (typing) {
				Spacer(Modifier.height(20.dp))
				Row(verticalAlignment = Alignment.CenterVertically) {
					val shape = RoundedCornerShape(Size.radius)
					androidx.compose.foundation.text.BasicTextField(
						value = text,
						onValueChange = { text = it.take(Progress.MAX_CAR_NAME) },
						singleLine = true,
						textStyle = Type.titleS.copy(color = p.text),
						cursorBrush = androidx.compose.ui.graphics.SolidColor(p.text),
						modifier = Modifier.weight(1f).heightIn(min = Size.touch).clip(shape).background(p.surfaceAlt).padding(horizontal = 28.dp, vertical = 18.dp),
						decorationBox = { inner -> if (text.isEmpty()) Txt("예: 붕붕이", Type.titleS, p.sub); inner() },
					)
					Spacer(Modifier.width(16.dp))
					BigButton("정하기", { if (text.isNotBlank()) { coach.setCarName(text); typing = false } })
				}
			}
		}
	}
}

@Composable
private fun Section(first: Boolean = false, content: @Composable () -> Unit) {
	val p = LocalPalette.current
	Column(Modifier.fillMaxWidth()) {
		if (!first) Box(Modifier.fillMaxWidth().height(1.dp).background(p.line))
		Box(Modifier.fillMaxWidth().padding(vertical = 32.dp)) { content() }
	}
}

private val CAR_NAMES = listOf(Progress.DEFAULT_CAR_NAME, "우리 차", "애마", "붕붕이")

private class ChoiceItem(val title: String, val sub: String, val selected: Boolean, val onClick: () -> Unit)

@Composable
private fun Choices(items: List<ChoiceItem>) {
	val p = LocalPalette.current
	Column(Modifier.fillMaxWidth()) {
		items.forEachIndexed { i, it ->
			if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(p.line))
			Choice(it.title, it.sub, it.selected, it.onClick)
		}
	}
}

@Composable
private fun Choice(title: String, sub: String, selected: Boolean, onClick: () -> Unit) {
	val p = LocalPalette.current
	Row(
		Modifier.fillMaxWidth().heightIn(min = 96.dp).clickable(role = Role.RadioButton, onClick = onClick).padding(vertical = 12.dp),
		verticalAlignment = Alignment.CenterVertically,
	) {
		Txt(title, if (selected) Type.bodyL.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold) else Type.bodyL, modifier = Modifier.widthIn(min = 240.dp))
		Txt(sub, Type.bodyM, p.sub, Modifier.weight(1f))
		Spacer(Modifier.width(16.dp))
		Radio(selected)
	}
}

@Composable
internal fun Radio(selected: Boolean) {
	val p = LocalPalette.current
	if (selected) CheckBadge(36.dp, p.text, p.bg)
	else Box(Modifier.size(36.dp).clip(androidx.compose.foundation.shape.CircleShape).border(2.dp, p.muted, androidx.compose.foundation.shape.CircleShape))
}

@Composable
internal fun Header(title: String, back: () -> Unit, trailing: @Composable () -> Unit = {}) {
	Row(Modifier.fillMaxWidth().height(96.dp), verticalAlignment = Alignment.CenterVertically) {
		BackButton(back)
		Spacer(Modifier.width(8.dp))
		Txt(title, Type.titleL.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold))
		Spacer(Modifier.weight(1f))
		trailing()
	}
}
