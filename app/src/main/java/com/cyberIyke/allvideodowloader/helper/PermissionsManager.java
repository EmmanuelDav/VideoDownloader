package com.cyberIyke.allvideodowloader.helper;

import android.app.Activity;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.cyberIyke.allvideodowloader.activities.MainActivity;

public abstract class PermissionsManager implements ActivityCompat.OnRequestPermissionsResultCallback {
    private Activity activity;
    private boolean grantedPermissions;
    private String[] permissions;
    private int requestCode;

    protected PermissionsManager(Activity activity) {
        this.activity = activity;
        ((MainActivity) activity).setOnRequestPermissionsResultListener(this);
    }

    private boolean notGrantedPermission(String permission) {
        return ContextCompat.checkSelfPermission(activity, permission) != PackageManager
                .PERMISSION_GRANTED;
    }

    public void checkPermissions(String permission, int requestCode) {
        checkPermissions(new String[]{permission}, requestCode);
    }

    public void checkPermissions(String[] permissions, int requestCode) {
        this.permissions = permissions;
        this.requestCode = requestCode;
        for (String permission : permissions) {
            if (notGrantedPermission(permission)) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                    showRequestPermissionRationale();
                } else {
                    requestPermissions();
                }
                break;
            } else grantedPermissions = true;
        }
        if (grantedPermissions) onPermissionsGranted();
    }

    public void requestPermissions() {
        ActivityCompat.requestPermissions(activity, permissions, requestCode);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,  int[] grantResults) {
        for (int i = 0; i < permissions.length; i++) {
            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                if (!ActivityCompat.shouldShowRequestPermissionRationale(activity,
                        permissions[i])) {
                    grantedPermissions = false;
                    requestDisallowedAction();
                } else {
                    grantedPermissions = false;
                    onPermissionsDenied();
                }
                break;
            } else grantedPermissions = true;
        }
        if (grantedPermissions) onPermissionsGranted();
    }

    /**
     * add code here to tell users what permissions you need granted and why you need each
     * permission. Should call requestPermissions() after showing rationale.
     */
    public abstract void showRequestPermissionRationale();

    /**
     * add code here when permissions can't be requested. Either disable feature, direct user to
     * settings to allow user to set permissions, ask user to uninstall, or etc.
     */
    public abstract void requestDisallowedAction();

    public abstract void onPermissionsGranted();

    public abstract void onPermissionsDenied();
}
