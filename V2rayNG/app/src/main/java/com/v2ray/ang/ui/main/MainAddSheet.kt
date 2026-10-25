package com.v2ray.ang.ui.main

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.v2ray.ang.AppConfig
import com.v2ray.ang.R
import com.v2ray.ang.enums.EConfigType
import com.v2ray.ang.util.LogUtil
import java.util.Locale

/** Widest the sheet gets on tablets and landscape phones. */
private val SheetMaxWidth = 560.dp

private val SheetShape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)

/** Link schemes the add sheet offers to take from the clipboard: subscription URLs and share links. */
private val importSchemes: List<String> =
    (listOf("https://", "http://") + EConfigType.entries.map { it.protocolScheme })
        .filter { it.isNotBlank() }
        .distinct()

/** True when [text] starts with a subscription URL or a server share link. */
internal fun isImportableText(text: String?): Boolean {
    val trimmed = text?.trim().orEmpty()
    if (trimmed.isEmpty()) return false
    val lower = trimmed.lowercase(Locale.ROOT)
    return importSchemes.any { scheme -> lower.startsWith(scheme) && lower.length > scheme.length }
}

/**
 * "Add subscription" bottom sheet of the design: clipboard suggestion, link field, QR / file /
 * manual options and the add button. Drawn as an overlay over the tabs so it needs no
 * experimental sheet API.
 */
@Composable
internal fun MainAddSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    onAction: (MainAction) -> Unit,
) {
    BackHandler(enabled = visible, onBack = onDismiss)
    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) {
            val dismissLabel = stringResource(R.string.bz_close)
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color(0xFF030807).copy(alpha = 0.62f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClickLabel = dismissLabel,
                        onClick = onDismiss,
                    )
            )
        }
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically { it },
            exit = slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                SheetContent(
                    maxHeight = maxHeight * 0.92f,
                    onDismiss = onDismiss,
                    onAction = onAction,
                )
            }
        }
    }
}

@Composable
private fun SheetContent(
    maxHeight: Dp,
    onDismiss: () -> Unit,
    onAction: (MainAction) -> Unit,
) {
    var link by rememberSaveable { mutableStateOf("") }
    var clipText by remember { mutableStateOf<String?>(null) }
    var showManualMenu by remember { mutableStateOf(false) }
    val clipboard = LocalClipboard.current

    // The sheet opened from a tap, so reading the clipboard once here is expected by the user.
    LaunchedEffect(Unit) {
        clipText = try {
            clipboard.getClipEntry()?.clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.text?.toString()
                ?.takeIf(::isImportableText)?.trim()
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Add sheet: reading the clipboard failed", e)
            null
        }
    }

    val submit = {
        val text = link.trim()
        if (text.isNotEmpty()) {
            onAction(MainAction.ImportBatchConfig(text))
            link = ""
            onDismiss()
        }
    }

    Column(
        Modifier
            .widthIn(max = SheetMaxWidth)
            .fillMaxWidth()
            .heightIn(max = maxHeight)
            .clip(SheetShape)
            .background(Bz.Sheet)
            .border(1.dp, Bz.Line2, SheetShape)
            // Swallow taps so they never reach the scrim behind the sheet.
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {})
            .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 22.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 4.dp)
                .size(width = 36.dp, height = 4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Bz.Line2)
        )
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.bz_add_subscription), style = bzText(19.sp, FontWeight.ExtraBold), textAlign = TextAlign.Center)
            Text(
                stringResource(R.string.bz_sheet_subtitle),
                style = bzText(13.sp, color = Bz.Muted),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 2.dp),
            )
        }

        clipText?.let { found ->
            val useLabel = stringResource(R.string.bz_sheet_use)
            Row(
                Modifier
                    .fillMaxWidth()
                    .glass(Bz.BoxShape)
                    .clickable(role = Role.Button, onClickLabel = useLabel) { link = found }
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .semantics(mergeDescendants = true) {},
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(painterResource(R.drawable.ic_ms_content_paste_go), contentDescription = null, tint = Bz.Accent, modifier = Modifier.size(22.dp))
                Text(
                    stringResource(R.string.bz_sheet_clip_found),
                    style = bzText(13.sp),
                    textAlign = TextAlign.Start,
                    maxLines = 2,
                    modifier = Modifier.weight(1f),
                )
                Text(useLabel, style = bzText(13.sp, FontWeight.Bold, Bz.Accent))
            }
        }

        OutlinedTextField(
            value = link,
            onValueChange = { link = it },
            label = { Text(stringResource(R.string.bz_sheet_link_label), style = bzText(12.sp, FontWeight.Bold, Color.Unspecified)) },
            singleLine = true,
            textStyle = bzText(13.5.sp).copy(textDirection = TextDirection.Ltr, textAlign = TextAlign.Left),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { submit() }),
            shape = Bz.BoxShape,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Bz.Accent,
                unfocusedBorderColor = Bz.Line2,
                focusedLabelColor = Bz.Accent,
                unfocusedLabelColor = Bz.Muted,
                cursorColor = Bz.Accent,
                focusedTextColor = Bz.Text,
                unfocusedTextColor = Bz.Text,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SheetOption(R.drawable.ic_ms_qr_code_scanner, stringResource(R.string.bz_sheet_qr)) {
                onDismiss()
                onAction(MainAction.ImportQRcode)
            }
            SheetOption(R.drawable.ic_ms_folder_open, stringResource(R.string.bz_sheet_file)) {
                onDismiss()
                onAction(MainAction.ImportConfigLocal)
            }
            Box(Modifier.weight(1f)) {
                SheetOptionBox(R.drawable.ic_ms_edit_note, stringResource(R.string.bz_sheet_manual)) { showManualMenu = true }
                DropdownMenu(expanded = showManualMenu, onDismissRequest = { showManualMenu = false }) {
                    ImportMenuContent(
                        onAction = { action ->
                            showManualMenu = false
                            onDismiss()
                            onAction(action)
                        }
                    )
                }
            }
        }

        BzButton(
            stringResource(R.string.bz_sheet_add),
            submit,
            Modifier.fillMaxWidth(),
            iconRes = R.drawable.ic_ms_download,
            enabled = link.isNotBlank(),
        )
    }
}

@Composable
private fun RowScope.SheetOption(@DrawableRes iconRes: Int, label: String, onClick: () -> Unit) {
    Box(Modifier.weight(1f)) { SheetOptionBox(iconRes, label, onClick) }
}

@Composable
private fun SheetOptionBox(@DrawableRes iconRes: Int, label: String, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .glass(Bz.BoxShape)
            .clickable(role = Role.Button, onClick = onClick)
            .clearAndSetSemantics { contentDescription = label }
            .padding(horizontal = 4.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(painterResource(iconRes), contentDescription = null, tint = Bz.Accent2, modifier = Modifier.size(24.dp))
        Text(label, style = bzText(12.sp, FontWeight.SemiBold, Bz.Muted), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
