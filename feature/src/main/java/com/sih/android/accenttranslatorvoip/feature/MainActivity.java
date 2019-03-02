package com.sih.android.accenttranslatorvoip.feature;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;

import io.socket.client.IO;
import io.socket.client.Socket;


public class MainActivity extends AppCompatActivity {

    private final String TAG = MainActivity.class.getSimpleName();
    private final int PORT = 3000;

    private static int[] mSampleRates = new int[] { 8000, 11025, 16000, 22050, 44100 };
    private final static int BITRATE = 16000;
    private final static float RECORD_BUFFER_TIME = 4;  // seconds
    private final static float PLAY_BUFFER_TIME = 4;  // seconds

    private boolean streaming = false;

    private ADPCM adpcm;
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

        adpcm = new ADPCM();

        try {
            socket = IO.socket("http://192.168.225.49:3000");
            socket.connect();
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

    public AudioRecord findAudioRecord() {
        for (int rate : mSampleRates) {
            for (short audioFormat : new short[] { AudioFormat.ENCODING_PCM_8BIT, AudioFormat.ENCODING_PCM_16BIT }) {
                for (short channelConfig : new short[] { AudioFormat.CHANNEL_IN_MONO, AudioFormat.CHANNEL_IN_STEREO }) {
                    try {
                        Log.d(TAG, "Attempting rate " + rate + "Hz, bits: " + audioFormat + ", channel: "
                                + channelConfig);
                        int bufferSize = AudioRecord.getMinBufferSize(rate, channelConfig, audioFormat);

                        if (bufferSize != AudioRecord.ERROR_BAD_VALUE) {
                            // check if we can instantiate and have a success
                            AudioRecord recorder = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, rate, channelConfig, audioFormat, bufferSize);

                            if (recorder.getState() == AudioRecord.STATE_INITIALIZED)
                                return recorder;
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error occurred.");
                        Log.e(TAG, e.getLocalizedMessage());
                    }
                }
            }
        }
        return null;
    }

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
        adpcm.resetEncoder();

        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Start recording... ");

                int minBufferSize = AudioRecord.getMinBufferSize(
                        BITRATE,
                        AudioFormat.CHANNEL_IN_STEREO,
                        AudioFormat.ENCODING_PCM_16BIT);

                int bufferSizeInBytes = (int)(RECORD_BUFFER_TIME * 2 * 2 * BITRATE);
                if (bufferSizeInBytes < minBufferSize)
                    bufferSizeInBytes = minBufferSize;

                record = new AudioRecord(
                        MediaRecorder.AudioSource.MIC,
                        BITRATE,
                        AudioFormat.CHANNEL_IN_STEREO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        bufferSizeInBytes);
//                    record = findAudioRecord();

                record.startRecording();

                int pos = 0;
                short[] recordChunk = new short[16384];
                double startTime = System.currentTimeMillis();

                while (streaming) {
                    pos += record.read(recordChunk, pos, recordChunk.length - pos);

                    if(pos == recordChunk.length) {
                        pos = 0;

                        final byte[] bytes = adpcm.encode(recordChunk);

                        JSONObject obj = new JSONObject();
                        try {
                            obj.put("audio-buffer", bytes);
                            socket.emit("data", obj);
                        } catch (JSONException e) {
                            Log.e(TAG, "Error occurred.");
                            Log.e(TAG, e.getLocalizedMessage());
                        }

                        Log.d(TAG, String.format("MinBufferSize: %d data sent%n", minBufferSize));

                        double endTime = System.currentTimeMillis();
                        int kilobytes = bytes.length / 1024;
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

//    private void startPlaying() {
//        adpcm.resetEncoder();
//
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                int minBufferSize = AudioTrack.getMinBufferSize(
//                        BITRATE,
//                        AudioFormat.CHANNEL_IN_STEREO,
//                        AudioFormat.ENCODING_PCM_16BIT);
//
//                int bufferSizeInBytes = (int) (PLAY_BUFFER_TIME * 2 * 2 * BITRATE);
//                if (bufferSizeInBytes < minBufferSize)
//                    bufferSizeInBytes = minBufferSize;
//
//                track = new AudioTrack(AudioManager.STREAM_MUSIC,
//                        BITRATE,
//                        AudioFormat.CHANNEL_OUT_STEREO,
//                        AudioFormat.ENCODING_PCM_16BIT,
//                        bufferSizeInBytes,
//                        AudioTrack.MODE_STREAM);
//
//                track.play();
//
//                int bufferWriteFrame = 0;
//                //double startTime = System.currentTimeMillis();
//
//                while (streaming) {
//                    StreamReadResult result = nabtoApi.streamRead(stream);
//                    NabtoStatus status = result.getStatus();
//                    if (status == NabtoStatus.INVALID_STREAM || status == NabtoStatus.STREAM_CLOSED) {
//                        stopStreaming();
//                        break;
//                    } else if (status != NabtoStatus.OK) {
//                        Log.v(this.getClass().toString(), "Read error: " + status);
//                        stopStreaming();
//                        break;
//                    }
//
//                    /*double endTime = System.currentTimeMillis();
//                    int kilobytes = result.getData().length / 1024;
//                    double seconds = (endTime-startTime) / 1000.0;
//                    double bandwidth = (kilobytes / seconds);  //kilobytes-per-second (kBs)
//                    startTime = endTime;
//                    Log.v(this.getClass().toString(), "receiving = " + bandwidth + " kBs");*/
//
//                    final short[] playChunk = adpcm.decode(result.getData());
//                    track.write(playChunk, 0, playChunk.length);
//
//                    bufferWriteFrame += playChunk.length / 2;
//                    int bufferPlaybackFrame = track.getPlaybackHeadPosition();
//                    double bufferFilled = (bufferWriteFrame - bufferPlaybackFrame) / (bufferSizeInBytes / 4.0);
//                    Log.v(this.getClass().toString(), "Play buffer filled: " + bufferFilled + "%");
//                }
//
//                track.stop();
//                track.release();
//                track = null;
//            }
//        }).start();
//    }
}
