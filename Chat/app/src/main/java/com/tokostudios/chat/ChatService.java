package com.tokostudios.chat;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.tokostudios.chat.rtcclient.EventListener;
import com.tokostudios.chat.rtcclient.WebRtcClient;

import java.net.URISyntaxException;

import io.socket.client.IO;
import io.socket.client.Socket;

public class ChatService extends Service {
    private static final String HOST = "http://192.168.0.9:5000/";
    private static final String LOG_TAG = ChatService.class.getSimpleName();
    Socket socket;
    MessageHandler messageHandler;
    EventListener eventListener;

    public class ChatBinder extends Binder {
        ChatService getService() {
            return ChatService.this;
        }
    }
    public ChatService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    private final IBinder binder = new ChatBinder();
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e(LOG_TAG,"Inside onStartCommand");
        try {
            socket = IO.socket(HOST);
        } catch (URISyntaxException e) {
            Log.e(LOG_TAG, "Socket Uri Syntax exception: " + e.getMessage());;
        }
        messageHandler = new MessageHandler(socket, this);
        socket.on(Socket.EVENT_CONNECT, messageHandler.onInit);
        socket.on("init_successful", messageHandler.onInitSuccessful);
        socket.on("call_requested", messageHandler.onCallRequested);
        socket.connect();
        return START_STICKY;
    }

    public void registerEventListener(EventListener eventListener) {
        this.eventListener = eventListener;
        registerForCallEvents();
    }

    private void registerForCallEvents(){
        messageHandler.setEventListener(eventListener);
        socket.on("call_accepted", messageHandler.onCallAccepted);
        socket.on("ice_candidates", messageHandler.onIceCandidates);
        socket.on("call_ended", messageHandler.onCallEnded);
        //socket.connect();
    }
}
