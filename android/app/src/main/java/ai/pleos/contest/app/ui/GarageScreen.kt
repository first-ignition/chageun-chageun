package ai.pleos.contest.app.ui

import ai.pleos.contest.app.Coach
import ai.pleos.contest.app.Item
import ai.pleos.contest.app.Missions
import ai.pleos.contest.app.Points
import ai.pleos.contest.app.R
import ai.pleos.contest.app.Slot
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 내 차(결정기록 D29). 안 산 것을 누르면 먼저 차에 달아 보여 주고(미리 보기), "OOO P로 달기"를 눌러야 산다. */
@Composable
fun GarageScreen(coach: Coach, back: () -> Unit) {
	val p = LocalPalette.current
	var preview by remember { mutableStateOf<Item?>(null) }
	val shown = coach.equipped.value.toMutableMap().apply { preview?.let { put(it.slot, it) } }
	val pick: (Item) -> Unit = { item ->
		when {
			item in coach.owned.value -> { coach.wear(item); preview = null }
			preview == item -> preview = null
			else -> preview = item
		}
	}
	BoxWithConstraints(Modifier.fillMaxSize().background(p.bg)) {
		val wide = maxWidth >= Width.wide
		Column(Modifier.fillMaxSize().padding(horizontal = gutter(maxWidth), vertical = 24.dp)) {
			Header("내 차", back) { PointsBlock(coach.points) }
			Spacer(Modifier.height(8.dp))
			if (wide) {
				Row(Modifier.fillMaxSize()) {
					Box(Modifier.weight(0.6f).fillMaxHeight().padding(end = 64.dp, bottom = 48.dp), contentAlignment = Alignment.Center) {
						CarOnRoute(coach, shown, Modifier.fillMaxWidth())
					}
					Box(Modifier.padding(top = 24.dp, bottom = 48.dp).width(1.dp).fillMaxHeight().background(p.line))
					Column(Modifier.weight(0.4f).fillMaxHeight().verticalScroll(rememberScrollState()).padding(start = 64.dp, bottom = 24.dp)) {
						Shelf(coach, shown, preview, pick) { preview = null }
					}
				}
			} else {
				// 차는 위에 고정: 아래 목록에서 고른 것을 미리 달아 볼 때도 차가 보이게
				Spacer(Modifier.height(16.dp))
				CarOnRoute(coach, shown, Modifier.fillMaxWidth())
				Spacer(Modifier.height(24.dp))
				Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
					Shelf(coach, shown, preview, pick) { preview = null }
				}
			}
		}
	}
}

@Composable
private fun PointsBlock(points: Int) {
	val p = LocalPalette.current
	Column(horizontalAlignment = Alignment.End) {
		Txt("%,d P".format(points), Type.titleL.copy(fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"))
		Txt("차가 확인할 때만 쌓여요", Type.label, p.sub)
	}
}

/** 노선 한 줄 위에 내 차. 앞바퀴가 지금 역(오늘의 역, 다 끝났으면 마지막 역) 위에 오게 하고, 화면 밖으로는 안 나가게 */
@Composable
private fun CarOnRoute(coach: Coach, shown: Map<Slot, Item>, modifier: Modifier) {
	val total = Missions.all.size
	val n = coach.learned
	val pending = coach.todayPending
	val dot = 16.dp
	BoxWithConstraints(modifier) {
		val w = maxWidth
		val carW = w * (if (w >= Width.compact) 0.66f else 0.86f)
		val carH = carW * (CAR_H / CAR_W)
		val routeH = dot * 2.2f
		// RouteLine과 같은 계산: 양 끝 여백 = 큰 점 반지름
		val big = dot / 2 * 1.55f
		val step = (w - big * 2) / (total - 1)
		val head = (if (pending) n else n - 1).coerceIn(0, total - 1)
		val left = (big + step * head - carW * FRONT_WHEEL_X).coerceIn(0.dp, w - carW)
		val ground = carH * GROUND_Y
		Column {
			Box(Modifier.fillMaxWidth().height(ground + routeH / 2)) {
				RouteLine(n, total, pending, Modifier.offset(y = ground - routeH / 2), dot = dot)
				CarPicture(shown, coach.carName.value, beginner = !coach.allDone, modifier = Modifier.offset(x = left).width(carW))
			}
			Spacer(Modifier.height(12.dp))
			Txt(
				"$n / ${total}역" + if (coach.allDone) " · 종점" else "",
				Type.label, LocalPalette.current.sub,
				Modifier.offset(x = (big + step * head).coerceIn(0.dp, w - 1.dp)).centerOn().offset(y = 16.dp),
			)
		}
	}
}

/**
 * car_base는 노선이 차 뒤로 지나가게 바탕색으로 덮는 층이다.
 * 그림은 Codex(GPT 이미지)로 그린 재료를 같은 굵기 선으로 맞춘 것(docs/04_아이디어/시안).
 */
@Composable
fun CarPicture(shown: Map<Slot, Item>, name: String, beginner: Boolean, modifier: Modifier = Modifier, tags: Boolean = true) {
	val p = LocalPalette.current
	val color = shown[Slot.COLOR]?.bodyTint?.let { Color(it) }
	val body = when {
		color == null -> p.surface
		p.night -> lerp(color, p.bg, 0.55f)
		else -> color
	}
	val roofFill = if (shown[Slot.COLOR] == Item.AMBER) p.surface else p.bubble
	BoxWithConstraints(modifier.heightIn(min = 1.dp)) {
		val w = maxWidth
		val h = w * (CAR_H / CAR_W)
		Box(Modifier.width(w).height(h)) {
			Layer(R.drawable.car_base, p.bg)
			Layer(R.drawable.car_body, body)
			shown[Slot.WINDOW]?.let { Accessory(it, p.surface) }
			Layer(R.drawable.car_line, p.text)
			shown[Slot.ROOF]?.let { Accessory(it, roofFill) }
			// 글자는 그림이 아니라 글자로(이름이 바뀌고, 선명하게). 크기는 차 크기를 따라가되 20sp 아래로는 안 줄인다
			// 첫 화면의 작은 차는 글자를 빼고 그림만(작은 꼬리표 글자는 한눈에 안 읽힌다)
			if (!tags) return@Box
			val fs = (w.value * 0.026f).coerceIn(20f, 26f).sp
			Tag(name, fs, Modifier.align(Alignment.TopStart).offset(x = w * PLATE_CX, y = h * PLATE_CY))
			if (beginner) Tag("기능 초보", fs, Modifier.align(Alignment.TopStart).offset(x = w * QUARTER_CX, y = h * QUARTER_CY))
		}
	}
}

@Composable
private fun Layer(res: Int, tint: Color) {
	Image(painterResource(res), null, Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds, colorFilter = ColorFilter.tint(tint))
}

@Composable
private fun Accessory(item: Item, fill: Color) {
	item.fill?.let { Layer(it, fill) }
	item.line?.let { Layer(it, LocalPalette.current.text) }
}

@Composable
private fun Tag(text: String, size: androidx.compose.ui.unit.TextUnit, modifier: Modifier) {
	val p = LocalPalette.current
	Box(
		modifier.centerOn()
			.clip(RoundedCornerShape(6.dp)).background(p.surface).border(2.dp, p.text, RoundedCornerShape(6.dp))
			.padding(horizontal = 10.dp, vertical = 2.dp),
	) { Txt(text, Type.label.copy(fontSize = size, lineHeight = size * 1.25f, fontWeight = FontWeight.SemiBold), maxLines = 1) }
}

/** 놓인 자리(offset)를 가운데로 삼는다 */
private fun Modifier.centerOn(): Modifier = this.then(
	Modifier.layout { m, c ->
		val pl = m.measure(c)
		layout(pl.width, pl.height) { pl.place(-pl.width / 2, -pl.height / 2) }
	},
)

@Composable
private fun Shelf(coach: Coach, shown: Map<Slot, Item>, preview: Item?, pick: (Item) -> Unit, done: () -> Unit) {
	val buy: (Item) -> Unit = { if (coach.wear(it)) done() }
	SectionTitle(Slot.ROOF.label)
	Rows(coach, Item.of(Slot.ROOF), shown, preview, pick, buy)
	Spacer(Modifier.height(40.dp))
	SectionTitle(Slot.COLOR.label)
	Swatches(coach, preview, pick, buy)
	Spacer(Modifier.height(40.dp))
	SectionTitle(Slot.WINDOW.label)
	Rows(coach, Item.of(Slot.WINDOW), shown, preview, pick, buy)
}

@Composable
private fun Rows(coach: Coach, items: List<Item>, shown: Map<Slot, Item>, preview: Item?, pick: (Item) -> Unit, buy: (Item) -> Unit) {
	val p = LocalPalette.current
	Column(Modifier.fillMaxWidth()) {
		items.forEachIndexed { i, item ->
			Box(Modifier.fillMaxWidth().height(1.dp).background(p.line))
			val owned = item in coach.owned.value
			val on = shown[item.slot] == item
			Row(
				Modifier.fillMaxWidth().heightIn(min = 96.dp).clickable(role = Role.RadioButton) { pick(item) }.padding(vertical = 12.dp),
				verticalAlignment = Alignment.CenterVertically,
			) {
				Txt(item.label, if (on) Type.bodyL.copy(fontWeight = FontWeight.SemiBold) else Type.bodyL)
				Spacer(Modifier.width(16.dp))
				Txt(
					when {
						owned && on -> "달았어요"
						owned -> "가지고 있어요"
						else -> "%,d P".format(item.price)
					},
					Type.bodyM.copy(fontFeatureSettings = "tnum"), p.sub, Modifier.weight(1f),
				)
				Spacer(Modifier.width(16.dp))
				Radio(on && owned)
			}
			if (preview == item) BuyLine(coach, item, buy)
			if (i == items.lastIndex) Box(Modifier.fillMaxWidth().height(1.dp).background(p.line))
		}
	}
}

@Composable
private fun BuyLine(coach: Coach, item: Item, buy: (Item) -> Unit) {
	val p = LocalPalette.current
	Column(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
		Txt("미리 달아 봤어요", Type.label, p.sub)
		Spacer(Modifier.height(12.dp))
		if (coach.points >= item.price) BigButton("%,d P로 달기".format(item.price), { buy(item) }, Modifier.widthIn(min = 280.dp))
		else Txt("%,d P 더 모으면 달 수 있어요. 새 기능 하나에 %d P예요.".format(item.price - coach.points, Points.PER_LEARNED), Type.bodyM, p.sub)
	}
}

@Composable
private fun Swatches(coach: Coach, preview: Item?, pick: (Item) -> Unit, buy: (Item) -> Unit) {
	val p = LocalPalette.current
	val colors = Item.of(Slot.COLOR)
	val current = coach.equipped.value[Slot.COLOR]
	Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
		Swatch("기본", p.surface, current == null) {
			coach.equipped.value[Slot.COLOR]?.let { coach.wear(it) }
			if (preview?.slot == Slot.COLOR) pick(preview)
		}
		colors.forEach { c -> c.bodyTint?.let { Swatch(c.label, Color(it), current == c) { pick(c) } } }
	}
	if (preview?.slot == Slot.COLOR) {
		Spacer(Modifier.height(20.dp))
		BuyLine(coach, preview, buy)
	}
}

@Composable
private fun Swatch(label: String, color: Color, selected: Boolean, onClick: () -> Unit) {
	val p = LocalPalette.current
	Column(
		Modifier.widthIn(min = 76.dp).clickable(role = Role.RadioButton, onClick = onClick).padding(vertical = 8.dp),
		horizontalAlignment = Alignment.CenterHorizontally,
	) {
		Box(Modifier.size(60.dp), contentAlignment = Alignment.Center) {
			Box(Modifier.size(60.dp).clip(CircleShape).background(if (p.night) lerp(color, p.bg, 0.55f) else color).border(2.dp, if (selected) p.text else p.line, CircleShape))
			if (selected) CheckBadge(32.dp, p.text, p.bg)
		}
		Spacer(Modifier.height(8.dp))
		Txt(label, Type.label, if (selected) p.text else p.sub)
	}
}

// 차 그림 원본(px)에서 잰 크기와 기준점
private const val CAR_W = 1560f
private const val CAR_H = 810f
private const val FRONT_WHEEL_X = 0.7994f
private const val GROUND_Y = 0.9704f
private const val PLATE_CX = 0.95f
private const val PLATE_CY = 0.7216f
private const val QUARTER_CX = 0.2055f
private const val QUARTER_CY = 0.43f
