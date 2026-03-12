package one.next.player.core.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import one.next.player.core.ui.designsystem.NextIcons

@Composable
fun NextSwitch(
    isChecked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    checkedIcon: ImageVector = NextIcons.Check,
) {
    val thumbContent: (@Composable () -> Unit)? = if (isChecked) {
        {
            Icon(
                imageVector = checkedIcon,
                contentDescription = null,
                modifier = Modifier.size(SwitchDefaults.IconSize),
            )
        }
    } else {
        null
    }

    Switch(
        checked = isChecked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = isEnabled,
        thumbContent = thumbContent,
    )
}
