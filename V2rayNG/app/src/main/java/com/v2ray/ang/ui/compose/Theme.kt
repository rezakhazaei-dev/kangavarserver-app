package com.v2ray.ang.ui.compose

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.v2ray.ang.AppConfig
import com.v2ray.ang.R
import com.v2ray.ang.handler.MmkvManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// Bazhiko "Aurora Mint" brand palette.
private val LightColor = lightColorScheme(
    primary = Color(0xFF0B7A57), // Deep Mint
    onPrimary = Color(0xFFFFFFFF), // White
    primaryContainer = Color(0xFFB8F5DD), // Pale Mint
    onPrimaryContainer = Color(0xFF002117), // Deep Teal
    secondary = Color(0xFF2E6B5C), // Teal
    onSecondary = Color(0xFFFFFFFF), // White
    secondaryContainer = Color(0xFFCDE9E0), // Pale Teal
    onSecondaryContainer = Color(0xFF062019), // Deep Teal
    tertiary = Color(0xFFA4502A), // Copper
    onTertiary = Color(0xFFFFFFFF), // White
    tertiaryContainer = Color(0xFFFFDBCB), // Pale Peach
    onTertiaryContainer = Color(0xFF351000), // Dark Brown
    error = Color(0xFFBA1A1A), // Red
    errorContainer = Color(0xFFFFDAD6), // Light Red
    onError = Color(0xFFFFFFFF), // White
    onErrorContainer = Color(0xFF410002), // Dark Red
    background = Color(0xFFF5FAF7), // Mint White
    onBackground = Color(0xFF0E1A17), // Teal Black
    surface = Color(0xFFF5FAF7), // Mint White
    onSurface = Color(0xFF0E1A17), // Teal Black
    surfaceVariant = Color(0xFFDCE7E2), // Mint Gray
    onSurfaceVariant = Color(0xFF3F4B47), // Dark Teal Gray
    outline = Color(0xFF6F7C77), // Teal Gray
    outlineVariant = Color(0xFFBFCAC5), // Light Teal Gray
    inverseSurface = Color(0xFF243230), // Dark Teal
    inverseOnSurface = Color(0xFFE9F3EF), // Mint White
    inversePrimary = Color(0xFF3DF5B0), // Bright Mint
    scrim = Color(0xFF000000), // Black
    surfaceTint = Color(0xFF0B7A57), // Deep Mint
    surfaceContainerLowest = Color(0xFFFFFFFF), // White
    surfaceContainerLow = Color(0xFFEFF5F2), // Mint Gray
    surfaceContainer = Color(0xFFE9F0ED), // Mint Gray
    surfaceContainerHigh = Color(0xFFE3EBE7), // Mint Gray
    surfaceContainerHighest = Color(0xFFDDE5E1), // Mint Gray
)

private val DarkColor = darkColorScheme(
    primary = Color(0xFF3DF5B0), // Bright Mint
    onPrimary = Color(0xFF06211A), // Deep Teal
    primaryContainer = Color(0xFF0F4D3B), // Dark Mint
    onPrimaryContainer = Color(0xFF9CF8DB), // Light Mint
    secondary = Color(0xFF9CF8DB), // Light Mint
    onSecondary = Color(0xFF06211A), // Deep Teal
    secondaryContainer = Color(0xFF17302C), // Teal Surface
    onSecondaryContainer = Color(0xFFD6FBEF), // Pale Mint
    tertiary = Color(0xFFFFB08A), // Warm Peach
    onTertiary = Color(0xFF2A1408), // Dark Brown
    tertiaryContainer = Color(0xFF5A2E18), // Brown
    onTertiaryContainer = Color(0xFFFFE9DD), // Pale Peach
    error = Color(0xFFFF6B6B), // Coral Red
    errorContainer = Color(0xFF5C1A1A), // Dark Red
    onError = Color(0xFF2A0A0A), // Deep Red
    onErrorContainer = Color(0xFFFFDAD6), // Light Red
    background = Color(0xFF08120F), // Teal Black
    onBackground = Color(0xFFF2FBF7), // Mint White
    surface = Color(0xFF08120F), // Teal Black
    onSurface = Color(0xFFF2FBF7), // Mint White
    surfaceVariant = Color(0xFF17302C), // Teal Surface
    onSurfaceVariant = Color(0xFF9DB5AD), // Muted Mint
    outline = Color(0xFF6C857D), // Teal Gray
    outlineVariant = Color(0xFF24403A), // Dark Teal Gray
    inverseSurface = Color(0xFFF2FBF7), // Mint White
    inverseOnSurface = Color(0xFF0F2B2A), // Dark Teal
    inversePrimary = Color(0xFF0B7A57), // Deep Mint
    scrim = Color(0xFF000000), // Black
    surfaceTint = Color(0xFF3DF5B0), // Bright Mint
    surfaceContainerLowest = Color(0xFF050D0B), // Near Black
    surfaceContainerLow = Color(0xFF0C1A17), // Teal Black
    surfaceContainer = Color(0xFF10221F), // Dark Teal
    surfaceContainerHigh = Color(0xFF142A26), // Dark Teal
    surfaceContainerHighest = Color(0xFF193330), // Dark Teal
)

private val Vazirmatn = FontFamily(
    Font(R.font.vazirmatn_regular, FontWeight.Normal),
    Font(R.font.vazirmatn_medium, FontWeight.Medium),
    Font(R.font.vazirmatn_semibold, FontWeight.SemiBold),
    Font(R.font.vazirmatn_bold, FontWeight.Bold),
    Font(R.font.vazirmatn_extrabold, FontWeight.ExtraBold),
    Font(R.font.vazirmatn_black, FontWeight.Black),
)

/** Brand wordmark faces: Outfit Bold for the Latin "BAZHIKO", Vazirmatn Black for «باژیکو». */
val BrandLatinFont = FontFamily(Font(R.font.outfit_bold, FontWeight.Bold))
val BrandPersianFont = Vazirmatn

private val AppTypography = Typography().let { base ->
    Typography(
        displayLarge = base.displayLarge.copy(fontFamily = Vazirmatn),
        displayMedium = base.displayMedium.copy(fontFamily = Vazirmatn),
        displaySmall = base.displaySmall.copy(fontFamily = Vazirmatn),
        headlineLarge = base.headlineLarge.copy(fontFamily = Vazirmatn),
        headlineMedium = base.headlineMedium.copy(fontFamily = Vazirmatn),
        headlineSmall = base.headlineSmall.copy(fontFamily = Vazirmatn),
        titleLarge = base.titleLarge.copy(fontFamily = Vazirmatn),
        titleMedium = base.titleMedium.copy(fontFamily = Vazirmatn),
        titleSmall = base.titleSmall.copy(fontFamily = Vazirmatn),
        bodyLarge = base.bodyLarge.copy(fontFamily = Vazirmatn),
        bodyMedium = base.bodyMedium.copy(fontFamily = Vazirmatn),
        bodySmall = base.bodySmall.copy(fontFamily = Vazirmatn),
        labelLarge = base.labelLarge.copy(fontFamily = Vazirmatn),
        labelMedium = base.labelMedium.copy(fontFamily = Vazirmatn),
        labelSmall = base.labelSmall.copy(fontFamily = Vazirmatn),
    )
}

// Low-radius panels per the brand design; pills (buttons, chips) keep CircleShape at call sites.
private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(10.dp),
    extraLarge = RoundedCornerShape(12.dp),
)

// Semantic Colors
val colorPing = Color(0xFF1FAF7A) // Mint Green
val colorPingRed = Color(0xFFFF6B6B) // Coral Red
val colorConfigType = Color(0xFFD9774A) // Copper
val colorFabActive = Color(0xFF0B8A63) // Deep Mint
val colorFabInactiveLight = Color(0xFF9C9C9C) // Gray
val colorFabInactiveDark = Color(0xFF646464) // Dark Gray
val dividerColorLight = Color(0xFFE0E0E0) // Light Gray
val dividerColorDark = Color(0xFF424242) // Dark Gray

// Toast Colors 85%
val toastNormalBgLight = Color(0xD9353A3E) // Dark Gray
val toastNormalBgDark = Color(0xD94A4F54) // Darker Gray
val toastSuccessBg = Color(0xD9388E3C) // Green
val toastErrorBg = Color(0xD9D50000) // Red
val toastInfoBg = Color(0xD90E5A43) // Deep mint
val toastIconCircleBg = Color(0x33FFFFFF) // Semi-transparent White
val toastTextColor = Color.White // White

object ThemeManager {
    private val _themeMode = MutableStateFlow(
        MmkvManager.decodeSettingsString(AppConfig.PREF_UI_MODE_NIGHT, AppConfig.DEFAULT_UI_MODE_NIGHT) ?: AppConfig.DEFAULT_UI_MODE_NIGHT
    )
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _dynamicColorEnabled = MutableStateFlow(
        MmkvManager.decodeSettingsBool(AppConfig.PREF_DYNAMIC_COLOR, AppConfig.DEFAULT_DYNAMIC_COLOR)
    )
    val dynamicColorEnabled: StateFlow<Boolean> = _dynamicColorEnabled.asStateFlow()

    fun setThemeMode(mode: String) {
        MmkvManager.encodeSettings(AppConfig.PREF_UI_MODE_NIGHT, mode)
        _themeMode.value = mode
    }

    fun setDynamicColorEnabled(enabled: Boolean) {
        MmkvManager.encodeSettings(AppConfig.PREF_DYNAMIC_COLOR, enabled)
        _dynamicColorEnabled.value = enabled
    }

    fun refresh() {
        _themeMode.value =
            MmkvManager.decodeSettingsString(AppConfig.PREF_UI_MODE_NIGHT, AppConfig.DEFAULT_UI_MODE_NIGHT) ?: AppConfig.DEFAULT_UI_MODE_NIGHT
        _dynamicColorEnabled.value =
            MmkvManager.decodeSettingsBool(AppConfig.PREF_DYNAMIC_COLOR, AppConfig.DEFAULT_DYNAMIC_COLOR)
    }
}

@Composable
fun resolveDarkTheme(): Boolean {
    val mode by ThemeManager.themeMode.collectAsState()
    return when (mode) {
        "1" -> false
        "2" -> true
        else -> isSystemInDarkTheme()
    }
}

val LocalDarkTheme = compositionLocalOf { false }

@Composable
fun AppTheme(
    darkTheme: Boolean = resolveDarkTheme(),
    content: @Composable () -> Unit
) {
    val dynamicColor by ThemeManager.dynamicColorEnabled.collectAsState()
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColor
        else -> LightColor
    }
    val snackbarController = rememberAppSnackbarController()

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as? Activity ?: return@SideEffect
            val window = activity.window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(
        LocalDarkTheme provides darkTheme,
        LocalAppSnackbar provides snackbarController
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            shapes = AppShapes
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AppSnackbarBridge(controller = snackbarController)
                content()
                AppSnackbarHost(hostState = snackbarController.hostState)
            }
        }
    }
}
