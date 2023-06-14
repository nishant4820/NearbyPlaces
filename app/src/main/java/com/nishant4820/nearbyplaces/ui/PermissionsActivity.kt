package com.nishant4820.nearbyplaces.ui

import android.Manifest
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.DialogOnDeniedPermissionListener
import com.karumi.dexter.listener.single.PermissionListener
import com.nishant4820.nearbyplaces.R

class PermissionsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permissions)
        if (ActivityCompat.checkSelfPermission(
                this.applicationContext,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this.applicationContext,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startActivity(Intent(this@PermissionsActivity, MapsActivity::class.java))
            finish()
            return
        }
        findViewById<Button>(R.id.allowButton).setOnClickListener {
            requestPermissions()
        }
        findViewById<Button>(R.id.denyButton).setOnClickListener {
            AlertDialog.Builder(this@PermissionsActivity).apply {
                setTitle("Location permission")
                setMessage("Location permission is needed to search nearby point of interest.")
                setPositiveButton(android.R.string.ok) { _, _ ->
                    requestPermissions()
                }
                setNegativeButton(android.R.string.cancel) { _, _ ->
                    finish()
                }
            }.create().show()
        }
    }

    private fun requestPermissions() {
        Dexter.withContext(this)
            .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            .withListener(object : PermissionListener
            {
                override fun onPermissionGranted(response: PermissionGrantedResponse) {
                    startActivity(Intent(this@PermissionsActivity, MapsActivity::class.java))
                    finish()
                }

                override fun onPermissionDenied(response: PermissionDeniedResponse) {
                    if (response.isPermanentlyDenied) {
                        AlertDialog.Builder(this@PermissionsActivity).apply {
                            setTitle("Permission Denied")
                            setMessage("Permission to access device location is permanently denied. Please allow it from device settings.")
                            setPositiveButton("Go to settings") { dialog, which ->
                                startActivity( Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(
                                    Uri.fromParts("package", packageName, null)))

                            }
                            setNegativeButton("Cancel", null)
                        }.create().show()
                    } else {
                        Toast.makeText(this@PermissionsActivity, "Permission Denied", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    permission: PermissionRequest,
                    token: PermissionToken
                ) {
                    token.continuePermissionRequest()
                }
            }
            ).check()
    }
}