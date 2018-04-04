package com.example.platformchannel

import android.app.Activity
import android.os.AsyncTask
import android.util.Log
import android.widget.Toast
import com.kontakt.sdk.android.ble.manager.listeners.simple.SimpleEddystoneListener
import com.kontakt.sdk.android.common.profile.IEddystoneDevice
import com.kontakt.sdk.android.common.profile.IEddystoneNamespace
import java.net.HttpURLConnection
import java.net.URL



class BeaconListener(val activity: MainActivity) : SimpleEddystoneListener() {

    private val ENDPOINT = "http://ec2-18-216-197-13.us-east-2.compute.amazonaws.com"

    private val USER_ROB = "6d318d24-6d8e-45c2-8e04-b1cb093a60e6"

    private val USER_MARK = "6d317d24-6d8e-42c2-8e05-b1cb093a60e6"

    private val USER_ID2 = "1d318d24-6d8e-45c2-8e04-b1cb093a60e6"

    private val USER_ID = USER_ROB

    override fun onEddystoneDiscovered(eddystone: IEddystoneDevice, namespace: IEddystoneNamespace?) {
        super.onEddystoneDiscovered(eddystone, namespace)
        onMessage("DISCOVER", eddystone)
    }

    override fun onEddystonesUpdated(eddystones: List<IEddystoneDevice>, namespace: IEddystoneNamespace) {
        eddystones.forEach { e ->
            onMessage("UPDATE", e)
        }
    }


    override fun onEddystoneLost(eddystone: IEddystoneDevice, namespace: IEddystoneNamespace?) {
        super.onEddystoneLost(eddystone, namespace)
        onMessage("LOSE", eddystone)
    }

    private fun say(msg: String) {
        Log.i("Eddy", msg)
//        activity.runOnUiThread { Toast.makeText(activity, msg, Toast.LENGTH_LONG).show() }
            activity.eventSink?.success(msg)

    }

    private fun onMessage(action: String, eddystone: IEddystoneDevice) {

            if (eddystone.instanceId == null) {
                say("Ignoring null $action")
                return
            }

            AsyncTask.execute {
                val payload: String = """
                {
                    "action": "$action",
                    "user_id": "$USER_ID",
                    "namespace": "${eddystone.namespace}",
                    "beacon_id": "${eddystone.instanceId}",
                    "distance": "${eddystone.distance}",
                    "rssi": "${eddystone.rssi}",
                    "url": "${eddystone.url}",
                    "txPower": "${eddystone.txPower}"
                }
                """

//                val endpoint = URL("$ENDPOINT/api/v1/beacon")
//                val connection = endpoint.openConnection() as HttpURLConnection
//                connection.setRequestMethod("POST")
//                connection.setRequestProperty("Content-Type", "application/json")
//                connection.setDoOutput(true)
//                connection.outputStream.write(payload.toByteArray())

//                if (connection.responseCode == 200) {
                    say(payload)
//                } else {
//                    say("$action failed (${connection.responseCode}): ${eddystone.key()}")
//                }

            }

    }

    fun IEddystoneDevice.key() = "${this.namespace}:${this.instanceId}"

    fun IEddystoneDevice.info() = "{ ${this.key()}, user_id=$USER_ID, distance=${this.distance}, rssi=${this.rssi} }"


}