package com.sih.android.accenttranslatorvoip.feature;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.Arrays;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;


public class MainActivity extends AppCompatActivity {

    private final String TAG = MainActivity.class.getSimpleName();

    private final static int BITRATE = 44100;
    private final static float RECORD_BUFFER_TIME = 4;  // seconds
    private final static float PLAY_BUFFER_TIME = 4;  // seconds

    private boolean streaming = false;

    private AudioRecord record;
    private AudioTrack track;

    private Socket socket;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button startButton = findViewById(R.id.start_button);
        Button stopButton = findViewById(R.id.stop_button);

        startButton.setOnClickListener(startListener);
        stopButton.setOnClickListener(stopListener);

        requestRecordAudioPermission();

        try {
            socket = IO.socket("http://192.168.225.49:3000");
            socket.connect();

            int minBufferSize = AudioTrack.getMinBufferSize(
                    BITRATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_FLOAT);

            int bufferSizeInBytes = (int) (PLAY_BUFFER_TIME * 2 * 2 * BITRATE);
            if (bufferSizeInBytes < minBufferSize)
                bufferSizeInBytes = minBufferSize;

            track = new AudioTrack(AudioManager.STREAM_MUSIC,
                    BITRATE,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_FLOAT,
                    bufferSizeInBytes,
                    AudioTrack.MODE_STREAM);

            final int finalBufferSizeInBytes = bufferSizeInBytes;
            socket.on("data-converted", new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    try {
                        handleIncomingData(finalBufferSizeInBytes, args);
                    } catch (JSONException e) {
                        Log.e(TAG, "Error occurred.");
                        Log.e(TAG, e.getLocalizedMessage());
                    }
                }
            });
            socket.on("error", new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    Log.e(TAG, String.valueOf(args[0]));
                }
            });
        } catch (URISyntaxException e) {
            Log.e(TAG, "Error occurred.");
            Log.e(TAG, e.getLocalizedMessage());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        socket.disconnect();
    }

    private void handleIncomingData(int bufferSizeInBytes, Object... args) throws JSONException {
        int packetNo = (int) args[0];
        String socketId = String.valueOf(args[1]);
        JSONObject jsonObject = (JSONObject) args[2];
        float[] audioBuffer = new float[jsonObject.length()];
        for (int i = 0; i < audioBuffer.length; i++) {
            audioBuffer[i] = (float) jsonObject.getDouble(String.valueOf(i));
        }

        int bufferWriteFrame = 0;
        track.flush();
        track.play();
        track.write(audioBuffer, 0, audioBuffer.length, AudioTrack.WRITE_BLOCKING);

        bufferWriteFrame += audioBuffer.length / 2;
        int bufferPlaybackFrame = track.getPlaybackHeadPosition();
        double bufferFilled = (bufferWriteFrame - bufferPlaybackFrame) / (bufferSizeInBytes / 4.0);
        Log.d(TAG, "Play buffer filled: " + bufferFilled + "%");

        track.stop();
    }


    private final View.OnClickListener startListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            streaming = true;
            startStreaming();
        }
    };

    private final View.OnClickListener stopListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            streaming = false;
            record.stop();
            record.release();
            record = null;
            Log.d(TAG, "Recorder Released.");
        }
    };

    private void requestRecordAudioPermission() {
        int currentapiVersion = android.os.Build.VERSION.SDK_INT;
        if (currentapiVersion > android.os.Build.VERSION_CODES.LOLLIPOP){

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {

                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)) {

                    // Show an expanation to the user *asynchronously* -- don't block
                    // this thread waiting for the user's response! After the user
                    // sees the explanation, try again to request the permission.

                } else {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 1);
                }
            }

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {

                    // Show an expanation to the user *asynchronously* -- don't block
                    // this thread waiting for the user's response! After the user
                    // sees the explanation, try again to request the permission.

                } else {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
                }
            }

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                    // Show an expanation to the user *asynchronously* -- don't block
                    // this thread waiting for the user's response! After the user
                    // sees the explanation, try again to request the permission.

                } else {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case 1: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("Activity", "Granted!");

                } else {
                    finish();
                }
            }
        }
    }

    private void startStreaming() {
        new Thread(new Runnable() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void run() {
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

                int pos = 0, packetNo = 0;
                float[] chunk = new float[65536 * 2];
                double startTime = System.currentTimeMillis();

                while (streaming) {
                    pos += record.read(chunk, pos, chunk.length - pos, AudioRecord.READ_BLOCKING);

                    if(pos == chunk.length) {
                        pos = 0;

                        JSONObject obj = new JSONObject();
                        try {
                            Log.d(TAG, Arrays.toString(chunk));
                            obj.put("packet-no", packetNo++);
                            obj.put("socket-id", socket.id());
                            obj.put("audio-buffer", JSONObject.wrap(chunk));
                            socket.emit("data-original", obj);
                        } catch (JSONException e) {
                            Log.e(TAG, "Error occurred.");
                            Log.e(TAG, e.getLocalizedMessage());
                        }

                        Log.d(TAG, String.format("MinBufferSize: %d data sent%n", bufferSizeInBytes));

                        double endTime = System.currentTimeMillis();
                        int kilobytes = chunk.length / 1024;
                        double seconds = (endTime - startTime) / 1000.0;
                        double bandwidth = (kilobytes / seconds);
                        startTime = endTime;
                        Log.d(TAG, "sending = " + bandwidth + " kBs");
                    }
                }
                Log.d(TAG, "Stopped recording...");
            }
        }).start();

    }
}
