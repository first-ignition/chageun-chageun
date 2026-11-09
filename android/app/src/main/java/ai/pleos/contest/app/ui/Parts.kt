package ai.pleos.contest.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Size as GSize
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun Txt(text: String, style: TextStyle, color: Color = LocalPalette.current.text, modifier: Modifier = Modifier, align: TextAlign? = null, maxLines: Int = Int.MAX_VALUE) {
	// 줄 수를 제한한 곳은 글자 중간에서 잘리지 않고 말줄임표로 끝낸다
	// 한국어도 낱말(띄어쓰기) 단위로 줄을 바꾼다 — "누 / 르면"처럼 낱말 중간에서 끊기지 않게
	Text(text = text, style = style.copy(lineBreak = KoreanLineBreak), color = color, modifier = modifier, textAlign = align, maxLines = maxLines,
		overflow = if (maxLines == Int.MAX_VALUE) androidx.compose.ui.text.style.TextOverflow.Clip else androidx.compose.ui.text.style.TextOverflow.Ellipsis)
}

private val KoreanLineBreak = androidx.compose.ui.text.style.LineBreak(
	strategy = androidx.compose.ui.text.style.LineBreak.Strategy.Balanced,
	strictness = androidx.compose.ui.text.style.LineBreak.Strictness.Normal,
	wordBreak = androidx.compose.ui.text.style.LineBreak.WordBreak.Phrase,
)

enum class BtnKind { Primary, Secondary, Quiet, Text }

@Composable
fun BigButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, kind: BtnKind = BtnKind.Primary, icon: (@Composable () -> Unit)? = null) {
	val p = LocalPalette.current
	val (bg, fg) = when (kind) {
		BtnKind.Primary -> p.primary to p.onPrimary
		BtnKind.Secondary -> p.surfaceAlt to p.text
		BtnKind.Quiet, BtnKind.Text -> Color.Transparent to p.text
	}
	val shape = RoundedCornerShape(Size.radius)
	val border = when {
		kind == BtnKind.Quiet -> Modifier.border(2.dp, p.muted, shape)
		else -> Modifier
	}
	Row(
		modifier = modifier
			.heightIn(min = Size.touch)
			.widthIn(min = 160.dp)
			.clip(shape)
			.background(bg)
			.then(border)
			.clickable(role = Role.Button, onClick = onClick)
			.padding(horizontal = if (kind == BtnKind.Text) 24.dp else 32.dp, vertical = 16.dp),
		verticalAlignment = Alignment.CenterVertically,
		horizontalArrangement = Arrangement.Center,
	) {
		if (icon != null) {
			icon()
			Spacer(Modifier.width(14.dp))
		}
		Txt(text, if (kind == BtnKind.Text) Type.titleS else Type.bodyL.copy(fontWeight = FontWeight.SemiBold), fg)
	}
}

@Composable
fun BackButton(onClick: () -> Unit) {
	val p = LocalPalette.current
	Box(
		Modifier.size(Size.touch).clip(RoundedCornerShape(Size.radius)).clickable(role = Role.Button, onClickLabel = "돌아가기", onClick = onClick),
		contentAlignment = Alignment.Center,
	) {
		Canvas(Modifier.size(36.dp)) {
			val w = size.width
			drawLine(p.text, Offset(w * 0.12f, w * 0.5f), Offset(w * 0.9f, w * 0.5f), 3.dp.toPx(), StrokeCap.Round)
			drawPath(Path().apply { moveTo(w * 0.45f, w * 0.15f); lineTo(w * 0.1f, w * 0.5f); lineTo(w * 0.45f, w * 0.85f) }, p.text,
				style = Stroke(3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
		}
	}
}

/** 둥근 판과 꼬리를 한 선으로 이어 그린다(붙여 만든 삼각형 꼬리 대신). */
private class BubbleShape(private val tailCenter: Boolean) : Shape {
	override fun createOutline(size: GSize, layoutDirection: LayoutDirection, density: Density): Outline = with(density) {
		val r = Size.radiusL.toPx()
		val tailH = 26.dp.toPx()
		val tailW = 40.dp.toPx()
		val x0 = if (tailCenter) size.width / 2 - tailW / 2 else 56.dp.toPx()
		val w = size.width
		val h = size.height - tailH
		val path = Path().apply {
			moveTo(r, 0f)
			lineTo(w - r, 0f)
			quadraticTo(w, 0f, w, r)
			lineTo(w, h - r)
			quadraticTo(w, h, w - r, h)
			lineTo(x0 + tailW, h)
			// 꼬리: 오른쪽에서 완만하게 내려와 끝이 살짝 둥근 뒤 왼쪽으로 올라간다
			cubicTo(x0 + tailW * 0.55f, h, x0 + tailW * 0.2f, h + tailH * 0.55f, x0 + 2.dp.toPx(), h + tailH)
			cubicTo(x0 + 4.dp.toPx(), h + tailH * 0.6f, x0 + 2.dp.toPx(), h + tailH * 0.2f, x0 - 6.dp.toPx(), h)
			lineTo(r, h)
			quadraticTo(0f, h, 0f, h - r)
			lineTo(0f, r)
			quadraticTo(0f, 0f, r, 0f)
			close()
		}
		Outline.Generic(path)
	}
}

/**
 * 글레오에게 하는 문장을 담는 말풍선. 말풍선이 곧 "말"이라 따옴표는 붙이지 않는다.
 * tailCenter: 가운데 정렬 화면(주행·칭찬)에서 꼬리를 가운데로.
 */
@Composable
fun Bubble(text: String, modifier: Modifier = Modifier, style: TextStyle = Type.bubble, tailCenter: Boolean = false) {
	val p = LocalPalette.current
	val shape = remember(tailCenter) { BubbleShape(tailCenter) }
	Box(
		modifier = modifier
			.clip(shape)
			.background(p.bubble)
			.padding(start = 48.dp, end = 48.dp, top = 34.dp, bottom = 34.dp + 26.dp),
	) {
		Txt(text, style, p.onBubble, align = if (tailCenter) TextAlign.Center else null)
	}
}

@Composable
fun CheckBadge(size: Dp, bg: Color, fg: Color, modifier: Modifier = Modifier) {
	Canvas(modifier.size(size).clip(CircleShape).background(bg)) {
		val w = this.size.width
		val path = Path().apply {
			moveTo(w * 0.28f, w * 0.52f)
			lineTo(w * 0.44f, w * 0.68f)
			lineTo(w * 0.74f, w * 0.36f)
		}
		drawPath(path, fg, style = Stroke(width = w * 0.09f, cap = StrokeCap.Round, join = StrokeJoin.Round))
	}
}

@Composable
fun SpeakerIcon(color: Color, size: Dp = 36.dp) {
	Canvas(Modifier.size(size)) {
		val w = this.size.width
		val body = Path().apply {
			moveTo(w * 0.10f, w * 0.38f)
			lineTo(w * 0.30f, w * 0.38f)
			lineTo(w * 0.55f, w * 0.15f)
			lineTo(w * 0.55f, w * 0.85f)
			lineTo(w * 0.30f, w * 0.62f)
			lineTo(w * 0.10f, w * 0.62f)
			close()
		}
		drawPath(body, color)
		drawArc(color, -45f, 90f, false, topLeft = Offset(w * 0.45f, w * 0.25f), size = androidx.compose.ui.geometry.Size(w * 0.35f, w * 0.5f), style = Stroke(w * 0.08f, cap = StrokeCap.Round))
		drawArc(color, -45f, 90f, false, topLeft = Offset(w * 0.45f, w * 0.10f), size = androidx.compose.ui.geometry.Size(w * 0.55f, w * 0.8f), style = Stroke(w * 0.08f, cap = StrokeCap.Round))
	}
}

/**
 * 진도 노선도: 익힌 말 = 지나온 역. 지나온 구간은 먹색 굵은 선과 채운 역, 오늘의 역은 호박색(먹색 테),
 * 남은 구간은 가는 선과 빈 역. 지하철 노선도처럼 "어디까지 왔는지"가 숫자보다 먼저 읽힌다(정지 그림).
 */
@Composable
fun RouteLine(done: Int, total: Int, todayPending: Boolean, modifier: Modifier = Modifier, dot: Dp = 18.dp) {
	val p = LocalPalette.current
	Canvas(modifier.fillMaxWidth().height(dot * 2.2f)) {
		val r = dot.toPx() / 2
		val big = r * 1.55f
		val cy = size.height / 2
		val step = if (total > 1) (size.width - big * 2) / (total - 1) else 0f
		val x = { i: Int -> big + step * i }
		val thick = 6.dp.toPx()
		val thin = 2.dp.toPx()
		val headIdx = if (todayPending) done else done - 1
		if (total > 1) drawLine(p.muted, Offset(x(headIdx.coerceAtLeast(0)), cy), Offset(x(total - 1), cy), strokeWidth = thin)
		if (headIdx > 0) drawLine(p.text, Offset(x(0), cy), Offset(x(headIdx.coerceAtMost(total - 1)), cy), strokeWidth = thick, cap = StrokeCap.Round)
		for (i in 0 until total) {
			val c = Offset(x(i), cy)
			when {
				i == total - 1 && done >= total -> {
					drawCircle(p.bg, big, c)
					drawCircle(p.text, big, c, style = Stroke(3.dp.toPx()))
					drawCircle(p.text, r * 0.75f, c)
				}
				i < done -> drawCircle(p.text, r, c)
				i == done && todayPending -> {
					drawCircle(p.bubble, big, c)
					drawCircle(p.text, big, c, style = Stroke(3.dp.toPx()))
				}
				else -> {
					drawCircle(p.bg, r * 0.8f, c)
					drawCircle(p.muted, r * 0.8f, c, style = Stroke(thin))
				}
			}
		}
	}
}

/**
 * 살짝 기울여(−6°) 손으로 찍은 승차권 도장처럼. 호박색은 말풍선 한 곳에만 남기려고 채우지 않는다.
 */
@Composable
fun Stamp(big: String, small: String) {
	val p = LocalPalette.current
	Box(Modifier.size(184.dp).rotate(-6f), contentAlignment = Alignment.Center) {
		Canvas(Modifier.matchParentSize()) {
			val r = this.size.minDimension / 2
			drawCircle(p.stamp, r - 2.dp.toPx(), style = Stroke(4.dp.toPx()))
			drawCircle(p.stamp, r - 13.dp.toPx(), style = Stroke(1.5.dp.toPx()))
		}
		androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.CenterHorizontally) {
			Txt(big, (if (big.length > 3) Type.titleL.copy(fontWeight = FontWeight.Bold) else Type.headlineL).copy(fontFeatureSettings = "tnum"), p.stamp)
			Txt(small, Type.label, p.stamp)
		}
	}
}

@Composable
fun StationStamp(n: Int, terminus: Boolean = false) =
	Stamp("$n", if (terminus) "종점" else "번째 역")
