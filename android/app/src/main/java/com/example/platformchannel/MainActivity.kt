// Copyright 2017 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.example.platformchannel

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.widget.Toast
import com.kontakt.sdk.android.ble.configuration.ScanMode
import com.kontakt.sdk.android.ble.configuration.ScanPeriod
import com.kontakt.sdk.android.ble.manager.ProximityManager
import com.kontakt.sdk.android.ble.manager.ProximityManagerFactory
import com.kontakt.sdk.android.ble.spec.EddystoneFrameType
import com.kontakt.sdk.android.common.KontaktSDK

import io.flutter.app.FlutterActivity
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.EventChannel.EventSink
import io.flutter.plugin.common.EventChannel.StreamHandler
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.MethodCall
import io.flutter.plugins.GeneratedPluginRegistrant
import java.util.*

const private val API_KEY = "ZEegDVjyFnAnInAZYsvZTyjiLHPzltYk"

class MainActivity : FlutterActivity() {

    private var proximityManager: ProximityManager? = null
    public var eventSink: EventSink? = null

    private val batteryLevel: Int
        get() {
            if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
                val batteryManager = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
                return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            } else {
                val intent = ContextWrapper(applicationContext).registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                return intent!!.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) * 100 / intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            }
        }

    override fun onStart() {
        super.onStart()
        startScanning()
    }

    override fun onStop() {
        proximityManager!!.stopScanning()
        super.onStop()
    }

    override fun onDestroy() {
        proximityManager!!.disconnect()
        proximityManager = null
        super.onDestroy()
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestPermissionsIfNecessary()
        initializeBeaconListener()
        GeneratedPluginRegistrant.registerWith(this)
        EventChannel(flutterView, CHARGING_CHANNEL).setStreamHandler(
                object : StreamHandler {
                    private var chargingStateChangeReceiver: BroadcastReceiver? = null
                    override fun onListen(arguments: Any?, events: EventSink?) {
                        eventSink = events // save it for BeaconListener to use
//                        chargingStateChangeReceiver = createChargingStateChangeReceiver(events)
//                        registerReceiver(
//                                chargingStateChangeReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                    }

                    override fun onCancel(arguments: Any?) {
//                        unregisterReceiver(chargingStateChangeReceiver)
//                        chargingStateChangeReceiver = null
                    }
                }
        )

        MethodChannel(flutterView, BATTERY_CHANNEL).setMethodCallHandler { call, result ->
            if (call.method == "getBatteryLevel") {
                val batteryLevel = batteryLevel

                if (batteryLevel != -1) {
                    result.success(batteryLevel)
                } else {
                    result.error("UNAVAILABLE", "Battery level not available.", null)
                }
            } else {
                result.notImplemented()
            }
        }
    }

    private fun createChargingStateChangeReceiver(events: EventSink?): BroadcastReceiver {
        return object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)

                if (status == BatteryManager.BATTERY_STATUS_UNKNOWN) {
//                    events!!.error("UNAVAILABLE", "Charging status unavailable", null)
                } else {
                    val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
//                    events!!.success(if (isCharging) "charging" else "discharging")
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED)) {
            runOnUiThread { Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show() }
        } else {
            runOnUiThread { Toast.makeText(this, "Fail Whale - permission not granted", Toast.LENGTH_SHORT).show() }
        }
    }

    private fun startScanning(): Unit {
        proximityManager = ProximityManagerFactory.create(this)
        proximityManager!!.configuration()
                .scanMode(ScanMode.LOW_LATENCY)
                .scanPeriod(ScanPeriod.RANGING)
                .eddystoneFrameTypes(Arrays.asList(EddystoneFrameType.UID))

        proximityManager!!.setEddystoneListener(BeaconListener(this))
        proximityManager!!.connect { proximityManager!!.startScanning() }
    }

    private fun requestPermissionsIfNecessary(): Unit {
        val needCoarseLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        val needBluetoothPrivileged = ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_PRIVILEGED) != PackageManager.PERMISSION_GRANTED

        if (needCoarseLocation || needBluetoothPrivileged) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH_PRIVILEGED), 0)
        }
    }

    private fun initializeBeaconListener() {
        KontaktSDK.initialize(API_KEY)
        proximityManager = ProximityManagerFactory.create(this)
        proximityManager!!.configuration()
                .scanMode(ScanMode.LOW_LATENCY)
                .scanPeriod(ScanPeriod.RANGING)
                .eddystoneFrameTypes(Arrays.asList(EddystoneFrameType.UID))

        proximityManager!!.setEddystoneListener(BeaconListener(this))
    }

    companion object {
        private val BATTERY_CHANNEL = "samples.flutter.io/battery"
        private val CHARGING_CHANNEL = "samples.flutter.io/charging"
    }
}
