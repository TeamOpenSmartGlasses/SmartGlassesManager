package com.example.wearableaidisplaymoverio.comms;

import static com.example.wearableaidisplaymoverio.ASPClientSocket.TAG;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.net.URI;
import java.net.URISyntaxException;

import org.java_websocket.client.WebSocketClient;
import org.json.JSONObject;

import io.reactivex.rxjava3.subjects.PublishSubject;

public class WebSocketManager implements Runnable{
    private String TAG = "WearableAI_WebSocket_Reconnector";
    public AsgWebSocketClient ws;
    private URI serverURI;

    //data to save and pass to the socket
    //observables to send data around app
    PublishSubject<JSONObject> dataObservable;
    String mySourceName;

    public void setObservable(PublishSubject<JSONObject> dataO){
        dataObservable = dataO;
    }

    public void setSourceName(String name){
        mySourceName = name;
    }

    public int getConnectionState(){
        if (ws != null) {
            return ws.getConnectionState();
        } else {
            return 0;
        }
    }

    public void sendJson(JSONObject data){
        Log.d(TAG, "SENDING JSON FROM ASG WS");
        if (ws != null) {
            ws.send(data.toString());
        }
    }

    public WebSocketManager(String ip, String port){
        if (port == null){
            port = "8887";
        }
        //create new URI to connect with
        try{
            this.serverURI = new URI("ws://" + ip + ":" + port);
        } catch (URISyntaxException e){
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        final Handler handler = new Handler(Looper.getMainLooper());
        final int delay = 2000; // 1000 milliseconds == 1 second

        handler.postDelayed(new Runnable() {
            public void run() {
                try {
                    Log.d(TAG, "Trying to connect...");
                    ws = new AsgWebSocketClient(WebSocketManager.this, serverURI);
                    ws.setObservable(dataObservable);
                    ws.setSourceName(mySourceName);
                    ws.connectBlocking();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (ws.getConnectionState() == 0 || ws.isClosed()){
                    ws.killme = true;
                    ws.stop();
                    handler.postDelayed(this, delay); //run again in delay milliseconds if we didn't successfully connect
                }
            }
        }, delay);


//        while(connected != 2) {
//            try {
//                Log.d(TAG, "Trying to connect...");
//                ws = new AsgWebSocketClient(this, serverURI);
//                ws.setObservable(dataObservable);
//                ws.setSourceName(mySourceName);
//                ws.connectBlocking();
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//            connected = ws.getConnectionState();
//        }
    }

    protected void onClose(){
        Log.d(TAG, "WebSocketManager onClose called");
        ws.stop();
        run();
    }
}