package ai.pleos.contest.app.ui

import ai.pleos.contest.app.Signal
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size as GSize
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 차 버튼 그림. 실제 차의 버튼 모양을 굵고 둥근 선으로 단순하게 그린다(정지 그림).
 * on = true면 "차가 켜진 것을 확인함"으로 칸이 성공색으로 채워진다 — 차가 확인한 항목만 그림이 바뀐다
 * (Tesla Car Status 방식, docs/06_레퍼런스/리서치_인카UX/04_종합_비교와_적용.md 후보 C2).
 */
@Composable
fun ControlIcon(signal: Signal, on: Boolean?, size: Dp = 64.dp) {
	val p = LocalPalette.current
	val lit = on == true
	val bg = if (lit) p.success else p.surfaceAlt
	val fg = if (lit) p.onSuccess else p.text
	Box(Modifier.size(size).clip(RoundedCornerShape(size * 0.28f)).background(bg), contentAlignment = Alignment.Center) {
		Canvas(Modifier.size(size).padding(size * 0.18f)) { drawControl(signal, fg) }
	}
}

@Composable
fun ControlGlyph(signal: Signal, color: Color, size: Dp = 32.dp) {
	Canvas(Modifier.size(size)) { drawControl(signal, color) }
}

private fun DrawScope.drawControl(s: Signal, c: Color) {
	val w = size.width
	val h = size.height
	val sw = w * 0.085f
	val st = Stroke(sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
	fun line(x1: Float, y1: Float, x2: Float, y2: Float) = drawLine(c, Offset(w * x1, h * y1), Offset(w * x2, h * y2), sw, StrokeCap.Round)
	fun path(block: Path.() -> Unit) = drawPath(Path().apply(block), c, style = st)
	// 위로 올라가는 물결 화살표 3개 (열·바람)
	fun heatWaves(y0: Float, y1: Float, xs: List<Float>) = xs.forEach { x ->
		path {
			moveTo(w * x, h * y0)
			cubicTo(w * (x - 0.07f), h * (y0 - (y0 - y1) * 0.33f), w * (x + 0.07f), h * (y0 - (y0 - y1) * 0.66f), w * x, h * y1)
		}
	}
	when (s) {
		Signal.FRONT_DEFROST -> {
			// 앞 유리(위가 좁은 사다리꼴, 아래가 둥근) + 물결 3개
			path {
				moveTo(w * 0.08f, h * 0.30f); quadraticTo(w * 0.5f, h * 0.10f, w * 0.92f, h * 0.30f)
				lineTo(w * 0.80f, h * 0.62f); quadraticTo(w * 0.5f, h * 0.52f, w * 0.20f, h * 0.62f); close()
			}
			heatWaves(0.98f, 0.66f, listOf(0.32f, 0.5f, 0.68f))
		}
		Signal.REAR_DEFROST -> {
			// 뒷유리(직사각형) + 가로 열선 + 물결
			drawRoundRect(c, Offset(w * 0.1f, h * 0.12f), GSize(w * 0.8f, h * 0.5f), CornerRadius(w * 0.08f), style = st)
			line(0.22f, 0.29f, 0.78f, 0.29f); line(0.22f, 0.45f, 0.78f, 0.45f)
			heatWaves(0.98f, 0.70f, listOf(0.32f, 0.5f, 0.68f))
		}
		Signal.HVAC_AUTO, Signal.HVAC_AC -> {
			// 팬 + 글자 대신 굵은 표시: AUTO는 'A', A/C는 눈송이
			drawCircle(c, w * 0.42f, Offset(w * 0.5f, h * 0.5f), style = st)
			if (s == Signal.HVAC_AUTO) {
				path { moveTo(w * 0.32f, h * 0.70f); lineTo(w * 0.5f, h * 0.28f); lineTo(w * 0.68f, h * 0.70f) }
				line(0.39f, 0.56f, 0.61f, 0.56f)
			} else {
				repeat(3) { i ->
					val a = Math.toRadians(90.0 + i * 60.0)
					val dx = (Math.cos(a) * 0.24).toFloat(); val dy = (Math.sin(a) * 0.24).toFloat()
					line(0.5f - dx, 0.5f - dy, 0.5f + dx, 0.5f + dy)
				}
			}
		}
		Signal.RECIRC -> {
			// 차 옆모습(둥근 지붕) + 실내에서 도는 화살표 — 실제 내기 순환 버튼 기호
			path {
				moveTo(w * 0.04f, h * 0.80f); lineTo(w * 0.04f, h * 0.60f); quadraticTo(w * 0.04f, h * 0.50f, w * 0.14f, h * 0.49f)
				lineTo(w * 0.24f, h * 0.48f); lineTo(w * 0.36f, h * 0.24f); quadraticTo(w * 0.39f, h * 0.18f, w * 0.46f, h * 0.18f)
				lineTo(w * 0.66f, h * 0.18f); quadraticTo(w * 0.72f, h * 0.18f, w * 0.75f, h * 0.24f); lineTo(w * 0.86f, h * 0.48f)
				quadraticTo(w * 0.96f, h * 0.50f, w * 0.96f, h * 0.60f); lineTo(w * 0.96f, h * 0.80f); close()
			}
			drawArc(c, 160f, 250f, false, Offset(w * 0.33f, h * 0.36f), GSize(w * 0.34f, h * 0.32f), style = st)
			path { moveTo(w * 0.27f, h * 0.46f); lineTo(w * 0.34f, h * 0.57f); lineTo(w * 0.43f, h * 0.48f) }
		}
		Signal.AIR_CLEANER -> {
			// 잎사귀
			path {
				moveTo(w * 0.2f, h * 0.85f); cubicTo(w * 0.1f, h * 0.35f, w * 0.5f, h * 0.1f, w * 0.88f, h * 0.12f)
				cubicTo(w * 0.9f, h * 0.55f, w * 0.6f, h * 0.85f, w * 0.2f, h * 0.85f); close()
			}
			line(0.2f, 0.85f, 0.62f, 0.42f)
		}
		Signal.FAN_SPEED -> {
			// 날개 3개(가운데서 뻗은 잎 모양)
			repeat(3) { i ->
				val a = Math.toRadians(i * 120.0 - 90.0)
				val ca = Math.cos(a).toFloat(); val sa = Math.sin(a).toFloat()
				val tipX = 0.5f + ca * 0.46f; val tipY = 0.5f + sa * 0.46f
				// 날개 양옆 조절점: 진행 방향에 수직으로 벌림
				val nx = -sa * 0.19f; val ny = ca * 0.19f
				path {
					moveTo(w * 0.5f, h * 0.5f)
					quadraticTo(w * (0.5f + ca * 0.22f + nx), h * (0.5f + sa * 0.22f + ny), w * tipX, h * tipY)
					quadraticTo(w * (0.5f + ca * 0.22f - nx), h * (0.5f + sa * 0.22f - ny), w * 0.5f, h * 0.5f)
				}
			}
			drawCircle(c, w * 0.08f, Offset(w * 0.5f, h * 0.5f))
		}
		Signal.TEMP_DRIVER, Signal.TEMP_PASSENGER -> {
			// 온도계(실제 차도 좌우 온도 버튼에 같은 기호를 쓴다 — 어느 자리인지는 글로 말한다)
			drawRoundRect(c, Offset(w * 0.40f, h * 0.06f), GSize(w * 0.2f, h * 0.6f), CornerRadius(w * 0.1f), style = st)
			drawCircle(c, w * 0.17f, Offset(w * 0.5f, h * 0.78f), style = st)
			line(0.5f, 0.38f, 0.5f, 0.66f)
		}
		Signal.WHEEL_HEAT -> {
			// 핸들 + 물결
			drawCircle(c, w * 0.30f, Offset(w * 0.5f, h * 0.38f), style = st)
			line(0.22f, 0.40f, 0.78f, 0.40f); line(0.5f, 0.40f, 0.5f, 0.66f)
			heatWaves(1.0f, 0.78f, listOf(0.3f, 0.5f, 0.7f))
		}
		Signal.SEAT_HEAT_DRIVER, Signal.SEAT_VENT_DRIVER -> {
			// 의자 옆모습(기울어진 등받이 + 방석) + 물결 셋(열선) 또는 점 셋(통풍)
			rotate(-12f, Offset(w * 0.26f, h * 0.62f)) {
				drawRoundRect(c, Offset(w * 0.14f, h * 0.08f), GSize(w * 0.20f, h * 0.56f), CornerRadius(w * 0.08f), style = st)
			}
			drawRoundRect(c, Offset(w * 0.16f, h * 0.66f), GSize(w * 0.66f, h * 0.18f), CornerRadius(w * 0.08f), style = st)
			if (s == Signal.SEAT_HEAT_DRIVER) heatWaves(0.56f, 0.16f, listOf(0.50f, 0.66f, 0.82f))
			else listOf(0.50f, 0.66f, 0.82f).forEach { x -> listOf(0.24f, 0.44f).forEach { y -> drawCircle(c, w * 0.045f, Offset(w * x, h * y)) } }
		}
		Signal.READING_LIGHT -> {
			// 천장 독서등: 위의 작은 등 + 아래로 퍼지는 빛 원뿔
			drawRoundRect(c, Offset(w * 0.34f, h * 0.08f), GSize(w * 0.32f, h * 0.16f), CornerRadius(w * 0.06f), style = st)
			line(0.36f, 0.36f, 0.20f, 0.86f); line(0.64f, 0.36f, 0.80f, 0.86f); line(0.5f, 0.38f, 0.5f, 0.86f)
		}
		Signal.AMBIENT_COLOR -> {
			// 무드등: 실내를 따라 흐르는 가는 빛줄기(아래 긴 막대) + 위로 번지는 짧은 빛 셋
			drawRoundRect(c, Offset(w * 0.08f, h * 0.62f), GSize(w * 0.84f, h * 0.14f), CornerRadius(w * 0.07f), style = st)
			line(0.30f, 0.46f, 0.26f, 0.30f); line(0.50f, 0.46f, 0.50f, 0.26f); line(0.70f, 0.46f, 0.74f, 0.30f)
		}
		Signal.HIGH_BEAM -> {
			// 전조등 + 수평 빛줄기
			path { moveTo(w * 0.42f, h * 0.18f); cubicTo(w * 0.02f, h * 0.18f, w * 0.02f, h * 0.82f, w * 0.42f, h * 0.82f); close() }
			listOf(0.26f, 0.42f, 0.58f, 0.74f).forEach { y -> line(0.56f, y, 0.94f, y) }
		}
		Signal.TURN_SIGNAL -> {
			// 좌우 화살표(계기판의 방향지시등 기호) — 다른 그림처럼 선으로
			path { moveTo(w * 0.06f, h * 0.5f); lineTo(w * 0.38f, h * 0.24f); lineTo(w * 0.38f, h * 0.76f); close() }
			path { moveTo(w * 0.94f, h * 0.5f); lineTo(w * 0.62f, h * 0.24f); lineTo(w * 0.62f, h * 0.76f); close() }
		}
		Signal.WIPER_FRONT -> {
			// 앞 유리(김 서림 그림과 같은 모양) + 아래에서 뻗은 와이퍼 날 둘
			path {
				moveTo(w * 0.06f, h * 0.30f); quadraticTo(w * 0.5f, h * 0.08f, w * 0.94f, h * 0.30f)
				lineTo(w * 0.80f, h * 0.80f); quadraticTo(w * 0.5f, h * 0.70f, w * 0.20f, h * 0.80f); close()
			}
			line(0.30f, 0.74f, 0.20f, 0.36f)
			line(0.62f, 0.72f, 0.50f, 0.30f)
		}
		Signal.CRUISE_TARGET -> {
			// 속도계 반원 + 바늘
			drawArc(c, 180f, 180f, false, Offset(w * 0.08f, h * 0.2f), GSize(w * 0.84f, h * 0.84f), style = st)
			line(0.5f, 0.62f, 0.72f, 0.36f)
			drawCircle(c, w * 0.06f, Offset(w * 0.5f, h * 0.62f))
		}
		Signal.GEAR -> {
			// 변속 레버: 둥근 손잡이 + 기둥 + 받침
			drawCircle(c, w * 0.16f, Offset(w * 0.5f, h * 0.24f), style = st)
			line(0.5f, 0.40f, 0.5f, 0.74f); line(0.26f, 0.86f, 0.74f, 0.86f)
		}
		Signal.ODOMETER -> {
			// 길: 멀리서 모이는 두 선 + 가운데 점선
			line(0.36f, 0.10f, 0.12f, 0.90f); line(0.64f, 0.10f, 0.88f, 0.90f)
			line(0.5f, 0.14f, 0.5f, 0.28f); line(0.5f, 0.44f, 0.5f, 0.58f); line(0.5f, 0.74f, 0.5f, 0.90f)
		}
	}
}

/** 아이콘 검수판(디버그 빌드의 --es page icons로만 열린다): 모든 차 버튼 그림을 꺼짐/켜짐으로 나란히 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun IconSheet() {
	val p = LocalPalette.current
	androidx.compose.foundation.layout.FlowRow(
		Modifier.background(p.bg).padding(48.dp),
		horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(24.dp),
		verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(24.dp),
	) {
		Signal.entries.forEach { s ->
			androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.CenterHorizontally) {
				androidx.compose.foundation.layout.Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
					ControlIcon(s, on = false, size = 96.dp)
					ControlIcon(s, on = true, size = 96.dp)
				}
				Txt(s.name.lowercase(), Type.label, p.sub)
			}
		}
	}
}
