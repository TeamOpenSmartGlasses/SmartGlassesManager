//represent the ASG

package com.wearableintelligencesystem.androidsmartphone;

import android.content.Context;


import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

import java.net.InetSocketAddress;
import android.widget.TextView;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import android.content.Intent;
import android.os.IBinder;
import java.util.Random;
import java.util.Queue;
import android.os.HandlerThread;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.util.Base64;
import android.app.Service;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import android.os.Environment;
import android.net.Uri;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.util.Size;
import android.view.SurfaceHolder;
import android.os.Binder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.os.Bundle;
import android.util.Log;
import java.util.HashMap;
import java.util.Map;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.InterfaceAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.util.Enumeration;


//custom, our code
import com.wearableintelligencesystem.androidsmartphone.comms.MessageTypes;

import com.wearableintelligencesystem.androidsmartphone.texttospeech.TextToSpeechSystem;
import com.wearableintelligencesystem.androidsmartphone.utils.FileUtils;

import com.wearableintelligencesystem.androidsmartphone.comms.AspWebsocketServer;
import com.wearableintelligencesystem.androidsmartphone.comms.AudioSystem;

import com.wearableintelligencesystem.androidsmartphone.database.mediafile.MediaFileRepository;

//rxjava
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.subjects.PublishSubject;

class ASGRepresentative {
    private static final String TAG = "WearableAi_ASGRepresentative";

    private static boolean killme = false;

    private static Handler heart_beat_handler;

    //receive/send data stream
    PublishSubject<JSONObject> dataObservable;
    Disposable dataSub;

    //images saving info
    private long lastImageSave = 0;
    private float imageSaveFrequency = 0.5f; //fps

    //database
    private MediaFileRepository mMediaFileRepository = null;

    //SOCKET STUFF
    //socket
    public AspWebsocketServer asgWebSocket; 

    //acutal socket
    ServerSocket serverSocket;
    Socket socket;
    boolean shouldDie = false;

    //socket threads
    Thread SocketThread = null;
    Thread ReceiveThread = null;
    Thread SendThread = null;
    //queue of data to send through the socket
    public  BlockingQueue<byte []> queue;
    //address info
    public  final int SERVER_PORT = 4567;
    //i/o
    public  DataOutputStream output;
    public  DataInputStream input;
    //state information
    public  int mConnectState = 0;
    public  int outbound_heart_beats = 0;

    //other
    final byte [] ack_id = {0x13, 0x37};
    final byte [] heart_beat_id = {0x19, 0x20};
    final byte [] img_id = {0x01, 0x10}; //id for images

    //audio streaming system
    AudioSystem audioSystem;

    Context context;

    ASGRepresentative(Context context, PublishSubject<JSONObject> dataObservable, MediaFileRepository mMediaFileRepository){
        this.context = context;
        this.mMediaFileRepository = mMediaFileRepository;

        //create a new queue to hold outbound message
        queue = new ArrayBlockingQueue<byte[]>(50);

        //receive/send data
        this.dataObservable = dataObservable;
        dataSub = this.dataObservable.subscribe(i -> handleDataStream(i));
    }

    //receive audio and send to vosk
    public void handleDataStream(JSONObject data){
        //first check if it's a type we should handle
        try{
            String type = data.getString(MessageTypes.MESSAGE_TYPE_LOCAL);
            if (type.equals(MessageTypes.POV_IMAGE)){
                //handleImage(data.getString(MessageTypes.JPG_BYTES_BASE64), data.getLong(MessageTypes.TIMESTAMP));
            } 
        } catch (JSONException e){
            e.printStackTrace();
        }

    }

    public void sendCommandResponse(String response){
        try{
            //build json object to send command result
            JSONObject commandResponseObject = new JSONObject();
            commandResponseObject.put(MessageTypes.MESSAGE_TYPE_LOCAL, MessageTypes.VOICE_COMMAND_STREAM_EVENT);
            commandResponseObject.put(MessageTypes.VOICE_COMMAND_STREAM_EVENT_TYPE, MessageTypes.TEXT_RESPONSE_EVENT_TYPE);
            commandResponseObject.put(MessageTypes.COMMAND_RESPONSE_DISPLAY_STRING, response);

            //send the command result to web socket, to send to asg
            dataObservable.onNext(commandResponseObject);
        } catch (JSONException e){
            e.printStackTrace();
        }
    }

    public void sendPovImage(byte [] img, long imageId, long imageTime){
        String encodedImage = Base64.encodeToString(img, Base64.DEFAULT);
        try{
            //build json object to send command result
            JSONObject commandResponseObject = new JSONObject();
            commandResponseObject.put(MessageTypes.MESSAGE_TYPE_LOCAL, MessageTypes.POV_IMAGE);
            commandResponseObject.put(MessageTypes.JPG_BYTES_BASE64, encodedImage);
            commandResponseObject.put(MessageTypes.TIMESTAMP, imageTime);
            commandResponseObject.put(MessageTypes.IMAGE_ID, imageId);

            //send the image to everyone else, so they can process it if they want it
            dataObservable.onNext(commandResponseObject);
        } catch (JSONException e){
            e.printStackTrace();
        }
    }

    public void sendSearchEngineResults(JSONObject results){
        try{
            //build json object to send command result
            JSONObject commandResponseObject = new JSONObject();
            commandResponseObject.put(MessageTypes.MESSAGE_TYPE_LOCAL, MessageTypes.SEARCH_ENGINE_RESULT);
            commandResponseObject.put(MessageTypes.SEARCH_ENGINE_RESULT_DATA, results.toString());

            //send the command result to web socket, to send to asg
            dataObservable.onNext(commandResponseObject);
        } catch (JSONException e){
            e.printStackTrace();
        }
    }

    public void sendVisualSearchResults(JSONArray results){
        try{
            //build json object to send command result
            JSONObject commandResponseObject = new JSONObject();
            commandResponseObject.put(MessageTypes.MESSAGE_TYPE_LOCAL, MessageTypes.VISUAL_SEARCH_RESULT);
            commandResponseObject.put(MessageTypes.VISUAL_SEARCH_DATA, results.toString());

            //send the command result to web socket, to send to asg
            dataObservable.onNext(commandResponseObject);
        } catch (JSONException e){
            e.printStackTrace();
        }
    }
    public void sendTranslateResults(String translatedText){
        try{
            //build json object to send command result
            JSONObject commandResponseObject = new JSONObject();
            commandResponseObject.put(MessageTypes.MESSAGE_TYPE_LOCAL, MessageTypes.TRANSLATE_TEXT_RESULT);
            commandResponseObject.put(MessageTypes.TRANSLATE_TEXT_RESULT_DATA, translatedText);

            //send the command result to web socket, to send to asg
            dataObservable.onNext(commandResponseObject);
        } catch (JSONException e){
            e.printStackTrace();
        }
    }

    public void sendReferenceTranslateResults(JSONObject results){
        try{
            //build json object to send command result
            JSONObject commandResponseObject = new JSONObject();
            commandResponseObject.put(MessageTypes.MESSAGE_TYPE_LOCAL, MessageTypes.SEARCH_ENGINE_RESULT);
            commandResponseObject.put(MessageTypes.SEARCH_ENGINE_RESULT_DATA, results.toString());

            //send the command result to web socket, to send to asg
            dataObservable.onNext(commandResponseObject);

            //speak translated text title
            speakTextToSpeechResults(results.getString("title"), results.getString("language"));
            //speak translated text summary
            speakTextToSpeechResults(results.getString("summary"), results.getString("language"));
        } catch (JSONException e){
            e.printStackTrace();
        }
    }

    public void speakTextToSpeechResults(String text, String languageCode){
        try{
            JSONObject textToBeSpokenObject = new JSONObject();
            textToBeSpokenObject.put(MessageTypes.MESSAGE_TYPE_LOCAL, MessageTypes.TEXT_TO_SPEECH_SPEAK);
            textToBeSpokenObject.put(MessageTypes.TEXT_TO_SPEECH_SPEAK_DATA, text);
            textToBeSpokenObject.put(MessageTypes.TEXT_TO_SPEECH_TARGET_LANGUAGE_CODE, languageCode);
            dataObservable.onNext(textToBeSpokenObject);
        }
        catch (JSONException e){
            e.printStackTrace();
        }
    }

    public void sendObjectTranslationResults(JSONObject results){
        try{
            //build json object to send command result
            JSONObject commandResponseObject = new JSONObject();
            commandResponseObject.put(MessageTypes.MESSAGE_TYPE_LOCAL, MessageTypes.OBJECT_TRANSLATION_RESULT);
            commandResponseObject.put(MessageTypes.OBJECT_TRANSLATION_RESULT_DATA, results.toString());

            //send the command result to web socket, to send to asg
            dataObservable.onNext(commandResponseObject);
        } catch (JSONException e){
            e.printStackTrace();
        }
    }

    public void startAsgConnection(){
        killme = false;

        startAsgWebSocketConnection();

        audioSystem = new AudioSystem(context, dataObservable);

        //start first socketThread
        Log.d(TAG, "running start socket");
        startSocket();
    }

    public void startAsgWebSocketConnection(){
        Log.d(TAG, "Starting WebSocket Server");
        //String address = "localhost:8887";
        //InetSocketAddress inetSockAddress = new InetSocketAddress(address);
        int port = 8887;
        asgWebSocket = new AspWebsocketServer(port);
        asgWebSocket.setObservable(dataObservable);
        asgWebSocket.start();
        Log.d(TAG, "WebSocket Server STARTED");
    }

    public void destroy(){
        Log.d(TAG, "ASG rep destroying");
        killme = true;

        //kill AudioSystem
        audioSystem.destroy();

        //kill asgWebSocket
        asgWebSocket.destroy();

        //stop sockets
        heart_beat_handler.removeCallbacksAndMessages(null);

        //kill this socket
//        try {
//            SocketThread.join();
//            SendThread.join();
//            ReceiveThread.join();
//        } catch (InterruptedException e){
//            e.printStackTrace();
//            Log.d(TAG, "Error waiting for threads to joing");
//        }

        killSocket();
        Log.d(TAG, "ASG rep destroy complete");

    }

    //SOCKET STUFF
    public void startSocket(){
        //start first socketThread
        Log.d(TAG, "socket val in startSocket: " + socket);
        if (socket == null) {
            Log.d(TAG, "starting new SocketThread" + socket);
            mConnectState = 1;
            SocketThread = new Thread(new SocketThread());
            SocketThread.start();

            //setup handler to handle keeping connection alive, all subsequent start of SocketThread
            //start a new handler thread to send heartbeats
            HandlerThread thread = new HandlerThread("HeartBeater");
            thread.start();
            heart_beat_handler = new Handler(thread.getLooper());
            final int hb_delay = 3000;
            final int min_hb_delay = 1000;
            final int max_hb_delay = 2000;
            Random rand = new Random();
            heart_beat_handler.postDelayed(new Runnable() {
                public void run() {
                    heartBeat();
                    //random hb_delay for heart beat so as to disallow synchronized failure between client and server
                    int random_hb_delay = rand.nextInt((max_hb_delay - min_hb_delay) + 1) + min_hb_delay;
                    heart_beat_handler.postDelayed(this, random_hb_delay);
                }
            }, hb_delay);

        } else {
            Log.d(TAG, "socket wasn't null, so not starting");
        }

    }

    class SocketThread implements Runnable {
        @Override
        public void run() {
            try {
                if (killme){
                    return;
                }
                Log.d(TAG, "Starting new socket, waiting for connection...");
                serverSocket = new ServerSocket(SERVER_PORT);
                //serverSocket.setSoTimeout(2000);
                try {
                    socket = serverSocket.accept();
                    if (killme){
                        return;
                    }
                    socket.setSoTimeout(10000);
                    Log.d(TAG, "Got socket connection.");
                    //output = new PrintWriter(socket.getOutputStream(), true);
                    output = new DataOutputStream(socket.getOutputStream());
                    input = new DataInputStream(new DataInputStream(socket.getInputStream()));
                    mConnectState = 2;
                    if (ReceiveThread == null) { //if the thread is null, make a new one (the first one)
                        ReceiveThread = new Thread(new ReceiveThread());
                        ReceiveThread.start();
                    } else if (!ReceiveThread.isAlive()) { //if the thread is not null but it's dead, let it join then start a new one
                        try {
                            ReceiveThread.join(); //make sure socket thread has joined before throwing off a new one
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        ReceiveThread = new Thread(new ReceiveThread());
                        ReceiveThread.start();
                    }
                    if (SendThread == null) { //if the thread is null, make a new one (the first one)
                    SendThread = new Thread(new SendThread());
                    SendThread.start();
                } else if (!SendThread.isAlive()) { //if the thread is not null but it's dead, let it join then start a new one
                    try {
                        SendThread.join(); //make sure socket thread has joined before throwing off a new one
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    SendThread =  new Thread(new SendThread());
                    SendThread.start();
                }
                } catch (IOException e) {
                    e.printStackTrace();
                    mConnectState = 0;
                }
            } catch (IOException e) {
                e.printStackTrace();
                mConnectState = 0;
            }
        }
    }

    //receives messages
    public void heartBeat(){
        //check if we are still connected.
        //if not , reconnect,
        //if we are connected, send a heart beat to make sure we are still connected
        if (mConnectState == 0 && !killme) {
            restartSocket();
        } else if (mConnectState == 2){
            //make sure we don't have a ton of outbound heart beats unresponded to
            if (outbound_heart_beats > 5) {
                restartSocket();
                return;
            }

            //increment counter
            outbound_heart_beats++;

            //send heart beat
            sendBytes(heart_beat_id, null);
        }
    }

    //receives messages
    public class ReceiveThread implements Runnable {
        @Override
        public void run() {
            //System.out.println("Receive Started, mconnect: " + mConnectState);
            while (true) {
                if (killme){
                    return;
                }
                if (mConnectState != 2){
                    break;
                }
                byte b1, b2;
                byte [] raw_data = null;
                byte goodbye1, goodbye2, goodbye3;
                try {
                    byte hello1 = input.readByte(); // read hello of incoming message
                    byte hello2 = input.readByte(); // read hello of incoming message
                    byte hello3 = input.readByte(); // read hello of incoming message

                    //make sure header is verified
                    if (hello1 != 0x01 || hello2 != 0x02 || hello3 != 0x03){
                        break;
                    }
                    //length of body
                    int body_len = input.readInt();

                    //read in message id bytes
                    b1 = input.readByte();
                    b2 = input.readByte();

                    //read in message body (if there is one)
                    if (body_len > 0){
                        raw_data = new byte[body_len];
                        input.readFully(raw_data, 0, body_len); // read the body
                    }
                    goodbye1 = input.readByte(); // read goodbye of incoming message
                    goodbye2 = input.readByte(); // read goodbye of incoming message
                    goodbye3 = input.readByte(); // read goodbye of incoming message
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }

                //make sure footer is verified
                if (goodbye1 != 0x03 || goodbye2 != 0x02 || goodbye3 != 0x01) {
                    break;
                }

                //now process the data that was sent to us
                if ((b1 == heart_beat_id[0]) && (b2 == heart_beat_id[1])){ //heart beat id tag
                    outbound_heart_beats--;
                } else if ((b1 == ack_id[0]) && (b2 == ack_id[1])){ //an ack id
                } else if ((b1 == img_id[0]) && (b2 == img_id[1])){ //an img id
                    if (raw_data != null) {
                        //remember the time we received it
                        long imageTime = System.currentTimeMillis();

                        //ping back the client to let it know we received the message
                        sendBytes(ack_id, null);

                        handleImage(raw_data, imageTime);
                    }
                } else {
                    break;
                }
            }
            throwBrokenSocket();
        }
    }

    //    public void handleImage(String raw_data_b64, long imageTime){
    //        //convert to jpg
    //        byte [] raw_data = Base64.decode(raw_data_b64, Base64.DEFAULT);
    public void handleImage(byte [] raw_data, long imageTime){
        //convert to bitmap
        Bitmap bitmap = BitmapFactory.decodeByteArray(raw_data, 0, raw_data.length);

        //save and process 1 image at set frequency
        long currTime = System.currentTimeMillis();
        if (((currTime - lastImageSave) / 1000) >= (1 / imageSaveFrequency)){ // divide by 1000 to convert to fps (per second) instead of per millisecond
            //save image
            Long imageId = FileUtils.savePicture(context, raw_data, imageTime, mMediaFileRepository);
            if (imageId == null){
                return;
            }
            lastImageSave = currTime;

            //send to everyone else
            sendPovImage(raw_data, imageId, imageTime);
        }
    }

    public void restartSocket(){
        mConnectState = 1;

        outbound_heart_beats = 0;

        //close the previous socket now that it's broken/being restarted
        killSocket();

        //make sure socket thread has joined before throwing off a new one
        try {
            SocketThread.join();
        } catch (InterruptedException e){
            e.printStackTrace();
        }

        //start a new socket thread
        SocketThread = new Thread(new SocketThread());
        SocketThread.start();
    }

    public void killSocket(){
        try {
            Log.d(TAG, "Closing socket, input, serverSocket, etc.");
            if (serverSocket != null && (!serverSocket.isClosed())) {
                serverSocket.close();
                serverSocket = null;
            }
            if (socket != null){
                socket.close();
                socket = null;
            }
            if (output != null){
                output.close();
                output = null;
            }
            if (input != null){
                input.close();
                input = null;
            }
        } catch (IOException e) {
            Log.d(TAG, "killSocket failed");
            e.printStackTrace();
        }
    }


    public byte[] my_int_to_bb_be(int myInteger){
        return ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(myInteger).array();
    }

    public void sendBytes(byte[] id, byte [] data){
        //first, send hello
        byte [] hello = {0x01, 0x02, 0x03};
        //then send length of body
        byte[] len;
        if (data != null) {
             len = my_int_to_bb_be(data.length);
        } else {
            len = my_int_to_bb_be(0);
        }
        //then send id of message type
        byte [] msg_id = id;
        //then send data
        byte [] body = data;
        //then send end tag - eventually make this unique to the image
        byte [] goodbye = {0x3, 0x2, 0x1};
        //combine those into a payload
        ByteArrayOutputStream outputStream;
        try {
            outputStream = new ByteArrayOutputStream();
            outputStream.write(hello);
            outputStream.write(len);
            outputStream.write(msg_id);
            if (body != null) {
                outputStream.write(body);
            }
            outputStream.write(goodbye);
        } catch (IOException e){
            mConnectState = 0;
            return;
        }
        byte [] payload = outputStream.toByteArray();

        //send it in a background thread
        //new Thread(new SendThread(payload)).start();
        queue.add(payload);
    }

    //this sends messages
     class SendThread implements Runnable {
        SendThread() {
        }
        @Override
        public void run() {
            queue.clear();
            while (true){
                if (killme){
                    return;
                }
                if (mConnectState != 2){
                    break;
                }
                if (queue.size() > 10){
                    break;
                }
                byte [] data;
                try {
                    data = queue.take(); //block until there is something we can pull out to send
                } catch (InterruptedException e){
                    e.printStackTrace();
                    break;
                }
                try {
                    output.write(data);           // write the message
                } catch (java.io.IOException e) {
                    e.printStackTrace();
                    break;
                }
            }
            throwBrokenSocket();
        }
    }

    public  void throwBrokenSocket(){
        if (mConnectState == 2){
            mConnectState = 0;
        }
    }

    //^^^ SOCKET STUFF



}
