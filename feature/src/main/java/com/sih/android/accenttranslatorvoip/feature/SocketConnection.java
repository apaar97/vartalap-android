package com.sih.android.accenttranslatorvoip.feature;

import android.util.Log;

import java.net.URISyntaxException;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class SocketConnection {

    private final String TAG = SocketConnection.class.getSimpleName();

    private Socket socket;

    public SocketConnection() {
        socket = createConnection();
    }

    private Socket createConnection() {
        if (socket == null) {
            try {
                socket = IO.socket("http://192.168.225.49:3000");
            } catch (URISyntaxException e) {
                Log.e(TAG, "Error occurred.");
                Log.e(TAG, e.getLocalizedMessage());
            }
        }
        socket.connect();
        return socket;
    }

    public void disconnect() {
        socket.disconnect();
    }

    public Emitter emit(final String event, final Object... args) {
        return socket.emit(event, args);
    }

    public Emitter on(String event, Emitter.Listener fn) {
        return socket.on(event, fn);
    }

    public String id() {
        return socket.id();
    }
}
