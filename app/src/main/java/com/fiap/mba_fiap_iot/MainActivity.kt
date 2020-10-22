package com.fiap.mba_fiap_iot

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fiap.mba_fiap_iot.adapter.AdapterSensors
import com.fiap.mba_fiap_iot.receiver.GeofenceBroadcastReceiver
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.gson.JsonObject
import com.pubnub.api.PNConfiguration
import com.pubnub.api.PubNub
import com.pubnub.api.callbacks.SubscribeCallback
import com.pubnub.api.models.consumer.PNStatus
import com.pubnub.api.models.consumer.objects_api.channel.PNChannelMetadataResult
import com.pubnub.api.models.consumer.objects_api.membership.PNMembershipResult
import com.pubnub.api.models.consumer.objects_api.uuid.PNUUIDMetadataResult
import com.pubnub.api.models.consumer.pubsub.PNMessageResult
import com.pubnub.api.models.consumer.pubsub.PNPresenceEventResult
import com.pubnub.api.models.consumer.pubsub.PNSignalResult
import com.pubnub.api.models.consumer.pubsub.files.PNFileEventResult
import com.pubnub.api.models.consumer.pubsub.message_actions.PNMessageActionResult
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*


class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager

    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager

    private var sensor_reading = false
    private var light_turned = false
    private var guardian_on = false

    private lateinit var pubnub: PubNub

    lateinit var geofencingClient: GeofencingClient

    lateinit var databaseReference: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setSupportActionBar(toolbar)

        configurePubnub()

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val deviceSensors: List<Sensor> = sensorManager.getSensorList(Sensor.TYPE_ALL)

        viewManager = LinearLayoutManager(this)
        viewAdapter = AdapterSensors(deviceSensors)

        recyclerView = findViewById<RecyclerView>(R.id.my_recycler_view).apply {
            setHasFixedSize(true)

            layoutManager = viewManager

            adapter = viewAdapter
        }

        geofencingClient = LocationServices.getGeofencingClient(this)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Log.e("geofence", "size ${getGeofencingRequest().geofences.size}")

            geofencingClient?.addGeofences(getGeofencingRequest(), geofencePendingIntent)?.run {
                addOnSuccessListener {
                    Log.e("geofence", "geofence success")
                }
                addOnFailureListener {
                    Log.e("geofence", "geofence falha")
                }
            }
        }

        val database: FirebaseDatabase = FirebaseDatabase.getInstance()
        databaseReference = database.getReference("guardian")
    }

    fun createGeofence(): Geofence = Geofence.Builder()
                .setRequestId("idCasaGeofence")
                .setCircularRegion(
                    -28.263642,
                    -52.397123,
                    400f
                )
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
                .build()

    private fun getGeofencingRequest(): GeofencingRequest {
        return GeofencingRequest.Builder().apply {
            setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_EXIT)
            addGeofence(createGeofence())
        }.build()
    }

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(this, GeofenceBroadcastReceiver::class.java)
        PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    fun configurePubnub(){
        val pnConfiguration = PNConfiguration()
        pnConfiguration.publishKey = "pub-c-2c9d5de0-71d1-4169-b64b-4f158b701211"
        pnConfiguration.subscribeKey = "sub-c-5c029dc6-1347-11eb-ae19-92aa6521e721"
        pnConfiguration.uuid = "mbaFiapMobile"

        pubnub = PubNub(pnConfiguration)

        pubnub.addListener(object : SubscribeCallback() {
            override fun message(pubnub: PubNub, event: PNMessageResult) {
                val message = event.message.asJsonObject
                Log.e("[MESSAGE: received]", "$message")
            }

            override fun status(pubnub: PubNub, event: PNStatus) {}
            override fun uuid(pubnub: PubNub, pnUUIDMetadataResult: PNUUIDMetadataResult) {}
            override fun presence(pubnub: PubNub, event: PNPresenceEventResult) {}
            override fun channel(pubnub: PubNub, pnChannelMetadataResult: PNChannelMetadataResult) {}
            override fun signal(pubnub: PubNub, event: PNSignalResult) {}
            override fun membership(pubnub: PubNub, event: PNMembershipResult) {}
            override fun file(pubnub: PubNub, pnFileEventResult: PNFileEventResult) {}
            override fun messageAction(pubnub: PubNub, event: PNMessageActionResult) {}
        })

        pubnub.subscribe().channels(Arrays.asList("mbaFiapMobile")).withPresence().execute()

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.action_rotate_sensor -> {
            sensor_reading = !sensor_reading
            turnOnOffRotate(sensor_reading)
            item.title = when (sensor_reading) {
                false -> getString(R.string.sensor_rotate_off)
                true -> getString(R.string.sensor_rotate_on)
            }
            true
        }
        R.id.action_guardian -> {
            guardian_on = !guardian_on
            guardianOnOff()
            item.title = when (guardian_on) {
                false -> getString(R.string.guardian_off)
                true -> getString(R.string.guardian_on)
            }
            true
        }
        else -> {
            super.onOptionsItemSelected(item)
        }
    }

    fun guardianOnOff(){
        databaseReference.setValue(
            guardian_on,
            DatabaseReference.CompletionListener { error, ref ->
                Log.e("firebase", "$error")
            }
        )
    }

    fun turnOnOffRotate(state: Boolean){
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

        if (state) {
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        } else {
            sensorManager.unregisterListener(this, sensor)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onSensorChanged(sensorEvent: SensorEvent?) {
        sensorEvent?.let { event ->
            if (event.values[0] > 5){
                if (!light_turned){
                    light_turned = true;
                    publishToPubnub(1)
                }
            } else {
                if (light_turned){
                    light_turned = false;
                    publishToPubnub(0)
                }
            }
        }
    }

    fun publishToPubnub(action: Int){
        val entryUpdate = JsonObject()
        entryUpdate.addProperty("action", action)

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