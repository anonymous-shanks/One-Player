package one.next.player.core.ui.composables

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import one.next.player.core.ui.R

@Stable
class RuntimePermissionState internal constructor(
    val permission: String,
) {
    var isGranted by mutableStateOf(false)
        internal set

    var shouldShowRationale by mutableStateOf(false)
        internal set

    internal var launchPermissionRequestState: () -> Unit = {}

    fun launchPermissionRequest() {
        launchPermissionRequestState()
    }
}

@Composable
fun rememberRuntimePermissionState(permission: String): RuntimePermissionState {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val state = remember(permission) { RuntimePermissionState(permission = permission) }
    val refreshState = {
        state.isGranted = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        state.shouldShowRationale = activity?.let {
            ActivityCompat.shouldShowRequestPermissionRationale(it, permission)
        } ?: false
    }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        state.isGranted = isGranted
        state.shouldShowRationale = activity?.let {
            ActivityCompat.shouldShowRequestPermissionRationale(it, permission)
        } ?: false
    }

    state.launchPermissionRequestState = { launcher.launch(permission) }
    refreshState()

    LifecycleEventEffect(event = Lifecycle.Event.ON_RESUME) {
        refreshState()
    }

    return state
}

@Composable
fun PermissionMissingView(
    isGranted: Boolean,
    shouldShowRationale: Boolean,
    permission: String,
    launchPermissionRequest: () -> Unit,
    content: @Composable () -> Unit,
) {
    if (isGranted) {
        content()
    } else if (shouldShowRationale) {
        PermissionRationaleDialog(
            text = stringResource(
                id = R.string.permission_info,
                permission,
            ),
            onConfirmButtonClick = launchPermissionRequest,
        )
    } else {
        PermissionDetailView(
            text = stringResource(
                id = R.string.permission_settings,
                permission,
            ),
        )
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
