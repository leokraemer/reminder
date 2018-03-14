package de.leo.fingerprint.datacollector.GeofencesWithPlayServices

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import de.leo.fingerprint.datacollector.R
import de.leo.fingerprint.datacollector.database.JitaiDatabase
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.maps.android.ui.IconGenerator
import de.leo.fingerprint.datacollector.database.GEOFENCE_IMAGE
import de.leo.fingerprint.datacollector.database.GEOFENCE_NAME
import de.leo.fingerprint.datacollector.database.ID
import kotlinx.android.synthetic.main.fragment_reminder_selection.*
import kotlinx.android.synthetic.main.geofence_activity_main.*
import kotlinx.android.synthetic.main.geofence_activity_main.view.*
import org.jetbrains.anko.toast


class GeofenceMapActivity : MainActivity(), OnMapReadyCallback, GoogleMap.OnMapLongClickListener {
    private lateinit var mMap: GoogleMap
    lateinit var db: JitaiDatabase
    var geofenceID: Int = -1

    var geofenceIcon = -1

    private var geofenceName = ""
        set(value) {
            if (field != value) {
                field = value
                if (latLng != null)
                    updateGeofenceOnMap(latLng!!, geofenceName, geofencesize.toFloat())
            }
        }
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

    val GEOFENCEID = "geofenceId"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.geofence_activity_main)
        geofenceIcon = intent.getIntExtra(GEOFENCE_IMAGE, -1)

        db = JitaiDatabase.getInstance(this)
        val intent = getIntent()
        //get geofenceId from intent or get new geofenceId
        geofenceID = intent.getIntExtra(ID, -1)
        if (geofenceID != -1) {
            val geofence = db.getMyGeofence(geofenceID)
            if (geofence != null) {
                latLng = LatLng(geofence.latitude, geofence.longitude)
                geofencesize = Math.floor(geofence.radius.toDouble()).toInt()
            }
        }
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
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
        geofenceoptions.nameField.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                geofenceName = s.toString()
            }
        })
        geofenceoptions.nameField.setText(intent.getStringExtra(GEOFENCE_NAME) ?: "")

    }

    private fun commitGeofence() {
        if (latLng != null && geofenceName.isNotBlank()) {
            //addGeofence(geofenceName, latLng!!, geofencesize.toFloat(), enter, exit, dwell)
            if (geofenceID != -1)
                db.enterGeofence(geofenceID, geofenceName, latLng!!, geofencesize
                    .toFloat(), true, false, false, fiveMinutes, geofenceIcon)
            else
                geofenceID = db.enterGeofence(geofenceName, latLng!!, geofencesize
                    .toFloat(), true, false, false, fiveMinutes, geofenceIcon)
            updateGeofenceOnMap(latLng!!, geofenceName, geofencesize.toFloat())
            val result = Intent()
            result.putExtra(ID, geofenceID)
            setResult(Activity.RESULT_OK, result)
            finish()
        } else if (latLng == null) {
            toast("Bitte markieren sie einen Ort durch Langklick auf die Karte")
        } else {
            toast("Bitte vergeben sie einen Namen")
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
        if (geofenceID != -1) {
            val geofence = db.getMyGeofence(geofenceID)
            if (geofence != null) {
                latLng = LatLng(geofence.latitude, geofence.longitude)
                geofencesize = Math.floor(geofence.radius.toDouble()).toInt()
                updateGeofenceOnMap(latLng!!, geofenceName, geofencesize.toFloat())
            }
        }
    }

    override fun onMapLongClick(latLng: LatLng) {
        this.latLng = latLng
        updateGeofenceOnMap(latLng, geofenceName, geofencesize.toFloat())
    }

    private fun updateGeofenceOnMap(latLng: LatLng, geofenceName: String, radius: Float) {
        fenceMarker = addMarker(geofenceName, latLng)
        fenceCircle = mMap.addCircle(CircleOptions().center(latLng).radius(radius.toDouble()).strokeColor(
            Color.BLUE))
    }

    private fun addMarker(text: CharSequence, position: LatLng): Marker {
        val iconFactory = IconGenerator(this)
        val markerOptions = MarkerOptions().icon(BitmapDescriptorFactory.fromBitmap(iconFactory.makeIcon(
            text))).position(position).anchor(iconFactory.anchorU, iconFactory.anchorV)
        return mMap.addMarker(markerOptions)
    }
}