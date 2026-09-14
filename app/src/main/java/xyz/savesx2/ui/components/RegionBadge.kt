package xyz.savesx2.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.savesx2.core.Ps2Region

/**
 * Compact, stylish badge displaying a savegame's regional origin (e.g. 🇺🇸 US, 🇪🇺 EU, 🇯🇵 JP).
 */
@Composable
fun RegionBadge(
    region: Ps2Region,
    modifier: Modifier = Modifier,
    showFullLabel: Boolean = false
) {
    if (region == Ps2Region.UNKNOWN) return

    val labelText = if (showFullLabel) {
        "${region.flagEmoji} ${region.displayName}"
    } else {
        "${region.flagEmoji} ${region.code}"
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color(region.badgeColor))
            .padding(horizontal = 5.dp, vertical = 1.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = labelText,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                letterSpacing = 0.3.sp
            ),
            color = Color(region.badgeTextColor),
            maxLines = 1
        )
    }
}
