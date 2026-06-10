package com.dobyllm.packly.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Backpack
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.dobyllm.packly.R
import com.dobyllm.packly.navigation.PacklyRoute
import com.dobyllm.packly.ui.theme.PacklyOnSecondaryContainer
import com.dobyllm.packly.ui.theme.PacklyOnSurfaceVariant
import com.dobyllm.packly.ui.theme.PacklyPrimary
import com.dobyllm.packly.ui.theme.PacklySecondaryContainer
import com.dobyllm.packly.ui.theme.PacklySurface
import com.dobyllm.packly.ui.theme.PacklySurfaceContainer
import com.dobyllm.packly.ui.theme.PacklySurfaceContainerHigh
import com.dobyllm.packly.ui.token.PacklyRadius
import com.dobyllm.packly.ui.token.PacklySpacing

@Immutable
data class PacklyTopLevelDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

@Immutable
data class PacklyFabAction(
    val contentDescription: String,
    val onClick: () -> Unit,
)

@Immutable
data class PacklyTopBarAction(
    val label: String,
    val onClick: () -> Unit,
    val icon: ImageVector? = null,
)

@Composable
fun packlyTopLevelDestinations() = listOf(
    PacklyTopLevelDestination(PacklyRoute.Home, stringResource(R.string.nav_home), Icons.Rounded.Home),
    PacklyTopLevelDestination(PacklyRoute.Items, stringResource(R.string.nav_items), Icons.Rounded.EditNote),
    PacklyTopLevelDestination(PacklyRoute.Lists, stringResource(R.string.nav_lists), Icons.Rounded.Checklist),
    PacklyTopLevelDestination(PacklyRoute.Trips, stringResource(R.string.nav_trips), Icons.Rounded.Backpack),
)

@Composable
fun PacklyScaffold(
    currentRoute: String?,
    canNavigateBack: Boolean,
    nestedTitle: String?,
    fabAction: PacklyFabAction?,
    topBarAction: PacklyTopBarAction? = null,
    useCloseNavigationIcon: Boolean = false,
    onBack: () -> Unit,
    onDestinationClick: (PacklyTopLevelDestination) -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    val topLevelDestinations = packlyTopLevelDestinations()
    val showBottomBar = topLevelDestinations.any { it.route == currentRoute }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            PacklyTopBar(
                canNavigateBack = canNavigateBack,
                title = if (showBottomBar) stringResource(R.string.app_name) else nestedTitle.orEmpty(),
                onBack = onBack,
                action = topBarAction,
                useCloseNavigationIcon = useCloseNavigationIcon,
            )
        },
        bottomBar = {
            if (showBottomBar) {
                PacklyBottomNavBar(
                    currentRoute = currentRoute,
                    destinations = topLevelDestinations,
                    onDestinationClick = onDestinationClick,
                )
            }
        },
        floatingActionButton = {
            if (showBottomBar && fabAction != null) {
                PacklyFab(action = fabAction)
            }
        },
        content = content,
    )
}

@Composable
fun PacklyTopBar(
    canNavigateBack: Boolean,
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    action: PacklyTopBarAction? = null,
    useCloseNavigationIcon: Boolean = false,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(PacklySurface)
            .statusBarsPadding()
            .height(64.dp)
            .padding(horizontal = PacklySpacing.marginMobile),
    ) {
        if (canNavigateBack) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.CenterStart).size(48.dp),
            ) {
                Icon(
                    imageVector = if (useCloseNavigationIcon) Icons.Rounded.Close else Icons.Rounded.ArrowBack,
                    contentDescription = if (useCloseNavigationIcon) stringResource(R.string.action_close) else stringResource(R.string.action_back),
                    tint = PacklyOnSurfaceVariant,
                )
            }
        }

        Text(
            text = title,
            modifier = Modifier.align(Alignment.Center),
            style = if (canNavigateBack) MaterialTheme.typography.titleMedium else MaterialTheme.typography.headlineMedium,
            color = if (title == stringResource(R.string.app_name)) PacklyPrimary else if (canNavigateBack) MaterialTheme.colorScheme.onSurface else PacklyPrimary,
            fontWeight = FontWeight.Bold,
        )

        if (action == null) {
            // Right-side spacer keeps the centered title optically balanced after removing settings.
            Box(modifier = Modifier.align(Alignment.CenterEnd).size(48.dp))
        } else {
            if (action.icon == null) {
                androidx.compose.material3.TextButton(
                    onClick = action.onClick,
                    modifier = Modifier.align(Alignment.CenterEnd).defaultMinSize(minHeight = 48.dp),
                ) {
                    Text(
                        text = action.label,
                        style = MaterialTheme.typography.titleMedium,
                        color = PacklyPrimary,
                        fontWeight = FontWeight.Bold,
                    )
                }
            } else {
                IconButton(
                    onClick = action.onClick,
                    modifier = Modifier.align(Alignment.CenterEnd).size(48.dp),
                ) {
                    Icon(imageVector = action.icon, contentDescription = action.label, tint = PacklyPrimary)
                }
            }
        }
    }
}

@Composable
fun PacklyBottomNavBar(
    currentRoute: String?,
    destinations: List<PacklyTopLevelDestination>,
    onDestinationClick: (PacklyTopLevelDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(topStart = PacklyRadius.lg, topEnd = PacklyRadius.lg),
                ambientColor = PacklySecondaryContainer.copy(alpha = 0.18f),
                spotColor = PacklySecondaryContainer.copy(alpha = 0.18f),
            )
            .clip(RoundedCornerShape(topStart = PacklyRadius.lg, topEnd = PacklyRadius.lg))
            .background(PacklySurfaceContainer)
            .navigationBarsPadding()
            .height(72.dp)
            .padding(horizontal = PacklySpacing.xs, vertical = PacklySpacing.xs),
        horizontalArrangement = Arrangement.spacedBy(PacklySpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        destinations.forEach { destination ->
            val selected = currentRoute == destination.route
            PacklyBottomNavItem(
                destination = destination,
                selected = selected,
                onClick = { onDestinationClick(destination) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun PacklyBottomNavItem(
    destination: PacklyTopLevelDestination,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val background = if (selected) PacklySecondaryContainer else androidx.compose.ui.graphics.Color.Transparent
    val contentColor = if (selected) PacklyOnSecondaryContainer else PacklyOnSurfaceVariant

    androidx.compose.material3.Surface(
        onClick = onClick,
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .padding(horizontal = 1.dp)
            .semantics {
                role = Role.Tab
                this.selected = selected
            },
        shape = RoundedCornerShape(PacklyRadius.md),
        color = background,
        contentColor = contentColor,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = PacklySpacing.xs, vertical = PacklySpacing.xs),
            horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = destination.icon,
                contentDescription = destination.label,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = destination.label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Bold,
            )
        }
    }
}

@Composable
fun PacklyFab(
    action: PacklyFabAction,
    modifier: Modifier = Modifier,
) {
    FloatingActionButton(
        onClick = action.onClick,
        modifier = modifier
            .size(64.dp)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = PacklyPrimary.copy(alpha = 0.30f),
                spotColor = PacklyPrimary.copy(alpha = 0.30f),
            ),
        shape = RoundedCornerShape(20.dp),
        containerColor = PacklyPrimary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
    ) {
        Icon(
            imageVector = Icons.Rounded.Add,
            contentDescription = action.contentDescription,
        )
    }
}
