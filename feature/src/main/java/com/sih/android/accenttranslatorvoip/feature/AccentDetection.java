package com.sih.android.accenttranslatorvoip.feature;

import android.annotation.SuppressLint;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;

import org.json.JSONException;
import org.json.JSONObject;

public class AccentDetection extends AppCompatActivity {

    private final String TAG = AccentDetection.class.getSimpleName();

    private final static int BITRATE = 44100;
    private final static float RECORD_BUFFER_TIME = 4;

    private boolean streaming = false;

    private Button accentDecectionMic;

    private AudioRecord record;
    private SocketConnection socket;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_accent_detection);

        accentDecectionMic = findViewById(R.id.accent_detection_mic);

        socket = new SocketConnection();

        Log.d(TAG, "Start recording... ");

        int minBufferSize = AudioRecord.getMinBufferSize(
                BITRATE,
                AudioFormat.CHANNEL_IN_STEREO,
                AudioFormat.ENCODING_PCM_FLOAT);

        int bufferSizeInBytes = (int)(RECORD_BUFFER_TIME * 2 * 2 * BITRATE);
        if (bufferSizeInBytes < minBufferSize)
            bufferSizeInBytes = minBufferSize;

        record = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                BITRATE,
                AudioFormat.CHANNEL_IN_STEREO,
                AudioFormat.ENCODING_PCM_FLOAT,
                bufferSizeInBytes);

        record.startRecording();

        final int[] pos = {0};
        final int[] packetNo = {0};
        final float[] chunk = new float[65536 * 2];
        final double[] startTime = {System.currentTimeMillis()};

        final int finalBufferSizeInBytes = bufferSizeInBytes;
        accentDecectionMic.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    streaming = true;
                    new Thread(new Runnable() {
                        @RequiresApi(api = Build.VERSION_CODES.M)
                        @Override
                        public void run() {
                            while (streaming) {
                                pos[0] += record.read(chunk, pos[0], chunk.length - pos[0], AudioRecord.READ_BLOCKING);

                                if(pos[0] == chunk.length) {
                                    pos[0] = 0;

                                    JSONObject obj = new JSONObject();
                                    try {
                                        obj.put("packet-no", packetNo[0]++);
                                        obj.put("socket-id", socket.id());
                                        obj.put("audio-buffer", JSONObject.wrap(chunk));
                                        socket.emit("data-accent-detect", obj);
                                    } catch (JSONException e) {
                                        Log.e(TAG, "Error occurred.");
                                        Log.e(TAG, e.getLocalizedMessage());
                                    }

                                    Log.d(TAG, String.format("MinBufferSize: %d data prepared%n", finalBufferSizeInBytes));

                                    double endTime = System.currentTimeMillis();
                                    int kilobytes = chunk.length / 1024;
                                    double seconds = (endTime - startTime[0]) / 1000.0;
                                    double bandwidth = (kilobytes / seconds);
                                    startTime[0] = endTime;
                                    Log.d(TAG, "Data to send = " + bandwidth + " kBs");
                                }
                            }
                            Log.d(TAG, "Stopped recording...");
                        }
                    }).start();
                    return true;
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    streaming = false;
                    JSONObject obj = new JSONObject();
                    try {
                        obj.put("packet-no", -1);
                        obj.put("socket-id", socket.id());
                        socket.emit("data-accent-detect", obj);
                    } catch (JSONException e) {
                        Log.e(TAG, "Error occurred.");
                        Log.e(TAG, e.getLocalizedMessage());
                    }
                    return false;
                }
                return true;
            }
        });
    }
}
