package com.v2ray.ang.ui.perappproxy

import android.os.Bundle
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.v2ray.ang.R
import com.v2ray.ang.dto.AppInfo
import com.v2ray.ang.extension.toastSuccess
import com.v2ray.ang.ui.base.BaseComponentActivity
import com.v2ray.ang.ui.compose.AppDropdownMenuItems
import com.v2ray.ang.ui.compose.BrandDarkColorScheme
import com.v2ray.ang.ui.main.Bz
import com.v2ray.ang.ui.main.BzBackdrop
import com.v2ray.ang.ui.main.BzDivider
import com.v2ray.ang.ui.main.BzIconButton
import com.v2ray.ang.ui.main.BzTitle
import com.v2ray.ang.ui.main.BzTopBar
import com.v2ray.ang.ui.main.LtrStartText
import com.v2ray.ang.ui.main.bzBackdrop
import com.v2ray.ang.ui.main.bzText
import com.v2ray.ang.ui.main.glass
import com.v2ray.ang.util.AppIconFetcher
import com.v2ray.ang.util.Utils

private enum class PerAppMenuAction(@StringRes val labelRes: Int) {
    SelectAll(R.string.menu_item_select_all),
    InvertSelection(R.string.menu_item_invert_selection),
    SelectProxyApps(R.string.menu_item_select_proxy_app),
    ImportSelection(R.string.menu_item_import_proxy_app),
    ExportSelection(R.string.menu_item_export_proxy_app)
}

class PerAppProxyActivity : BaseComponentActivity() {

    private val viewModel: PerAppProxyViewModel by viewModels()

    override val alwaysDarkTheme: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.loadApps(this)
    }

    @Composable
    override fun ScreenContent() {
        val apps by viewModel.displayedApps.collectAsStateWithLifecycle()
        val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
        val blacklist by viewModel.blacklist.collectAsStateWithLifecycle()
        val bypassApps by viewModel.bypassApps.collectAsStateWithLifecycle()

        PerAppProxyScreen(
            apps = apps,
            isLoading = isLoading,
            blacklist = blacklist,
            bypassApps = bypassApps,
            onBackClick = { finish() },
            onBypassAppsChanged = { viewModel.setBypassAppsEnabled(it) },
            onToggleApp = { viewModel.toggle(it) },
            onSearch = { viewModel.filterApps(it) },
            onSelectAll = { viewModel.selectAll() },
            onInvertSelection = { viewModel.invertSelection() },
            onSelectProxyAuto = { viewModel.selectProxyAppAuto(this) },
            onImportProxyApp = {
                val content = Utils.getClipboard(applicationContext)
                viewModel.importProxyApp(content, this)
            },
            onExportProxyApp = {
                val export = viewModel.exportProxyApp()
                Utils.setClipboard(applicationContext, export)
                toastSuccess(R.string.toast_success)
            }
        )
    }
}

/**
 * Per-app screen of the Bazhiko design: mode switch, tip, search and the app list. Selecting at
 * least one app turns per-app routing on; clearing the selection sends every app through the VPN.
 */
@Composable
fun PerAppProxyScreen(
    apps: List<AppInfo>,
    isLoading: Boolean,
    blacklist: Set<String>,
    bypassApps: Boolean,
    onBackClick: () -> Unit,
    onBypassAppsChanged: (Boolean) -> Unit,
    onToggleApp: (String) -> Unit,
    onSearch: (String) -> Unit,
    onSelectAll: () -> Unit,
    onInvertSelection: () -> Unit,
    onSelectProxyAuto: () -> Unit,
    onImportProxyApp: () -> Unit,
    onExportProxyApp: () -> Unit
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        onSearch(searchQuery)
    }

    MaterialTheme(colorScheme = BrandDarkColorScheme, typography = MaterialTheme.typography, shapes = MaterialTheme.shapes) {
        Column(
            Modifier
                .fillMaxSize()
                .bzBackdrop(BzBackdrop.Default)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
                .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime).only(WindowInsetsSides.Bottom))
                .padding(horizontal = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(Modifier.widthIn(max = 640.dp).fillMaxWidth()) {
                BzTopBar(
                    start = {
                        BzIconButton(R.drawable.ic_ms_arrow_back, stringResource(R.string.acc_back), onBackClick)
                        Box(Modifier.size(10.dp))
                        BzTitle(stringResource(R.string.bz_apps))
                    },
                    actions = {
                        if (isLoading) {
                            CircularProgressIndicator(Modifier.size(22.dp), color = Bz.Accent, strokeWidth = 2.dp)
                        }
                        Box(Modifier.wrapContentSize(Alignment.TopEnd)) {
                            BzIconButton(R.drawable.ic_ms_more_vert, stringResource(R.string.acc_more), { showMenu = true })
                            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                AppDropdownMenuItems(PerAppMenuAction.entries, { it.labelRes }) { action ->
                                    showMenu = false
                                    when (action) {
                                        PerAppMenuAction.SelectAll -> onSelectAll()
                                        PerAppMenuAction.InvertSelection -> onInvertSelection()
                                        PerAppMenuAction.SelectProxyApps -> onSelectProxyAuto()
                                        PerAppMenuAction.ImportSelection -> onImportProxyApp()
                                        PerAppMenuAction.ExportSelection -> onExportProxyApp()
                                    }
                                }
                            }
                        }
                    },
                )
                ModeSwitch(bypassApps, onBypassAppsChanged)
                Tip(stringResource(if (bypassApps) R.string.bz_apps_hint_bypass else R.string.bz_apps_hint_only))
                SearchField(searchQuery) { query ->
                    searchQuery = query
                    onSearch(query)
                }
            }
            when {
                apps.isEmpty() && isLoading -> Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Bz.Accent)
                }

                apps.isEmpty() -> Text(
                    stringResource(R.string.bz_apps_none),
                    style = bzText(14.sp, color = Bz.Muted),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 32.dp),
                )

                else -> LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .widthIn(max = 640.dp)
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .glass(Bz.CardShape),
                ) {
                    itemsIndexed(items = apps, key = { _, app -> app.packageName }) { index, app ->
                        if (index > 0) BzDivider()
                        AppRow(app, checked = app.packageName in blacklist) { onToggleApp(app.packageName) }
                    }
                }
            }
        }
    }
}

/** "Only these use VPN" / "All except these", the two per-app routing modes. */
@Composable
private fun ModeSwitch(bypassApps: Boolean, onBypassAppsChanged: (Boolean) -> Unit) {
    Row(
        Modifier
            .padding(top = 8.dp, bottom = 10.dp)
            .fillMaxWidth()
            .height(44.dp)
            .clip(CircleShape)
            .border(1.dp, Bz.Line2, CircleShape)
            .selectableGroup(),
    ) {
        ModeOption(stringResource(R.string.bz_apps_only_vpn), selected = !bypassApps) { onBypassAppsChanged(false) }
        Box(Modifier.width(1.dp).fillMaxHeight().background(Bz.Line2))
        ModeOption(stringResource(R.string.bz_apps_all_except), selected = bypassApps) { onBypassAppsChanged(true) }
    }
}

@Composable
private fun RowScope.ModeOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .weight(1f)
            .fillMaxHeight()
            .background(if (selected) Bz.AccentDim else Color.Transparent)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = bzText(14.sp, FontWeight.Bold, if (selected) Bz.Accent else Bz.Muted),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
    }
}

@Composable
private fun Tip(text: String) {
    Row(
        Modifier
            .padding(bottom = 10.dp)
            .fillMaxWidth()
            .glass(RoundedCornerShape(6.dp), fill = Bz.AccentDim, border = Bz.Accent.copy(alpha = 0.25f))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(painterResource(R.drawable.ic_ms_lightbulb_fill), contentDescription = null, tint = Bz.Accent, modifier = Modifier.size(20.dp))
        Text(text, style = bzText(12.5.sp, color = Bz.Accent2), modifier = Modifier.weight(1f))
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    val focusManager = LocalFocusManager.current
    val label = stringResource(R.string.bz_apps_search)
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        textStyle = bzText(14.sp),
        cursorBrush = SolidColor(Bz.Accent),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
        modifier = Modifier
            .padding(bottom = 10.dp)
            .fillMaxWidth()
            .semantics { contentDescription = label },
        decorationBox = { inner ->
            Row(
                Modifier
                    .heightIn(min = 46.dp)
                    .glass(CircleShape)
                    .padding(start = 16.dp, end = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(painterResource(R.drawable.ic_ms_search), contentDescription = null, tint = Bz.Muted, modifier = Modifier.size(20.dp))
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) Text(label, style = bzText(14.sp, color = Bz.Muted), maxLines = 1)
                    inner()
                }
                if (query.isNotEmpty()) {
                    BzIconButton(R.drawable.ic_ms_close, stringResource(R.string.bz_apps_clear_search), { onQueryChange("") }, small = true, tint = Bz.Muted)
                } else {
                    Box(Modifier.size(36.dp))
                }
            }
        },
    )
}

/** One app: icon, name and package; the whole row toggles the checkbox. */
@Composable
private fun AppRow(app: AppInfo, checked: Boolean, onToggle: () -> Unit) {
    val context = LocalContext.current
    val iconRequest = remember(app.packageName) {
        ImageRequest.Builder(context)
            .data("appicon:${app.packageName}")
            .fetcherFactory(AppIconFetcher.Factory(context))
            .build()
    }
    Row(
        Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = { onToggle() })
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AsyncImage(
            model = iconRequest,
            contentDescription = null,
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Fit,
            error = painterResource(R.drawable.ic_ms_apps),
            fallback = painterResource(R.drawable.ic_ms_apps),
        )
        Column(Modifier.weight(1f)) {
            Text(app.appName, style = bzText(14.sp, FontWeight.Bold), maxLines = 1, overflow = TextOverflow.Ellipsis)
            // The package ID is shown but not read out; the app name names the row.
            LtrStartText(app.packageName, bzText(11.sp, color = Bz.Muted), Modifier.clearAndSetSemantics {})
        }
        CheckMark(checked)
    }
}

@Composable
private fun CheckMark(checked: Boolean) {
    val shape = RoundedCornerShape(4.dp)
    Box(
        Modifier
            .size(22.dp)
            .clip(shape)
            .then(if (checked) Modifier.background(Bz.Accent) else Modifier.border(2.dp, Bz.Line2, shape)),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            Icon(painterResource(R.drawable.ic_ms_check), contentDescription = null, tint = Bz.On, modifier = Modifier.size(17.dp))
        }
    }
}
