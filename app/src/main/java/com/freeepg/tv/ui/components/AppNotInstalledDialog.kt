package com.freeepg.tv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GetApp
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.freeepg.tv.deeplink.LaunchResult
import com.freeepg.tv.ui.theme.AccentCyan
import com.freeepg.tv.ui.theme.BgCard
import com.freeepg.tv.ui.theme.BgSurface
import com.freeepg.tv.ui.theme.BorderFocused
import com.freeepg.tv.ui.theme.BorderUnfocused
import com.freeepg.tv.ui.theme.TextPrimary
import com.freeepg.tv.ui.theme.TextSecondary
import com.freeepg.tv.ui.theme.TextTertiary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNotInstalledDialog(
    request: LaunchResult.AppNotInstalled,
    onInstallClick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .width(520.dp)
            .background(BgCard, RoundedCornerShape(12.dp))
            .border(1.5.dp, BorderUnfocused, RoundedCornerShape(12.dp))
            .padding(24.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .background(request.broadcaster.brandColor, RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = request.broadcaster.displayName,
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "App Not Installed",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Body
            Text(
                text = "To watch '${request.programme.title}' live on ${request.channel.name}, the ${request.broadcaster.displayName} TV app is needed.",
                color = TextPrimary,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Would you like to install it from the Google Play Store now?",
                color = TextSecondary,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                DialogButton(
                    text = "Cancel",
                    icon = null,
                    onClick = onDismiss
                )

                Spacer(modifier = Modifier.width(10.dp))

                DialogButton(
                    text = "Install from Store",
                    icon = Icons.Default.GetApp,
                    accentColor = AccentCyan,
                    isPrimary = true,
                    onClick = { onInstallClick(request.primaryPackage) }
                )
            }
        }
    }
}

@Composable
private fun DialogButton(
    text: String,
    icon: ImageVector?,
    accentColor: Color = TextSecondary,
    isPrimary: Boolean = false,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused = interactionSource.collectIsFocusedAsState().value

    val bg = when {
        isFocused && isPrimary -> AccentCyan
        isFocused -> BgSurface
        isPrimary -> AccentCyan.copy(alpha = 0.2f)
        else -> Color.Transparent
    }

    val contentColor = when {
        isFocused && isPrimary -> Color.Black
        isFocused -> TextPrimary
        isPrimary -> AccentCyan
        else -> TextTertiary
    }

    val border = when {
        isFocused -> BorderFocused
        isPrimary -> AccentCyan.copy(alpha = 0.5f)
        else -> BorderUnfocused
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource)
            .background(bg, RoundedCornerShape(8.dp))
            .border(1.5.dp, border, RoundedCornerShape(8.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = text,
            color = contentColor,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )
    }
}
