package com.cyberIyke.allvideodowloader.helper

import android.app.Activity
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.cyberIyke.allvideodowloader.activities.MainActivity

abstract class PermissionsManager protected constructor(private val activity: Activity) :
    ActivityCompat.OnRequestPermissionsResultCallback {
    private var grantedPermissions: Boolean = false
    private lateinit var permissions: Array<String>
    private var requestCode: Int = 0

    init {
        (activity as MainActivity).setOnRequestPermissionsResultListener(this)
    }

    private fun notGrantedPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            activity,
            permission
        ) != PackageManager.PERMISSION_GRANTED
    }

    fun checkPermissions(permission: String, requestCode: Int) {
        checkPermissions(arrayOf(permission), requestCode)
    }

    fun checkPermissions(permissions: Array<String>, requestCode: Int) {
        this.permissions = permissions
        this.requestCode = requestCode
        for (permission: String in permissions) {
            if (notGrantedPermission(permission)) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                    showRequestPermissionRationale()
                } else {
                    requestPermissions()
                }
                break
            } else grantedPermissions = true
        }
        if (grantedPermissions) onPermissionsGranted()
    }

    fun requestPermissions() {
        ActivityCompat.requestPermissions(activity, permissions, requestCode)
    }

    public override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        for (i in permissions.indices) {
            if (grantResults.get(i) != PackageManager.PERMISSION_GRANTED) {
                if (!ActivityCompat.shouldShowRequestPermissionRationale(
                        activity,
                        permissions.get(i)
                    )
                ) {
                    grantedPermissions = false
                    requestDisallowedAction()
                } else {
                    grantedPermissions = false
                    onPermissionsDenied()
                }
                break
            } else grantedPermissions = true
        }
        if (grantedPermissions) onPermissionsGranted()
    }

    /**
     * add code here to tell users what permissions you need granted and why you need each
     * permission. Should call requestPermissions() after showing rationale.
     */
    abstract fun showRequestPermissionRationale()

    /**
     * add code here when permissions can't be requested. Either disable feature, direct user to
     * settings to allow user to set permissions, ask user to uninstall, or etc.
     */
    abstract fun requestDisallowedAction()
    abstract fun onPermissionsGranted()
    abstract fun onPermissionsDenied()
}