package com.hextech.vango

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Responsive density within a device shell (spacing, card radius).
 * For phone vs tablet **shell** choice use [isPhoneUi] / [isTablet].
 */
enum class AppWidthClass {
    Compact,
    Expanded,
}

data class AppLayoutMetrics(
    val widthClass: AppWidthClass,
    val maxWidth: Dp,
) {
    val isCompact: Boolean get() = widthClass == AppWidthClass.Compact
    val pagePadding: PaddingValues
        get() = if (isCompact) {
            PaddingValues(horizontal = 12.dp, vertical = 8.dp)
        } else {
            PaddingValues(horizontal = 14.dp, vertical = 10.dp)
        }
    val sectionGap: Dp get() = if (isCompact) 10.dp else 12.dp
    val cardRadius: Dp get() = if (isCompact) 14.dp else 18.dp
}

val LocalAppLayout = staticCompositionLocalOf {
    AppLayoutMetrics(AppWidthClass.Expanded, 960.dp)
}

/** Tighter spacing when pane is narrow (e.g. tablet portrait). Always true on phone. */
@Composable
fun isCompactLayout(): Boolean {
    val local = LocalAppLayout.current
    return local.isCompact || isPhoneUi()
}

@Composable
fun ProvideAppLayout(content: @Composable () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val fromWidth = if (maxWidth < 600.dp) AppWidthClass.Compact else AppWidthClass.Expanded
        val widthClass = if (isPhoneUi()) AppWidthClass.Compact else fromWidth
        CompositionLocalProvider(
            LocalAppLayout provides AppLayoutMetrics(widthClass, maxWidth),
        ) {
            content()
        }
    }
}
