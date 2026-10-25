package com.v2ray.ang.ui.main

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.v2ray.ang.dto.entities.ProfileItem
import com.v2ray.ang.ui.compose.BrandDarkColorScheme
import com.v2ray.ang.ui.compose.QRCodeDialog

/** Main screen of the design: home, servers, subscription and settings tabs plus the add sheet. */
@Composable
fun MainScreen(
    mainViewModel: MainViewModel,
    onAction: (MainAction) -> Unit,
    onNavigate: (MainDestination) -> Unit,
) {
    val uiState by mainViewModel.uiState.collectAsStateWithLifecycle()
    val catalog by mainViewModel.serverCatalog.collectAsStateWithLifecycle()
    val confirmRemove = uiState.confirmRemove
    val shareQRCodeBitmap = uiState.shareQRCodeBitmap

    var selectedTab by rememberSaveable { mutableStateOf(MainTab.Home) }
    var showAddSheet by rememberSaveable { mutableStateOf(false) }
    var showDelAllConfirm by remember { mutableStateOf(false) }
    var showDelDuplicateConfirm by remember { mutableStateOf(false) }
    var showDelInvalidConfirm by remember { mutableStateOf(false) }
    var showRemoveConfirm by rememberSaveable(stateSaver = ServerDeleteTarget.Saver) {
        mutableStateOf<ServerDeleteTarget?>(null)
    }
    var shareTarget by remember { mutableStateOf<Pair<String, ProfileItem>?>(null) }

    val removeServer: (String, String) -> Unit = { guid, profileName ->
        if (confirmRemove) {
            showRemoveConfirm = ServerDeleteTarget(guid, profileName)
        } else {
            onAction(MainAction.RemoveServer(guid))
        }
    }
    val onMoreMenuAction: (MainMoreMenuAction) -> Unit = { action ->
        when (action) {
            MainMoreMenuAction.RestartService -> onAction(MainAction.RestartService)
            MainMoreMenuAction.DeleteAll -> showDelAllConfirm = true
            MainMoreMenuAction.DeleteDuplicate -> showDelDuplicateConfirm = true
            MainMoreMenuAction.DeleteInvalid -> showDelInvalidConfirm = true
            MainMoreMenuAction.ExportAll -> onAction(MainAction.ExportAll)
            MainMoreMenuAction.LocateSelected -> onAction(MainAction.LocateSelectedServer)
            MainMoreMenuAction.SortByTestResults -> onAction(MainAction.SortByTestResults)
            MainMoreMenuAction.TestAll -> onAction(MainAction.TestAllServers)
            MainMoreMenuAction.TestAllRealPing -> onAction(MainAction.TestRealAllServers)
            MainMoreMenuAction.UpdateSubscriptions -> onAction(MainAction.UpdateSubscriptions)
        }
    }
    val openAddSheet = { showAddSheet = true }

    // Back from another tab returns home first; the activity's handler then backgrounds the app.
    BackHandler(enabled = selectedTab != MainTab.Home && !showAddSheet) { selectedTab = MainTab.Home }

    // The main screen always uses the brand's dark palette, whatever the app theme or wallpaper colours.
    MaterialTheme(colorScheme = BrandDarkColorScheme, typography = MaterialTheme.typography, shapes = MaterialTheme.shapes) {
        MainDialogs(
            showDelAllConfirm = showDelAllConfirm,
            onDismissDelAll = { showDelAllConfirm = false },
            onConfirmDelAll = { showDelAllConfirm = false; onAction(MainAction.RemoveAllServers) },
            showDelDuplicateConfirm = showDelDuplicateConfirm,
            onDismissDelDuplicate = { showDelDuplicateConfirm = false },
            onConfirmDelDuplicate = { showDelDuplicateConfirm = false; onAction(MainAction.RemoveDuplicateServers) },
            showDelInvalidConfirm = showDelInvalidConfirm,
            onDismissDelInvalid = { showDelInvalidConfirm = false },
            onConfirmDelInvalid = { showDelInvalidConfirm = false; onAction(MainAction.RemoveInvalidServers) },
            showRemoveConfirm = showRemoveConfirm,
            onDismissRemove = { showRemoveConfirm = null },
            onConfirmRemove = { guid -> showRemoveConfirm = null; onAction(MainAction.RemoveServer(guid)) }
        )
        shareTarget?.let { (guid, profile) ->
            ShareMethodDialog(
                guid = guid,
                profile = profile,
                more = true,
                onDismiss = { shareTarget = null },
                onAction = onAction,
                onRemove = removeServer,
            )
        }
        if (shareQRCodeBitmap != null) {
            QRCodeDialog(bitmap = shareQRCodeBitmap, onDismiss = { onAction(MainAction.DismissQRCodeDialog) })
        }

        // The welcome screen is a full-screen first run, so it hides the tabs like the design.
        val welcome = selectedTab == MainTab.Home && catalog.loaded && catalog.rows.isEmpty() && !uiState.isRunning
        Box(
            Modifier
                .fillMaxSize()
                .background(Bz.Bg)
        ) {
            MainAdaptiveScaffold(
                selectedTab = selectedTab,
                onSelectTab = { selectedTab = it },
                showNavigation = !welcome,
            ) { layout ->
                when (selectedTab) {
                    MainTab.Home -> {
                        // Collected here so the once-per-second samples recompose only the home tab.
                        val liveTraffic by mainViewModel.liveTraffic.collectAsStateWithLifecycle()
                        MainHomeTab(
                            uiState = uiState,
                            catalog = catalog,
                            liveTraffic = liveTraffic,
                            layout = layout,
                            onAction = onAction,
                            onSelectTab = { selectedTab = it },
                            onOpenAddSheet = openAddSheet,
                        )
                    }

                    MainTab.Servers -> MainServersTab(
                        uiState = uiState,
                        catalog = catalog,
                        layout = layout,
                        onAction = onAction,
                        onOpenAddSheet = openAddSheet,
                        onMoreMenuAction = onMoreMenuAction,
                        onServerOptions = { guid, profile -> shareTarget = guid to profile },
                    )

                    MainTab.Subscription -> MainSubscriptionTab(
                        uiState = uiState,
                        catalog = catalog,
                        layout = layout,
                        onAction = onAction,
                        onNavigate = onNavigate,
                        onOpenAddSheet = openAddSheet,
                    )

                    MainTab.More -> MainSettingsTab(
                        settings = uiState.quickSettings,
                        layout = layout,
                        onAction = onAction,
                        onNavigate = onNavigate,
                    )
                }
            }
            MainAddSheet(
                visible = showAddSheet,
                onDismiss = { showAddSheet = false },
                onAction = onAction,
            )
        }
    }
}
