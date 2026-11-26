package com.example.playerlock.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.playerlock.data.BadgeGlossaryData
import com.example.playerlock.data.BadgeSeedData
import com.example.playerlock.data.PlayerAttribute
import com.example.playerlock.data.PlayerBadge
import com.example.playerlock.progression.ProgressionCalculator

private val CardShape = RoundedCornerShape(16.dp)

private fun getBadgeTierColor(tier: String): Color {
    return when (tier.uppercase()) {
        "BRONZE" -> Color(0xFFCD7F32)
        "SILVER" -> Color(0xFFC0C0C0)
        "GOLD" -> Color(0xFFFFD700)
        "HOF" -> Color(0xFFA855F7)
        "LEGEND" -> Color(0xFF06B6D4)
        else -> Color(0xFF888888)
    }
}

@Composable
fun UpgradeShopTab(
    viewModel: PlayerLockViewModel,
    hasPendingUpgrades: Boolean = false,
    modifier: Modifier = Modifier
) {
    var shopTab by remember { mutableIntStateOf(0) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        Text(
            text = "UPGRADE SHOP",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 2.sp
        )

        // ── Segmented Switcher ────────────────────────────────────────────────
        SegmentedPillSwitcher(
            options = listOf("Attributes", "Badges"),
            selectedIndex = shopTab,
            onOptionSelected = { shopTab = it },
            modifier = Modifier.fillMaxWidth()
        )

        val uiState by viewModel.uiState.collectAsState()

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (shopTab) {
                0 -> AttributeShop(
                    attributes = uiState.attributes,
                    profile = uiState.profile,
                    hasPendingUpgrades = hasPendingUpgrades,
                    onUpgrade = { attributeId, newValue ->
                        viewModel.buyAttributeUpgrade(attributeId, newValue)
                    }
                )
                1 -> BadgeShop(
                    badges = uiState.badges,
                    profile = uiState.profile,
                    hasPendingUpgrades = hasPendingUpgrades,
                    onUpgrade = { badgeId, targetTier ->
                        viewModel.buyBadgeUpgrade(badgeId, targetTier)
                    }
                )
            }
        }
    }
}

@Composable
private fun SegmentedPillSwitcher(
    options: List<String>,
    selectedIndex: Int,
    onOptionSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier.height(44.dp)
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            options.forEachIndexed { index, option ->
                val selected = selectedIndex == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable { onOptionSelected(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = option,
                        style = TextStyle(
                            fontSize = 14.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun AttributeShop(
    attributes: List<PlayerAttribute>,
    profile: com.example.playerlock.data.PlayerProfile?,
    hasPendingUpgrades: Boolean = false,
    onUpgrade: (Int, Int) -> Unit
) {
    // Exact category order matching AttributeSeedData (1..36)
    val attributeCategoryOrder = listOf(
        "Offense",
        "Defense",
        "Athleticism",
        "Mental",
        "Misc"
    )

    // Pre-group and sort within each category strictly by sortOrder
    val grouped = attributeCategoryOrder.associateWith { category ->
        attributes.filter { it.category == category }.sortedBy { it.sortOrder }
    }

    val expandedCategories = remember {
        mutableStateMapOf(*attributeCategoryOrder.map { it to true }.toTypedArray())
    }

    val bottomPadding = if (hasPendingUpgrades) 72.dp + 16.dp else 16.dp

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = 0.dp,
            bottom = bottomPadding
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Explicitly iterate through attributeCategoryOrder so order is always deterministic
        attributeCategoryOrder.forEach { category ->
            val catAttrs = grouped[category] ?: return@forEach
            if (catAttrs.isNotEmpty()) {
                item(key = "attr_card_$category") {
                    val isExpanded = expandedCategories[category] != false

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = CardShape,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { expandedCategories[category] = !isExpanded }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = category,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Icon(
                                    imageVector = if (isExpanded) Icons.Rounded.KeyboardArrowDown else Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                    contentDescription = if (isExpanded) "Collapse $category" else "Expand $category",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            AnimatedVisibility(
                                visible = isExpanded,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Column(
                                    modifier = Modifier.padding(top = 12.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // catAttrs already sorted by sortOrder
                                    catAttrs.forEach { attr ->
                                        AttributeRow(
                                            attribute = attr,
                                            profile = profile ?: com.example.playerlock.data.PlayerProfile(),
                                            onUpgrade = { newValue ->
                                                onUpgrade(attr.id, newValue)
                                            }
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

@Composable
private fun AttributeRow(
    attribute: PlayerAttribute,
    profile: com.example.playerlock.data.PlayerProfile,
    onUpgrade: (Int) -> Unit
) {
    val cost = ProgressionCalculator.attributeUpgradeCost(attribute.currentValue)
    val canAfford = ProgressionCalculator.canAfford(profile, cost)
    val canUpgrade = ProgressionCalculator.canUpgradeAttribute(attribute.currentValue)
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val onSecondaryColor = MaterialTheme.colorScheme.onSecondary

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = attribute.attributeName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${attribute.category}  •  ${attribute.currentValue}/99",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (canUpgrade) {
            Button(
                onClick = {
                    if (canAfford) {
                        onUpgrade(attribute.currentValue + 1)
                    }
                },
                enabled = canAfford,
                shape = RoundedCornerShape(50),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (canAfford) secondaryColor else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (canAfford) onSecondaryColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                ),
                modifier = Modifier.height(36.dp)
            ) {
                Text(
                    text = "+1 (${cost.cost} AP)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.height(36.dp)
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "MAX",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}

@Composable
private fun BadgeShop(
    badges: List<PlayerBadge>,
    profile: com.example.playerlock.data.PlayerProfile?,
    hasPendingUpgrades: Boolean = false,
    onUpgrade: (Int, String) -> Unit
) {
    val tiers = listOf("None", "Bronze", "Silver", "Gold", "HOF", "Legend")
    val categoryOrder = listOf("Finishing", "Shooting", "Playmaking", "Defense", "Physicals", "Rebounding")
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surface

    var searchQuery by remember { mutableStateOf("") }
    var showGlossaryDialog by remember { mutableStateOf(false) }
    val expandedCategories = remember {
        mutableStateMapOf(*categoryOrder.map { it to true }.toTypedArray(), "Custom" to true)
    }

    val filtered = badges.filter {
        searchQuery.isBlank() || it.badgeName.contains(searchQuery, ignoreCase = true)
    }

    val grouped = filtered.groupBy { it.category }.mapValues { (_, badges) ->
        badges.sortedBy { it.sortOrder }
    }

    val displayOrder = if (searchQuery.isBlank()) {
        categoryOrder
    } else {
        categoryOrder.filter { grouped.containsKey(it) }
    }

    val otherBadges = filtered.filter { it.category !in categoryOrder }

    val bottomPadding = if (hasPendingUpgrades) 72.dp + 16.dp else 16.dp

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = 0.dp,
                bottom = bottomPadding
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Glossary browser button
            item {
                Surface(
                    onClick = { showGlossaryDialog = true },
                    shape = RoundedCornerShape(14.dp),
                    color = surfaceColor,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Badge Glossary",
                            tint = primaryColor,
                            modifier = Modifier.size(18.dp).padding(end = 4.dp)
                        )
                        Text(
                            text = "Badge Glossary",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = primaryColor
                        )
                    }
                }
            }

            // Streamlined Search bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search badges…", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = primaryColor,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        cursorColor = primaryColor,
                        focusedContainerColor = surfaceColor,
                        unfocusedContainerColor = surfaceColor
                    )
                )
            }

            if (filtered.isEmpty()) {
                item {
                    Text(
                        text = if (searchQuery.isBlank()) "No badges yet."
                               else "No badges match \"$searchQuery\".",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            } else {
                // Render standard categories wrapped in surface Cards
                displayOrder.forEach { category ->
                    val catBadges = grouped[category] ?: return@forEach
                    val isExpanded = expandedCategories[category] != false

                    item(key = "card_$category") {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = CardShape,
                            colors = CardDefaults.cardColors(containerColor = surfaceColor),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { expandedCategories[category] = !isExpanded }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = category,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Icon(
                                        imageVector = if (isExpanded) Icons.Rounded.KeyboardArrowDown else Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                        contentDescription = if (isExpanded) "Collapse $category" else "Expand $category",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                AnimatedVisibility(
                                    visible = isExpanded,
                                    enter = expandVertically() + fadeIn(),
                                    exit = shrinkVertically() + fadeOut()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(top = 12.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        catBadges.forEach { badge ->
                                            BadgeRow(
                                                badge = badge,
                                                tiers = tiers,
                                                profile = profile ?: com.example.playerlock.data.PlayerProfile(),
                                                onUpgrade = { targetTier -> onUpgrade(badge.id, targetTier) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Render other / custom badges wrapped in surface Card
                if (otherBadges.isNotEmpty()) {
                    val isExpanded = expandedCategories["Custom"] != false

                    item(key = "card_custom") {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = CardShape,
                            colors = CardDefaults.cardColors(containerColor = surfaceColor),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { expandedCategories["Custom"] = !isExpanded }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Custom",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Icon(
                                        imageVector = if (isExpanded) Icons.Rounded.KeyboardArrowDown else Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                        contentDescription = if (isExpanded) "Collapse Custom" else "Expand Custom",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                AnimatedVisibility(
                                    visible = isExpanded,
                                    enter = expandVertically() + fadeIn(),
                                    exit = shrinkVertically() + fadeOut()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(top = 12.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        otherBadges.forEach { badge ->
                                            BadgeRow(
                                                badge = badge,
                                                tiers = tiers,
                                                profile = profile ?: com.example.playerlock.data.PlayerProfile(),
                                                onUpgrade = { targetTier -> onUpgrade(badge.id, targetTier) }
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

        if (showGlossaryDialog) {
            BadgeGlossaryDialog(
                onDismiss = { showGlossaryDialog = false }
            )
        }
    }
}

@Composable
private fun BadgeGlossaryDialog(
    onDismiss: () -> Unit
) {
    var glossarySearch by remember { mutableStateOf("") }
    val categoryOrder = listOf("Finishing", "Shooting", "Playmaking", "Defense", "Physicals", "Rebounding")
    val allBadges = BadgeSeedData.BADGES

    val filteredBadges = remember(glossarySearch) {
        if (glossarySearch.isBlank()) allBadges
        else allBadges.filter {
            it.badgeName.contains(glossarySearch, ignoreCase = true) ||
            it.category.contains(glossarySearch, ignoreCase = true) ||
            BadgeGlossaryData.descriptionFor(it.badgeName).contains(glossarySearch, ignoreCase = true)
        }
    }

    val grouped = remember(filteredBadges) {
        filteredBadges.groupBy { it.category }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "2K Badge Glossary",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = glossarySearch,
                    onValueChange = { glossarySearch = it },
                    placeholder = { Text("Search 53 NBA 2K Badges…") },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    categoryOrder.forEach { category ->
                        val badgesInCategory = grouped[category] ?: emptyList()
                        if (badgesInCategory.isNotEmpty()) {
                            item(key = category) {
                                Text(
                                    text = category.uppercase(),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                            items(badgesInCategory, key = { it.badgeName }) { badge ->
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = badge.badgeName,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = BadgeGlossaryData.descriptionFor(badge.badgeName),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
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

@Composable
private fun BadgeRow(
    badge: PlayerBadge,
    tiers: List<String>,
    profile: com.example.playerlock.data.PlayerProfile,
    onUpgrade: (String) -> Unit
) {
    val currentTier = badge.tier
    val currentTierIndex = tiers.indexOf(currentTier)
    val nextTierIndex = currentTierIndex + 1
    val nextTier = if (nextTierIndex < tiers.size) tiers[nextTierIndex] else null

    val cost = if (nextTier != null) {
        ProgressionCalculator.badgeUpgradeCost(nextTier)
    } else {
        ProgressionCalculator.UpgradeCost(0, "BP")
    }
    val canAfford = ProgressionCalculator.canAfford(profile, cost)
    val canUpgrade = nextTier != null && ProgressionCalculator.canApplyBadge(nextTier)

    val tierColor = getBadgeTierColor(currentTier)
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val onSecondaryColor = MaterialTheme.colorScheme.onSecondary

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = badge.badgeName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "${badge.category} •",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = currentTier,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = tierColor
                )
            }
        }

        if (nextTier != null) {
            Button(
                onClick = {
                    if (canAfford && canUpgrade) {
                        onUpgrade(nextTier)
                    }
                },
                enabled = canAfford && canUpgrade,
                shape = RoundedCornerShape(50),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (canAfford && canUpgrade) secondaryColor else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (canAfford && canUpgrade) onSecondaryColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                ),
                modifier = Modifier.height(36.dp)
            ) {
                Text(
                    text = "+1 (${cost.cost} BP)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.height(36.dp)
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "MAX",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}
