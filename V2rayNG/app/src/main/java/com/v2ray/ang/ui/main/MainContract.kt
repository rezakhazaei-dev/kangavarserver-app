package com.v2ray.ang.ui.main

import com.v2ray.ang.dto.ConnectionTestResult
import com.v2ray.ang.dto.GroupMapItem
import com.v2ray.ang.dto.LocateTarget

/** Locale-neutral state formatted only when it reaches the main UI. */
sealed interface MainStatus {
    data object Disconnected : MainStatus
    data object Connected : MainStatus
    data object Testing : MainStatus
    data class TestProgress(val progress: String) : MainStatus
    data class ConnectionTest(val result: ConnectionTestResult) : MainStatus
}

/**
 * Main UI state
 */
data class MainUiState(
    val groups: List<GroupMapItem> = emptyList(),
    val selectedGroupId: String = "",
    val selectedGuid: String? = null,
    val isRunning: Boolean = false,
    /** A start was requested and the service has not reported success or failure yet. */
    val isConnecting: Boolean = false,
    /** When this session's connection started; null when unknown or not connected. */
    val connectedSinceMillis: Long? = null,
    val selectedServer: SelectedServerUiModel? = null,
    val isTesting: Boolean = false,
    val status: MainStatus = MainStatus.Disconnected,
    val locateTarget: LocateTarget? = null,
    val confirmRemove: Boolean = false,
    val doubleColumnDisplay: Boolean = false,
    val shareQRCodeBitmap: android.graphics.Bitmap? = null,
    /** Connect to the fastest measured server instead of [selectedGuid]. */
    val smartSelect: Boolean = true,
    /** The last start attempt failed; null once dismissed or connected. */
    val connectionError: ConnectionErrorUiModel? = null,
    /** Delay and exit IP of the running connection, measured right after it started. */
    val currentServerTest: ConnectionTestResult? = null,
    val quickSettings: QuickSettingsUiModel = QuickSettingsUiModel(),
)

/** Why the home tab shows its error screen. */
data class ConnectionErrorUiModel(
    val serverGuid: String?,
    val serverName: String?,
)

/**
 * All possible user interaction intents
 */
sealed interface MainAction {
    data object Initialize : MainAction
    data object RefreshGroups : MainAction
    data object ToggleService : MainAction
    data object TestCurrentServer : MainAction
    data object TestAllServers : MainAction
    data object TestRealAllServers : MainAction
    data object CancelTesting : MainAction
    data object RemoveAllServers : MainAction
    data object RemoveDuplicateServers : MainAction
    data object RemoveInvalidServers : MainAction
    data object SortByTestResults : MainAction
    data object UpdateSubscriptions : MainAction
    data class UpdateGroupSubscription(val groupId: String) : MainAction
    data object ExportAll : MainAction

    data object ImportQRcode : MainAction
    data object ImportClipboard : MainAction
    data object ImportConfigLocal : MainAction
    data class ImportManually(val type: Int) : MainAction
    data object RestartService : MainAction
    data object LocateSelectedServer : MainAction

    data class SelectGroup(val groupId: String) : MainAction
    data class SelectServer(val guid: String) : MainAction
    data class RemoveServer(val guid: String) : MainAction
    data class EditServer(val guid: String, val profile: com.v2ray.ang.dto.entities.ProfileItem) : MainAction
    data class Search(val query: String) : MainAction
    data class ShareQRCode(val guid: String) : MainAction
    data class ShareClipboard(val guid: String) : MainAction
    data class ShareFullContent(val guid: String) : MainAction
    data object DismissQRCodeDialog : MainAction
    data class OpenSubscriptionLink(val url: String) : MainAction

    data class ImportBatchConfig(val configText: String) : MainAction

    data object LocateHandled : MainAction

    /** Connect through the fastest server of one country (home quick-connect chips). */
    data class QuickConnect(val country: ServerCountry) : MainAction
    /** Use smart selection again (servers tab "smart" row). */
    data object SelectSmart : MainAction
    /** After a failed start: connect through the fastest server other than the failed one. */
    data object RetryOtherServer : MainAction
    data object DismissConnectionError : MainAction
    data class ChangeQuickSetting(val setting: QuickSetting, val enabled: Boolean) : MainAction
}
