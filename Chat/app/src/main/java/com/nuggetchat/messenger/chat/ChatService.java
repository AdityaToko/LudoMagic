package com.nuggetchat.messenger.chat;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.nuggetchat.messenger.rtcclient.EventListener;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import io.socket.client.IO;
import io.socket.client.Socket;

public class ChatService extends Service {
    private static final String HOST = "http://chat.nuggetkids.com/";
    //private static final String HOST = "http://192.168.0.118:5000";
    private static final String LOG_TAG = ChatService.class.getSimpleName();
    public Socket socket;
    MessageHandler messageHandler;
    EventListener eventListener;

    public class ChatBinder extends Binder {
       public ChatService getService() {
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
            Log.e(LOG_TAG, "onStartCommand: inside try");
            socket = IO.socket(HOST);
        } catch (URISyntaxException e) {
            Log.e(LOG_TAG, "onStartCommand: inside catch");
            Log.e(LOG_TAG, "Socket Uri Syntax exception: " + e.getMessage());
        }
        messageHandler = new MessageHandler(socket, this);
        Log.e(LOG_TAG, "onStartCommand: registering events");
        socket.on(Socket.EVENT_CONNECT, messageHandler.onInit);
        socket.on("init_successful", messageHandler.onInitSuccessful);
        socket.on("call_requested", messageHandler.onCallRequested);
        socket.on(Socket.EVENT_DISCONNECT, messageHandler.onDisconnect);
        socket.connect();
        Log.e(LOG_TAG, "onStartCommand: after socket connect" );
        return START_STICKY;
    }

    public void registerEventListener(EventListener eventListener) {
        this.eventListener = eventListener;
        registerForCallEvents(eventListener);
    }

    public void unregisterEventListener(EventListener eventListener){
        this.eventListener = eventListener;
        this.eventListener = null;
    }

    private void registerForCallEvents(EventListener eventListener){
        messageHandler.addEventListener(eventListener);
        socket.on("call_accepted", messageHandler.onCallAccepted);
        socket.on("ice_candidates", messageHandler.onIceCandidates);
        socket.on("call_ended", messageHandler.onCallEnded);
        socket.on("game_link", messageHandler.onGameLink);
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Intent intent = new Intent();
        intent.setAction("com.android.ServiceStopped");
        sendBroadcast(intent);
    }
}
