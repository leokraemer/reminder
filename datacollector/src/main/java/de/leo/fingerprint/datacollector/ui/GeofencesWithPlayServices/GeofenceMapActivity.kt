package de.leo.fingerprint.datacollector.ui.GeofencesWithPlayServices

import android.app.Activity
import android.content.Intent
import android.graphics.Point
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import de.leo.fingerprint.datacollector.R
import de.leo.fingerprint.datacollector.datacollection.database.GEOFENCE_IMAGE
import de.leo.fingerprint.datacollector.datacollection.database.GEOFENCE_NAME
import de.leo.fingerprint.datacollector.datacollection.database.ID
import de.leo.fingerprint.datacollector.datacollection.database.JitaiDatabase
import kotlinx.android.synthetic.main.geofence_activity_main.*
import kotlinx.android.synthetic.main.geofence_activity_main.view.*
import org.jetbrains.anko.toast


class GeofenceMapActivity : MainActivity(), OnMapReadyCallback {
    private lateinit var mMap: GoogleMap
    lateinit var db: JitaiDatabase
    var geofenceID: Int = -1

    var geofenceIcon = -1

    private var geofenceName = ""
        set(value) {
            if (field != value) {
                field = value
            }
        }
    //Konstanz center
    private var latLng: LatLng? = LatLng(47.675176, 9.167927)

    val MIN_GEOFENCE_SIZE = 50
    private var geofencesize: Int = 0
        set(value) {
            if (value != field) {
                field = Math.max(value, MIN_GEOFENCE_SIZE)
                updateSizeText(geofencesize.toFloat())
                updateRadarOverlay()
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
                    .toFloat(), true, false, false, false, fiveMinutes, geofenceIcon)
            else
                geofenceID = db.enterGeofence(geofenceName, latLng!!, geofencesize
                    .toFloat(), true, false, false, false, fiveMinutes, geofenceIcon)
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
        mMap.uiSettings.isTiltGesturesEnabled = false
        mMap.isMyLocationEnabled = true
        geofencesize = 100
        if (geofenceID != -1) {
            val geofence = db.getMyGeofence(geofenceID)
            if (geofence != null) {
                latLng = LatLng(geofence.latitude, geofence.longitude)
                geofencesize = Math.floor(geofence.radius.toDouble()).toInt()
                updateRadarOverlay()
            }
        }
        mMap.setOnCameraMoveListener {
            this.latLng = mMap.getCameraPosition().target
            updateRadarOverlay()
        }
        mMap.setOnCameraIdleListener {
            this.latLng = mMap.getCameraPosition().target
            updateRadarOverlay()
        }
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 13.0f))
    }

    /* number of km per degree = ~111km (111.32 in google maps, but range varies
    between 110.567km at the equator and 111.699km at the poles)
    1km in degree = 1 / 111.32km = 0.0089
    1m in degree = 0.0089 / 1000 = 0.0000089*/
    val coef = 0.0000089;

    /**
     * Utility function that adds n meters in latitude.
     */
    fun addMetersToLatitude(latlng: LatLng, meters: Double): LatLng {
        return LatLng(latLng!!.latitude + meters * coef, latlng.longitude)
    }

    fun updateRadarOverlay() {
        // Compute the area of the circle each time the camera changes

        val centerPointOnMap = mMap.projection.fromScreenLocation(
            Point(radar.getCenterX(), radar.getCenterY()))
        val someMetersApart = addMetersToLatitude(centerPointOnMap, geofencesize.toDouble())
        val someMetersApartOnScreen = mMap.projection.toScreenLocation(someMetersApart)

        radar.radius = Math.abs(radar.centerY - someMetersApartOnScreen.y).toFloat()
        // Uncomment to inspect the difference between
        // RadarOverlayView circle and geographic circle:
        // mMap.clear();
        // Circle circle = mMap.addCircle(new CircleOptions()
        //        .center(cameraPosition.target)
        //        .radius(geoRadius)
        //        .strokeColor(Color.GREEN)
        //        .fillColor(Color.BLUE));

        //Toast.makeText(this, "Area: $geoArea", Toast.LENGTH_SHORT).show()
    }
}