package com.example.playerlock.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.playerlock.data.PlayerAttribute
import com.example.playerlock.data.PlayerBadge
import com.example.playerlock.data.PlayerProfile
import com.example.playerlock.data.UpgradeHistory
import com.example.playerlock.progression.ProgressionCalculator
import kotlinx.coroutines.launch

private data class TabItem(val label: String, val icon: ImageVector)

private val tabs = listOf(
    TabItem("Dashboard", Icons.Rounded.Dashboard),
    TabItem("Log Game", Icons.Rounded.AddCircleOutline),
    TabItem("Shop", Icons.AutoMirrored.Rounded.TrendingUp),
    TabItem("History", Icons.Rounded.History)
)

private data class ResolvedUpgrade(
    val latestHistory: UpgradeHistory,
    val displayName: String,
    val fromLabel: String,
    val toLabel: String,
    val totalCost: Int,
    val currencyType: String,
    val canIncrease: Boolean,
    val onIncrease: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: PlayerLockViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val profile = uiState.profile

    if (uiState.showFirstLaunchSetup || profile == null) {
        PlayerSetupScreen(
            onPlayerCreated = { name, position, ovr, archetype, heightInches, weightLbs, jerseyNum, team, customAttrs, customBadges, avatarPath ->
                viewModel.createPlayer(name, position, ovr, archetype, heightInches, weightLbs, jerseyNum, team, customAttrs, customBadges, avatarPath)
            },
            onSkip = { viewModel.createPlayer("MyPlayer", "PG", 60) },
            viewModel = viewModel
        )
        return
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    var showOvrDialog by remember { mutableStateOf(false) }
    var showApplySheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    val unappliedHistory = uiState.upgradeHistory.filter { !it.isAppliedIn2k }
    val resolvedUpgrades = remember(unappliedHistory, uiState.attributes, uiState.badges, profile) {
        resolveUpgrades(
            unapplied = unappliedHistory,
            attributes = uiState.attributes,
            badges = uiState.badges,
            profile = profile,
            onUpgradeAttribute = { id, newVal -> viewModel.buyAttributeUpgrade(id, newVal) },
            onUpgradeBadge = { id, tier -> viewModel.buyBadgeUpgrade(id, tier) }
        )
    }
    val unappliedCount = resolvedUpgrades.size

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val backgroundColor = MaterialTheme.colorScheme.background
    val surfaceColor = MaterialTheme.colorScheme.surface
    val onSurfaceVarColor = MaterialTheme.colorScheme.onSurfaceVariant

    Scaffold(
        modifier = modifier,
        containerColor = backgroundColor,
        bottomBar = {
            NavigationBar(containerColor = surfaceColor) {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = {
                            selectedTab = index
                            if (index == 1) viewModel.clearXpPreview()
                        },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.label
                            )
                        },
                        label = { Text(tab.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = primaryColor,
                            selectedTextColor = primaryColor,
                            unselectedIconColor = onSurfaceVarColor,
                            unselectedTextColor = onSurfaceVarColor,
                            indicatorColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            when (selectedTab) {
                0 -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    DashboardTab(
                        profile = profile,
                        gameLogs = uiState.gameLogs,
                        awards = uiState.awards,
                        viewModel = viewModel,
                        onOvrClick = { showOvrDialog = true },
                        onSwitchProfile = { viewModel.switchProfile() }
                    )
                }
                1 -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    LogGameTab(viewModel = viewModel)
                }
                2 -> UpgradeShopTab(
                    viewModel = viewModel,
                    hasPendingUpgrades = unappliedCount > 0
                )
                3 -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    HistorySettingsTab(viewModel = viewModel)
                }
            }

            // Floating "Apply" pill overlaid directly on top of Shop content
            if (selectedTab == 2 && unappliedCount > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Transparent,
                                    backgroundColor.copy(alpha = 0.8f),
                                    backgroundColor
                                )
                            )
                        )
                        .padding(horizontal = 32.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = { showApplySheet = true },
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryColor,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text(
                            text = "Apply Upgrades ($unappliedCount) ▲",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }

    // ─── Apply Upgrades ModalBottomSheet ─────────────────────────────────────
    if (showApplySheet) {
        ModalBottomSheet(
            onDismissRequest = { showApplySheet = false },
            sheetState = sheetState,
            containerColor = surfaceColor
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp)
            ) {
                // Header
                Text(
                    text = "APPLY IN NBA 2K",
                    style = MaterialTheme.typography.labelMedium,
                    color = onSurfaceVarColor,
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = "Enter these upgrades in MyNBA Edit Player, then mark them done.",
                    style = MaterialTheme.typography.bodySmall,
                    color = onSurfaceVarColor,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(8.dp))

                if (resolvedUpgrades.isEmpty()) {
                    Text(
                        text = "All pending upgrades refunded.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = onSurfaceVarColor,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    resolvedUpgrades.forEach { upgrade ->
                        ApplyUpgradeRow(
                            upgrade = upgrade,
                            onDecrease = {
                                viewModel.undoUpgrade(upgrade.latestHistory.id)
                            },
                            onIncrease = upgrade.onIncrease
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (resolvedUpgrades.isNotEmpty()) {
                            viewModel.markAllUpgradesAppliedIn2k()
                        }
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            showApplySheet = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (resolvedUpgrades.isNotEmpty()) secondaryColor else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (resolvedUpgrades.isNotEmpty()) MaterialTheme.colorScheme.onSecondary else onSurfaceVarColor.copy(alpha = 0.4f)
                    )
                ) {
                    Text(
                        text = if (resolvedUpgrades.isNotEmpty()) "Mark All Applied in 2K" else "Close",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    // ─── OVR edit dialog ─────────────────────────────────────────────────────
    if (showOvrDialog) {
        var ovrInput by remember { mutableStateOf(profile.overallRating.toString()) }
        AlertDialog(
            onDismissRequest = { showOvrDialog = false },
            title = { Text("Update OVR") },
            text = {
                Column {
                    Text("Enter your current overall rating from NBA 2K")
                    OutlinedTextField(
                        value = ovrInput,
                        onValueChange = { ovrInput = it.filter(Char::isDigit).take(2) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateManualOvr(ovrInput.toIntOrNull() ?: profile.overallRating)
                    showOvrDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showOvrDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ApplyUpgradeRow(
    upgrade: ResolvedUpgrade,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val surfaceVarColor = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceVarColor = MaterialTheme.colorScheme.onSurfaceVariant
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = upgrade.displayName,
                style = MaterialTheme.typography.bodyMedium,
                color = onSurfaceColor,
                fontWeight = FontWeight.Medium
            )
            val currencyColor = if (upgrade.currencyType == "AP") primaryColor else secondaryColor
            Text(
                text = "${upgrade.totalCost} ${upgrade.currencyType}",
                style = MaterialTheme.typography.labelSmall,
                color = currencyColor
            )
        }

        Text(
            text = "${upgrade.fromLabel} → ${upgrade.toLabel}",
            style = MaterialTheme.typography.bodyMedium,
            color = primaryColor,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = surfaceVarColor,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDecrease,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                        contentDescription = "Decrease or refund upgrade",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }

                VerticalDivider(
                    modifier = Modifier.height(20.dp),
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                IconButton(
                    onClick = onIncrease,
                    enabled = upgrade.canIncrease,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = "Increase upgrade",
                        tint = if (upgrade.canIncrease) Color(0xFF10B981) else onSurfaceVarColor.copy(alpha = 0.4f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
}

private fun resolveUpgrades(
    unapplied: List<UpgradeHistory>,
    attributes: List<PlayerAttribute>,
    badges: List<PlayerBadge>,
    profile: PlayerProfile?,
    onUpgradeAttribute: (Int, Int) -> Unit,
    onUpgradeBadge: (Int, String) -> Unit
): List<ResolvedUpgrade> {
    val grouped = unapplied
        .groupBy { it.entityType to it.entityId }
        .entries
        .sortedWith(
            compareBy(
                { if (it.key.first == "ATTRIBUTE") 0 else 1 },
                { it.key.second }
            )
        )

    val tiers = listOf("None", "Bronze", "Silver", "Gold", "HOF", "Legend")

    return grouped.mapNotNull { (key, rows) ->
        val (type, entityId) = key
        val earliest = rows.minByOrNull { it.id } ?: return@mapNotNull null
        val latest   = rows.maxByOrNull { it.id } ?: return@mapNotNull null
        val totalCost = rows.sumOf { it.costSpent }
        val currencyType = latest.currencyType

        when (type) {
            "ATTRIBUTE" -> {
                val attr = attributes.find { it.id == entityId }
                val name = attr?.attributeName ?: "Attribute #$entityId"
                val nextValue = latest.newValue + 1
                val isUnderMax = latest.newValue < 99
                val nextCost = ProgressionCalculator.attributeUpgradeCost(latest.newValue)
                val canAfford = profile != null && ProgressionCalculator.canAfford(profile, nextCost)
                val canIncrease = isUnderMax && canAfford

                ResolvedUpgrade(
                    latestHistory = latest,
                    displayName = name,
                    fromLabel = earliest.oldValue.toString(),
                    toLabel = latest.newValue.toString(),
                    totalCost = totalCost,
                    currencyType = currencyType,
                    canIncrease = canIncrease,
                    onIncrease = { onUpgradeAttribute(entityId, nextValue) }
                )
            }
            "BADGE" -> {
                val badge = badges.find { it.id == entityId }
                val name = badge?.badgeName ?: "Badge #$entityId"
                val fromTier = tiers.getOrElse(earliest.oldValue) { "None" }
                val toTier   = tiers.getOrElse(latest.newValue)   { "Legend" }

                val nextTierIndex = latest.newValue + 1
                val nextTier = if (nextTierIndex < tiers.size) tiers[nextTierIndex] else null
                val nextCost = if (nextTier != null) ProgressionCalculator.badgeUpgradeCost(nextTier) else null
                val canAfford = profile != null && nextCost != null && ProgressionCalculator.canAfford(profile, nextCost)
                val canIncrease = nextTier != null && canAfford

                ResolvedUpgrade(
                    latestHistory = latest,
                    displayName = name,
                    fromLabel = fromTier,
                    toLabel = toTier,
                    totalCost = totalCost,
                    currencyType = currencyType,
                    canIncrease = canIncrease,
                    onIncrease = { if (nextTier != null) onUpgradeBadge(entityId, nextTier) }
                )
            }
            else -> null
        }
    }
}
