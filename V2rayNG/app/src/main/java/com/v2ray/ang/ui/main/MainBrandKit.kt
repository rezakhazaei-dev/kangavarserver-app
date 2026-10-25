package com.v2ray.ang.ui.main

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.v2ray.ang.R
import com.v2ray.ang.ui.compose.BrandLatinFont
import com.v2ray.ang.ui.compose.BrandPersianFont

/** Aurora Mint design tokens of the Bazhiko main screen (see the approved app design). */
internal object Bz {
    val Bg = Color(0xFF08120F)
    val Bg2 = Color(0xFF0F2B2A)
    val Glass = Color.White.copy(alpha = 0.055f)
    val Glass2 = Color.White.copy(alpha = 0.085f)
    val Line = Color.White.copy(alpha = 0.10f)
    val Line2 = Color.White.copy(alpha = 0.16f)
    val Accent = Color(0xFF3DF5B0)
    val Accent2 = Color(0xFF9CF8DB)
    val AccentDim = Accent.copy(alpha = 0.14f)
    val Warm = Color(0xFFFFB08A)
    val WarmDim = Warm.copy(alpha = 0.14f)
    val Text = Color(0xFFF2FBF7)
    val Muted = Color(0xFF9DB5AD)
    val Faint = Color(0xFF6C857D)
    val On = Color(0xFF06211A)
    val Error = Color(0xFFFF6B6B)
    val Sheet = Color(0xFF11211E)

    val CardShape = RoundedCornerShape(10.dp)
    val RowShape = RoundedCornerShape(8.dp)
    val BoxShape = RoundedCornerShape(6.dp)

    val AccentGradient = Brush.horizontalGradient(listOf(Accent2, Accent))
}

internal fun pingColor(quality: PingQuality): Color = when (quality) {
    PingQuality.Good -> Bz.Accent
    PingQuality.Medium -> Bz.Warm
    PingQuality.Bad -> Bz.Error
    PingQuality.Unknown -> Bz.Faint
}

/** Screen grounds of the design: default, connected glow, expired (warm) and error (red). */
internal enum class BzBackdrop { Default, Connected, Warm, Error }

/** Paints the design's gradient ground, top glow and fading 48dp grid; cached until the size changes. */
internal fun Modifier.bzBackdrop(kind: BzBackdrop): Modifier = drawWithCache {
    val top = when (kind) {
        BzBackdrop.Default -> Bz.Bg2
        BzBackdrop.Connected -> Color(0xFF0E3329)
        BzBackdrop.Warm -> Color(0xFF1F211C)
        BzBackdrop.Error -> Color(0xFF1E1F1C)
    }
    val vertical = Brush.verticalGradient(
        0f to top,
        (if (kind == BzBackdrop.Connected) 0.68f else 0.62f) to Bz.Bg,
        startY = 0f,
        endY = size.height,
    )
    val glowColor = when (kind) {
        BzBackdrop.Default -> Bz.Accent.copy(alpha = 0.20f)
        BzBackdrop.Connected -> Bz.Accent.copy(alpha = 0.30f)
        BzBackdrop.Warm -> Bz.Warm.copy(alpha = 0.16f)
        BzBackdrop.Error -> Bz.Error.copy(alpha = 0.14f)
    }
    val glowCenter = Offset(size.width / 2f, if (kind == BzBackdrop.Connected) 150.dp.toPx() else -40.dp.toPx())
    val glowRadius = (if (kind == BzBackdrop.Connected) 330.dp else 300.dp).toPx()
    val glow = Brush.radialGradient(
        0f to glowColor,
        0.72f to Color.Transparent,
        center = glowCenter,
        radius = glowRadius,
    )
    val gridStep = 48.dp.toPx()
    val gridFadeEnd = size.height * 0.55f
    val gridColor = Color.White.copy(alpha = 0.025f)
    val verticalLineBrush = Brush.verticalGradient(0f to gridColor, 1f to Color.Transparent, endY = gridFadeEnd)
    onDrawBehind {
        drawRect(vertical)
        drawRect(glow)
        var x = gridStep
        while (x < size.width) {
            drawLine(verticalLineBrush, Offset(x, 0f), Offset(x, gridFadeEnd), strokeWidth = 1f)
            x += gridStep
        }
        var y = gridStep
        while (y < gridFadeEnd) {
            drawLine(gridColor.copy(alpha = 0.025f * (1f - y / gridFadeEnd)), Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
            y += gridStep
        }
    }
}

/** The design's translucent card: glass fill with a hairline border. */
internal fun Modifier.glass(shape: Shape = Bz.RowShape, fill: Color = Bz.Glass, border: Color = Bz.Line): Modifier =
    clip(shape).background(fill).border(1.dp, border, shape)

internal fun bzText(size: TextUnit, weight: FontWeight = FontWeight.Normal, color: Color = Bz.Text) = TextStyle(
    fontFamily = BrandPersianFont,
    fontSize = size,
    fontWeight = weight,
    color = color,
)

/** Logo mark next to the brand name, mark no taller than the name (Reza's sizing rule). */
@Composable
internal fun BzWordmark(modifier: Modifier = Modifier, small: Boolean = false) {
    val name = stringResource(R.string.app_name)
    val persian = isArabicScript(name)
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        Image(
            painter = painterResource(R.drawable.brand_mark_dark),
            contentDescription = null,
            modifier = Modifier.size(if (small) 17.dp else 19.dp),
        )
        Text(
            text = name,
            style = TextStyle(
                fontFamily = if (persian) BrandPersianFont else BrandLatinFont,
                fontWeight = if (persian) FontWeight.Black else FontWeight.Bold,
                fontSize = if (small) 16.sp else 18.sp,
                letterSpacing = if (persian) 0.sp else 1.2.sp,
                color = Bz.Text,
            ),
        )
    }
}

/** Round 40dp glass button of the top bars. */
@Composable
internal fun BzIconButton(
    @DrawableRes iconRes: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    small: Boolean = false,
    tint: Color = Bz.Text,
) {
    Box(
        modifier
            .size(if (small) 36.dp else 40.dp)
            .glass(CircleShape)
            .clickable(role = Role.Button, onClickLabel = contentDescription, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(iconRes), contentDescription = contentDescription, tint = tint, modifier = Modifier.size(21.dp))
    }
}

/** Top bar with a title (or any start content) and round action buttons at the end. */
@Composable
internal fun BzTopBar(
    modifier: Modifier = Modifier,
    height: Dp = 52.dp,
    start: @Composable RowScope.() -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier
            .fillMaxWidth()
            .height(height),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        start()
        Spacer(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, content = actions)
    }
}

@Composable
internal fun BzTitle(text: String) {
    Text(text, style = bzText(20.sp, FontWeight.ExtraBold))
}

internal enum class BzButtonStyle { Filled, Outlined, Text }

/** Pill button of the design: mint gradient, glass outline or warm text. */
@Composable
internal fun BzButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: BzButtonStyle = BzButtonStyle.Filled,
    @DrawableRes iconRes: Int? = null,
    height: Dp = 52.dp,
    enabled: Boolean = true,
    textColor: Color? = null,
    fontSize: TextUnit = 15.sp,
) {
    val content = when (style) {
        BzButtonStyle.Filled -> Bz.On
        BzButtonStyle.Outlined -> Bz.Text
        BzButtonStyle.Text -> Bz.Warm
    }
    val shapeModifier = when (style) {
        BzButtonStyle.Filled -> Modifier
            .clip(CircleShape)
            .background(if (enabled) Bz.AccentGradient else Brush.horizontalGradient(listOf(Bz.Glass2, Bz.Glass2)))

        BzButtonStyle.Outlined -> Modifier.glass(CircleShape, border = Bz.Line2)
        BzButtonStyle.Text -> Modifier.clip(CircleShape)
    }
    Row(
        modifier
            .heightIn(min = if (style == BzButtonStyle.Text) 40.dp else height)
            .then(shapeModifier)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val color = textColor ?: if (enabled) content else Bz.Faint
        if (iconRes != null) {
            Icon(painterResource(iconRes), contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        }
        Text(
            text,
            style = bzText(fontSize, if (style == BzButtonStyle.Text) FontWeight.Bold else FontWeight.ExtraBold, color),
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}

/** Small rounded status pill with a coloured dot; the dot blinks while [blinking]. */
@Composable
internal fun BzStatePill(text: String, dotColor: Color, textColor: Color, borderColor: Color, blinking: Boolean = false) {
    val dotAlpha = if (blinking) {
        val transition = rememberInfiniteTransition(label = "pillDot")
        transition.animateFloat(
            initialValue = 1f,
            targetValue = 0.25f,
            animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse),
            label = "pillDotAlpha",
        )
    } else {
        null
    }
    Row(
        Modifier
            .height(30.dp)
            .glass(CircleShape, border = borderColor)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier
                .size(8.dp)
                .drawBehind {
                    val alpha = dotAlpha?.value ?: 1f
                    if (dotColor == Bz.Accent) drawCircle(Bz.AccentDim, radius = size.minDimension / 2 + 4.dp.toPx())
                    drawCircle(dotColor.copy(alpha = alpha))
                }
        )
        Text(text, style = bzText(13.sp, FontWeight.SemiBold, textColor))
    }
}

internal enum class BzMarkTone { Ok, Error, Warm }

/**
 * The Bazhiko mark inside its glow, dashed orbit and inner ring. The mark fills about 45% of the
 * circle so it sits inside it (Reza, 2026-10-25). An optional badge sits at the bottom left.
 */
@Composable
internal fun BzMark(
    size: Dp,
    tone: BzMarkTone = BzMarkTone.Ok,
    @DrawableRes badgeIconRes: Int? = null,
    animate: Boolean = true,
) {
    val color = when (tone) {
        BzMarkTone.Ok -> Bz.Accent
        BzMarkTone.Error -> Bz.Error
        BzMarkTone.Warm -> Bz.Warm
    }
    val markRes = when (tone) {
        BzMarkTone.Ok -> R.drawable.brand_mark_dark
        BzMarkTone.Error -> R.drawable.brand_mark_error
        BzMarkTone.Warm -> R.drawable.brand_mark_warm
    }
    val rotation = if (animate) {
        val transition = rememberInfiniteTransition(label = "markOrbit")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(24_000, easing = LinearEasing)),
            label = "markOrbitRotation",
        )
    } else {
        null
    }
    val scale = size / 168.dp
    Box(
        Modifier
            .size(size)
            .drawBehind {
                val r = this.size.minDimension / 2f
                drawCircle(
                    Brush.radialGradient(
                        0f to color.copy(alpha = 0.30f),
                        0.55f to color.copy(alpha = 0.06f),
                        0.70f to Color.Transparent,
                        center = center,
                        radius = r - 6.dp.toPx() * scale,
                    ),
                    radius = r - 6.dp.toPx() * scale,
                )
                drawCircle(color.copy(alpha = 0.18f), radius = r - 20.dp.toPx() * scale, style = Stroke(1.dp.toPx()))
                rotate(rotation?.value ?: 0f) {
                    drawCircle(
                        color.copy(alpha = 0.35f),
                        radius = r - 0.5.dp.toPx(),
                        style = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx()))),
                    )
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Image(painterResource(markRes), contentDescription = null, modifier = Modifier.size(size * 0.45f))
        if (badgeIconRes != null) {
            Box(
                Modifier
                    .align(AbsoluteAlignment.BottomLeft)
                    .offset(x = 30.dp * scale, y = (-22).dp * scale)
                    .size(38.dp * scale.coerceAtLeast(0.8f))
                    .border(4.dp, Bz.Bg, CircleShape)
                    .padding(4.dp)
                    .clip(CircleShape)
                    .background(color),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(badgeIconRes), contentDescription = null, tint = Bz.On, modifier = Modifier.size(20.dp))
            }
        }
    }
}

/** Round country flag; countries without a drawn flag get a globe. */
@Composable
internal fun BzFlag(country: ServerCountry, size: Dp, ring: Boolean = true) {
    val ringModifier = if (ring) Modifier.border(2.dp, Color.White.copy(alpha = 0.12f), CircleShape) else Modifier
    val flagRes = country.flagRes
    if (flagRes != null) {
        Image(
            painterResource(flagRes),
            contentDescription = null,
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .then(ringModifier),
        )
    } else {
        Box(
            Modifier
                .size(size)
                .clip(CircleShape)
                .background(Bz.Glass2)
                .then(ringModifier),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(R.drawable.ic_ms_language), contentDescription = null, tint = Bz.Muted, modifier = Modifier.size(size * 0.6f))
        }
    }
}

/** Circle with the smart-selection sparkle. */
@Composable
internal fun BzSmartIcon(size: Dp = 38.dp) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(Bz.AccentDim),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(R.drawable.ic_ms_auto_awesome_fill), contentDescription = null, tint = Bz.Accent, modifier = Modifier.size(size * 0.55f))
    }
}

/** Four signal bars and the delay in ms; a dash while untested. */
@Composable
internal fun BzPing(delayMillis: Long, digits: (Long) -> String) {
    val level = pingLevel(delayMillis)
    val color = pingColor(pingQuality(delayMillis))
    Column(Modifier.width(46.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(Modifier.height(15.dp), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.Bottom) {
            listOf(5.dp, 8.dp, 11.dp, 15.dp).forEachIndexed { index, barHeight ->
                Box(
                    Modifier
                        .size(width = 4.dp, height = barHeight)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (index < level) color else Color.White.copy(alpha = 0.14f))
                )
            }
        }
        Text(
            text = if (delayMillis.isMeasuredDelay()) stringResource(R.string.bz_ping_ms, digits(delayMillis)) else "—",
            style = bzText(12.sp, FontWeight.ExtraBold, if (delayMillis.isMeasuredDelay()) color else Bz.Faint),
            maxLines = 1,
        )
    }
}

/** Small tag of server rows: protocol (left-to-right) or a warm note. */
@Composable
internal fun BzTag(text: String, warm: Boolean = false) {
    Text(
        text,
        style = bzText(11.sp, FontWeight.Bold, if (warm) Bz.Warm else Bz.Muted),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .clip(Bz.BoxShape)
            .background(if (warm) Bz.WarmDim else Color.White.copy(alpha = 0.07f))
            .padding(horizontal = 8.dp, vertical = 1.dp),
    )
}

/** Radio dot of the server list. */
@Composable
internal fun BzRadio(selected: Boolean) {
    Box(
        Modifier
            .size(22.dp)
            .border(2.dp, if (selected) Bz.Accent else Bz.Line2, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Box(
                Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(Bz.Accent)
            )
        }
    }
}

/** Square-ish icon tile at the start of list rows. */
@Composable
internal fun BzRowIcon(@DrawableRes iconRes: Int, tint: Color = Bz.Accent2) {
    Box(
        Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(Color.White.copy(alpha = 0.06f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(iconRes), contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
    }
}

/** Glass list group with hairline dividers between its rows. */
@Composable
internal fun BzListGroup(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .glass(Bz.RowShape)
    ) { content() }
}

@Composable
internal fun BzDivider() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Bz.Line)
    )
}

/** Accent section header of the settings tab, aligned to the start (right in Persian). */
@Composable
internal fun BzSectionHeader(text: String) {
    Text(
        text,
        style = bzText(12.sp, FontWeight.Bold, Bz.Accent),
        textAlign = TextAlign.Start,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 6.dp, end = 6.dp, top = 16.dp, bottom = 6.dp),
    )
}

/** Brand-coloured switch: mint track with a dark thumb when on. */
@Composable
internal fun BzSwitch(checked: Boolean) {
    Switch(
        checked = checked,
        onCheckedChange = null,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Bz.On,
            checkedTrackColor = Bz.Accent,
            checkedBorderColor = Bz.Accent,
            uncheckedThumbColor = Bz.Muted,
            uncheckedTrackColor = Color.White.copy(alpha = 0.10f),
            uncheckedBorderColor = Bz.Line2,
        ),
    )
}


