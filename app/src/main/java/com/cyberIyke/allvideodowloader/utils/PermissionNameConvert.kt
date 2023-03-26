package com.cyberIyke.allvideodowloader.utils

import android.content.Context
import android.os.Build
import com.cyberIyke.allvideodowloader.R
import com.hjq.permissions.Permission

object PermissionNameConvert {
    fun getPermissionString(context: Context?, permissions: List<String>?): String {
        return listToString(permissionsToNames(context, permissions))
    }

    fun listToString(hints: List<String?>?): String {
        if (hints == null || hints.isEmpty()) {
            return ""
        }
        val builder = StringBuilder()
        for (text in hints) {
            if (builder.length == 0) {
                builder.append(text)
            } else {
                builder.append("、")
                    .append(text)
            }
        }
        return builder.toString()
    }

    fun permissionsToNames(context: Context?, permissions: List<String>?): List<String?> {
        val permissionNames: MutableList<String?> = ArrayList()
        if (context == null) {
            return permissionNames
        }
        if (permissions == null) {
            return permissionNames
        }
        for (permission in permissions) {
            when (permission) {
                Permission.READ_EXTERNAL_STORAGE, Permission.WRITE_EXTERNAL_STORAGE -> {
                    val hint = context.getString(R.string.common_permission_storage)
                    if (!permissionNames.contains(hint)) {
                        permissionNames.add(hint)
                    }
                }
                Permission.READ_MEDIA_IMAGES, Permission.READ_MEDIA_VIDEO -> {}
                Permission.READ_MEDIA_AUDIO -> {}
                Permission.CAMERA -> {
                    val hint = context.getString(R.string.common_permission_camera)
                    if (!permissionNames.contains(hint)) {
                        permissionNames.add(hint)
                    }
                }
                Permission.RECORD_AUDIO -> {
                    val hint = context.getString(R.string.common_permission_microphone)
                    if (!permissionNames.contains(hint)) {
                        permissionNames.add(hint)
                    }
                }
                Permission.ACCESS_FINE_LOCATION, Permission.ACCESS_COARSE_LOCATION, Permission.ACCESS_BACKGROUND_LOCATION -> {
                    var hint: String
                    hint = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                        !permissions.contains(Permission.ACCESS_FINE_LOCATION) &&
                        !permissions.contains(Permission.ACCESS_COARSE_LOCATION)
                    ) {
                        context.getString(R.string.common_permission_location_background)
                    } else {
                        context.getString(R.string.common_permission_location)
                    }
                    if (!permissionNames.contains(hint)) {
                        permissionNames.add(hint)
                    }
                }
                Permission.BODY_SENSORS, Permission.BODY_SENSORS_BACKGROUND -> {
                    var hint: String
                    hint = context.getString(R.string.common_permission_sensors)
                    if (!permissionNames.contains(hint)) {
                        permissionNames.add(hint)
                    }
                }
                Permission.BLUETOOTH_SCAN, Permission.BLUETOOTH_CONNECT, Permission.BLUETOOTH_ADVERTISE -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val hint = context.getString(R.string.common_permission_wireless_devices)
                        if (!permissionNames.contains(hint)) {
                            permissionNames.add(hint)
                        }
                    }
                }
                Permission.NEARBY_WIFI_DEVICES -> {}
                Permission.READ_PHONE_STATE, Permission.CALL_PHONE, Permission.ADD_VOICEMAIL, Permission.USE_SIP, Permission.READ_PHONE_NUMBERS, Permission.ANSWER_PHONE_CALLS -> {
                    val hint = context.getString(R.string.common_permission_phone)
                    if (!permissionNames.contains(hint)) {
                        permissionNames.add(hint)
                    }
                }
                Permission.GET_ACCOUNTS, Permission.READ_CONTACTS, Permission.WRITE_CONTACTS -> {
                    val hint = context.getString(R.string.common_permission_contacts)
                    if (!permissionNames.contains(hint)) {
                        permissionNames.add(hint)
                    }
                }
                Permission.READ_CALENDAR, Permission.WRITE_CALENDAR -> {
                    val hint = context.getString(R.string.common_permission_calendar)
                    if (!permissionNames.contains(hint)) {
                        permissionNames.add(hint)
                    }
                }
                Permission.READ_CALL_LOG, Permission.WRITE_CALL_LOG, Permission.PROCESS_OUTGOING_CALLS -> {
                    val hint =
                        context.getString(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) R.string.common_permission_call_log else R.string.common_permission_phone)
                    if (!permissionNames.contains(hint)) {
                        permissionNames.add(hint)
                    }
                }
                Permission.ACTIVITY_RECOGNITION -> {
                    val hint =
                        context.getString(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) R.string.common_permission_activity_recognition_30 else R.string.common_permission_activity_recognition_29)
                    if (!permissionNames.contains(hint)) {
                        permissionNames.add(hint)
                    }
                }
                Permission.ACCESS_MEDIA_LOCATION -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val hint = context.getString(R.string.common_permission_media_location)
                        if (!permissionNames.contains(hint)) {
                            permissionNames.add(hint)
                        }
                    }
                }
                Permission.SEND_SMS, Permission.RECEIVE_SMS, Permission.READ_SMS, Permission.RECEIVE_WAP_PUSH, Permission.RECEIVE_MMS -> {
                    val hint = context.getString(R.string.common_permission_sms)
                    if (!permissionNames.contains(hint)) {
                        permissionNames.add(hint)
                    }
                }
                Permission.MANAGE_EXTERNAL_STORAGE -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        val hint = context.getString(R.string.common_permission_manage_storage)
                        if (!permissionNames.contains(hint)) {
                            permissionNames.add(hint)
                        }
                    }
                }
                Permission.REQUEST_INSTALL_PACKAGES -> {
                    val hint = context.getString(R.string.common_permission_install)
                    if (!permissionNames.contains(hint)) {
                        permissionNames.add(hint)
                    }
                }
                Permission.SYSTEM_ALERT_WINDOW -> {
                    val hint = context.getString(R.string.common_permission_window)
                    if (!permissionNames.contains(hint)) {
                        permissionNames.add(hint)
                    }
                }
                Permission.WRITE_SETTINGS -> {
                    val hint = context.getString(R.string.common_permission_setting)
                    if (!permissionNames.contains(hint)) {
                        permissionNames.add(hint)
                    }
                }
                Permission.NOTIFICATION_SERVICE -> {
                    val hint = context.getString(R.string.common_permission_notification)
                    if (!permissionNames.contains(hint)) {
                        permissionNames.add(hint)
                    }
                }
                Permission.POST_NOTIFICATIONS -> {}
                Permission.BIND_NOTIFICATION_LISTENER_SERVICE -> {
                    val hint = context.getString(R.string.common_permission_notification_listener)
                    if (!permissionNames.contains(hint)) {
                        permissionNames.add(hint)
                    }
                }
                Permission.PACKAGE_USAGE_STATS -> {
                    val hint = context.getString(R.string.common_permission_task)
                    if (!permissionNames.contains(hint)) {
                        permissionNames.add(hint)
                    }
                }
                Permission.SCHEDULE_EXACT_ALARM -> {
                    val hint = context.getString(R.string.common_permission_alarm)
                    if (!permissionNames.contains(hint)) {
                        permissionNames.add(hint)
                    }
                }
                Permission.ACCESS_NOTIFICATION_POLICY -> {
                    val hint = context.getString(R.string.common_permission_not_disturb)
                    if (!permissionNames.contains(hint)) {
                        permissionNames.add(hint)
                    }
                }
                Permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS -> {
                    val hint = context.getString(R.string.common_permission_ignore_battery)
                    if (!permissionNames.contains(hint)) {
                        permissionNames.add(hint)
                    }
                }
                Permission.BIND_VPN_SERVICE -> {
                    val hint = context.getString(R.string.common_permission_vpn)
                    if (!permissionNames.contains(hint)) {
                        permissionNames.add(hint)
                    }
                }
                Permission.PICTURE_IN_PICTURE -> {
                    val hint = context.getString(R.string.common_permission_picture_in_picture)
                    if (!permissionNames.contains(hint)) {
                        permissionNames.add(hint)
                    }
                }
                else -> {}
            }
        }
        return permissionNames
    }
}