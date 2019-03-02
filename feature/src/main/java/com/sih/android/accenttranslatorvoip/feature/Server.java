package com.sih.android.accenttranslatorvoip.feature;

import android.media.AudioRecord;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Arrays;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

public class Server {

    private static final String TAG = Server.class.getSimpleName();
    private static final int PORT = 8000;
    private static final int SAMPLE_RATE = 44100;
    private static final int CHANNEL_CONFIG = android.media.AudioFormat.CHANNEL_OUT_MONO;
    private static final int AUDIO_FORMAT = android.media.AudioFormat.ENCODING_PCM_16BIT;

    private static boolean status = true;
    private static int minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);

    private static AudioInputStream audioInputStream;
    private static AudioFormat audioFormat;


    public static void main(String[] args) {
        try {
            DatagramSocket datagramSocket = new DatagramSocket(PORT);
            byte[] buffer = new byte[minBufferSize];

            audioFormat = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);

            while (status) {
                final DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                datagramSocket.receive(packet);

                ByteArrayInputStream stream = new ByteArrayInputStream(packet.getData());
                audioInputStream = new AudioInputStream(stream, audioFormat, packet.getLength());

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        toSpeaker(packet.getData());
                    }
                }).start();
            }

        } catch (IOException e) {
            Log.e(TAG, "Error occurred.");
            Log.e(TAG, e.getLocalizedMessage());
        }
    }

    private static void toSpeaker(byte[] data) {
        try {
            DataLine.Info dataLineInfo = new DataLine.Info(SourceDataLine.class, audioFormat);
            SourceDataLine sourceDataLine = (SourceDataLine) AudioSystem.getLine(dataLineInfo);
            sourceDataLine.open(audioFormat);

            FloatControl volumeControl = (FloatControl) sourceDataLine.getControl(FloatControl.Type.MASTER_GAIN);
            volumeControl.setValue(100.0f);
            sourceDataLine.start();

            Log.d(TAG,"Format :" + sourceDataLine.getFormat());
            sourceDataLine.write(data, 0, data.length);

            Log.d(TAG, Arrays.toString(data));
            sourceDataLine.drain();
            sourceDataLine.close();

        } catch (LineUnavailableException e) {
            Log.e(TAG, "Error occurred.");
            Log.e(TAG, e.getLocalizedMessage());
        }
    }

}
