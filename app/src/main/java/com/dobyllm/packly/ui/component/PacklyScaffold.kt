package com.dobyllm.packly.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Backpack
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Home
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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

val PacklyTopLevelDestinations = listOf(
    PacklyTopLevelDestination(PacklyRoute.Home, "Home", Icons.Rounded.Home),
    PacklyTopLevelDestination(PacklyRoute.Items, "Items", Icons.Rounded.EditNote),
    PacklyTopLevelDestination(PacklyRoute.Lists, "Lists", Icons.Rounded.Checklist),
    PacklyTopLevelDestination(PacklyRoute.Trips, "Trips", Icons.Rounded.Backpack),
)

@Composable
fun PacklyScaffold(
    currentRoute: String?,
    canNavigateBack: Boolean,
    nestedTitle: String?,
    fabAction: PacklyFabAction?,
    onBack: () -> Unit,
    onDestinationClick: (PacklyTopLevelDestination) -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    val showBottomBar = PacklyTopLevelDestinations.any { it.route == currentRoute }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            PacklyTopBar(
                canNavigateBack = canNavigateBack,
                title = if (showBottomBar) "Packly" else nestedTitle.orEmpty(),
                onBack = onBack,
            )
        },
        bottomBar = {
            if (showBottomBar) {
                PacklyBottomNavBar(
                    currentRoute = currentRoute,
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
                    imageVector = Icons.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = PacklyOnSurfaceVariant,
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(48.dp),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.packly_logo),
                    contentDescription = "Packly logo",
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(PacklySurfaceContainer)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                    contentScale = ContentScale.Crop,
                )
            }
        }

        Text(
            text = title,
            modifier = Modifier.align(Alignment.Center),
            style = if (canNavigateBack) MaterialTheme.typography.titleMedium else MaterialTheme.typography.headlineMedium,
            color = if (canNavigateBack) MaterialTheme.colorScheme.onSurface else PacklyPrimary,
            fontWeight = if (canNavigateBack) FontWeight.SemiBold else FontWeight.Bold,
        )

        // Right-side spacer keeps the centered title optically balanced after removing settings.
        Box(modifier = Modifier.align(Alignment.CenterEnd).size(48.dp))
    }
}

@Composable
fun PacklyBottomNavBar(
    currentRoute: String?,
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
        PacklyTopLevelDestinations.forEach { destination ->
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
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
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
