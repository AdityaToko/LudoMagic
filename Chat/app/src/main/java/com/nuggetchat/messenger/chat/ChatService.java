package com.nuggetchat.messenger.chat;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.nuggetchat.lib.Conf;
import com.nuggetchat.messenger.NuggetInjector;
import com.nuggetchat.messenger.rtcclient.EventListener;
import com.nuggetchat.messenger.utils.MyLog;

import java.net.URISyntaxException;

import io.socket.client.IO;
import io.socket.client.Socket;

public class ChatService extends Service {
    private static final String LOG_TAG = ChatService.class.getSimpleName();
    public Socket socket;
    MessageHandler messageHandler;
    EventListener eventListener;
    private NuggetInjector nuggetInjector = NuggetInjector.getInstance();

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
        MyLog.e(LOG_TAG,"Inside onStartCommand");
        nuggetInjector.setChatServiceRunning(true);
        try {
            MyLog.e(LOG_TAG, "onStartCommand: inside try");
            socket = IO.socket(Conf.CHAT_WEBRTC_SERVER);
        } catch (URISyntaxException e) {
            MyLog.e(LOG_TAG, "onStartCommand: inside catch");
            MyLog.e(LOG_TAG, "Socket Uri Syntax exception: " + e.getMessage());
        }
        messageHandler = new MessageHandler(socket, this);
        MyLog.e(LOG_TAG, "onStartCommand: registering events");
        socket.on(Socket.EVENT_CONNECT, messageHandler.onInit);
        socket.on("init_successful", messageHandler.onInitSuccessful);
        socket.on("pre_call_handshake", messageHandler.onPreCallHandshake);
        socket.on("handshake_complete", messageHandler.onHandshakeComplete);
        socket.on("call_requested", messageHandler.onCallRequested);
        socket.on("call_ended", messageHandler.onCallEnded);
        socket.on(Socket.EVENT_DISCONNECT, messageHandler.onDisconnect);
        socket.connect();
        MyLog.e(LOG_TAG, "onStartCommand: after socket connect" );
        return START_NOT_STICKY;
    }

    public MessageHandler getMessageHandler() {
        return messageHandler;
    }

    public void registerEventListener(EventListener eventListener) {
        this.eventListener = eventListener;
        registerForCallEvents(eventListener);
    }

    public void unregisterEventListener(EventListener eventListener){
        this.eventListener = eventListener;
        this.eventListener = null;
    }
    public void registerUpdatesListener(UpdateInterface updateListner){
        if(messageHandler != null){
            messageHandler.registerUpdatesListener(updateListner);
        } else {
            Log.e(LOG_TAG, "registerUpdatesListener: listener not registered" );
        }
    }
    private void registerForCallEvents(EventListener eventListener){
        messageHandler.addEventListener(eventListener);
        socket.on("call_accepted", messageHandler.onCallAccepted);
        socket.on("call_rejected", messageHandler.onCallRejected);
        socket.on("call_ongoing", messageHandler.onCallOngoing);
        socket.on("ice_candidates", messageHandler.onIceCandidates);
        socket.on("game_link", messageHandler.onGameLink);
        socket.on("game_left", messageHandler.onGameLeft);
        socket.on("socket_error", messageHandler.onSocketError);
    }

    @Override
    public void onDestroy() {
        Log.e(LOG_TAG, "onDestroy");
        nuggetInjector.setChatServiceRunning(false);
        super.onDestroy();
    }
}
