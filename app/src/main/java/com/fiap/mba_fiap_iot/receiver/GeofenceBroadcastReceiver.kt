package com.fiap.mba_fiap_iot.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import com.google.gson.JsonObject
import com.pubnub.api.PNConfiguration
import com.pubnub.api.PubNub

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent.hasError()) {
            val errorMessage = GeofenceStatusCodes.getStatusCodeString(geofencingEvent.errorCode)
            return
        }

        val geofenceTransition = geofencingEvent.geofenceTransition

        val pnConfiguration = PNConfiguration()
        pnConfiguration.publishKey = "pub-c-2c9d5de0-71d1-4169-b64b-4f158b701211"
        pnConfiguration.subscribeKey = "sub-c-5c029dc6-1347-11eb-ae19-92aa6521e721"
        pnConfiguration.uuid = "mbaFiapMobile"

        val pubnub = PubNub(pnConfiguration)
        val entryUpdate = JsonObject()

        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            entryUpdate.addProperty("action", 0)
        } else if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
            entryUpdate.addProperty("action", 1)
        }

        pubnub.publish().channel("mbaFiapMobile").message(entryUpdate).async { result, status ->
            if (status.isError) {
                status.errorData.throwable.printStackTrace()
            } else {
                Log.e(
                    "[PUBLISH: sent]",
                    "timetoken: " + result?.timetoken)
            }
        }
    }

}