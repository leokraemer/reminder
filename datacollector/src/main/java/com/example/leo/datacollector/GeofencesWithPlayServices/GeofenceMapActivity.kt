package com.example.leo.datacollector.GeofencesWithPlayServices

import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.preference.PreferenceManager
import android.text.Editable
import android.text.TextWatcher
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.Toast
import com.example.leo.datacollector.DailyRoutines.RoutineProvider
import com.example.leo.datacollector.GeofencesWithPlayServices.Constants.GEOFENCE_EXPIRATION_IN_MILLISECONDS
import com.example.leo.datacollector.R
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.maps.android.ui.IconGenerator
import kotlinx.android.synthetic.main.geofence_activity_main.*
import kotlinx.android.synthetic.main.geofence_activity_main.view.*


class GeofenceMapActivity : MainActivity(), OnMapReadyCallback, GoogleMap.OnMapLongClickListener {
    private lateinit var mMap: GoogleMap
    private var enter = true;
    private var exit = false;
    private var dwell = false;
    private var geofenceName = "name"
        set(value) {
            if (field != value) {
                field = value
                updateGeofenceOnMap(latLng!!, geofenceName, geofencesize.toFloat())
            }
        }
    private var rp: RoutineProvider? = null
    private var fenceMarker: Marker? = null
        set(value) {
            if (field != null)
                field!!.remove()
            field = value
        }
    private var fenceCircle: Circle? = null
        set(value) {
            if (field != null)
                field!!.remove()
            field = value
        }
    private var latLng: LatLng? = null

    private var geofencesize: Int = 0
        get() = field + 100
        set(value) {
            if (value != field) {
                field = value
                updateSizeText(geofencesize.toFloat())
                if (latLng != null)
                    updateGeofenceOnMap(latLng!!, geofenceName, geofencesize.toFloat())
                sizeBar.setProgress(field)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.geofence_activity_main)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        rp = RoutineProvider(this)
        val sizeChangeListener = object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                geofencesize = progress
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        }
        sizeBar.setOnSeekBarChangeListener(sizeChangeListener)

        done.setOnClickListener {
            commitGeofence()
        }
        geofenceoptions.enter.setOnCheckedChangeListener { _, checked ->
            enter = checked
        }
        geofenceoptions.exit.setOnCheckedChangeListener { _, checked ->
            exit = checked
        }
        geofenceoptions.dwell.setOnCheckedChangeListener { _, checked ->
            dwell = checked
        }
        geofenceoptions.nameField.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                geofenceName = s.toString()
            }
        })


    }

    private fun commitGeofence() {
        if (latLng != null)
            if (enter or exit or dwell) {
                addGeofence(geofenceName, latLng!!, geofencesize.toFloat(), enter, exit, dwell)
                val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
                val editor = sharedPreferences.edit()
                editor.putString(GEOFENCE_NAME, geofenceName)
                editor.putFloat(GEOFENCE_LAT, latLng!!.latitude.toFloat())
                editor.putFloat(GEOFENCE_LONG, latLng!!.longitude.toFloat())
                editor.putInt(GEOFENCE_RADIUS, geofencesize)
                editor.putLong(GEOFENCE_DATE_ADDED, System.currentTimeMillis())
                editor.putLong(GEOFENCE_VALIDITY, GEOFENCE_EXPIRATION_IN_MILLISECONDS)
                editor.putBoolean(GEOFENCE_DWELL, dwell)
                editor.putBoolean(GEOFENCE_ENTER, enter)
                editor.putBoolean(GEOFENCE_EXIT, exit)
                editor.apply()
                updateGeofenceOnMap(latLng!!, geofenceName, geofencesize.toFloat())
            } else {
                Toast.makeText(this, "activate one of the transition types", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateSizeText(size: Float) {
        sizeText.setText("${size} m")
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @SuppressWarnings("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.setOnMapLongClickListener(this)
        mMap.isMyLocationEnabled = true
        val sp = PreferenceManager.getDefaultSharedPreferences(this)
        loadGeofenceFromSharedPreferences(sp)
    }

    private fun loadGeofenceFromSharedPreferences(sp: SharedPreferences) {
        val timestamp = sp.getLong(GEOFENCE_DATE_ADDED, 0)
        val validity = sp.getLong(GEOFENCE_VALIDITY, 0)
        if (System.currentTimeMillis() < timestamp + validity) {
            val fence_lat = sp.getFloat(GEOFENCE_LAT, 0f)
            val fence_long = sp.getFloat(GEOFENCE_LONG, 0f)
            geofenceoptions.enter.setChecked(sp.getBoolean(GEOFENCE_ENTER, false))
            geofenceoptions.exit.setChecked(sp.getBoolean(GEOFENCE_EXIT, false))
            geofenceoptions.dwell.setChecked(sp.getBoolean(GEOFENCE_DWELL, false))
            latLng = LatLng(fence_lat.toDouble(), fence_long.toDouble())
            geofencesize = sp.getInt(GEOFENCE_RADIUS, 0)
            geofenceName = sp.getString(GEOFENCE_NAME, null)
        }
    }

    override fun onMapLongClick(latLng: LatLng) {
        this.latLng = latLng
        updateGeofenceOnMap(latLng, geofenceName, geofencesize.toFloat())
    }

    private fun updateGeofenceOnMap(latLng: LatLng, geofenceName: String, radius: Float) {
        fenceMarker = addMarker(geofenceName, latLng)
        fenceCircle = mMap.addCircle(CircleOptions().center(latLng).radius(radius.toDouble()).strokeColor(Color.BLUE))
    }

    private fun addMarker(text: CharSequence, position: LatLng): Marker {
        val iconFactory = IconGenerator(this)
        val markerOptions = MarkerOptions().icon(BitmapDescriptorFactory.fromBitmap(iconFactory.makeIcon(text))).position(position).anchor(iconFactory.anchorU, iconFactory.anchorV)
        return mMap.addMarker(markerOptions)
    }


    companion object {
        private val ALPHA_ADJUSTMENT = 0x77000000
        val POPULAR_PLACES_CLUSTER_INDEX = "popularPlacesClusterIndex"
        val GEOFENCE_NAME = "geofenceName"
        val GEOFENCE_LAT = "geofenceLat"
        val GEOFENCE_LONG = "geofenceLong"
        val GEOFENCE_RADIUS = "geofenceRadius"
        val GEOFENCE_ENTER = "geofenceEnter"
        val GEOFENCE_EXIT = "geofenceExit"
        val GEOFENCE_DWELL = "geofenceDwell"
        private val GEOFENCE_DATE_ADDED = "geofenceTimestamp"
        private val GEOFENCE_VALIDITY = "geofenceValidity"
        val STEP_LENGTH = 0.7874f
    }
}
