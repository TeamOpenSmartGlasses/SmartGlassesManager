package com.example.wearableaidisplaymoverio;

import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadLocalRandom;

//singleton clientsocket class
public class ClientSocket {
    public static String TAG = "WearableAiDisplayMoverio";
    //singleton instance
    private static ClientSocket clientsocket;
    //socket data
    static Thread SocketThread = null;
    static Thread ReceiveThread = null;
    static Thread SendThread = null;
    static private DataOutputStream output;
    //static private BufferedReader input;
    static private DataInputStream input;
    static String SERVER_IP = "192.168.1.175"; //temporarily hardcoded
    static int SERVER_PORT = 4567;
    private static int mConnectState = 0;

    private static boolean gotAck = false;

    //our actual socket connection object
    private static Socket socket;

    //remember how many packets we have in our buffer
    private static int packets_in_buf = 0;

    //queue of data to send through the socket
    private static BlockingQueue<byte []> queue;

    private ClientSocket(){
        //create send queue and a thread to handle sendingo
        queue = new ArrayBlockingQueue<byte[]>(50);
    }

    public static ClientSocket getInstance(){
        if (clientsocket == null){
            clientsocket = new ClientSocket();
        }
        return clientsocket;
    }

    public void startSocket(){
        //start first socketThread
        if (socket == null) {
            mConnectState = 1;
            Log.d(TAG, "onCreate starting");
            SocketThread = new Thread(new SocketThread());
            SocketThread.start();
            Log.d(TAG, "STARTED");

            //setup handler to handle keeping connection alive, all subsequent start of SocketThread
            //start a new handler thread to send heartbeats
            HandlerThread thread = new HandlerThread("HeartBeater");
            thread.start();
            Handler handler = new Handler(thread.getLooper());
            final int delay = 1000;
            final int min_delay = 3000;
            final int max_delay = 4000;
            Random rand = new Random();
            handler.postDelayed(new Runnable() {
                public void run() {
                    heartBeat();
                    //random delay for heart beat so as to disallow synchronized failure between client and server
                    int random_delay = rand.nextInt((max_delay - min_delay) + 1) + min_delay;
                    handler.postDelayed(this, random_delay);
                }
            }, delay);
        }
    }

    private void heartBeat(){
        //check if we are still connected.
        //if not , reconnect,
        //we don't need to actively send heart beats from the client, as it's assumed that we are ALWAYS streaming data. Later, if we have periods of time where no data is sent, we will want to send a heart beat perhaps. but the client doesn't really need to, we just need to check if we are still connected
        if (mConnectState == 0) {
            restartSocket();
        }
    }

    public static void restartSocket() {
        Log.d(TAG, "Restarting socket");
        mConnectState = 1;
        if (socket != null && (!socket.isClosed())){
            try {
                output.close();
                input.close();
                socket.close();
            } catch (IOException e) {
                System.out.println("FAILED TO CLOSE SOCKET, SOMETHING IS WRONG");
            }
        }


//        //kill threads
//        stopThread(SendThread);
//        stopThread(ReceiveThread);

        //restart socket thread
        SocketThread = new Thread(new SocketThread());
        SocketThread.start();
    }

    public static void stopThread(Thread thread){
        if(thread!=null){
            thread.interrupt();
            thread = null;
        }
    }

    public static  byte[] my_int_to_bb_be(int myInteger){
        return ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(myInteger).array();
    }

    public void sendBytes(byte [] data){
        Log.d(TAG, "QUEUEING DATA OF LENGTH: " + data.length);
        //first, send hello
        byte [] hello = {0x01, 0x02, 0x03};
        //then send length of body
        byte [] len = my_int_to_bb_be(data.length);
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
            outputStream.write(body);
            outputStream.write(goodbye);
        } catch (IOException e){
            mConnectState = 0;
            return;
        }
        byte [] payload = outputStream.toByteArray( );

        //send it in a background thread
        //new Thread(new SendThread(payload)).start();
        queue.add(payload);
    }

    public int getConnected(){
        return mConnectState;
    }

    static class SocketThread implements Runnable {
        public void run() {
            try {
                System.out.println("TRYING TO CONNECT");
                socket = new Socket(SERVER_IP, SERVER_PORT);
                System.out.println("CONNECTED!");
                output = new DataOutputStream(socket.getOutputStream());
                //input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                input = new DataInputStream(new DataInputStream(socket.getInputStream()));
                mConnectState = 2;
                Log.d(TAG, "SET MCONNECT STATE TO 2");
                //make the threads that will send and receive
                if (ReceiveThread == null) { //if the thread is null, make a new one (the first one)
                    ReceiveThread = new Thread(new ReceiveThread());
                    ReceiveThread.start();
                } else if (!ReceiveThread.isAlive()) { //if the thread is not null but it's dead, let it join then start a new one
                    Log.d(TAG, "IN SocketThread< WAITING FOR receive THREAD JOING");
                    try {
                        ReceiveThread.join(); //make sure socket thread has joined before throwing off a new one
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    Log.d(TAG, "receive JOINED");
                    ReceiveThread = new Thread(new ReceiveThread());
                    ReceiveThread.start();
                }
                if (SendThread == null) { //if the thread is null, make a new one (the first one)
                    SendThread = new Thread(new SendThread());
                    SendThread.start();
                } else if (!SendThread.isAlive()) { //if the thread is not null but it's dead, let it join then start a new one
                    Log.d(TAG, "IN SocketThread< WAITING FOR send THREAD JOING");
                    try {
                        SendThread.join(); //make sure socket thread has joined before throwing off a new one
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    Log.d(TAG, "send JOINED");
                    SendThread =  new Thread(new SendThread());
                    SendThread.start();
                }
            } catch (IOException e) {
                Log.d(TAG, "Connection Refused on socket");
                e.printStackTrace();
                mConnectState = 0;
            }
        }
    }

    static class ReceiveThread implements Runnable {
        @Override
        public void run() {
            while (true) {
                if (mConnectState != 2){
                    System.out.println("MCONNECTED IS FALSE IN REEIVE THREAD, BREAKING");
                    break;
                }
                try {
                    byte b1 = input.readByte();
                    byte b2 = input.readByte();
                    if ((b1 == 0x13) && (b2 == 0x37)){ //got ack response
                        System.out.println("ACK RECEIVED");
                        gotAck = true;
                    } else if ((b1 == 0x19) && (b2 == 0x20)) { //heart beat check if alive
                        //got heart beat, respond with heart beat
                        byte [] hb = {0x19, 0x20};
                        clientsocket.sendBytes(hb);
                    } else {
                        System.out.println("BAD SIGNAL, RECONNECT");
                        mConnectState = 0;
                        break;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    mConnectState = 0;
                    break;
                }
            }
        }
    }
    static class SendThread implements Runnable {
        SendThread() {
        }
        @Override
        public void run() {
            //clear queue so we don't have a buildup of images
            queue.clear();
            while (true) {
                if (packets_in_buf > 5) { //if 5 packets in buffer (NOT QUEUE, BUF NETWORK BUFFER), restart socket
                    break;
                }
                byte[] data;
                try {
                    data = queue.take(); //block until there is something we can pull out to send
                } catch (InterruptedException e){
                    e.printStackTrace();
                    break;
                }
                try {
                    packets_in_buf++;
                    output.writeInt(data.length); // write length of the message
                    output.write(data);           // write the message
                    packets_in_buf--;
                } catch (java.io.IOException e) {
                    e.printStackTrace();
                    break;
                }
            }
            mConnectState = 0;
        }
    }

}
