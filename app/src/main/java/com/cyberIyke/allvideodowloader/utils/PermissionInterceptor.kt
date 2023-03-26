package com.cyberIyke.allvideodowloader.utils

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.Window
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.cyberIyke.allvideodowloader.R
import com.hjq.permissions.*

class PermissionInterceptor : IPermissionInterceptor {
    override fun grantedPermissions(
        activity: Activity, allPermissions: List<String>, grantedPermissions: List<String>,
        all: Boolean, callback: OnPermissionCallback
    ) {
        if (callback == null) {
            return
        }
        callback.onGranted(grantedPermissions, all)
    }

    override fun deniedPermissions(
        activity: Activity, allPermissions: List<String>, deniedPermissions: List<String>,
        never: Boolean, callback: OnPermissionCallback
    ) {
        if (callback != null) {
            callback.onDenied(deniedPermissions, never)
        }
        if (never) {
            if (deniedPermissions.size == 1 && Permission.ACCESS_MEDIA_LOCATION == deniedPermissions[0]) {
                //Toast.makeText(activity, R.string.common_permission_media_location_hint_fail, Toast.LENGTH_SHORT).show();
                return
            }
            showPermissionSettingDialog(activity, allPermissions, deniedPermissions, callback)
            return
        }
        if (deniedPermissions.size == 1) {
            val deniedPermission = deniedPermissions[0]
            if (Permission.ACCESS_BACKGROUND_LOCATION == deniedPermission) {
                //Toast.makeText(activity, R.string.common_permission_background_location_fail_hint, Toast.LENGTH_SHORT).show();
                return
            }
            if (Permission.BODY_SENSORS_BACKGROUND == deniedPermission) {
                // Toast.makeText(activity, R.string.common_permission_background_sensors_fail_hint, Toast.LENGTH_SHORT).show();
                return
            }
        }
        val message: String
        val permissionNames = PermissionNameConvert.permissionsToNames(activity, deniedPermissions)
        message = if (!permissionNames.isEmpty()) {
            activity.getString(
                R.string.common_permission_fail_assign_hint,
                PermissionNameConvert.listToString(permissionNames)
            )
        } else {
            activity.getString(R.string.common_permission_fail_hint)
        }
        //Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
        val dialog = Dialog(activity)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_permission_deny)
        val txtCancel = dialog.findViewById<TextView>(R.id.txtCancel)
        val txtSetting = dialog.findViewById<TextView>(R.id.txtSetting)
        txtCancel.setOnClickListener { dialog.dismiss() }
        txtSetting.setOnClickListener {
            dialog.dismiss()
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            val uri = Uri.fromParts("package", activity.packageName, null)
            intent.data = uri
            activity.startActivity(intent)
        }
        dialog.show()
        dialog.window!!.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT
        )
        dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }

    private fun showPermissionSettingDialog(
        activity: Activity?, allPermissions: List<String>,
        deniedPermissions: List<String>, callback: OnPermissionCallback?
    ) {
        if (activity == null || activity.isFinishing ||
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && activity.isDestroyed
        ) {
            return
        }
        val message: String
        val permissionNames = PermissionNameConvert.permissionsToNames(activity, deniedPermissions)
        message = if (!permissionNames.isEmpty()) {
            activity.getString(
                R.string.common_permission_manual_assign_fail_hint,
                PermissionNameConvert.listToString(permissionNames)
            )
        } else {
            activity.getString(R.string.common_permission_manual_fail_hint)
        }
        AlertDialog.Builder(activity)
            .setTitle(R.string.common_permission_alert)
            .setMessage(message)
            .setPositiveButton(R.string.common_permission_goto_setting_page) { dialog, which ->
                dialog.dismiss()
                XXPermissions.startPermissionActivity(activity,
                    deniedPermissions, object : OnPermissionPageCallback {
                        override fun onGranted() {
                            if (callback == null) {
                                return
                            }
                            callback.onGranted(allPermissions, true)
                        }

                        override fun onDenied() {
                            showPermissionSettingDialog(
                                activity, allPermissions,
                                XXPermissions.getDenied(activity, allPermissions), callback
                            )
                        }
                    })
            }
            .show()
    }
}