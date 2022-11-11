package io.github.thibaultbee.streampack.example.utils

import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts

class PermissionsManager(
    private val activity: ComponentActivity,
    private val requiredPermissions: List<String>,
    private val onAllGranted: () -> Unit,
    private val onShowPermissionRationale: (List<String>) -> Boolean,
    private val onDenied: (List<String>) -> Unit
) {
    private fun hasPermissions(): Boolean {
        return requiredPermissions.all {
            activity.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun requestPermissions() {
        if (hasPermissions()) {
            onAllGranted()
        } else {
            var hasShownRationale = false
            requiredPermissions.forEach {
                if (activity.shouldShowRequestPermissionRationale(it)) {
                    hasShownRationale = true
                    // Last chance to show rationale
                    if (onShowPermissionRationale(requiredPermissions)) {
                        requestMultiplePermissions.launch(arrayOf(it))
                    }
                }
            }
            if (!hasShownRationale) {
                requestMultiplePermissions.launch(requiredPermissions.toTypedArray())
            }
        }
    }

    private val requestMultiplePermissions =
        activity.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            permissions.filter { !it.value }.keys.toList().let {
                if (it.isNotEmpty()) {
                    onDenied(it)
                } else {
                    onAllGranted()
                }
            }
        }
}