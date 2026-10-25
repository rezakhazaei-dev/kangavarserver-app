package com.v2ray.ang.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.v2ray.ang.R
import com.v2ray.ang.dto.entities.ProfileItem

/** Saved value of the country filter that shows every server. */
private const val FILTER_ALL = "all"

/** Servers tab of the design: country filter, smart row, and servers grouped by country. */
@Composable
internal fun MainServersTab(
    uiState: MainUiState,
    catalog: ServerCatalogUiModel,
    layout: MainLayoutInfo,
    onAction: (MainAction) -> Unit,
    onOpenAddSheet: () -> Unit,
    onMoreMenuAction: (MainMoreMenuAction) -> Unit,
    onServerOptions: (String, ProfileItem) -> Unit,
) {
    var filter by rememberSaveable { mutableStateOf(FILTER_ALL) }
    val presentCountries = remember(catalog) { catalog.countries.map { it.country }.sortedBy { it.ordinal } }
    val activeFilter = filter.takeIf { name -> name == FILTER_ALL || presentCountries.any { it.name == name } } ?: FILTER_ALL
    val groups = remember(catalog, activeFilter) {
        catalog.countries.filter { activeFilter == FILTER_ALL || it.country.name == activeFilter }
    }
    val numbers = rememberBzNumbers()
    val columns = when {
        layout.wideScreen -> 3
        layout.twoColumns -> 2
        else -> 1
    }

    Column(
        Modifier
            .fillMaxSize()
            .bzBackdrop(BzBackdrop.Default)
            .windowInsetsPadding(TopInsets)
            .padding(horizontal = if (layout.useRail) 28.dp else if (layout.small) 14.dp else 18.dp),
    ) {
        ServersTopBar(uiState.isTesting, layout, onAction, onOpenAddSheet, onMoreMenuAction)
        if (catalog.loaded && catalog.rows.isEmpty()) {
            EmptyServers(onOpenAddSheet)
            return@Column
        }
        if (presentCountries.size > 1) {
            CountryFilter(
                countries = presentCountries,
                counts = remember(catalog) { catalog.countries.associate { it.country to it.rows.size } },
                total = catalog.rows.size,
                active = activeFilter,
                numbers = numbers,
                onSelect = { filter = it },
            )
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            state = rememberLazyGridState(),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (catalog.rows.size > 1) {
                item(key = "smart", span = { GridItemSpan(maxLineSpan) }) {
                    SmartRow(uiState.smartSelect, catalog, numbers) { onAction(MainAction.SelectSmart) }
                }
            }
            groups.forEach { group ->
                item(key = "header-${group.country.name}", span = { GridItemSpan(maxLineSpan) }) {
                    CountryHeader(group.country)
                }
                group.rows.forEach { row ->
                    item(key = row.guid) {
                        ServerRow(
                            row = row,
                            selected = !uiState.smartSelect && row.guid == uiState.selectedGuid,
                            numbers = numbers,
                            onClick = { onAction(MainAction.SelectServer(row.guid)) },
                            onLongClick = { onServerOptions(row.guid, row.profile) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ServersTopBar(
    isTesting: Boolean,
    layout: MainLayoutInfo,
    onAction: (MainAction) -> Unit,
    onOpenAddSheet: () -> Unit,
    onMoreMenuAction: (MainMoreMenuAction) -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }
    BzTopBar(
        height = if (layout.shortHeight) 44.dp else 52.dp,
        start = { BzTitle(stringResource(R.string.tab_servers)) },
        actions = {
            if (isTesting) {
                Box(Modifier.size(if (layout.small) 36.dp else 40.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(36.dp), color = Bz.Accent, strokeWidth = 2.dp)
                    BzIconButton(
                        R.drawable.ic_ms_close,
                        stringResource(R.string.bz_cancel_test),
                        { onAction(MainAction.CancelTesting) },
                        small = true,
                    )
                }
            } else {
                BzIconButton(R.drawable.ic_ms_speed, stringResource(R.string.bz_test_all), { onAction(MainAction.TestRealAllServers) }, small = layout.small)
            }
            BzIconButton(R.drawable.ic_ms_add, stringResource(R.string.bz_add_subscription), onOpenAddSheet, small = layout.small)
            Box(Modifier.wrapContentSize(Alignment.TopEnd)) {
                BzIconButton(R.drawable.ic_ms_more_vert, stringResource(R.string.acc_more), { showMenu = true }, small = layout.small)
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    MoreMenuContent { action ->
                        showMenu = false
                        onMoreMenuAction(action)
                    }
                }
            }
        },
    )
}

@Composable
private fun CountryFilter(
    countries: List<ServerCountry>,
    counts: Map<ServerCountry, Int>,
    total: Int,
    active: String,
    numbers: BzNumbers,
    onSelect: (String) -> Unit,
) {
    val chips = listOf(Triple(FILTER_ALL, stringResource(R.string.bz_filter_all), total)) +
        countries.map { Triple(it.name, stringResource(it.labelRes), counts[it] ?: 0) }
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(top = 4.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        chips.forEach { (key, label, count) ->
            val selected = key == active
            Row(
                Modifier
                    .height(34.dp)
                    .widthIn(min = 72.dp)
                    .then(
                        if (selected) Modifier
                            .clip(CircleShape)
                            .background(Bz.Text)
                        else Modifier.glass(CircleShape)
                    )
                    .clickable(role = Role.Tab) { onSelect(key) }
                    .semantics { this.selected = selected }
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(label, style = bzText(13.sp, FontWeight.Bold, if (selected) Bz.On else Bz.Muted), maxLines = 1)
                Box(
                    Modifier
                        .heightIn(min = 18.dp)
                        .widthIn(min = 18.dp)
                        .clip(CircleShape)
                        .background(if (selected) Bz.On else Color.White.copy(alpha = 0.08f))
                        .padding(horizontal = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(numbers.int(count.toLong()), style = bzText(11.sp, FontWeight.Bold, if (selected) Bz.Accent else Bz.Muted))
                }
            }
        }
    }
}

/** Card look shared by the smart row and the server rows; the selected one gets the mint edge. */
private fun Modifier.serverCard(selected: Boolean): Modifier =
    if (selected) {
        this
            .clip(Bz.RowShape)
            .background(Brush.horizontalGradient(listOf(Bz.Accent.copy(alpha = 0.03f), Bz.Accent.copy(alpha = 0.12f))))
            .border(1.dp, Bz.Accent.copy(alpha = 0.55f), Bz.RowShape)
    } else {
        glass(Bz.RowShape)
    }

@Composable
private fun SmartRow(selected: Boolean, catalog: ServerCatalogUiModel, numbers: BzNumbers, onClick: () -> Unit) {
    val best = catalog.row(catalog.bestGuid)
    val selectedDescription = stringResource(R.string.acc_selected_server)
    Row(
        Modifier
            .fillMaxWidth()
            .serverCard(selected)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .semantics(mergeDescendants = true) {
                this.selected = selected
                if (selected) stateDescription = selectedDescription
            }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BzRadio(selected)
        BzSmartIcon(34.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(stringResource(R.string.bz_smart_select), style = bzText(15.sp, FontWeight.Bold))
            Text(
                if (best != null) {
                    stringResource(R.string.bz_smart_now, best.displayName, numbers.int(best.testDelayMillis))
                } else {
                    stringResource(R.string.bz_smart_hint)
                },
                style = bzText(12.sp, color = Bz.Muted),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun CountryHeader(country: ServerCountry) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 4.dp, top = 6.dp)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BzFlag(country, 20.dp, ring = false)
        Text(stringResource(country.labelRes), style = bzText(13.sp, FontWeight.Bold, Bz.Muted))
        if (country == ServerCountry.Usa) {
            Spacer(Modifier.weight(1f))
            BzTag(stringResource(R.string.bz_us_tag), warm = true)
        }
    }
}

@Composable
private fun ServerRow(
    row: ServerRowUiModel,
    selected: Boolean,
    numbers: BzNumbers,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val selectedDescription = stringResource(R.string.acc_selected_server)
    Row(
        Modifier
            .fillMaxWidth()
            .serverCard(selected)
            .combinedClickable(
                role = Role.RadioButton,
                onLongClickLabel = stringResource(R.string.bz_server_options),
                onLongClick = onLongClick,
                onClick = onClick,
            )
            .semantics(mergeDescendants = true) {
                this.selected = selected
                if (selected) stateDescription = selectedDescription
            }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BzRadio(selected)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(row.displayName, style = bzText(15.sp, FontWeight.Bold), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (row.typeDescription.isNotBlank()) BzTag(row.typeDescription)
                if (row.testDelayMillis == 0L) BzTag(stringResource(R.string.bz_untested))
            }
        }
        BzPing(row.testDelayMillis) { numbers.int(it) }
    }
}

@Composable
private fun EmptyServers(onOpenAddSheet: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BzMark(120.dp, animate = false)
        Text(stringResource(R.string.bz_servers_empty), style = bzText(18.sp, FontWeight.ExtraBold))
        Text(stringResource(R.string.home_no_server_hint), style = bzText(13.sp, color = Bz.Muted))
        BzButton(
            stringResource(R.string.bz_add_subscription),
            onOpenAddSheet,
            Modifier.padding(top = 8.dp),
            iconRes = R.drawable.ic_ms_add_link,
            height = 48.dp,
        )
    }
}
