package com.example.playerlock.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.SaveAlt
import com.example.playerlock.data.CustomPlayerTemplate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.playerlock.data.AttributeSeedData
import com.example.playerlock.data.BadgeGlossaryData
import com.example.playerlock.data.BadgeSeedData
import com.example.playerlock.data.PlayerBadge
import com.example.playerlock.data.NbaTeamCatalog
import com.example.playerlock.data.NbaTeamScheme
import com.example.playerlock.progression.ProgressionCalculator
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import kotlin.math.roundToInt

private val BadgeTierList = listOf("None", "Bronze", "Silver", "Gold", "HOF", "Legend")

private fun normalizeBadgeTier(tierRaw: String): String {
    return when (tierRaw.trim().uppercase(Locale.ROOT)) {
        "1", "BRONZE" -> "Bronze"
        "2", "SILVER" -> "Silver"
        "3", "GOLD" -> "Gold"
        "4", "HOF", "HALL OF FAME", "HALL_OF_FAME" -> "HOF"
        "5", "LEGEND" -> "Legend"
        else -> "None"
    }
}

private fun getBadgeTierIndex(tier: String): Int {
    return when (normalizeBadgeTier(tier)) {
        "Bronze" -> 1
        "Silver" -> 2
        "Gold" -> 3
        "HOF" -> 4
        "Legend" -> 5
        else -> 0
    }
}

private fun getBadgeTierColor(tier: String): Color {
    return when (normalizeBadgeTier(tier)) {
        "Bronze" -> Color(0xFFCD7F32)
        "Silver" -> Color(0xFFC0C0C0)
        "Gold" -> Color(0xFFFFD700)
        "HOF" -> Color(0xFFA855F7)
        "Legend" -> Color(0xFF06B6D4)
        else -> Color(0xFF555555)
    }
}



private fun loadBitmapFromUri(context: android.content.Context, uri: Uri): ImageBitmap? {
    return try {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream)?.asImageBitmap()
        }
    } catch (_: Exception) {
        null
    }
}

fun cropAndSaveCircularAvatar(
    context: android.content.Context,
    sourceBitmap: ImageBitmap,
    panOffset: Offset,
    zoom: Float,
    viewportSizePx: Float
): String? {
    return try {
        val srcAndroidBitmap = sourceBitmap.asAndroidBitmap()
        val bw = srcAndroidBitmap.width.toFloat()
        val bh = srcAndroidBitmap.height.toFloat()

        val baseScale = maxOf(
            viewportSizePx / bw,
            viewportSizePx / bh
        )
        val totalScale = baseScale * zoom
        val drawnWidth = bw * totalScale
        val drawnHeight = bh * totalScale

        val maxOffsetX = ((drawnWidth - viewportSizePx) / 2f).coerceAtLeast(0f)
        val maxOffsetY = ((drawnHeight - viewportSizePx) / 2f).coerceAtLeast(0f)
        val clampedX = panOffset.x.coerceIn(-maxOffsetX, maxOffsetX)
        val clampedY = panOffset.y.coerceIn(-maxOffsetY, maxOffsetY)

        val outputSizePx = 256
        val outRatio = outputSizePx.toFloat() / viewportSizePx

        val outBitmap = Bitmap.createBitmap(outputSizePx, outputSizePx, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(outBitmap)

        val paint = android.graphics.Paint(
            android.graphics.Paint.ANTI_ALIAS_FLAG or android.graphics.Paint.FILTER_BITMAP_FLAG
        )

        canvas.drawARGB(0, 0, 0, 0)
        canvas.drawCircle(outputSizePx / 2f, outputSizePx / 2f, outputSizePx / 2f, paint)

        paint.xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_IN)

        val matrix = android.graphics.Matrix().apply {
            postScale(totalScale * outRatio, totalScale * outRatio)
            postTranslate(
                ((viewportSizePx - drawnWidth) / 2f + clampedX) * outRatio,
                ((viewportSizePx - drawnHeight) / 2f + clampedY) * outRatio
            )
        }

        canvas.drawBitmap(srcAndroidBitmap, matrix, paint)

        val avatarFile = File(context.filesDir, "avatar_${System.currentTimeMillis()}.png")
        FileOutputStream(avatarFile).use { out ->
            outBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }

        avatarFile.absolutePath
    } catch (_: Exception) {
        null
    }
}

@Composable
fun PlayerSetupScreen(
    onPlayerCreated: (
        playerName: String,
        position: String,
        startingOvr: Int,
        archetype: String,
        heightInches: Int,
        weightLbs: Int,
        jerseyNumber: Int,
        teamName: String,
        customAttributes: Map<String, Int>?,
        customBadges: Map<String, String>?,
        avatarPath: String?
    ) -> Unit,
    onSkip: () -> Unit,
    onCareerImported: (() -> Unit)? = null,
    viewModel: PlayerLockViewModel? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val surfaceVarColor = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val onSurfaceVarColor = MaterialTheme.colorScheme.onSurfaceVariant

    var playerName by remember { mutableStateOf("") }
    var position by remember { mutableStateOf("PG") }
    var setupMode by remember { mutableIntStateOf(0) } // 0 = Archetype Preset, 1 = Custom / Existing Player
    var selectedArchetype by remember { mutableStateOf("3PT Playmaker") }
    var startingOvr by remember { mutableIntStateOf(60) }

    // Vitals Studio state
    var heightInches by remember { mutableIntStateOf(78) } // 6'6"
    var weightLbs by remember { mutableIntStateOf(210) }
    var jerseyNumber by remember { mutableIntStateOf(23) }
    var selectedTeamScheme by remember { mutableStateOf(NbaTeamCatalog.getScheme("Draft Prospect")) }
    var showVitalsStudio by remember { mutableStateOf(false) }

    // Avatar state
    var avatarBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var savedAvatarPath by remember { mutableStateOf<String?>(null) }
    var pendingCroppingBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

    // Status message for Import / Export
    var importExportStatus by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    // Custom Starting Attributes map (35 attributes initialized to 60)
    val customAttributesMap = remember {
        mutableStateMapOf<String, Int>().apply {
            AttributeSeedData.ATTRIBUTES.forEach { attr ->
                put(attr.attributeName, 60)
            }
        }
    }

    // Custom Starting Badges map (initialized to None)
    val customBadgesMap = remember {
        mutableStateMapOf<String, String>().apply {
            BadgeSeedData.BADGES.forEach { badge ->
                put(badge.badgeName, "None")
            }
        }
    }

    val expandedBadgeCategories = remember {
        mutableStateMapOf<String, Boolean>().apply {
            listOf("Finishing", "Shooting", "Playmaking", "Defense", "Physicals", "Rebounding").forEach { category ->
                put(category, true)
            }
        }
    }

    var showAttributesSection by remember { mutableStateOf(false) }
    var showBadgesSection by remember { mutableStateOf(false) }
    var badgeSearchQuery by remember { mutableStateOf("") }

    val positions = listOf("PG", "SG", "SF", "PF", "C")
    val archetypes = listOf(
        "3PT Playmaker",
        "Slashing Playmaker",
        "Lockdown Defender",
        "Paint Beast",
        "Sharpshooter",
        "Balanced"
    )

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val bitmap = loadBitmapFromUri(context, uri)
            if (bitmap != null) {
                pendingCroppingBitmap = bitmap
            }
        }
    }

    val importJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                val vm = viewModel
                val parseResult = if (vm != null) {
                    vm.parsePlayerSetupFromUri(context, uri)
                } else {
                    val manager = com.example.playerlock.data.CareerBackupManager(
                        com.example.playerlock.progression.ProgressionRepository(
                            com.example.playerlock.data.AppDatabase.getDatabase(context)
                        ),
                        com.example.playerlock.data.SettingsDataStore(context)
                    )
                    manager.parsePlayerSetupFromUri(context, uri)
                }

                withContext(Dispatchers.Main) {
                    parseResult.onSuccess { template ->
                        playerName = template.name
                        position = template.position
                        heightInches = template.heightInches
                        weightLbs = template.weightLbs
                        jerseyNumber = template.jerseyNumber
                        selectedTeamScheme = NbaTeamCatalog.getScheme(template.teamName)
                        selectedArchetype = template.archetype
                        startingOvr = template.startingOvr

                        template.attributes.forEach { (name, value) ->
                            customAttributesMap[name] = value
                        }
                        template.badges.forEach { (name, tier) ->
                            customBadgesMap[name] = tier
                        }

                        setupMode = 1 // Switch to Custom Player tab
                        importExportStatus = true to "Player data loaded — edit any stats below or tap Create Player!"
                    }.onFailure { e ->
                        importExportStatus = false to (e.localizedMessage ?: "Invalid JSON file format.")
                    }
                }
            }
        }
    }

    val exportJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                try {
                    val vm = viewModel
                    if (vm == null) {
                        withContext(Dispatchers.Main) {
                            importExportStatus = false to "ViewModel not available for JSON export."
                        }
                        return@launch
                    }

                    val currentOvr = if (setupMode == 1) {
                        ProgressionCalculator.calculateOverallRating(position, customAttributesMap.toMap())
                    } else {
                        startingOvr
                    }
                    val template = CustomPlayerTemplate(
                        name = playerName.ifBlank { "Custom Player" },
                        position = position,
                        heightInches = heightInches,
                        weightLbs = weightLbs,
                        jerseyNumber = jerseyNumber,
                        teamName = selectedTeamScheme.name,
                        archetype = selectedArchetype,
                        startingOvr = currentOvr,
                        attributes = customAttributesMap.toMap(),
                        badges = customBadgesMap.toMap()
                    )

                    val jsonString = vm.exportCustomPlayerTemplateToJson(template)
                    context.contentResolver.openOutputStream(uri, "wt")?.use { out ->
                        out.write(jsonString.toByteArray(Charsets.UTF_8))
                    }

                    withContext(Dispatchers.Main) {
                        importExportStatus = true to "Template exported successfully!"
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        importExportStatus = false to "Export failed: ${e.localizedMessage}"
                    }
                }
            }
        }
    }

    if (pendingCroppingBitmap != null) {
        AvatarCropDialog(
            imageBitmap = pendingCroppingBitmap!!,
            onDismiss = { pendingCroppingBitmap = null },
            onSave = { panOffset, zoom, viewportSizePx ->
                val savedPath = cropAndSaveCircularAvatar(
                    context = context,
                    sourceBitmap = pendingCroppingBitmap!!,
                    panOffset = panOffset,
                    zoom = zoom,
                    viewportSizePx = viewportSizePx
                )
                if (savedPath != null) {
                    savedAvatarPath = savedPath
                    avatarBitmap = BitmapFactory.decodeFile(savedPath)?.asImageBitmap()
                }
                pendingCroppingBitmap = null
            }
        )
    }

    if (showVitalsStudio) {
        VitalsStudioBottomSheet(
            heightInches = heightInches,
            onHeightChange = { heightInches = it },
            weightLbs = weightLbs,
            onWeightChange = { weightLbs = it },
            jerseyNumber = jerseyNumber,
            onJerseyNumberChange = { jerseyNumber = it },
            selectedTeamScheme = selectedTeamScheme,
            onTeamSchemeChange = { selectedTeamScheme = it },
            onDismiss = { showVitalsStudio = false }
        )
    }

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "CREATE YOUR PLAYER",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = primaryColor,
                modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
            )

            // Basic Info Card (Prospect Card: Avatar, Name, Position, OVR, Vitals Button)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = surfaceColor),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    // Side-by-side: Avatar picker + Player Name
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Circular avatar button
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .border(BorderStroke(2.dp, primaryColor), CircleShape)
                                .background(surfaceVarColor)
                                .clickable { imagePickerLauncher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            if (avatarBitmap != null) {
                                Image(
                                    bitmap = avatarBitmap!!,
                                    contentDescription = "Player Avatar",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "Add Photo",
                                        tint = primaryColor,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        tint = primaryColor,
                                        modifier = Modifier
                                            .size(14.dp)
                                            .align(Alignment.BottomEnd)
                                    )
                                }
                            }
                        }

                        // Player Name text field
                        OutlinedTextField(
                            value = playerName,
                            onValueChange = { playerName = it },
                            label = { Text("Player Name") },
                            placeholder = { Text("e.g. Stephen Curry") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    val previewOvr = if (setupMode == 1) {
                        ProgressionCalculator.calculateOverallRating(position, customAttributesMap.toMap())
                    } else {
                        startingOvr
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Position",
                            style = MaterialTheme.typography.labelMedium,
                            color = onSurfaceVarColor
                        )
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = surfaceVarColor,
                            border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.6f))
                        ) {
                            Text(
                                text = "$previewOvr OVR",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = primaryColor,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        positions.forEach { pos ->
                            val selected = pos == position
                            Button(
                                onClick = { position = pos },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(0.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selected) primaryColor else surfaceVarColor,
                                    contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else onSurfaceColor
                                )
                            ) { Text(pos) }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Live Formatted Vitals on Prospect Card
                    Text(
                        text = "${heightInches / 12}'${heightInches % 12}\" • $weightLbs lbs • #$jerseyNumber • ${selectedTeamScheme.name}",
                        style = MaterialTheme.typography.labelSmall,
                        color = primaryColor.copy(alpha = 0.9f),
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Vitals & Team Jersey Studio Trigger Button
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = surfaceVarColor,
                        border = BorderStroke(1.dp, primaryColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showVitalsStudio = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = "Edit Vitals",
                                    tint = primaryColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Vitals • ${heightInches / 12}'${heightInches % 12}\" | $weightLbs lbs | #$jerseyNumber (${selectedTeamScheme.abbreviation})",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = onSurfaceColor
                                )
                            }
                            Text(
                                text = "STUDIO",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = primaryColor,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Mode Selector: Archetype Preset vs Custom / Existing Player
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = surfaceColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
            ) {
                Row(
                    modifier = Modifier.padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf("Archetype Preset", "Custom / Existing Player").forEachIndexed { index, option ->
                        val selected = setupMode == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selected) primaryColor else Color.Transparent)
                                .clickable { setupMode = index },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = option,
                                fontSize = 13.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = if (selected) MaterialTheme.colorScheme.onPrimary else onSurfaceVarColor
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Mode 0: Archetype Preset
            if (setupMode == 0) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = surfaceColor),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Archetype Preset",
                            style = MaterialTheme.typography.labelMedium,
                            color = onSurfaceVarColor,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        // Archetype Buttons Grid (2 rows x 3 cols)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            archetypes.chunked(3).forEach { row ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    row.forEach { arch ->
                                        val selected = arch == selectedArchetype
                                        Button(
                                            onClick = { selectedArchetype = arch },
                                            modifier = Modifier.weight(1f),
                                            contentPadding = PaddingValues(vertical = 8.dp, horizontal = 4.dp),
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (selected) primaryColor else surfaceVarColor,
                                                contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else onSurfaceColor
                                            )
                                        ) {
                                            Text(
                                                text = arch,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Live Preview Feedback: Top Boosted Attributes summary
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = surfaceVarColor,
                            border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = AttributeSeedData.getArchetypeKeyStrengths(selectedArchetype),
                                fontSize = 12.sp,
                                color = primaryColor,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Starting Overall Rating (60-80)",
                                style = MaterialTheme.typography.labelMedium,
                                color = onSurfaceVarColor
                            )
                            Text(
                                text = "$startingOvr OVR",
                                style = MaterialTheme.typography.titleMedium,
                                color = primaryColor,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Slider(
                            value = startingOvr.coerceIn(60, 80).toFloat(),
                            onValueChange = { startingOvr = it.roundToInt().coerceIn(60, 80) },
                            valueRange = 60f..80f,
                            colors = SliderDefaults.colors(
                                thumbColor = primaryColor,
                                activeTrackColor = primaryColor,
                                inactiveTrackColor = surfaceVarColor
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            } else {
                // Mode 1: Custom / Existing Player
                val customOvr = ProgressionCalculator.calculateOverallRating(position, customAttributesMap.toMap())

                // ── Import / Export (.json) Card ─────────────────────────────
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = surfaceColor),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "IMPORT / EXPORT (.JSON)",
                            style = MaterialTheme.typography.labelMedium,
                            color = onSurfaceVarColor,
                            letterSpacing = 1.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    val safeName = playerName.trim().replace(Regex("""[\\/:*?"<>|]"""), "_").ifBlank { "Custom_Player" }
                                    exportJsonLauncher.launch("${safeName}_template.json")
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.SaveAlt,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "Export (.json)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    importJsonLauncher.launch(arrayOf("application/json", "*/*"))
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, primaryColor)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.FileOpen,
                                    contentDescription = null,
                                    tint = primaryColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "Import (.json)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = primaryColor
                                )
                            }
                        }

                        importExportStatus?.let { (isSuccess, message) ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSuccess) primaryColor.copy(alpha = 0.12f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f),
                                border = BorderStroke(1.dp, if (isSuccess) primaryColor.copy(alpha = 0.4f) else MaterialTheme.colorScheme.error),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(if (isSuccess) "✅" else "⚠️", fontSize = 14.sp)
                                    Text(
                                        text = message,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isSuccess) onSurfaceColor else MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = surfaceColor),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Calculated Overall Rating",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = onSurfaceVarColor
                                )
                                Text(
                                    text = "Position-weighted 2K OVR updates live as you edit attributes",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = onSurfaceVarColor.copy(alpha = 0.7f)
                                )
                            }
                            Text(
                                text = "$customOvr OVR",
                                style = MaterialTheme.typography.headlineMedium,
                                color = primaryColor,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Quick Presets",
                            style = MaterialTheme.typography.labelSmall,
                            color = onSurfaceVarColor,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    AttributeSeedData.ATTRIBUTES.forEach { attr ->
                                        customAttributesMap[attr.attributeName] = 60
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = surfaceVarColor,
                                    contentColor = onSurfaceColor
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    text = "Set All 60",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1
                                )
                            }

                            Button(
                                onClick = {
                                    AttributeSeedData.ATTRIBUTES.forEach { attr ->
                                        customAttributesMap[attr.attributeName] = 70
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = surfaceVarColor,
                                    contentColor = onSurfaceColor
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    text = "Set All 70",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1
                                )
                            }

                            Button(
                                onClick = {
                                    val generated = AttributeSeedData.generateAttributesForArchetype(
                                        archetype = selectedArchetype,
                                        position = position,
                                        startingOvr = startingOvr
                                    )
                                    generated.forEach { (name, value) ->
                                        customAttributesMap[name] = value
                                    }
                                },
                                modifier = Modifier.weight(1.3f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = surfaceVarColor,
                                    contentColor = primaryColor
                                ),
                                border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = "Fill from Archetype",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Collapsible 1: Custom Starting Attributes
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = surfaceColor),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showAttributesSection = !showAttributesSection },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Custom Starting Attributes",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = onSurfaceColor
                            )
                            Icon(
                                imageVector = if (showAttributesSection) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = "Toggle Attributes",
                                tint = primaryColor
                            )
                        }

                        AnimatedVisibility(visible = showAttributesSection) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                val categoryOrder = listOf("Offense", "Defense", "Athleticism", "Mental", "Misc")
                                val groupedAttributes = AttributeSeedData.ATTRIBUTES.groupBy { it.category }
                                categoryOrder.forEach { category ->
                                    val attrs = (groupedAttributes[category] ?: emptyList()).sortedBy { it.sortOrder }
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = category,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = primaryColor,
                                            fontWeight = FontWeight.Bold
                                        )

                                        attrs.forEach { attr ->
                                            val currentVal = customAttributesMap[attr.attributeName] ?: 60
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = attr.attributeName,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = onSurfaceColor,
                                                    modifier = Modifier.weight(1f)
                                                )

                                                Surface(
                                                    shape = RoundedCornerShape(10.dp),
                                                    color = surfaceVarColor,
                                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        IconButton(
                                                            onClick = {
                                                                customAttributesMap[attr.attributeName] = (currentVal - 1).coerceIn(25, 99)
                                                            },
                                                            modifier = Modifier.size(32.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Remove,
                                                                contentDescription = "Decrease",
                                                                tint = onSurfaceVarColor,
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                        }

                                                        Text(
                                                            text = currentVal.toString(),
                                                            fontSize = 14.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = primaryColor,
                                                            modifier = Modifier.padding(horizontal = 8.dp)
                                                        )

                                                        IconButton(
                                                            onClick = {
                                                                customAttributesMap[attr.attributeName] = (currentVal + 1).coerceIn(25, 99)
                                                            },
                                                            modifier = Modifier.size(32.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Add,
                                                                contentDescription = "Increase",
                                                                tint = onSurfaceVarColor,
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                }
                            }
                        }


                    }
                }

                                        HorizontalDivider(color = surfaceVarColor, thickness = 0.5.dp)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Collapsible 2: Custom Starting Badges
                val totalActiveBadgesCount = customBadgesMap.values.count { normalizeBadgeTier(it) != "None" }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = surfaceColor),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showBadgesSection = !showBadgesSection },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (totalActiveBadgesCount > 0) "Custom Starting Badges ($totalActiveBadgesCount active)" else "Custom Starting Badges",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = onSurfaceColor
                                )
                            }
                            Icon(
                                imageVector = if (showBadgesSection) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = "Toggle Badges",
                                tint = primaryColor
                            )
                        }

                        AnimatedVisibility(visible = showBadgesSection) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = badgeSearchQuery,
                                    onValueChange = { badgeSearchQuery = it },
                                    placeholder = { Text("Search badges...") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Search,
                                            contentDescription = "Search Badges",
                                            tint = onSurfaceVarColor
                                        )
                                    },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                val filteredBadges = BadgeSeedData.BADGES.filter {
                                    it.badgeName.contains(badgeSearchQuery, ignoreCase = true) ||
                                            it.category.contains(badgeSearchQuery, ignoreCase = true)
                                }

                                val badgeCategoryOrder = listOf(
                                    "Finishing", "Shooting", "Playmaking",
                                    "Defense", "Physicals", "Rebounding"
                                )
                                val groupedBadges = filteredBadges
                                    .sortedBy { it.sortOrder }
                                    .groupBy { it.category }

                                badgeCategoryOrder.forEach { category ->
                                    val catBadges = groupedBadges[category] ?: return@forEach
                                    val isCategoryExpanded = expandedBadgeCategories[category] ?: true
                                    val activeCountInCat = catBadges.count { normalizeBadgeTier(customBadgesMap[it.badgeName] ?: "None") != "None" }

                                    Surface(
                                        onClick = { expandedBadgeCategories[category] = !isCategoryExpanded },
                                        shape = RoundedCornerShape(8.dp),
                                        color = surfaceVarColor,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 8.dp, bottom = 4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (isCategoryExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                                    contentDescription = "Toggle $category",
                                                    tint = primaryColor,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Text(
                                                    text = category.uppercase(),
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = onSurfaceColor
                                                )
                                            }

                                            Surface(
                                                shape = RoundedCornerShape(50),
                                                color = if (activeCountInCat > 0) primaryColor.copy(alpha = 0.15f) else Color.Transparent,
                                                border = if (activeCountInCat > 0) BorderStroke(1.dp, primaryColor.copy(alpha = 0.5f)) else null
                                            ) {
                                                Text(
                                                    text = if (activeCountInCat > 0) "$activeCountInCat selected" else "0 selected",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (activeCountInCat > 0) primaryColor else onSurfaceVarColor.copy(alpha = 0.7f),
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }

                                    AnimatedVisibility(visible = isCategoryExpanded) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 4.dp, bottom = 8.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            catBadges.forEach { badge ->
                                                val currentTier = normalizeBadgeTier(customBadgesMap[badge.badgeName] ?: "None")
                                                val currentTierIndex = getBadgeTierIndex(currentTier)
                                                val tierColor = getBadgeTierColor(currentTier)

                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .background(surfaceVarColor)
                                                        .clickable {
                                                            val nextIndex = (currentTierIndex + 1) % BadgeTierList.size
                                                            customBadgesMap[badge.badgeName] = BadgeTierList[nextIndex]
                                                        }
                                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = badge.badgeName,
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = onSurfaceColor
                                                        )
                                                        Text(
                                                            text = badge.category,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = onSurfaceVarColor
                                                        )
                                                    }

                                                    Surface(
                                                        shape = RoundedCornerShape(50),
                                                        color = tierColor.copy(alpha = 0.2f),
                                                        border = BorderStroke(1.dp, tierColor),
                                                        modifier = Modifier.clickable {
                                                            val nextIndex = (currentTierIndex + 1) % BadgeTierList.size
                                                            customBadgesMap[badge.badgeName] = BadgeTierList[nextIndex]
                                                        }
                                                    ) {
                                                        Text(
                                                            text = currentTier,
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = tierColor,
                                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onSkip) { Text("Skip for now") }
                Button(
                    onClick = {
                        if (playerName.isNotBlank()) {
                            val customAttrs = if (setupMode == 1) customAttributesMap.toMap() else null
                            val customBadges = if (setupMode == 1) customBadgesMap.toMap() else null
                            val effectiveOvr = if (setupMode == 1) {
                                ProgressionCalculator.calculateOverallRating(position, customAttributesMap.toMap())
                            } else {
                                startingOvr
                            }
                            onPlayerCreated(
                                playerName.trim(),
                                position,
                                effectiveOvr,
                                selectedArchetype,
                                heightInches,
                                weightLbs,
                                jerseyNumber,
                                selectedTeamScheme.name,
                                customAttrs,
                                customBadges,
                                savedAvatarPath
                            )
                        }
                    },
                    enabled = playerName.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(0.65f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = secondaryColor,
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    )
                ) {
                    Text(
                        text = "Create Player",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Interactive "Center Player Face" Crop Dialog
// ---------------------------------------------------------------------------

@Composable
fun AvatarCropDialog(
    imageBitmap: ImageBitmap,
    onDismiss: () -> Unit,
    onSave: (panOffset: Offset, zoom: Float, viewportSizePx: Float) -> Unit
) {
    val density = LocalDensity.current
    val viewportDp = 240.dp
    val viewportSizePx = with(density) { viewportDp.toPx() }
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val surfaceVarColor = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceVarColor = MaterialTheme.colorScheme.onSurfaceVariant
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface

    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var zoom by remember { mutableFloatStateOf(1f) }

    val bw = imageBitmap.width.toFloat()
    val bh = imageBitmap.height.toFloat()

    val baseScale = maxOf(
        viewportSizePx / bw,
        viewportSizePx / bh
    )
    val totalScale = baseScale * zoom
    val drawnWidth = bw * totalScale
    val drawnHeight = bh * totalScale

    val maxOffsetX = ((drawnWidth - viewportSizePx) / 2f).coerceAtLeast(0f)
    val maxOffsetY = ((drawnHeight - viewportSizePx) / 2f).coerceAtLeast(0f)

    val clampedX = panOffset.x.coerceIn(-maxOffsetX, maxOffsetX)
    val clampedY = panOffset.y.coerceIn(-maxOffsetY, maxOffsetY)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = surfaceColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Center Player Face",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = onSurfaceColor,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Circular preview viewport with primary ring
                Box(
                    modifier = Modifier
                        .size(viewportDp)
                        .border(BorderStroke(3.dp, primaryColor), CircleShape)
                        .clip(CircleShape)
                        .background(surfaceVarColor)
                        .pointerInput(imageBitmap) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                panOffset = Offset(
                                    panOffset.x + dragAmount.x,
                                    panOffset.y + dragAmount.y
                                )
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = imageBitmap,
                        contentDescription = "Crop Preview",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = zoom
                                scaleY = zoom
                                translationX = clampedX
                                translationY = clampedY
                            }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Zoom label + slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Zoom", color = onSurfaceVarColor, fontSize = 13.sp)
                    Text(
                        text = String.format(Locale.US, "%.1fx", zoom),
                        color = primaryColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Slider(
                    value = zoom,
                    onValueChange = { zoom = it },
                    valueRange = 1f..3.5f,
                    colors = SliderDefaults.colors(
                        thumbColor = primaryColor,
                        activeTrackColor = primaryColor,
                        inactiveTrackColor = surfaceVarColor
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    ) {
                        Text("Cancel", color = onSurfaceVarColor)
                    }
                    Button(
                        onClick = { onSave(Offset(clampedX, clampedY), zoom, viewportSizePx) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = secondaryColor,
                            contentColor = MaterialTheme.colorScheme.onSecondary
                        )
                    ) {
                        Text("Save Photo", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Vitals & Interactive Team Jersey Studio BottomSheet
// ---------------------------------------------------------------------------

private fun triggerTickVibration(context: Context) {
    try {
        @Suppress("DEPRECATION")
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator ?: (context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)
        } else {
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

        vibrator?.let {
            if (it.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    it.vibrate(VibrationEffect.createOneShot(25L, 210))
                } else {
                    @Suppress("DEPRECATION")
                    it.vibrate(25L)
                }
            }
        }
    } catch (_: Exception) {
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VitalsStudioBottomSheet(
    heightInches: Int,
    onHeightChange: (Int) -> Unit,
    weightLbs: Int,
    onWeightChange: (Int) -> Unit,
    jerseyNumber: Int,
    onJerseyNumberChange: (Int) -> Unit,
    selectedTeamScheme: NbaTeamScheme,
    onTeamSchemeChange: (NbaTeamScheme) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = 0) { 3 }
    val tabs = listOf("HEIGHT", "WEIGHT", "JERSEY & TEAM")
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val surfaceVarColor = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceVarColor = MaterialTheme.colorScheme.onSurfaceVariant

    var lastHapticInch by remember { mutableIntStateOf(heightInches) }
    var lastHapticWeight by remember { mutableIntStateOf(weightLbs) }
    var lastHapticJersey by remember { mutableIntStateOf(jerseyNumber) }
    var lastHapticTeamIndex by remember { mutableIntStateOf(NbaTeamCatalog.TEAMS.indexOf(selectedTeamScheme)) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "VITALS & TEAM STUDIO",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = primaryColor,
                letterSpacing = 1.5.sp,
                modifier = Modifier.padding(top = 2.dp, bottom = 2.dp)
            )
            Text(
                text = "Customize measurements and official NBA uniform",
                fontSize = 12.sp,
                color = onSurfaceVarColor,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Top Tab Indicators (HEIGHT, WEIGHT, JERSEY)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tabs.forEachIndexed { index, tabTitle ->
                    val selected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selected) primaryColor else surfaceVarColor)
                            .border(
                                BorderStroke(
                                    1.dp,
                                    if (selected) primaryColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                ),
                                RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                coroutineScope.launch { pagerState.animateScrollToPage(index) }
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tabTitle,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selected) MaterialTheme.colorScheme.onPrimary else onSurfaceVarColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3-page HorizontalPager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(370.dp)
            ) { page ->
                when (page) {
                    0 -> HeightStudioPage(
                        heightInches = heightInches,
                        onHeightChange = { newInches ->
                            if (newInches != lastHapticInch) {
                                lastHapticInch = newInches
                                triggerTickVibration(context)
                                onHeightChange(newInches)
                            }
                        }
                    )
                    1 -> WeightStudioPage(
                        weightLbs = weightLbs,
                        onWeightChange = { newWeight ->
                            if (newWeight != lastHapticWeight) {
                                lastHapticWeight = newWeight
                                triggerTickVibration(context)
                                onWeightChange(newWeight)
                            }
                        }
                    )
                    2 -> JerseyStudioPage(
                        jerseyNumber = jerseyNumber,
                        onJerseyNumberChange = { newNumber ->
                            if (newNumber != lastHapticJersey) {
                                lastHapticJersey = newNumber
                                triggerTickVibration(context)
                                onJerseyNumberChange(newNumber)
                            }
                        },
                        selectedTeamScheme = selectedTeamScheme,
                        onTeamSchemeChange = { newScheme ->
                            val newIdx = NbaTeamCatalog.TEAMS.indexOf(newScheme)
                            if (newIdx != lastHapticTeamIndex) {
                                lastHapticTeamIndex = newIdx
                                triggerTickVibration(context)
                                onTeamSchemeChange(newScheme)
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Confirm button
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = secondaryColor,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                )
            ) {
                Text("CONFIRM VITALS", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun HeightStudioPage(
    heightInches: Int,
    onHeightChange: (Int) -> Unit
) {
    val minHeightInches = 64 // 5'4"
    val maxHeightInches = 91 // 7'7"
    val totalRange = maxHeightInches - minHeightInches
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceVarColor = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceVarColor = MaterialTheme.colorScheme.onSurfaceVariant
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface

    val scoutingNote = when {
        heightInches <= 71 -> "Speed Demon: Elite perimeter speed and acceleration, vulnerable to interior contests."
        heightInches in 72..76 -> "Classic Guard: Excellent lateral quickness and ball control, standard guard wingspan."
        heightInches in 77..80 -> "Ideal Wing: Versatile two-way size, great balance of perimeter speed and contest height."
        heightInches in 81..83 -> "Modern Forward: Mismatch threat, interior shot contest with wing mobility."
        else -> "Towering Big: Elite rim protection and paint presence, slower lateral recovery."
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left column: Readout + fine-tune buttons
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "${heightInches / 12}'${heightInches % 12}\"",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = primaryColor
                )
                Text(
                    text = "$heightInches TOTAL INCHES",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = onSurfaceVarColor,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    IconButton(
                        onClick = { onHeightChange((heightInches - 1).coerceIn(minHeightInches, maxHeightInches)) },
                        modifier = Modifier
                            .size(42.dp)
                            .background(surfaceVarColor, CircleShape)
                            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)), CircleShape)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrease Height", tint = primaryColor)
                    }
                    IconButton(
                        onClick = { onHeightChange((heightInches + 1).coerceIn(minHeightInches, maxHeightInches)) },
                        modifier = Modifier
                            .size(42.dp)
                            .background(surfaceVarColor, CircleShape)
                            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)), CircleShape)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Increase Height", tint = primaryColor)
                    }
                }
            }

            // Right column: Interactive vertical ruler slider
            var rulerHeightPx by remember { mutableFloatStateOf(1f) }
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .fillMaxHeight()
                    .padding(vertical = 10.dp)
                    .onSizeChanged { rulerHeightPx = it.height.toFloat().coerceAtLeast(1f) }
                    .pointerInput(Unit) {
                        detectDragGestures { change, _ ->
                            change.consume()
                            val fraction = (1f - (change.position.y / rulerHeightPx)).coerceIn(0f, 1f)
                            val newInches = (minHeightInches + fraction * totalRange).roundToInt()
                            onHeightChange(newInches.coerceIn(minHeightInches, maxHeightInches))
                        }
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    // Vertical guide axis
                    drawLine(
                        color = Color(0xFF333333),
                        start = Offset(w * 0.45f, 0f),
                        end = Offset(w * 0.45f, h),
                        strokeWidth = 2f
                    )

                    // Draw ruler ticks
                    for (inch in minHeightInches..maxHeightInches) {
                        val fraction = (inch - minHeightInches).toFloat() / totalRange
                        val y = h * (1f - fraction)
                        val isFoot = inch % 12 == 0
                        val isHalfFoot = inch % 6 == 0

                        val tickLength = when {
                            isFoot -> 36f
                            isHalfFoot -> 24f
                            else -> 14f
                        }
                        val tickColor = when {
                            isFoot -> primaryColor
                            isHalfFoot -> Color.White.copy(alpha = 0.8f)
                            else -> Color(0xFF555555)
                        }
                        val stroke = if (isFoot) 3f else 1.5f

                        drawLine(
                            color = tickColor,
                            start = Offset(w * 0.45f - tickLength, y),
                            end = Offset(w * 0.45f, y),
                            strokeWidth = stroke
                        )
                    }

                    // Active Height Pointer Line
                    val activeFraction = (heightInches - minHeightInches).toFloat() / totalRange
                    val activeY = h * (1f - activeFraction)

                    drawLine(
                        color = primaryColor,
                        start = Offset(0f, activeY),
                        end = Offset(w * 0.70f, activeY),
                        strokeWidth = 4f
                    )
                    drawCircle(
                        color = primaryColor,
                        radius = 7f,
                        center = Offset(w * 0.70f, activeY)
                    )
                }
            }
        }

        // 2K Scouting Report Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = surfaceVarColor),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "2K SCOUTING REPORT",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryColor,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = scoutingNote,
                    fontSize = 12.sp,
                    color = onSurfaceColor,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
private fun WeightStudioPage(
    weightLbs: Int,
    onWeightChange: (Int) -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val surfaceVarColor = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceVarColor = MaterialTheme.colorScheme.onSurfaceVariant

    val (frameTitle, frameDesc, frameColor) = when {
        weightLbs < 185 -> Triple(
            "Featherweight / Quick",
            "Maximum acceleration & first-step speed. Lower collision strength and paint absorption.",
            secondaryColor
        )
        weightLbs in 185..220 -> Triple(
            "Lean / Athletic",
            "Optimal balance of stamina, lateral burst, and vertical pop across perimeter and transition.",
            primaryColor
        )
        weightLbs in 221..260 -> Triple(
            "Balanced Frame",
            "Imposing physical strength for driving through contact, boxing out, and setting brick screens.",
            Color(0xFFFFA000)
        )
        else -> Triple(
            "Heavy / Bruiser",
            "Dominant interior paint weight and post backdown leverage with trade-offs in top-end sprint.",
            MaterialTheme.colorScheme.error
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "$weightLbs lbs",
                fontSize = 50.sp,
                fontWeight = FontWeight.ExtraBold,
                color = primaryColor
            )

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                shape = RoundedCornerShape(50),
                color = frameColor.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, frameColor)
            ) {
                Text(
                    text = frameTitle,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = frameColor,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = frameDesc,
                fontSize = 12.sp,
                color = onSurfaceVarColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { onWeightChange((weightLbs - 1).coerceIn(115, 350)) },
                    modifier = Modifier
                        .size(40.dp)
                        .background(surfaceVarColor, CircleShape)
                        .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)), CircleShape)
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Decrease Weight", tint = primaryColor)
                }

                Slider(
                    value = weightLbs.toFloat(),
                    onValueChange = { onWeightChange(it.roundToInt().coerceIn(115, 350)) },
                    valueRange = 115f..350f,
                    colors = SliderDefaults.colors(
                        thumbColor = primaryColor,
                        activeTrackColor = primaryColor,
                        inactiveTrackColor = surfaceVarColor
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp)
                )

                IconButton(
                    onClick = { onWeightChange((weightLbs + 1).coerceIn(115, 350)) },
                    modifier = Modifier
                        .size(40.dp)
                        .background(surfaceVarColor, CircleShape)
                        .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)), CircleShape)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Increase Weight", tint = primaryColor)
                }
            }
        }
    }
}

@Composable
private fun JerseyStudioPage(
    jerseyNumber: Int,
    onJerseyNumberChange: (Int) -> Unit,
    selectedTeamScheme: NbaTeamScheme,
    onTeamSchemeChange: (NbaTeamScheme) -> Unit
) {
    val teams = NbaTeamCatalog.TEAMS
    val currentTeamIndex = teams.indexOf(selectedTeamScheme).coerceAtLeast(0)
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val onSurfaceVarColor = MaterialTheme.colorScheme.onSurfaceVariant

    val currentJerseyNumber by rememberUpdatedState(jerseyNumber)
    val onNumberChangeState by rememberUpdatedState(onJerseyNumberChange)
    var accumulatedDrag by remember { mutableFloatStateOf(0f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Sleeveless Basketball Jersey with vertical drag slider & +/- tap zones
        Box(
            modifier = Modifier
                .width(180.dp)
                .height(220.dp)
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragStart = { accumulatedDrag = 0f },
                        onDragEnd = { accumulatedDrag = 0f },
                        onDragCancel = { accumulatedDrag = 0f }
                    ) { change, dragAmount ->
                        change.consume()
                        accumulatedDrag -= dragAmount
                        val stepPx = 10f
                        if (kotlin.math.abs(accumulatedDrag) >= stepPx) {
                            val steps = (accumulatedDrag / stepPx).toInt()
                            accumulatedDrag -= steps * stepPx
                            val nextNum = (currentJerseyNumber + steps).coerceIn(0, 99)
                            if (nextNum != currentJerseyNumber) {
                                onNumberChangeState(nextNum)
                            }
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // 1. Dark inner neck cutout
                val innerNeckCutout = Path().apply {
                    moveTo(w * 0.33f, h * 0.08f)
                    quadraticTo(w * 0.50f, h * 0.22f, w * 0.67f, h * 0.08f)
                    close()
                }
                drawPath(innerNeckCutout, color = Color(0xFF141414))

                // 2. Jersey body path
                val jerseyBodyPath = Path().apply {
                    moveTo(w * 0.16f, h * 0.12f)
                    lineTo(w * 0.33f, h * 0.07f)
                    lineTo(w * 0.50f, h * 0.23f)
                    lineTo(w * 0.67f, h * 0.07f)
                    lineTo(w * 0.84f, h * 0.12f)
                    quadraticTo(w * 0.74f, h * 0.28f, w * 0.87f, h * 0.44f)
                    lineTo(w * 0.85f, h * 0.94f)
                    quadraticTo(w * 0.50f, h * 0.96f, w * 0.15f, h * 0.94f)
                    lineTo(w * 0.13f, h * 0.44f)
                    quadraticTo(w * 0.26f, h * 0.28f, w * 0.16f, h * 0.12f)
                    close()
                }
                drawPath(jerseyBodyPath, color = selectedTeamScheme.primaryColor)

                // 3. V-neck collar rib band
                val vNeckCollarPath = Path().apply {
                    moveTo(w * 0.33f, h * 0.07f)
                    lineTo(w * 0.50f, h * 0.23f)
                    lineTo(w * 0.67f, h * 0.07f)
                }
                drawPath(
                    path = vNeckCollarPath,
                    color = selectedTeamScheme.secondaryColor,
                    style = Stroke(width = 8f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                // 4. Armhole trim borders
                val leftArmholeTrim = Path().apply {
                    moveTo(w * 0.16f, h * 0.12f)
                    quadraticTo(w * 0.26f, h * 0.28f, w * 0.13f, h * 0.44f)
                }
                drawPath(
                    path = leftArmholeTrim,
                    color = selectedTeamScheme.secondaryColor,
                    style = Stroke(width = 7f, cap = StrokeCap.Round)
                )

                val rightArmholeTrim = Path().apply {
                    moveTo(w * 0.84f, h * 0.12f)
                    quadraticTo(w * 0.74f, h * 0.28f, w * 0.87f, h * 0.44f)
                }
                drawPath(
                    path = rightArmholeTrim,
                    color = selectedTeamScheme.secondaryColor,
                    style = Stroke(width = 7f, cap = StrokeCap.Round)
                )

                // 5. Vertical side-panel stripes
                drawLine(
                    color = selectedTeamScheme.secondaryColor,
                    start = Offset(w * 0.145f, h * 0.45f),
                    end = Offset(w * 0.165f, h * 0.94f),
                    strokeWidth = 10f
                )
                drawLine(
                    color = selectedTeamScheme.numberColor.copy(alpha = 0.85f),
                    start = Offset(w * 0.185f, h * 0.45f),
                    end = Offset(w * 0.205f, h * 0.94f),
                    strokeWidth = 2.5f
                )

                drawLine(
                    color = selectedTeamScheme.secondaryColor,
                    start = Offset(w * 0.855f, h * 0.45f),
                    end = Offset(w * 0.835f, h * 0.94f),
                    strokeWidth = 10f
                )
                drawLine(
                    color = selectedTeamScheme.numberColor.copy(alpha = 0.85f),
                    start = Offset(w * 0.815f, h * 0.45f),
                    end = Offset(w * 0.795f, h * 0.94f),
                    strokeWidth = 2.5f
                )

                // 6. Scale indicator
                val scaleX = w * 0.94f
                val scaleTop = h * 0.32f
                val scaleBottom = h * 0.88f

                drawLine(
                    color = Color.White.copy(alpha = 0.22f),
                    start = Offset(scaleX, scaleTop),
                    end = Offset(scaleX, scaleBottom),
                    strokeWidth = 2.5f
                )

                val numFraction = jerseyNumber / 99f
                val indicatorY = scaleBottom - (numFraction * (scaleBottom - scaleTop))
                drawCircle(
                    color = primaryColor,
                    radius = 5.5f,
                    center = Offset(scaleX, indicatorY)
                )
                drawCircle(
                    color = Color(0xFF141414),
                    radius = 2.5f,
                    center = Offset(scaleX, indicatorY)
                )
            }

            // Chest details: Team Abbreviation Wordmark + Large Number
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(top = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = selectedTeamScheme.abbreviation,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.5.sp,
                        color = selectedTeamScheme.secondaryColor,
                        modifier = Modifier.offset(x = 1.dp, y = 1.dp)
                    )
                    Text(
                        text = selectedTeamScheme.abbreviation,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.5.sp,
                        color = selectedTeamScheme.numberColor
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = jerseyNumber.toString(),
                        fontSize = 54.sp,
                        fontWeight = FontWeight.Black,
                        color = selectedTeamScheme.secondaryColor,
                        modifier = Modifier.offset(x = 2.dp, y = 2.dp)
                    )
                    Text(
                        text = jerseyNumber.toString(),
                        fontSize = 54.sp,
                        fontWeight = FontWeight.Black,
                        color = selectedTeamScheme.numberColor
                    )
                }
            }

            // Single-digit precision increment (+) tap zone at top
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 6.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.45f))
                    .border(BorderStroke(1.dp, primaryColor.copy(alpha = 0.5f)), RoundedCornerShape(12.dp))
                    .clickable {
                        if (jerseyNumber < 99) onJerseyNumberChange(jerseyNumber + 1)
                    }
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Increment Jersey Number",
                    tint = primaryColor,
                    modifier = Modifier.size(16.dp)
                )
            }

            // Single-digit precision decrement (-) tap zone at bottom
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 6.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.45f))
                    .border(BorderStroke(1.dp, primaryColor.copy(alpha = 0.5f)), RoundedCornerShape(12.dp))
                    .clickable {
                        if (jerseyNumber > 0) onJerseyNumberChange(jerseyNumber - 1)
                    }
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "Decrement Jersey Number",
                    tint = primaryColor,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Text(
            text = "Drag Jersey Up/Down or Tap +/- for #$jerseyNumber",
            fontSize = 11.sp,
            color = onSurfaceVarColor,
            letterSpacing = 0.5.sp
        )

        // Team Scheme Stepper + Slider
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        if (currentTeamIndex > 0) onTeamSchemeChange(teams[currentTeamIndex - 1])
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Team", tint = primaryColor)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = selectedTeamScheme.primaryColor,
                        border = BorderStroke(1.5.dp, selectedTeamScheme.secondaryColor)
                    ) {
                        Text(
                            text = selectedTeamScheme.abbreviation,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = selectedTeamScheme.numberColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Text(
                        text = selectedTeamScheme.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = onSurfaceColor
                    )
                }

                IconButton(
                    onClick = {
                        if (currentTeamIndex < teams.size - 1) onTeamSchemeChange(teams[currentTeamIndex + 1])
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Next Team", tint = primaryColor)
                }
            }

            Slider(
                value = currentTeamIndex.toFloat(),
                onValueChange = {
                    val idx = it.roundToInt().coerceIn(0, teams.size - 1)
                    onTeamSchemeChange(teams[idx])
                },
                valueRange = 0f..(teams.size - 1).toFloat(),
                steps = teams.size - 2,
                colors = SliderDefaults.colors(
                    thumbColor = primaryColor,
                    activeTrackColor = primaryColor,
                    inactiveTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            )
        }
    }
}
