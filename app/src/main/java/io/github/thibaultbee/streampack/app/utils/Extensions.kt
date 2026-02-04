package io.github.thibaultbee.streampack.app.utils

import android.content.Context
import android.content.DialogInterface
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog

fun Context.toast(message: String) =
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

fun Context.showDialog(
    title: String,
    message: String = "",
    @StringRes
    positiveButtonText: Int = android.R.string.ok,
    @StringRes
    negativeButtonText: Int = android.R.string.cancel,
    onPositiveButtonClick: () -> Unit = {},
    onNegativeButtonClick: () -> Unit = {}
) {
    AlertDialog.Builder(this)
        .setTitle(title)
        .setMessage(message)
        .apply {
            if (positiveButtonText != 0) {
                setPositiveButton(positiveButtonText) { dialogInterface: DialogInterface, _: Int ->
                    dialogInterface.dismiss()
                    onPositiveButtonClick()
                }
            }
            if (negativeButtonText != 0) {
                setNegativeButton(negativeButtonText) { dialogInterface: DialogInterface, _: Int ->
                    dialogInterface.dismiss()
                    onNegativeButtonClick()
                }
            }
        }
        .show()
}