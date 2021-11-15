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
import org.java_websocket.server.WebSocketServer;

import com.example.wearableaidisplaymoverio.R;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.enums.ReadyState;

import org.java_websocket.drafts.Draft;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONObject;

/**
 * This example demonstrates how to create a websocket connection to a server. Only the most
 * important callbacks are overloaded.
 */
public class AsgWebSocketClient extends WebSocketClient {
    private int connected = 0;

    public AsgWebSocketClient(URI serverUri, Draft draft) {
        super(serverUri, draft);

    }

    public AsgWebSocketClient(URI serverURI) {
        super(serverURI);
    }

    public AsgWebSocketClient(URI serverUri, Map<String, String> httpHeaders) {
        super(serverUri, httpHeaders);
    }

    public void start(){
        connected = 1;
        connect();
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
        System.out.println("received: " + message);
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        if (connected == 2) {
            connected = 1;
        }

        // The codecodes are documented in class org.java_websocket.framing.CloseFrame
        System.out.println(
                "Connection closed by " + (remote ? "remote peer" : "us") + " Code: " + code + " Reason: "
                        + reason);
    }

    @Override
    public void onError(Exception ex) {
        ex.printStackTrace();
        // if the error is fatal then onClose will be called additionally
    }

    public void sendJson(JSONObject data){
        Log.d(TAG, "SENDING JSON FROM ASG WS");
        send(data.toString());
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