package com.example.wearableaidisplaymoverio.comms;

/*
 * Copyright (c) 2010-2020 Nathan Rajlich
 *
 *  Permission is hereby granted, free of charge, to any person
 *  obtaining a copy of this software and associated documentation
 *  files (the "Software"), to deal in the Software without
 *  restriction, including without limitation the rights to use,
 *  copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the
 *  Software is furnished to do so, subject to the following
 *  conditions:
 *
 *  The above copyright notice and this permission notice shall be
 *  included in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 *  EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 *  OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 *  NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 *  HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 *  WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 *  FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 *  OTHER DEALINGS IN THE SOFTWARE.
 */

import static com.example.wearableaidisplaymoverio.ASPClientSocket.TAG;

import android.util.Log;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Map;
import org.java_websocket.client.WebSocketClient;

import org.java_websocket.drafts.Draft;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONException;
import org.json.JSONObject;
import io.reactivex.rxjava3.subjects.PublishSubject;

/**
 * This example demonstrates how to create a websocket connection to a server. Only the most
 * important callbacks are overloaded.
 */
public class AsgWebSocketClient extends WebSocketClient {
    private int connected = 0;
    private URI serverURI;
    private WebSocketManager webSocketManager;
    public boolean killme = false;

    //observables to send data around app
    PublishSubject<JSONObject> dataObservable;

    private String mySourceName;

    public AsgWebSocketClient(URI serverUri, Draft draft) {
        super(serverUri, draft);

    }

    public void sendHeartBeat(){
        Log.d(TAG, "send heartbeat");
        send("ping");
    }

    public AsgWebSocketClient(WebSocketManager manager, URI serverURI) {
        super(serverURI);
        connected = 0;
        webSocketManager = manager;
        this.serverURI = serverURI;
        setup();
    }

    public AsgWebSocketClient(URI serverUri, Map<String, String> httpHeaders) {
        super(serverUri, httpHeaders);
        setup();
    }

    private void setup(){
        //dataObservable = PublishSubject.create();
        mySourceName = "web_socket";
        setConnectionLostTimeout(3);
    }

    public void setObservable(PublishSubject<JSONObject> dataO){
        dataObservable = dataO;
    }

    public void setSourceName(String name){
        mySourceName = name;
    }

    public PublishSubject<JSONObject> getDataObservable(){
        return dataObservable;
    }

    public void stop(){
        connected = 0;
        close();
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        connected = 2;
        System.out.println("opened connection");
        // if you plan to refuse connection based on ip or httpfields overload: onWebsocketHandshakeReceivedAsClient
    }

    @Override
    public void onMessage(String message) {
        try {
            System.out.println("received: " + message);
            JSONObject json_message = new JSONObject(message);
            json_message.put("local_source", mySourceName); //ad our set name so rest of program knows the source of this message
            dataObservable.onNext(json_message);
        } catch (JSONException e){
            e.printStackTrace();
        }
    }

    @Override
    public void onMessage(ByteBuffer message) {
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        connected = 0;

        if (remote && !killme) {
            webSocketManager.onClose(); //tell manager that we are done and it should make a new socket to reconnect
        }

        // The codes are documented in class org.java_websocket.framing.CloseFrame
        System.out.println(
                "Connection closed by " + (remote ? "remote peer" : "us") + " Code: " + code + " Reason: "
                        + reason);
    }

    @Override
    public void onError(Exception ex) {
        ex.printStackTrace();
        // if the error is fatal then onClose will be called additionally
    }


    public int getConnectionState(){
//        ReadyState ready_state = getReadyState();
//        if (ready_state == ReadyState.OPEN){
//            return 2;
//        } else if ((ready_state == ReadyState.CLOSING) || (ready_state == ReadyState.CLOSED)){
//            return 1;
//        } else {
//            return 0;
//        }
        return connected;
    }

}