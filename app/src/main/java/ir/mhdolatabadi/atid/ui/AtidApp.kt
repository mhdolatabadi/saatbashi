package ir.mhdolatabadi.atid.ui

import android.app.Activity
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.border
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import ir.mhdolatabadi.atid.R
import ir.mhdolatabadi.atid.ui.components.rememberHeroPalette
import ir.mhdolatabadi.atid.ui.components.pressScale
import ir.mhdolatabadi.atid.ui.dashboard.DashboardScreen
import ir.mhdolatabadi.atid.ui.home.HomeScreen
import ir.mhdolatabadi.atid.ui.notifications.PrayerTimesScreen
import ir.mhdolatabadi.atid.ui.theme.Atid

private sealed class AtidDestination(val route: String, val labelRes: Int, val iconRes: Int) {
    object Home : AtidDestination("home", R.string.title_home, R.drawable.ic_tab_qada)
    object Dashboard : AtidDestination("dashboard", R.string.title_dashboard, R.drawable.ic_tab_calendar)
    object PrayerTimes : AtidDestination("prayer_times", R.string.title_notifications, R.drawable.ic_tab_prayer_times)
}

private val bottomBarDestinations = listOf(
    AtidDestination.Dashboard,
    AtidDestination.PrayerTimes
)

@Composable
fun AtidApp() {
    val navController = rememberNavController()
    val colors = Atid.colors
    val hero = rememberHeroPalette()
    // The sky runs behind the status bar and the ground behind the navigation bar; their icons follow each.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = hero.isBright
                isAppearanceLightNavigationBars = !colors.isDark
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(colors.ground)) {
        NavHost(
            navController = navController,
            startDestination = AtidDestination.Dashboard.route,
            modifier = Modifier.fillMaxSize(),
            enterTransition = { fadeIn(tween(320)) + slideInVertically(tween(420)) { it / 24 } },
            exitTransition = { fadeOut(tween(160)) },
            popEnterTransition = { fadeIn(tween(320)) },
            popExitTransition = { fadeOut(tween(160)) }
        ) {
            composable(AtidDestination.Home.route) { HomeScreen() }
            composable(AtidDestination.Dashboard.route) {
                DashboardScreen(onOpenPrayerTimes = {
                    navController.navigate(AtidDestination.PrayerTimes.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                })
            }
            composable(AtidDestination.PrayerTimes.route) { PrayerTimesScreen() }
        }

        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = backStackEntry?.destination
        val selectedIndex = bottomBarDestinations.indexOfFirst { destination ->
            currentRoute?.hierarchy?.any { it.route == destination.route } == true
        }.coerceAtLeast(0)

        FloatingTabBar(
            selectedIndex = selectedIndex,
            onSelect = { destination ->
                navController.navigate(destination.route) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 14.dp)
        )
    }
}

/** A solid pill (web: .app-shell__nav): the active tab is filled with the accent and the fill glides between tabs. */
@Composable
private fun FloatingTabBar(
    selectedIndex: Int,
    onSelect: (AtidDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = Atid.colors
    val barShape = RoundedCornerShape(30.dp)
    BoxWithConstraints(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .widthIn(max = 340.dp)
            .fillMaxWidth()
            .shadow(16.dp, barShape, ambientColor = Color(0x591B2366), spotColor = Color(0x591B2366))
            .background(colors.surface, barShape)
            .border(1.dp, colors.surfaceLine, barShape)
            .padding(5.dp)
    ) {
        val tabWidth = maxWidth / bottomBarDestinations.size
        // offset() follows layout direction, so in RTL the pill moves leftwards from the first tab.
        val indicatorOffset by animateDpAsState(tabWidth * selectedIndex, tween(420), label = "tabIndicator")
        Box(
            modifier = Modifier
                .offset(x = indicatorOffset)
                .width(tabWidth)
                .height(48.dp)
                .background(colors.accent, RoundedCornerShape(24.dp))
        )
        Row(modifier = Modifier.fillMaxWidth().height(48.dp)) {
            bottomBarDestinations.forEachIndexed { index, destination ->
                val selected = index == selectedIndex
                val interaction = remember { MutableInteractionSource() }
                val tint by animateColorAsState(if (selected) colors.onAccent else colors.muted, tween(220), label = "tabTint")
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .pressScale(interaction)
                        .clickable(
                            interactionSource = interaction,
                            indication = null,
                            role = Role.Tab,
                            onClick = { onSelect(destination) }
                        ),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(destination.iconRes),
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = stringResource(destination.labelRes),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                        color = tint
                    )
                }
            }
        }
    }
}
