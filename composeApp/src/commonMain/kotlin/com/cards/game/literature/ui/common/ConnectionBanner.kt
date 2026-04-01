package com.cards.game.literature.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cards.game.literature.repository.ConnectionState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import literature.composeapp.generated.resources.Res
import literature.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

enum class BannerState { DISCONNECTED, RECONNECTING, RECONNECTED }

@Composable
fun ConnectionBanner(
    connectionState: StateFlow<ConnectionState>,
    modifier: Modifier = Modifier
) {
    val currentState by connectionState.collectAsState()
    var bannerState by remember { mutableStateOf<BannerState?>(null) }

    LaunchedEffect(Unit) {
        var wasDisconnected = false
        snapshotFlow { currentState }.collect { current ->
            when (current) {
                ConnectionState.DISCONNECTED -> {
                    wasDisconnected = true
                    bannerState = BannerState.DISCONNECTED
                }
                ConnectionState.RECONNECTING, ConnectionState.CONNECTING -> {
                    wasDisconnected = true
                    bannerState = BannerState.RECONNECTING
                }
                ConnectionState.CONNECTED -> {
                    if (wasDisconnected) {
                        bannerState = BannerState.RECONNECTED
                        delay(1000L)
                        bannerState = null
                    }
                    wasDisconnected = false
                }
            }
        }
    }

    AnimatedVisibility(
        visible = bannerState != null,
        enter = expandVertically(),
        exit = shrinkVertically(),
        modifier = modifier
    ) {
        val bgColor = when (bannerState) {
            BannerState.DISCONNECTED -> MaterialTheme.colorScheme.error.copy(alpha = 0.9f)
            BannerState.RECONNECTING -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f)
            BannerState.RECONNECTED -> Color(0xFF388E3C).copy(alpha = 0.9f)
            null -> Color.Transparent
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(bgColor)
                .padding(8.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                when (bannerState) {
                    BannerState.RECONNECTING -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            stringResource(Res.string.connection_reconnecting),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                    BannerState.RECONNECTED -> Text(
                        stringResource(Res.string.connection_reconnected),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    else -> Text(
                        stringResource(Res.string.connection_disconnected),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onError
                    )
                }
            }
        }
    }
}
