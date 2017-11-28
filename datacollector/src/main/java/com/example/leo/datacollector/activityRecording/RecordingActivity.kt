package com.example.leo.datacollector.activityRecording

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import com.example.leo.datacollector.R
import com.example.leo.datacollector.database.SqliteDatabase
import com.example.leo.datacollector.services.DataCollectorService
import com.example.leo.datacollector.utils.START_RECORDING
import com.example.leo.datacollector.utils.STOP_RECORDING
import kotlinx.android.synthetic.main.recording_activity.*
import android.content.IntentFilter
import android.os.CountDownTimer
import android.support.v4.content.LocalBroadcastManager
import com.example.leo.datacollector.utils.IS_RECORDING
import java.util.concurrent.TimeUnit


const val RECORDING_ID = "rec_id"

/**
 * Created by Leo on 23.11.2017.
 */
class RecordingActivity : Activity() {

    var broadcastReciever: BroadcastReceiver? = null
    var isRecording: Boolean = false;

    private lateinit var db: SqliteDatabase

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
                    STOP_RECORDING -> {
                        recordingStoppedCallback()
                    }
                    IS_RECORDING -> {
                        isRecordingCallback(intent)
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.recording_activity)
        db = SqliteDatabase.getInstance(applicationContext)
        recordingIdInt = db.getLatestRecordingId() + 1
        start_recording.setOnClickListener({ v -> startRecording() })
    }

    override fun onStart() {
        super.onStart()
        //setup reciever to communicate with service
        if (broadcastReciever == null) broadcastReciever = DataReciever()
        val intentFilter = IntentFilter(START_RECORDING)
        intentFilter.addAction(STOP_RECORDING)
        intentFilter.addAction(IS_RECORDING)
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReciever, intentFilter)
        val checkIsRecording = Intent(this, DataCollectorService::class.java)
        checkIsRecording.setAction(IS_RECORDING)
        startService(checkIsRecording)
    }

    override fun onStop() {
        super.onStop()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReciever)
        broadcastReciever = null
    }


    private fun isRecordingCallback(intent: Intent) {
        isRecording = intent.getBooleanExtra(IS_RECORDING, false)
    }

    private fun recordingStartedCallback(intent: Intent) {
        recordingIdInt = intent.getIntExtra(RECORDING_ID, -1)
        startTime = System.currentTimeMillis()
        remainingTimeCounter = object : CountDownTimer(TimeUnit.MINUTES.toMillis(5), 1000) {
            override fun onTick(millisUntilFinished: Long) {
                remainingTime = millisUntilFinished / 1000
                runOnUiThread(updateTime)
            }

            override fun onFinish() {
                stopRecording()
            }
        }.start()

        Toast.makeText(this, "Aufnahme gestartet.", Toast.LENGTH_SHORT).show()
        start_recording.setText("Aufnahme beenden")
        start_recording.setOnClickListener({ _ -> stopRecording() })
        start_recording.isEnabled = true
        isRecording = true
    }

    val updateTime = object : Runnable {
        override fun run() {
            //00:00
            recordingTime.setText(String.format("%02d:%02d", remainingTime / 60, remainingTime % 60))
        }

    }

    private fun recordingStoppedCallback() {
        recordingIdInt = db.getLatestRecordingId() + 1
        remainingTime = 5 * 60 //minutes * seconds
        recordingTime.setText(String.format("%02d:%02d", remainingTime / 60, remainingTime % 60))
        remainingTimeCounter?.cancel()
        Toast.makeText(this, "Aufnahme beendet.", Toast.LENGTH_SHORT).show()
        start_recording.setText("Aufnahme starten")
        start_recording.setOnClickListener({ _ -> startRecording() })
        start_recording.isEnabled = true
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