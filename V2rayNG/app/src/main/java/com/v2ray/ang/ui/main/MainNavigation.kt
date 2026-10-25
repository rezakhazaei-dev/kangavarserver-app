package com.v2ray.ang.ui.main

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.v2ray.ang.R

/** Screens opened from the settings tab; each maps to an activity in [MainActivity]. */
enum class MainDestination(@DrawableRes val iconRes: Int, @StringRes val labelRes: Int) {
    Subscriptions(R.drawable.ic_subscriptions_24dp, R.string.title_sub_setting),
    PerAppProxy(R.drawable.ic_per_apps_24dp, R.string.per_app_proxy_settings),
    Routing(R.drawable.ic_routing_24dp, R.string.routing_settings_title),
    UserAssets(R.drawable.ic_file_24dp, R.string.title_user_asset_setting),
    Settings(R.drawable.ic_settings_24dp, R.string.title_settings),
    Promotion(R.drawable.ic_promotion_24dp, R.string.title_pref_promotion),
    Logcat(R.drawable.ic_logcat_24dp, R.string.title_logcat),
    CheckUpdate(R.drawable.ic_check_update_24dp, R.string.update_check_for_update),
    BackupRestore(R.drawable.ic_restore_24dp, R.string.title_configuration_backup_restore),
    About(R.drawable.ic_about_24dp, R.string.title_about)
}

/** Top-level tabs of the main screen, in navigation order; the filled icon marks the selected tab. */
enum class MainTab(@DrawableRes val iconRes: Int, @DrawableRes val selectedIconRes: Int, @StringRes val labelRes: Int) {
    Home(R.drawable.ic_ms_home, R.drawable.ic_ms_home_fill, R.string.tab_home),
    Servers(R.drawable.ic_ms_language, R.drawable.ic_ms_language_fill, R.string.tab_servers),
    Subscription(R.drawable.ic_ms_card_membership, R.drawable.ic_ms_card_membership_fill, R.string.tab_subscription),
    More(R.drawable.ic_ms_settings, R.drawable.ic_ms_settings_fill, R.string.tab_more),
}

/** Width from which the tabs move from a bottom bar to a side navigation rail. */
private val RailMinWidth: Dp = 600.dp

/** Phones in landscape are short; a rail keeps the vertical space for content. */
private val RailMaxCompactHeight: Dp = 480.dp

/** Content width from which the home and subscription tabs switch to two columns. */
private val TwoColumnMinWidth: Dp = 640.dp

/** Rail width of [NavigationRail], removed from the width left for the content. */
private val RailWidth: Dp = 80.dp

/** Phones narrower than this get the smaller type and control sizes of the design. */
private val SmallPhoneMaxWidth: Dp = 330.dp

/** Landscape phones: the hero drops its pill and quick-connect chips. */
private val ShortHeightMax: Dp = 520.dp

/** Short portrait phones: the quick-connect caption is dropped. */
private val CompactHeightMax: Dp = 700.dp

/** Tablets and unfolded foldables: the extra speed chart and side cards appear. */
private val WideScreenMinWidth: Dp = 900.dp

/** Layout facts the tab contents adapt to. */
data class MainLayoutInfo(
    val useRail: Boolean,
    /** Enough width for two columns (landscape phones, foldables, tablets). */
    val twoColumns: Boolean,
    /** Narrow phones (under 330dp) get smaller hero sizes. */
    val small: Boolean,
    /** Rail layout on a short window (landscape phone). */
    val shortHeight: Boolean = false,
    /** Window under 700dp tall. */
    val compactHeight: Boolean = false,
    /** Window at least 900dp wide. */
    val wideScreen: Boolean = false,
)

/**
 * Main-screen shell that follows the Material 3 window size classes: a bottom
 * [NavigationBar] on compact phones and a [NavigationRail] on landscape phones,
 * foldables and tablets. [showNavigation] false hides both (welcome screen).
 */
@Composable
fun MainAdaptiveScaffold(
    selectedTab: MainTab,
    onSelectTab: (MainTab) -> Unit,
    showNavigation: Boolean = true,
    content: @Composable (MainLayoutInfo) -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val windowWidth = maxWidth
        val windowHeight = maxHeight
        val useRail = windowWidth >= RailMinWidth || (windowWidth > windowHeight && windowHeight < RailMaxCompactHeight)
        val compactHeight = windowHeight < CompactHeightMax
        val wideScreen = windowWidth >= WideScreenMinWidth
        if (useRail) {
            val layout = MainLayoutInfo(
                useRail = true,
                twoColumns = (windowWidth - RailWidth) >= TwoColumnMinWidth || windowWidth > windowHeight,
                small = false,
                shortHeight = windowHeight < ShortHeightMax,
                compactHeight = compactHeight,
                wideScreen = wideScreen,
            )
            Row(Modifier.fillMaxSize()) {
                if (showNavigation) {
                    NavigationRail(
                        containerColor = Bz.Bg.copy(alpha = 0.4f),
                        contentColor = Bz.Muted,
                        modifier = Modifier
                            .fillMaxHeight()
                            .drawBehind {
                                // Hairline between the rail and the content, on the content side.
                                val x = if (layoutDirection == LayoutDirection.Rtl) 0f else size.width - 1.dp.toPx()
                                drawRect(Bz.Line, topLeft = Offset(x, 0f), size = Size(1.dp.toPx(), size.height))
                            },
                    ) {
                        Spacer(Modifier.height(if (layout.shortHeight) 8.dp else 16.dp))
                        MainTab.entries.forEach { tab ->
                            val selected = tab == selectedTab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { onSelectTab(tab) },
                                icon = { TabIcon(tab, selected) },
                                label = { TabLabel(tab) },
                                colors = NavigationRailItemDefaults.colors(
                                    indicatorColor = Bz.AccentDim,
                                    selectedIconColor = Bz.Accent,
                                    selectedTextColor = Bz.Text,
                                    unselectedIconColor = Bz.Muted,
                                    unselectedTextColor = Bz.Muted,
                                ),
                            )
                        }
                    }
                }
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .then(
                            if (showNavigation) Modifier.consumeWindowInsets(WindowInsets.systemBars.only(WindowInsetsSides.Start))
                            else Modifier
                        )
                ) {
                    content(layout)
                }
            }
        } else {
            val layout = MainLayoutInfo(
                useRail = false,
                twoColumns = false,
                small = windowWidth < SmallPhoneMaxWidth,
                compactHeight = compactHeight,
                wideScreen = false,
            )
            Column(Modifier.fillMaxSize()) {
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .then(
                            if (showNavigation) Modifier.consumeWindowInsets(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
                            else Modifier
                        )
                ) {
                    content(layout)
                }
                if (showNavigation) {
                    NavigationBar(
                        containerColor = Bz.Bg.copy(alpha = 0.92f),
                        contentColor = Bz.Muted,
                        tonalElevation = 0.dp,
                        modifier = Modifier.drawBehind {
                            drawRect(Bz.Line, size = Size(size.width, 1.dp.toPx()))
                        },
                    ) {
                        MainTab.entries.forEach { tab ->
                            val selected = tab == selectedTab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { onSelectTab(tab) },
                                icon = { TabIcon(tab, selected) },
                                label = { TabLabel(tab) },
                                colors = NavigationBarItemDefaults.colors(
                                    indicatorColor = Bz.AccentDim,
                                    selectedIconColor = Bz.Accent,
                                    selectedTextColor = Bz.Text,
                                    unselectedIconColor = Bz.Muted,
                                    unselectedTextColor = Bz.Muted,
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TabIcon(tab: MainTab, selected: Boolean) {
    Icon(
        painterResource(if (selected) tab.selectedIconRes else tab.iconRes),
        contentDescription = null,
        modifier = Modifier.size(23.dp),
    )
}

@Composable
private fun TabLabel(tab: MainTab) {
    Text(stringResource(tab.labelRes), style = bzText(12.sp, FontWeight.SemiBold, Color.Unspecified))
}
