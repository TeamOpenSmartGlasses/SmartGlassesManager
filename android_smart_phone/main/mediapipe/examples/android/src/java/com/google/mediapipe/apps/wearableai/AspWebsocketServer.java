package com.google.mediapipe.apps.wearableai;

import java.net.InetSocketAddress;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import java.util.Map;
import java.util.UUID;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;
import android.util.Log;
import org.json.JSONObject;
import org.json.JSONException;

import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.subjects.PublishSubject;

public class AspWebsocketServer extends WebSocketServer {
    //data observable we can send data through
    private static PublishSubject dataObservable;

    private final String TAG = "WearableAI_AspWebsocketServer";

    private Map<String, WebSocket> clients = new ConcurrentHashMap<>();

    public AspWebsocketServer(int port)
    {
        super(new InetSocketAddress(port));
    }

    public AspWebsocketServer(InetSocketAddress address)
    {
        super(address);
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        String uniqueID = UUID.randomUUID().toString();
        clients.put(uniqueID, conn);
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        Log.d(TAG, "onClose called");
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        Log.d(TAG, "GOT MESSAGE: " + message);
        try {
            JSONObject json_obj = new JSONObject(message);
            Log.d(TAG, "TRANSCRIPT: " + json_obj.getString("transcript"));
            dataObservable.onNext(json_obj);
        } catch (JSONException e){
            e.printStackTrace();
        }
    }

    @Override
    public void onMessage(WebSocket conn, ByteBuffer message) {
        Log.d(TAG, "GOT MESSAGE BYTES");
    }

    @Override
    public void onError(WebSocket conn, Exception ex)
    {
        ex.printStackTrace();
        if (conn != null) {
            // some errors like port binding failed may not be assignable to a specific websocket
        }
    }

    @Override
    public void onStart()
    {
        //LogHelper.e(TAG, "Server started!");
        setConnectionLostTimeout(3);
    }


    public void setObservable(PublishSubject observable){
        dataObservable = observable;
    }

    //need to call this so if we get "Force Stop"ped, we will clean up sockets so we can connect on restart
    public void destroy(){
        try{
            stop(3);
        } catch (InterruptedException e){
            e.printStackTrace();
        }
    }


}
