package com.v2ray.ang.ui.main

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.v2ray.ang.BuildConfig
import com.v2ray.ang.R

/** Settings tab of the design: everyday switches up front, v2rayNG's advanced screens behind "pro mode". */
@Composable
internal fun MainSettingsTab(
    settings: QuickSettingsUiModel,
    layout: MainLayoutInfo,
    onAction: (MainAction) -> Unit,
    onNavigate: (MainDestination) -> Unit,
) {
    val numbers = rememberBzNumbers()
    val change: (QuickSetting, Boolean) -> Unit = { setting, enabled -> onAction(MainAction.ChangeQuickSetting(setting, enabled)) }
    Column(
        Modifier
            .fillMaxSize()
            .bzBackdrop(BzBackdrop.Default)
            .windowInsetsPadding(TopInsets)
            .padding(horizontal = if (layout.useRail) 28.dp else if (layout.small) 14.dp else 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(Modifier.widthIn(max = 640.dp).fillMaxWidth()) {
            BzTopBar(
                height = if (layout.shortHeight) 44.dp else 52.dp,
                start = { BzTitle(stringResource(R.string.tab_more)) },
            )
        }
        Column(
            Modifier
                .weight(1f)
                .widthIn(max = 640.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 16.dp),
        ) {
            BzSectionHeader(stringResource(R.string.bz_section_connection))
            BzListGroup {
                ModeRow(settings.vpnMode) { vpn -> change(QuickSetting.VpnMode, vpn) }
                BzDivider()
                SwitchRow(
                    R.drawable.ic_ms_alt_route,
                    stringResource(R.string.bz_iran_direct),
                    stringResource(R.string.bz_iran_direct_sub),
                    settings.iranDirect,
                ) { change(QuickSetting.IranDirect, it) }
                BzDivider()
                NavRow(
                    R.drawable.ic_ms_apps,
                    stringResource(R.string.bz_apps),
                    when {
                        !settings.perAppEnabled -> stringResource(R.string.bz_apps_all)
                        settings.perAppBypass -> pluralStringResource(R.plurals.bz_apps_bypass, settings.perAppCount, numbers.int(settings.perAppCount.toLong()))
                        else -> pluralStringResource(R.plurals.bz_apps_only, settings.perAppCount, numbers.int(settings.perAppCount.toLong()))
                    },
                ) { onNavigate(MainDestination.PerAppProxy) }
                BzDivider()
                SwitchRow(
                    R.drawable.ic_ms_shield_with_heart,
                    stringResource(R.string.bz_anti_filter),
                    stringResource(R.string.bz_anti_filter_sub),
                    settings.antiFilter,
                ) { change(QuickSetting.AntiFilter, it) }
                BzDivider()
                SwitchRow(
                    R.drawable.ic_ms_restart_alt,
                    stringResource(R.string.bz_auto_connect),
                    stringResource(R.string.bz_auto_connect_sub),
                    settings.autoConnect,
                ) { change(QuickSetting.AutoConnect, it) }
            }

            BzSectionHeader(stringResource(R.string.bz_section_pro))
            BzListGroup {
                SwitchRow(
                    R.drawable.ic_ms_tune,
                    stringResource(R.string.bz_pro_mode),
                    stringResource(R.string.bz_pro_mode_sub),
                    settings.proMode,
                ) { change(QuickSetting.ProMode, it) }
                if (settings.proMode) {
                    BzDivider()
                    NavRow(R.drawable.ic_ms_dns, stringResource(R.string.bz_routing), stringResource(R.string.bz_routing_sub)) {
                        onNavigate(MainDestination.Routing)
                    }
                    BzDivider()
                    NavRow(R.drawable.ic_ms_settings, stringResource(R.string.bz_core_settings), stringResource(R.string.bz_core_settings_sub)) {
                        onNavigate(MainDestination.Settings)
                    }
                    BzDivider()
                    NavRow(R.drawable.ic_ms_terminal, stringResource(R.string.bz_logs), stringResource(R.string.bz_logs_sub)) {
                        onNavigate(MainDestination.Logcat)
                    }
                    BzDivider()
                    NavRow(R.drawable.ic_ms_card_membership, stringResource(R.string.title_sub_setting), stringResource(R.string.bz_subscriptions_sub)) {
                        onNavigate(MainDestination.Subscriptions)
                    }
                    BzDivider()
                    NavRow(R.drawable.ic_ms_folder_open, stringResource(R.string.title_user_asset_setting), stringResource(R.string.bz_assets_sub)) {
                        onNavigate(MainDestination.UserAssets)
                    }
                    BzDivider()
                    NavRow(R.drawable.ic_ms_download, stringResource(R.string.update_check_for_update), null) {
                        onNavigate(MainDestination.CheckUpdate)
                    }
                }
                BzDivider()
                NavRow(R.drawable.ic_ms_bug_report, stringResource(R.string.bz_send_report), stringResource(R.string.bz_send_report_sub)) {
                    onNavigate(MainDestination.Logcat)
                }
                BzDivider()
                NavRow(R.drawable.ic_ms_backup, stringResource(R.string.bz_backup), stringResource(R.string.bz_backup_sub)) {
                    onNavigate(MainDestination.BackupRestore)
                }
            }

            BzSectionHeader(stringResource(R.string.bz_section_telegram))
            BzListGroup { TelegramRows(onAction) }

            BzSectionHeader(stringResource(R.string.bz_section_about))
            BzListGroup {
                NavRow(
                    R.drawable.ic_ms_info,
                    stringResource(R.string.bz_version, BuildConfig.VERSION_NAME),
                    stringResource(R.string.bz_based_on),
                    showChevron = false,
                ) { onNavigate(MainDestination.About) }
            }
        }
    }
}

@Composable
private fun RowTexts(title: String, subtitle: String?, modifier: Modifier) {
    Column(modifier) {
        Text(title, style = bzText(14.sp, FontWeight.Bold), textAlign = TextAlign.Start, modifier = Modifier.fillMaxWidth())
        if (subtitle != null) {
            Text(subtitle, style = bzText(12.sp, color = Bz.Muted), textAlign = TextAlign.Start, modifier = Modifier.fillMaxWidth())
        }
    }
}

/** List row with a switch; the whole row toggles. */
@Composable
private fun SwitchRow(
    @DrawableRes iconRes: Int,
    title: String,
    subtitle: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BzRowIcon(iconRes)
        RowTexts(title, subtitle, Modifier.weight(1f))
        BzSwitch(checked)
    }
}

/** List row that opens another screen. */
@Composable
private fun NavRow(
    @DrawableRes iconRes: Int,
    title: String,
    subtitle: String?,
    showChevron: Boolean = true,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) {}
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BzRowIcon(iconRes)
        RowTexts(title, subtitle, Modifier.weight(1f))
        if (showChevron) {
            // chevron_right is auto-mirrored, so it points left in Persian like the design.
            Icon(painterResource(R.drawable.ic_ms_chevron_right), contentDescription = null, tint = Bz.Muted, modifier = Modifier.size(22.dp))
        }
    }
}

/** "Connection mode" row with the full-VPN / proxy-only segmented control under it. */
@Composable
private fun ModeRow(vpnMode: Boolean, onSelect: (Boolean) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 12.dp)) {
        Row(
            Modifier.semantics(mergeDescendants = true) {},
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BzRowIcon(R.drawable.ic_ms_vpn_lock)
            RowTexts(
                stringResource(R.string.bz_mode_title),
                stringResource(if (vpnMode) R.string.bz_mode_vpn_sub else R.string.bz_mode_proxy_sub),
                Modifier.weight(1f),
            )
        }
        Row(
            Modifier
                .padding(top = 10.dp)
                .fillMaxWidth()
                .height(40.dp)
                .clip(CircleShape)
                .border(1.dp, Bz.Line2, CircleShape)
                .selectableGroup(),
        ) {
            ModeOption(R.drawable.ic_ms_shield, stringResource(R.string.bz_mode_vpn), vpnMode, Modifier.weight(1f)) { onSelect(true) }
            ModeOption(R.drawable.ic_ms_lan, stringResource(R.string.bz_mode_proxy), !vpnMode, Modifier.weight(1f)) { onSelect(false) }
        }
    }
}

@Composable
private fun ModeOption(@DrawableRes iconRes: Int, label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val color = if (selected) Bz.Accent else Bz.Muted
    Row(
        modifier
            .fillMaxHeight()
            .background(if (selected) Bz.AccentDim else Color.Transparent)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(iconRes), contentDescription = null, tint = color, modifier = Modifier.size(17.dp))
        Text(label, style = bzText(13.sp, FontWeight.Bold, color), maxLines = 1)
    }
}
