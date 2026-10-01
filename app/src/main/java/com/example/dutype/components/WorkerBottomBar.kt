package com.example.dutype.components

import com.example.dutype.ui.theme.bg
import com.example.dutype.ui.theme.fg
import com.example.dutype.ui.theme.bd
import com.dutype.app.R
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.RectangleShape
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.dutype.ui.theme.AppTypography
import com.example.dutype.ui.theme.IconSizes
import com.example.dutype.ui.theme.WorkerColors

/**
 * Worker bottom bar — the same flat, edge-to-edge docked bar as [EmployerBottomBar] (white, top
 * divider, 60 dp row, 24 dp icons, 11 sp labels). Tabs: Home, Jobs, My Jobs, Account. The Map tab
 * is hidden (the map screen still exists and opens from the home screen).
 */
@Composable
fun WorkerBottomBar(
    navController: NavController,
    backgroundColor: Color = WorkerColors.BottomNavBackground,
    selectedItemColor: Color = WorkerColors.BottomNavSelected,
    unselectedItemColor: Color = WorkerColors.BottomNavUnselected,
    modifier: Modifier = Modifier
) {
    val navBackStackEntry = navController.currentBackStackEntryAsState().value
    val currentRoute = navBackStackEntry?.destination?.route

    // Jobs carries query params, so it is matched by route prefix.
    val items = listOf(
        WorkerBottomBarItem(
            route = com.example.dutype.navigation.WorkerBottomRoutes.HOME,
            labelResId = R.string.bottom_nav_home,
            iconResUnfilled = R.drawable.ic_home_unfilled,
            iconResFilled = R.drawable.ic_home_filled
        ),
        WorkerBottomBarItem(
            route = com.example.dutype.navigation.WorkerBottomRoutes.JOBS,
            matchPrefix = com.example.dutype.navigation.Routes.WORKER_ALL_JOBS,
            labelResId = R.string.bottom_nav_jobs,
            icon = Icons.Outlined.Work,
            iconSelected = Icons.Filled.Work
        ),
        // Map tab hidden from the bar:
        // WorkerBottomBarItem(route = WorkerBottomRoutes.MAP, matchPrefix = Routes.WORKER_JOB_MAP,
        //     labelResId = R.string.bottom_nav_map, icon = Icons.Outlined.Map, iconSelected = Icons.Filled.Map),
        WorkerBottomBarItem(
            route = com.example.dutype.navigation.WorkerBottomRoutes.MY_JOBS,
            labelResId = R.string.bottom_nav_my_jobs,
            iconResUnfilled = R.drawable.myjobs,
            iconResFilled = R.drawable.myjobs
        ),
        WorkerBottomBarItem(
            route = com.example.dutype.navigation.WorkerBottomRoutes.PROFILE,
            labelResId = R.string.bottom_nav_account,
            iconResUnfilled = R.drawable.ic_person_unfilled,
            iconResFilled = R.drawable.ic_person_filled
        )
    )
    val selectedColor = Color(0xFF0F172A).fg()
    val unselectedColor = Color(0xFF64748B).fg()

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.White.bg(),
        shape = RectangleShape,
        shadowElevation = 6.dp,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            HorizontalDivider(thickness = 1.dp, color = Color(0xFFE2E8F0).bd())
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { item ->
                    val isSelected = if (item.matchPrefix != null) {
                        currentRoute?.startsWith(item.matchPrefix) == true
                    } else {
                        currentRoute == item.route
                    }
                    val label = stringResource(id = item.labelResId)
                    val tint = if (isSelected) selectedColor else unselectedColor
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                if (!isSelected) navigateToWorkerTab(navController, item.route)
                            }
                    ) {
                        val iconRes = if (isSelected) item.iconResFilled else item.iconResUnfilled
                        val vectorIcon = if (isSelected) (item.iconSelected ?: item.icon) else item.icon
                        if (iconRes != null) {
                            Icon(painter = painterResource(id = iconRes), contentDescription = label, modifier = Modifier.size(24.dp), tint = tint)
                        } else if (vectorIcon != null) {
                            Icon(imageVector = vectorIcon, contentDescription = label, modifier = Modifier.size(24.dp), tint = tint)
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = tint,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

/**
 * The ONLY way to switch worker tabs (Home / Jobs / Map / My Jobs / Account), from the
 * bottom bar or from inside a screen ("See all", "My Jobs" button, ...).
 *
 * Tab-switch fix: screens used to reach tab routes with a plain `navigate()`, which
 * stacked tabs on top of each other (e.g. Jobs -> Job detail -> My Jobs). The next
 * bottom-bar tap then saved/restored that whole mixed stack, so tapping "Jobs" could
 * land back on "My Jobs" and the bar looked broken. Every tab now sits directly on
 * Home, so each saved tab stack only ever contains that tab's own screens.
 *
 * @param restoreState false when the caller passes new arguments (e.g. a category
 *   filter) that must win over the tab's previously saved state.
 */
fun navigateToWorkerTab(navController: NavController, route: String, restoreState: Boolean = true) {
    val startId = navController.graph.findStartDestination().id
    if (route == com.example.dutype.navigation.WorkerBottomRoutes.HOME) {
        // Home is the graph root: pop back to it (saving the tab we leave) instead of
        // pushing a second Home.
        if (!navController.popBackStack(startId, inclusive = false, saveState = true)) {
            navController.navigate(route) { launchSingleTop = true }
        }
        return
    }
    navController.navigate(route) {
        popUpTo(startId) { saveState = true }
        launchSingleTop = true
        this.restoreState = restoreState
    }
}

// Data class for worker bottom bar items
private data class WorkerBottomBarItem(
    val route: String,
    @StringRes val labelResId: Int,
    val icon: ImageVector? = null,
    val iconSelected: ImageVector? = null,
    @DrawableRes val iconResUnfilled: Int? = null,
    @DrawableRes val iconResFilled: Int? = null,
    // When set, this item is "selected" whenever the current route starts
    // with this prefix (used for routes carrying query params, e.g. Jobs).
    val matchPrefix: String? = null
)
