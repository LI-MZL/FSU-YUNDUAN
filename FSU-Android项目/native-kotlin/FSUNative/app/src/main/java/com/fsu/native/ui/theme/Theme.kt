package com.fsu.native.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * FSU主题定义 - 品牌色: 铁塔红 #DC2626
 */

// 铁塔红品牌色
val TowerRed = Color(0xFFDC2626)
val TowerRedLight = Color(0xFFEF4444)
val TowerRedDark = Color(0xFFB91C1C)

// 主题扩展色
val TowerGreen = Color(0xFF22C55E)  // 在线/正常状态
val TowerBlue = Color(0xFF3B82F6)   // 信息/链接
val TowerYellow = Color(0xFFF59E0B) // 告警/警告
val TowerOrange = Color(0xFFF97316) // 二级告警
val TowerGray = Color(0xFF6B7280)    // 离线/禁用

private val LightColorScheme = lightColorScheme(
    primary = TowerRed,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFCDD2),
    onPrimaryContainer = Color(0xFF7F0000),
    secondary = TowerBlue,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFBBDEFB),
    onSecondaryContainer = Color(0xFF003258),
    tertiary = TowerGreen,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFC8E6C9),
    onTertiaryContainer = Color(0xFF003300),
    error = TowerRed,
    onError = Color.White,
    errorContainer = Color(0xFFFFCDD2),
    onErrorContainer = Color(0xFF7F0000),
    background = Color(0xFFFFFBFE),
    onBackground = Color(0xFF1C1B1F),
    surface = Color(0xFFFFFBFE),
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFF3E0E0),
    onSurfaceVariant = Color(0xFF49454F)
)

private val DarkColorScheme = darkColorScheme(
    primary = TowerRedLight,
    onPrimary = Color(0xFF690005),
    primaryContainer = Color(0xFF950000),
    onPrimaryContainer = Color(0xFFFFDAD6),
    secondary = TowerBlue,
    onSecondary = Color(0xFF062E5F),
    secondaryContainer = Color(0xFF0D4A8C),
    onSecondaryContainer = Color(0xFFD6E3FF),
    tertiary = TowerGreen,
    onTertiary = Color(0xFF00390E),
    tertiaryContainer = Color(0xFF005319),
    onTertiaryContainer = Color(0xFFA4F593),
    error = TowerRedLight,
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF1C1B1F),
    onBackground = Color(0xFFE6E1E5),
    surface = Color(0xFF1C1B1F),
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFCAC4D0)
)

@Composable
fun FSUNativeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // 禁用Material You动态取色
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
