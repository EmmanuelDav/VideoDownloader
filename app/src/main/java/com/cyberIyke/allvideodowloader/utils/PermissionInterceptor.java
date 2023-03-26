package com.cyberIyke.allvideodowloader.utils;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.hjq.permissions.IPermissionInterceptor;
import com.hjq.permissions.OnPermissionCallback;
import com.hjq.permissions.OnPermissionPageCallback;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;
import com.cyberIyke.allvideodowloader.R;

import java.util.List;

public class PermissionInterceptor implements IPermissionInterceptor {

    @Override
    public void grantedPermissions(Activity activity, List<String> allPermissions, List<String> grantedPermissions,
                                   boolean all, OnPermissionCallback callback) {
        if (callback == null) {
            return;
        }
        callback.onGranted(grantedPermissions, all);
    }

    @Override
    public void deniedPermissions(Activity activity, List<String> allPermissions, List<String> deniedPermissions,
                                  boolean never, OnPermissionCallback callback) {
        if (callback != null) {
            callback.onDenied(deniedPermissions, never);
        }

        if (never) {
            if (deniedPermissions.size() == 1 && Permission.ACCESS_MEDIA_LOCATION.equals(deniedPermissions.get(0))) {
                //Toast.makeText(activity, R.string.common_permission_media_location_hint_fail, Toast.LENGTH_SHORT).show();
                return;
            }

            showPermissionSettingDialog(activity, allPermissions, deniedPermissions, callback);
            return;
        }

        if (deniedPermissions.size() == 1) {

            String deniedPermission = deniedPermissions.get(0);

            if (Permission.ACCESS_BACKGROUND_LOCATION.equals(deniedPermission)) {
                //Toast.makeText(activity, R.string.common_permission_background_location_fail_hint, Toast.LENGTH_SHORT).show();
                return;
            }

            if (Permission.BODY_SENSORS_BACKGROUND.equals(deniedPermission)) {
               // Toast.makeText(activity, R.string.common_permission_background_sensors_fail_hint, Toast.LENGTH_SHORT).show();
                return;
            }
        }

        final String message;
        List<String> permissionNames = PermissionNameConvert.permissionsToNames(activity, deniedPermissions);
        if (!permissionNames.isEmpty()) {
            message = activity.getString(R.string.common_permission_fail_assign_hint, PermissionNameConvert.listToString(permissionNames));
        } else {
            message = activity.getString(R.string.common_permission_fail_hint);
        }
        //Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
        Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_permission_deny);

        TextView txtCancel = dialog.findViewById(R.id.txtCancel);
        TextView txtSetting = dialog.findViewById(R.id.txtSetting);
        txtCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        txtSetting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", activity.getPackageName(), null);
                intent.setData(uri);
                activity.startActivity(intent);
            }
        });
        dialog.show();
        dialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    }


    private void showPermissionSettingDialog(Activity activity, List<String> allPermissions,
                                             List<String> deniedPermissions, OnPermissionCallback callback) {
        if (activity == null || activity.isFinishing() ||
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && activity.isDestroyed())) {
            return;
        }

        final String message;

        List<String> permissionNames = PermissionNameConvert.permissionsToNames(activity, deniedPermissions);
        if (!permissionNames.isEmpty()) {
            message = activity.getString(R.string.common_permission_manual_assign_fail_hint, PermissionNameConvert.listToString(permissionNames));
        } else {
            message = activity.getString(R.string.common_permission_manual_fail_hint);
        }

        new AlertDialog.Builder(activity)
                .setTitle(R.string.common_permission_alert)
                .setMessage(message)
                .setPositiveButton(R.string.common_permission_goto_setting_page, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        XXPermissions.startPermissionActivity(activity,
                                deniedPermissions, new OnPermissionPageCallback() {

                                    @Override
                                    public void onGranted() {
                                        if (callback == null) {
                                            return;
                                        }
                                        callback.onGranted(allPermissions, true);
                                    }

                                    @Override
                                    public void onDenied() {
                                        showPermissionSettingDialog(activity, allPermissions,
                                                XXPermissions.getDenied(activity, allPermissions), callback);
                                    }
                                });
                    }
                })
                .show();
    }
}
