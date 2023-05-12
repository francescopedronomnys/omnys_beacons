//  Copyright (c) 2018 Loup Inc.
//  Licensed under Apache License v2.0

package com.omnys.ble.beacons.omnys_beacons.logic

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.omnys.ble.beacons.omnys_beacons.OmnysBeaconsPlugin
import com.omnys.ble.beacons.omnys_beacons.data.Permission
import com.omnys.ble.beacons.omnys_beacons.data.Result
import io.flutter.plugin.common.PluginRegistry
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class PermissionClient {

    val listener: PluginRegistry.RequestPermissionsResultListener =
        PluginRegistry.RequestPermissionsResultListener { id, _, grantResults ->
            if (id == OmnysBeaconsPlugin.Intents.PermissionRequestId) {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    permissionCallbacks.forEach { it.success(Unit) }
                } else {
                    permissionCallbacks.forEach { it.failure(Unit) }
                }
                permissionCallbacks.clear()
                return@RequestPermissionsResultListener true
            }

            return@RequestPermissionsResultListener false
        }

    private var activity: Activity? = null
    private val permissionCallbacks = ArrayList<Callback<Unit, Unit>>()


    fun bind(activity: Activity) {
        this.activity = activity
    }

    fun unbind() {
        activity = null
    }

    fun check(permission: Permission): PermissionResult {
        if (!checkDeclaration(permission)) {
            return PermissionResult.MissingDeclaration
        }

        if (!checkGranted()) {
            return PermissionResult.Denied
        }

        return PermissionResult.Granted
    }

    suspend fun request(permission: Permission): PermissionResult = suspendCoroutine { cont ->
        val current = check(permission)
        when (current) {
            is PermissionResult.MissingDeclaration,
            is PermissionResult.Granted -> cont.resume(current)

            is PermissionResult.Denied -> {
                val callback = Callback<Unit, Unit>(
                    success = { _ -> cont.resume(PermissionResult.Granted) },
                    failure = { _ -> cont.resume(PermissionResult.Denied) }
                )
                permissionCallbacks.add(callback)
                ActivityCompat.requestPermissions(
                    activity!!,
                    arrayOf(permission.manifestValue),
                    OmnysBeaconsPlugin.Intents.PermissionRequestId
                )
            }
        }

    }

    // Internals

    private fun checkDeclaration(permission: Permission): Boolean {
        val permissions = activity?.packageManager?.getPackageInfo(
            activity!!.packageName,
            PackageManager.GET_PERMISSIONS
        )?.requestedPermissions ?: arrayOf<String>()

        return when {
            permissions.count { it == Manifest.permission.ACCESS_FINE_LOCATION } > 0 -> true
            permissions.count { it == Manifest.permission.ACCESS_COARSE_LOCATION } > 0 && permission == Permission.Coarse -> true
            else -> false
        }
    }

    private fun checkGranted() =
        ContextCompat.checkSelfPermission(
            activity!!,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    activity!!,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED


    class Callback<in T, in E>(val success: (T) -> Unit, val failure: (E) -> Unit)

    sealed class PermissionResult(val result: Result) {
        object MissingDeclaration : PermissionResult(
            Result.failure(
                Result.Error.Type.Runtime,
                message = "Missing location permission in AndroidManifest.xml. You need to addBackgroundListener one of ACCESS_FINE_LOCATION or ACCESS_COARSE_LOCATION. See readme for details.",
                fatal = true
            )
        )

        object Denied : PermissionResult(Result.failure(Result.Error.Type.PermissionDenied))
        object Granted : PermissionResult(Result.success(true))
    }
}