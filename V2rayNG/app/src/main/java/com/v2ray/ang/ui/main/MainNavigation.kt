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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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

/** Top-level tabs of the main screen, in navigation order. */
enum class MainTab(@DrawableRes val iconRes: Int, @StringRes val labelRes: Int) {
    Home(R.drawable.ic_home_24dp, R.string.tab_home),
    Servers(R.drawable.ic_public_24dp, R.string.tab_servers),
    Subscription(R.drawable.ic_card_membership_24dp, R.string.tab_subscription),
    More(R.drawable.ic_settings_24dp, R.string.tab_more),
}

/** Width from which the tabs move from a bottom bar to a side navigation rail. */
private val RailMinWidth: Dp = 600.dp

/** Phones in landscape are short; a rail keeps the vertical space for content. */
private val RailMaxCompactHeight: Dp = 480.dp

/** Content width from which the home and subscription tabs switch to two columns. */
private val TwoColumnMinWidth: Dp = 640.dp

/** Rail width of [NavigationRail], removed from the width left for the content. */
private val RailWidth: Dp = 80.dp

/** Layout facts the tab contents adapt to. */
data class MainLayoutInfo(
    val useRail: Boolean,
    /** Enough width for two columns (landscape phones, foldables, tablets). */
    val twoColumns: Boolean,
    /** Narrow phones (under 340dp) get smaller hero sizes. */
    val small: Boolean,
)

/**
 * Main-screen shell that follows the Material 3 window size classes: a bottom
 * [NavigationBar] on compact phones and a [NavigationRail] on landscape phones,
 * foldables and tablets.
 */
@Composable
fun MainAdaptiveScaffold(
    selectedTab: MainTab,
    onSelectTab: (MainTab) -> Unit,
    content: @Composable (MainLayoutInfo) -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val windowWidth = maxWidth
        val windowHeight = maxHeight
        val useRail = windowWidth >= RailMinWidth || (windowWidth > windowHeight && windowHeight < RailMaxCompactHeight)
        if (useRail) {
            Row(Modifier.fillMaxSize()) {
                NavigationRail(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxHeight(),
                ) {
                    MainTab.entries.forEach { tab ->
                        NavigationRailItem(
                            selected = tab == selectedTab,
                            onClick = { onSelectTab(tab) },
                            icon = { Icon(painterResource(tab.iconRes), contentDescription = null) },
                            label = { Text(stringResource(tab.labelRes)) },
                            colors = NavigationRailItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            ),
                        )
                    }
                }
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .consumeWindowInsets(WindowInsets.systemBars.only(WindowInsetsSides.Start))
                ) {
                    val contentWidth = windowWidth - RailWidth
                    content(
                        MainLayoutInfo(
                            useRail = true,
                            twoColumns = contentWidth >= TwoColumnMinWidth || windowWidth > windowHeight,
                            small = false,
                        )
                    )
                }
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .consumeWindowInsets(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
                ) {
                    content(MainLayoutInfo(useRail = false, twoColumns = false, small = windowWidth < 340.dp))
                }
                NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
                    MainTab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = tab == selectedTab,
                            onClick = { onSelectTab(tab) },
                            icon = { Icon(painterResource(tab.iconRes), contentDescription = null) },
                            label = { Text(stringResource(tab.labelRes)) },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            ),
                        )
                    }
                }
            }
        }
    }
}
