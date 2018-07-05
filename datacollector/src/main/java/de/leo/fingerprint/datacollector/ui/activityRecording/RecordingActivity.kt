package de.leo.fingerprint.datacollector.ui.activityRecording

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.support.v4.content.LocalBroadcastManager
import android.widget.Toast
import de.leo.fingerprint.datacollector.R
import de.leo.fingerprint.datacollector.datacollection.DataCollectorService
import de.leo.fingerprint.datacollector.datacollection.database.JitaiDatabase
import de.leo.fingerprint.datacollector.ui.uiElements.ACCELERATION
import de.leo.fingerprint.datacollector.utils.IS_RECORDING
import de.leo.fingerprint.datacollector.utils.START_RECORDING
import de.leo.fingerprint.datacollector.utils.STOP_RECORDING
import kotlinx.android.synthetic.main.recording_activity.*
import java.util.concurrent.TimeUnit


const val RECORDING_ID = "rec_id"

/**
 * Created by Leo on 23.11.2017.
 */

private const val TOTAL_RECORDING_TIME = 5L

class RecordingActivity : Activity() {

    var reference: ActivityRecord? = null
    var broadcastReciever: BroadcastReceiver? = null
    var isRecording: Boolean = false;

    private lateinit var db: JitaiDatabase

    var recordingIdInt: Int = -1
        get() = field
        set(value) {
            if (field != value) {
                if (value < 1) {
                    field = 1
                } else {
                    field = value
                }
                recordingId.setText("${field}")
            }
        }

    private var handler = Handler()

    private var spinnerUpdater = object : Runnable {
        override fun run() {
            val oldProgress = recordingSpinner.progress
            recordingSpinner.progress = ((System.currentTimeMillis() - startTime) % 5000L).toInt()
            if (oldProgress > recordingSpinner.progress)
            //updateDataView()
                handler.postDelayed(this, 10)
        }
    }

    private fun updateDataView() {
        val data = db.getReferenceRecording(db.getLatestRecordingId(), reference?.recordLength
            ?: 20)
        recording_chart.setData(ACCELERATION, data)
    }

    private var startTime: Long = 0L;

    private var remainingTimeCounter: CountDownTimer? = null

    private var remainingTime: Long = 0;

    inner class DataReciever : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent != null) {
                when (intent.action) {
                    START_RECORDING -> {
                        recordingStartedCallback(intent)
                    }
                    STOP_RECORDING  -> {
                        recordingStoppedCallback()
                    }
                    IS_RECORDING    -> {
                        isRecordingCallback(intent)
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.recording_activity)
        db = JitaiDatabase.getInstance(applicationContext)
        recordingIdInt = db.getLatestRecordingId() + 1
        if (db.getRecognizedActivitiesId() > 0) {
            reference = db.getReferenceRecording(db.getRecognizedActivitiesId(), 20)
            reference_chart.setData(ACCELERATION, reference!!)
        }
        start_recording.setOnClickListener({ _ -> startRecording() })
    }

    override fun onStart() {
        super.onStart()
        //setup reciever to communicate with service
        if (broadcastReciever == null) broadcastReciever = DataReciever()
        val intentFilter = IntentFilter(START_RECORDING)
        intentFilter.addAction(STOP_RECORDING)
        intentFilter.addAction(IS_RECORDING)
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReciever!!, intentFilter)
        val checkIsRecording = Intent(this, DataCollectorService::class.java)
        checkIsRecording.setAction(IS_RECORDING)
        startService(checkIsRecording)
    }

    override fun onStop() {
        super.onStop()
        broadcastReciever?.let {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(it)
        }
        broadcastReciever = null
    }


    private fun isRecordingCallback(intent: Intent) {
        isRecording = intent.getBooleanExtra(IS_RECORDING, false)
    }

    private fun recordingStartedCallback(intent: Intent) {
        recordingIdInt = intent.getIntExtra(RECORDING_ID, -1)
        db.setRecordingName("ohne Name", recordingIdInt)
        startTime = System.currentTimeMillis()
        remainingTimeCounter = object : CountDownTimer(TimeUnit.MINUTES.toMillis(
            TOTAL_RECORDING_TIME), 100) {
            override fun onTick(millisUntilFinished: Long) {
                remainingTime = TimeUnit.MINUTES.toSeconds(TOTAL_RECORDING_TIME) - (System.currentTimeMillis() -
                    startTime) / 1000
                runOnUiThread(updateTime)
            }

            override fun onFinish() {
                stopRecording()
            }
        }.start()
        handler.post(spinnerUpdater)
        Toast.makeText(this, "Aufnahme gestartet.", Toast.LENGTH_SHORT).show()
        start_recording.setText("Aufnahme beenden")
        start_recording.setOnClickListener({ _ -> stopRecording() })
        start_recording.isEnabled = true
        isRecording = true
    }

    val updateTime = object : Runnable {
        override fun run() {
            //00:00
            recordingTime.setText(String.format("%02d:%02d",
                                                remainingTime / 60,
                                                remainingTime % 60))
        }

    }

    private fun recordingStoppedCallback() {
        recordingIdInt = db.getLatestRecordingId() + 1
        remainingTime = TOTAL_RECORDING_TIME * 60 //minutes * seconds
        recordingTime.setText(String.format("%02d:%02d", remainingTime / 60, remainingTime % 60))
        remainingTimeCounter?.cancel()
        Toast.makeText(this, "Aufnahme beendet.", Toast.LENGTH_SHORT).show()
        start_recording.setText("Aufnahme starten")
        start_recording.setOnClickListener({ _ -> startRecording() })
        start_recording.isEnabled = true
        handler.removeCallbacks(spinnerUpdater)
        isRecording = false;
    }

    private fun startRecording() {
        start_recording.isEnabled = false
        val startRecodingIntent = Intent(this, DataCollectorService::class.java)
        startRecodingIntent.setAction(START_RECORDING)
        startRecodingIntent.putExtra(RECORDING_ID, recordingIdInt)
        startService(startRecodingIntent)
    }

    private fun stopRecording() {
        start_recording.isEnabled = false
        val startRecodingIntent = Intent(this, DataCollectorService::class.java)
        startRecodingIntent.setAction(STOP_RECORDING)
        startService(startRecodingIntent)
    }
}