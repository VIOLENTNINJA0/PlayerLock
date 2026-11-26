package com.example.playerlock.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import java.text.SimpleDateFormat
import java.util.Date
import androidx.documentfile.provider.DocumentFile
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.playerlock.data.AppSettings
import com.example.playerlock.ui.theme.*
import kotlinx.coroutines.launch
import java.util.Locale

private val CardShape = RoundedCornerShape(16.dp)

@Composable
fun SettingsDialog(
    settings: AppSettings,
    playerName: String,
    activeTeamName: String,
    viewModel: PlayerLockViewModel,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SETTINGS",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        letterSpacing = 1.sp
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Settings",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                SettingsCard(
                    settings = settings,
                    playerName = playerName,
                    activeTeamName = activeTeamName,
                    viewModel = viewModel,
                    onQuarterLengthChanged = { viewModel.updateQuarterLength(it) },
                    onNormalizeChanged = { viewModel.updateNormalizeQuarterXp(it) },
                    onExportToJson = { viewModel.exportActiveCareerToJson() },
                    onImportJson = { json, onSuccess, onError ->
                        viewModel.importCareerFromJson(json, onSuccess, onError)
                    }
                )
            }
        }
    }
}

@Composable
fun SettingsCard(
    settings: AppSettings,
    playerName: String,
    activeTeamName: String,
    viewModel: PlayerLockViewModel,
    onQuarterLengthChanged: (Int) -> Unit,
    onNormalizeChanged: (Boolean) -> Unit,
    onExportToJson: suspend () -> String?,
    onImportJson: (String, () -> Unit, (String) -> Unit) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingExportJson by remember { mutableStateOf<String?>(null) }
    var showThemeStudio by remember { mutableStateOf(false) }

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null && pendingExportJson != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(pendingExportJson!!.toByteArray(Charsets.UTF_8))
                }
                Toast.makeText(context, "Career save exported successfully!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
        pendingExportJson = null
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                val jsonString = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    inputStream.bufferedReader().use { it.readText() }
                }
                if (!jsonString.isNullOrBlank()) {
                    onImportJson(
                        jsonString,
                        {
                            Toast.makeText(context, "Career imported successfully!", Toast.LENGTH_LONG).show()
                        },
                        { err ->
                            Toast.makeText(context, "Import failed: $err", Toast.LENGTH_LONG).show()
                        }
                    )
                } else {
                    Toast.makeText(context, "Import failed: File is empty", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Import failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            try {
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, flags)
                val uriString = uri.toString()
                viewModel.updateAutoSaveFolderUri(uriString)
                viewModel.updateAutoSaveEnabled(true)
                viewModel.triggerAutoSave()
                Toast.makeText(context, "Auto-save folder set!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Folder error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val folderName = remember(settings.autoSaveFolderUri) {
        if (settings.autoSaveFolderUri.isNullOrBlank()) {
            "No folder selected"
        } else {
            try {
                val treeUri = Uri.parse(settings.autoSaveFolderUri)
                val docFile = androidx.documentfile.provider.DocumentFile.fromTreeUri(context, treeUri)
                docFile?.name ?: Uri.decode(treeUri.lastPathSegment ?: "Selected Folder")
            } catch (_: Exception) {
                "Selected Folder"
            }
        }
    }

    val quarterLength = settings.quarterLengthMinutes
    val multiplier = if (settings.normalizeQuarterXp) (12.0 / quarterLength) * 0.85 else 1.0

    val activePreset = remember(settings.selectedThemeId, activeTeamName, settings.personalizePrimaryColor, settings.personalizeSecondaryColor) {
        AppThemeCatalog.getPresetById(
            id = settings.selectedThemeId,
            activeTeamName = activeTeamName,
            customPrimary = Color(settings.personalizePrimaryColor),
            customSecondary = Color(settings.personalizeSecondaryColor)
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── 0. APPEARANCE & THEME CARD ───────────────────────────────────────
        Card(
            onClick = { showThemeStudio = true },
            modifier = Modifier.fillMaxWidth(),
            shape = CardShape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "APPEARANCE & THEME",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = primaryColor.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, primaryColor)
                    ) {
                        Text(
                            text = "Theme Studio",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = activePreset.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        val modeText = when (settings.themeMode) {
                            "LIGHT" -> "Light Mode"
                            "SYSTEM" -> "System Default"
                            else -> if (settings.pureBlackAmoled) "Dark Mode (Pure Black AMOLED)" else "Dark Mode"
                        }
                        Text(
                            text = modeText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy((-8).dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(activePreset.primaryDark)
                                .border(2.dp, Color.White, CircleShape)
                        )
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(activePreset.secondaryDark)
                                .border(2.dp, Color.White, CircleShape)
                        )
                    }
                }
            }
        }

        // ── 1. GAME SETTINGS CARD ─────────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = CardShape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "GAME SETTINGS",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Quarter Length",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "$quarterLength Minutes • ${multiplier.format("0.00")}x XP",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor
                    )
                }

                Slider(
                    value = quarterLength.toFloat(),
                    onValueChange = { onQuarterLengthChanged(it.toInt()) },
                    valueRange = 4f..12f,
                    steps = 7,
                    colors = SliderDefaults.colors(
                        activeTrackColor = primaryColor,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                        thumbColor = primaryColor
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), thickness = 0.5.dp)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Quarter XP Normalization",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Scale XP based on quarter length",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = settings.normalizeQuarterXp,
                        onCheckedChange = { onNormalizeChanged(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = primaryColor,
                            uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }
            }
        }

        Text(
            text = "Shorter quarters = faster XP pace. Normalization scales XP so 6-minute quarters feel like 12-minute quarters.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        // ── 2. CAREER BACKUP CARD ─────────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = CardShape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "CAREER BACKUP",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Export your active player save to JSON or restore a career save from a backup file.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Auto-Save Backup Switch Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Auto-Save Backup",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Automatically save career JSON on progress",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = settings.autoSaveEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled && settings.autoSaveFolderUri.isNullOrBlank()) {
                                folderPickerLauncher.launch(null)
                            } else {
                                viewModel.updateAutoSaveEnabled(enabled)
                                if (enabled) {
                                    viewModel.triggerAutoSave()
                                }
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = primaryColor,
                            uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }

                // Backup Folder Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(
                            text = "Backup Folder",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = folderName,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (settings.autoSaveFolderUri.isNullOrBlank()) MaterialTheme.colorScheme.error else primaryColor,
                            maxLines = 1
                        )
                    }
                    OutlinedButton(
                        onClick = { folderPickerLauncher.launch(null) },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (settings.autoSaveFolderUri.isNullOrBlank()) "Choose Folder" else "Change",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Subtle status caption
                val dateFormat = remember { SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault()) }
                val lastSavedText = remember(settings.lastAutoSaveTimestamp) {
                    if (settings.lastAutoSaveTimestamp > 0L) {
                        "Last auto-saved: ${dateFormat.format(Date(settings.lastAutoSaveTimestamp))}"
                    } else {
                        "Not auto-saved yet"
                    }
                }

                Text(
                    text = lastSavedText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), thickness = 0.5.dp)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            val sanitized = playerName.lowercase(Locale.ROOT).replace("\\s+".toRegex(), "_")
                            val fileName = "${sanitized}_save.json"
                            scope.launch {
                                val json = onExportToJson()
                                if (json != null) {
                                    pendingExportJson = json
                                    exportLauncher.launch(fileName)
                                } else {
                                    Toast.makeText(context, "No active profile to export", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryColor,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(
                            text = "Export Save (.json)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = {
                            importLauncher.launch(arrayOf("application/json", "*/*"))
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = secondaryColor,
                            contentColor = MaterialTheme.colorScheme.onSecondary
                        )
                    ) {
                        Text(
                            text = "Import Save (.json)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    if (showThemeStudio) {
        ThemeStudioDialog(
            settings = settings,
            activeTeamName = activeTeamName,
            viewModel = viewModel,
            onDismiss = { showThemeStudio = false }
        )
    }
}

@Composable
private fun ThemeStudioDialog(
    settings: AppSettings,
    activeTeamName: String?,
    viewModel: PlayerLockViewModel,
    onDismiss: () -> Unit
) {
    var categoryFilter by remember { mutableStateOf("All") }
    val customPrimary = Color(settings.personalizePrimaryColor)
    val customSecondary = Color(settings.personalizeSecondaryColor)

    val presets = remember(settings.selectedThemeId, activeTeamName, settings.personalizePrimaryColor, settings.personalizeSecondaryColor, categoryFilter) {
        AppThemeCatalog.getThemePresets(
            activeTeamName = activeTeamName,
            customPrimary = customPrimary,
            customSecondary = customSecondary,
            categoryFilter = categoryFilter
        )
    }

    val activePreset = remember(settings.selectedThemeId, activeTeamName, settings.personalizePrimaryColor, settings.personalizeSecondaryColor) {
        AppThemeCatalog.getPresetById(
            id = settings.selectedThemeId,
            activeTeamName = activeTeamName,
            customPrimary = customPrimary,
            customSecondary = customSecondary
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ── Header Row ─────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "THEME STUDIO",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        letterSpacing = 1.sp
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Theme Studio",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // ── Live Mini-App Preview Card ──────────────────────────────
                LiveMiniAppPreviewCard(
                    preset = activePreset,
                    themeMode = settings.themeMode,
                    pureBlackAmoled = settings.pureBlackAmoled
                )

                // ── Mode Switcher & Pure Black Toggle ──────────────────────
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "COLOR MODE",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Mode Switcher (Dark | Light | System)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp))
                                .padding(3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf("DARK" to "Dark", "LIGHT" to "Light", "SYSTEM" to "System").forEach { (modeKey, modeLabel) ->
                                val isSelected = settings.themeMode == modeKey
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                        .clickable { viewModel.updateThemeMode(modeKey) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = modeLabel,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Pure Black (AMOLED) Switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Pure Black (AMOLED) Dark Mode",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "True #000000 background for OLED screens",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = settings.pureBlackAmoled,
                                onCheckedChange = { viewModel.updatePureBlackAmoled(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                }

                // ── Category Filter Chips ───────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("All", "Featured", "NBA Teams").forEach { cat ->
                        val isSel = categoryFilter == cat
                        Surface(
                            onClick = { categoryFilter = cat },
                            shape = RoundedCornerShape(50),
                            color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = cat,
                                fontSize = 12.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // ── 2-Column Theme Grid ─────────────────────────────────────
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    presets.chunked(2).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowItems.forEach { preset ->
                                ThemePresetCard(
                                    preset = preset,
                                    isSelected = settings.selectedThemeId == preset.id,
                                    onSelect = { viewModel.updateSelectedThemeId(preset.id) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (rowItems.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                // ── Inline Custom Color Editor (When "Personalize" Selected) ─
                if (settings.selectedThemeId == "personalize") {
                    PersonalizeColorEditor(
                        currentPrimary = customPrimary,
                        currentSecondary = customSecondary,
                        onColorsChanged = { newPrim, newSec ->
                            viewModel.updatePersonalizeColors(newPrim, newSec)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun LiveMiniAppPreviewCard(
    preset: AppThemePreset,
    themeMode: String,
    pureBlackAmoled: Boolean
) {
    val isDark = themeMode != "LIGHT"
    val bg = when {
        isDark && pureBlackAmoled -> Color(0xFF000000)
        isDark -> Color(0xFF121212)
        else -> Color(0xFFF5F6F8)
    }
    val cardBg = when {
        isDark && pureBlackAmoled -> Color(0xFF0D0D0D)
        isDark -> Color(0xFF1E1E1E)
        else -> Color(0xFFFFFFFF)
    }
    val prim = if (isDark) preset.primaryDark else preset.primaryLight
    val sec = if (isDark) preset.secondaryDark else preset.secondaryLight
    val txtColor = if (isDark) Color.White else Color(0xFF121212)
    val subTxtColor = if (isDark) Color(0xFFAAAAAA) else Color(0xFF666666)

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = bg,
        border = BorderStroke(1.dp, prim.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "LIVE PREVIEW",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = prim,
                letterSpacing = 1.sp
            )

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = cardBg,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Header row: Player name + OVR Badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(prim.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("23", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = prim)
                            }
                            Column {
                                Text("MyPlayer", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = txtColor)
                                Text("PG • LA Lakers", fontSize = 11.sp, color = subTxtColor)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(50),
                            color = prim
                        ) {
                            Text(
                                text = "88 OVR",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.Black,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    // AP Progress Bar
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Attribute Points (AP)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = txtColor)
                            Text("12 AP", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = prim)
                        }
                        LinearProgressIndicator(
                            progress = { 0.75f },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = prim,
                            trackColor = prim.copy(alpha = 0.2f)
                        )
                    }

                    // BP Progress Bar
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Badge Points (BP)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = txtColor)
                            Text("4 BP", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = sec)
                        }
                        LinearProgressIndicator(
                            progress = { 0.40f },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = sec,
                            trackColor = sec.copy(alpha = 0.2f)
                        )
                    }

                    // Buttons row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {},
                            modifier = Modifier.weight(1f).height(32.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = prim, contentColor = Color.Black),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Preview XP", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Surface(
                            shape = RoundedCornerShape(50),
                            color = sec.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, sec)
                        ) {
                            Text(
                                text = "🏆 Playoff",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = sec,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemePresetCard(
    preset: AppThemePreset,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val borderColor = if (isSelected) primaryColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    val containerBg = if (isSelected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface

    Surface(
        onClick = onSelect,
        shape = RoundedCornerShape(12.dp),
        color = containerBg,
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = preset.name,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = primaryColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Swatch Bar / Palette Icon
            if (preset.id == "personalize") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(20.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp)),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(preset.primaryDark)
                    )
                    Spacer(Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(preset.secondaryDark)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("🎨 Custom", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = primaryColor)
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(18.dp)
                        .clip(RoundedCornerShape(6.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(preset.primaryDark)
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(preset.secondaryDark)
                    )
                }
            }
        }
    }
}

@Composable
private fun PersonalizeColorEditor(
    currentPrimary: Color,
    currentSecondary: Color,
    onColorsChanged: (Color, Color) -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "CUSTOM COLOR STUDIO",
                style = MaterialTheme.typography.labelSmall,
                color = primaryColor,
                letterSpacing = 1.sp
            )

            // Primary Accent Picker
            ColorPickerSection(
                title = "Primary Accent",
                currentColor = currentPrimary,
                onColorChange = { newPrim -> onColorsChanged(newPrim, currentSecondary) }
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), thickness = 0.5.dp)

            // Secondary Accent Picker
            ColorPickerSection(
                title = "Secondary Accent",
                currentColor = currentSecondary,
                onColorChange = { newSec -> onColorsChanged(currentPrimary, newSec) }
            )
        }
    }
}

@Composable
private fun ColorPickerSection(
    title: String,
    currentColor: Color,
    onColorChange: (Color) -> Unit
) {
    val hsv = remember(currentColor) {
        val arr = FloatArray(3)
        android.graphics.Color.colorToHSV(currentColor.toArgb(), arr)
        Triple(arr[0], arr[1], arr[2])
    }

    val quickDots = listOf(
        Color(0xFFFFB300),
        Color(0xFF10B981),
        Color(0xFF00E5FF),
        Color(0xFF2979FF),
        Color(0xFFB388FF),
        Color(0xFFFF4081),
        Color(0xFFFF6D00),
        Color(0xFFFFFFFF)
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(currentColor)
                    .border(1.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
            )
        }

        // Quick Preset Dots
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            quickDots.forEach { dotColor ->
                val isSel = dotColor.toArgb() == currentColor.toArgb()
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                        .border(if (isSel) 2.dp else 1.dp, if (isSel) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), CircleShape)
                        .clickable { onColorChange(dotColor) }
                )
            }
        }

        // Hue Slider
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("Hue", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Slider(
                value = hsv.first,
                onValueChange = { newHue ->
                    val argb = android.graphics.Color.HSVToColor(floatArrayOf(newHue, hsv.second.coerceAtLeast(0.4f), hsv.third.coerceAtLeast(0.7f)))
                    onColorChange(Color(argb))
                },
                valueRange = 0f..360f,
                colors = SliderDefaults.colors(
                    activeTrackColor = currentColor,
                    inactiveTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    thumbColor = currentColor
                )
            )
        }

        // Brightness Slider
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("Brightness / Saturation", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Slider(
                value = hsv.third,
                onValueChange = { newVal ->
                    val argb = android.graphics.Color.HSVToColor(floatArrayOf(hsv.first, hsv.second, newVal.coerceIn(0.2f, 1f)))
                    onColorChange(Color(argb))
                },
                valueRange = 0.2f..1f,
                colors = SliderDefaults.colors(
                    activeTrackColor = currentColor,
                    inactiveTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    thumbColor = currentColor
                )
            )
        }
    }
}

private fun Double.format(format: String): String {
    return java.text.DecimalFormat(format).format(this)
}
