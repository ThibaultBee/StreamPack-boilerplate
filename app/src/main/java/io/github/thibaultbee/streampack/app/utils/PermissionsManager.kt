package io.github.thibaultbee.streampack.app.utils

import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts

class PermissionsManager(
    private val activity: ComponentActivity,
    private val requiredPermissions: List<String>,
    private val onAllGranted: () -> Unit,
    private val onShowPermissionRationale: (List<String>, () -> Unit) -> Unit,
    private val onDenied: (List<String>) -> Unit
) {
    private fun hasAllPermissions(): Boolean {
        return requiredPermissions.all {
            activity.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun hasShownAllRationales(): Boolean {
        return requiredPermissions.all {
            !activity.shouldShowRequestPermissionRationale(it)
        }
    }

    fun requestPermissions() {
        if (hasAllPermissions()) {
            onAllGranted()
        } else {
            // Either we ask for rationale or we ask for all permissions
            if (requestRationales()) {
                requestMultiplePermissions.launch(requiredPermissions.toTypedArray())
            }
        }
    }

    private fun requestRationales(): Boolean {
        // List of permissions that have been denied once. In this case, we should explain why we need them.
        val rationalesPermission = mutableListOf<String>()
        requiredPermissions.filter {
            activity.shouldShowRequestPermissionRationale(it)
        }.forEach {
            rationalesPermission.add(it)
        }
        if (rationalesPermission.isNotEmpty()) {
            onShowPermissionRationale(rationalesPermission) {
                requestMultiplePermissions.launch(rationalesPermission.toTypedArray())
            }
        }

        return rationalesPermission.isEmpty()
    }

    private val requestMultiplePermissions =
        activity.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val deniedPermissions = permissions.filter { !it.value }.keys.toList()
            if (deniedPermissions.isNotEmpty()) {
                if (!hasShownAllRationales()) {
                    requestRationales()
                } else {
                    onDenied(deniedPermissions)
                }
            } else {
                onAllGranted()
            }
        }
}