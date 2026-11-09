package ai.pleos.contest.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import ai.pleos.contest.app.R
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.em
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 색: 낮(종이)·밤(먹) 두 벌. 강조색은 호박색 하나 — 화면마다 말풍선이나 "오늘의 역" 한 곳에만 쓴다.
 * 버튼·체크·지나온 역은 먹색(잉크). 초록·남색은 쓰지 않는다(디자인 v2, docs/05_개발/디자인_v2.md).
 * 글자 대비: 본문 15.5:1 이상, 보조 글자 6.3:1(낮)·8.2:1(밤). muted는 글자가 아닌 선·빈 역에만(3:1 이상).
 */
@Immutable
data class Palette(
	val bg: Color,
	val surface: Color,
	val surfaceAlt: Color,
	val text: Color,
	val sub: Color,
	val line: Color,
	val primary: Color,
	val onPrimary: Color,
	val bubble: Color,
	val onBubble: Color,
	val success: Color,
	val onSuccess: Color,
	val muted: Color,
	/** 도장 잉크: 진짜 도장처럼 먹색과 다른 갈색(낮)·호박(밤). 대비 5.3:1 / 8.3:1 */
	val stamp: Color,
	val night: Boolean = false,
)

private val Day = Palette(
	bg = Color(0xFFF5F1EA),
	surface = Color(0xFFFFFDF9),
	surfaceAlt = Color(0xFFEBE5DA),
	text = Color(0xFF1B1A17),
	sub = Color(0xFF5E574C),
	line = Color(0xFFE2DBCF),
	primary = Color(0xFF1B1A17),
	onPrimary = Color(0xFFFFFDF9),
	bubble = Color(0xFFF6C453),
	onBubble = Color(0xFF1B1A17),
	success = Color(0xFF1B1A17),
	onSuccess = Color(0xFFFFFDF9),
	muted = Color(0xFF8C8478),
	stamp = Color(0xFF8A5A12),
)

// 밤: 따뜻한 먹색. 호박색은 조금 낮춰 검은 시스템 화면 옆에서 눈부시지 않게
private val Night = Palette(
	bg = Color(0xFF121110),
	surface = Color(0xFF1C1A17),
	surfaceAlt = Color(0xFF2A2723),
	text = Color(0xFFF2EDE4),
	sub = Color(0xFFB3AA9C),
	line = Color(0xFF2E2A25),
	// 밤의 주 버튼: 낮의 "종이 위 먹색"을 뒤집되 한 단계 낮춘 종이색(가장 밝은 면이 되지 않게, 꺼진 버튼처럼 보이지도 않게)
	primary = Color(0xFFB5AD9F),
	onPrimary = Color(0xFF121110),
	bubble = Color(0xFFD9A441),
	onBubble = Color(0xFF1B1A17),
	success = Color(0xFFF2EDE4),
	onSuccess = Color(0xFF121110),
	muted = Color(0xFF7A7266),
	stamp = Color(0xFFD9A441),
	night = true,
)

/**
 * 글꼴: Asta Sans(플레오스 시스템 글꼴, 42dot, SIL OFL 1.1 — assets/licenses/AstaSans-OFL.txt).
 * 기본 글꼴로 두면 한글이 Noto Sans CJK Regular 한 벌로 그려지고 굵기는 흉내만 낸다. 시스템 화면과 같은 글꼴을 앱에 넣어
 * 차 화면 옆에서 이질감이 없고, 굵기 단계가 진짜로 나온다.
 */
val AppFont = FontFamily(
	Font(R.font.asta_regular, FontWeight.Normal),
	Font(R.font.asta_medium, FontWeight.Medium),
	Font(R.font.asta_semibold, FontWeight.SemiBold),
	Font(R.font.asta_bold, FontWeight.Bold),
)

// 글자 상자 위아래 여백을 글꼴 높이에 딱 맞춘다 — 아이콘·버튼과 세로 가운데가 정확히 맞게
private val Tight = PlatformTextStyle(includeFontPadding = false)
private val Center = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None)

private fun style(size: Int, line: Int, weight: FontWeight, track: Float = 0f) = TextStyle(
	fontFamily = AppFont, fontSize = size.sp, lineHeight = line.sp, fontWeight = weight,
	letterSpacing = track.em, platformStyle = Tight, lineHeightStyle = Center,
)

/**
 * 글자 단계: Pleos 디자인 시스템 크기(Headline 60·56·48 / Title 40·36·32 / Body 30·28·26 / Label 24)는 그대로,
 * 굵기는 셋(Bold 문장·SemiBold 제목·Regular 본문)과 라벨용 Medium만. 큰 글자는 자간을 조금 좁히고(한글 큰 제목이 헐거워 보이지 않게),
 * 본문은 줄 간격을 1.45배 안팎으로 넓혀 읽기 편하게.
 * 가장 작은 글자도 24sp — 정차 전용 화면 권장(24sp)과 최소 12sp를 모두 넘는다.
 */
object Type {
	val count = style(96, 100, FontWeight.Bold, -0.03f).copy(fontFeatureSettings = "tnum")
	/** 주행 중 말풍선(넓은 화면): 앱에서 가장 큰 글자 — 1~2초에 읽혀야 하는 화면이라 */
	val bubbleXL = style(84, 100, FontWeight.SemiBold, -0.03f)
	val bubbleL = style(72, 88, FontWeight.SemiBold, -0.025f)
	val bubble = style(56, 70, FontWeight.SemiBold, -0.02f)
	val bubbleM = style(52, 66, FontWeight.SemiBold, -0.02f)
	val bubbleS = style(48, 60, FontWeight.SemiBold, -0.02f)
	val headlineL = style(60, 78, FontWeight.Bold, -0.02f)
	val headlineM = style(56, 72, FontWeight.Bold, -0.02f)
	val headlineS = style(48, 62, FontWeight.Bold, -0.015f)
	val titleL = style(40, 54, FontWeight.SemiBold, -0.01f)
	val titleM = style(36, 50, FontWeight.SemiBold, -0.01f)
	val titleS = style(32, 44, FontWeight.SemiBold, -0.005f)
	val bodyL = style(30, 42, FontWeight.Normal)
	val bodyM = style(28, 40, FontWeight.Normal)
	val bodyS = style(26, 38, FontWeight.Normal)
	val label = style(24, 32, FontWeight.Medium, 0.01f)
}

/** 크기: 터치 타깃 76dp, 여백은 8의 배수(8·16·24·32·48·64·96) (Google Design for Driving 수치, 07_디자인_가이드) */
object Size {
	val touch = 76.dp
	val pad = 48.dp
	/** 모서리는 셋만: 줄·아이콘 칸 12 / 버튼·판 20 / 말풍선 32 */
	val radiusS = 12.dp
	val radius = 20.dp
	val radiusL = 32.dp
}

object Width {
	val narrow = 700.dp
	val compact = 900.dp
	val split = 1300.dp
	val wide = 1500.dp
}

fun gutter(width: Dp): Dp = if (width < Width.compact) 32.dp else Size.pad

val LocalPalette = staticCompositionLocalOf { Day }

@Composable
fun AppTheme(content: @Composable () -> Unit) {
	val p = if (isSystemInDarkTheme()) Night else Day
	CompositionLocalProvider(LocalPalette provides p, content = content)
}
