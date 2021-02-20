package com.example.wearableaimobilecompute;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

@SuppressLint("SetTextI18n")
public class MainActivity extends AppCompatActivity {
    public String TAG = "WearableAiMobileCompute";
    ServerSocket serverSocket;
    Thread SocketThread = null;
    static Thread ReceiveThread = null;
    static Thread SendThread = null;
    TextView tvIP, tvPort;
    TextView tvMessages;
    EditText etMessage;
    Button btnSend;
    public static String SERVER_IP = "";
    public static final int SERVER_PORT = 4567;
    String message;
    private DataOutputStream output;
    private DataInputStream input;
    byte [] image_data_rcv_jpg;
    ImageView wearcam_view;

    private static int mConnectState = 0;

    private static int outbound_heart_beats = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvIP = findViewById(R.id.tvIP);
        tvPort = findViewById(R.id.tvPort);
        tvMessages = findViewById(R.id.tvMessages);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
        try {
            SERVER_IP = getLocalIpAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        //start first socketThread
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
        handler.postDelayed(new Runnable() {
            public void run() {
                heartBeat();
                handler.postDelayed(this, delay);
            }
        }, delay);

        //setup image view
        wearcam_view = (ImageView)findViewById(R.id.imageView);
    }

    private String getLocalIpAddress() throws UnknownHostException {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        assert wifiManager != null;
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        int ipInt = wifiInfo.getIpAddress();
        return InetAddress.getByAddress(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(ipInt).array()).getHostAddress();
    }

    class SocketThread implements Runnable {
        @Override
        public void run() {
            Log.d(TAG, "STARTED NEW SOCKET THREAD");
            Socket socket;
            try {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tvMessages.setText("Not connected");
                        tvIP.setText("IP: " + SERVER_IP);
                        tvPort.setText("Port: " + String.valueOf(SERVER_PORT));
                    }
                });

                Log.d(TAG, "LISTENING FOR CONNECTIONS");
                serverSocket = new ServerSocket(SERVER_PORT);
                Log.d(TAG, "SOCKET MADE");
                try {
                    socket = serverSocket.accept();
                    Log.d(TAG, "CONNECTION MADE");
                    //output = new PrintWriter(socket.getOutputStream(), true);
                    output = new DataOutputStream(socket.getOutputStream());
                    input = new DataInputStream(new DataInputStream(socket.getInputStream()));
                    mConnectState = 2;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            tvMessages.setText("Connected\n");
                        }
                    });
                    if (ReceiveThread != null && !ReceiveThread.isAlive()) {
                        Log.d(TAG, "IN SocketThread< WAITING FOR receive THREAD JOING");
                        try {
                            ReceiveThread.join(); //make sure socket thread has joined before throwing off a new one
                        } catch (InterruptedException e){
                            e.printStackTrace();
                        }
                        Log.d(TAG, "receive JOINED");
                    }
                    ReceiveThread = new Thread(new ReceiveThread());
                    ReceiveThread.start();
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

    public static int my_bb_to_int_le(byte [] byteBarray){
        return ByteBuffer.wrap(byteBarray).order(ByteOrder.LITTLE_ENDIAN).getInt();
    }

    private void heartBeat(){
        //check if we are still connected.
        //if not , reconnect,
        //if we are connected, send a heart beat to make sure we are still connected
        if (mConnectState == 0) {
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
            byte[] hb = {0x19, 0x20};
            new Thread(new SendThread(hb)).start();
        }
    }

    //receives messages
    private class ReceiveThread implements Runnable {
        boolean firstTime = true;
        boolean abort = false;
        @Override
        public void run() {
            System.out.println("Receive Started, mconnect: " + mConnectState);
            while (true) {
                if (mConnectState != 2){
                    break;
                }
                System.out.println("LISTENING FOR MESSAGES" + firstTime);
                firstTime = false;
                try {
                    int claimed_length = input.readInt();
                    Log.d(TAG,"CLAIMED LENGTH IS " + claimed_length);
                    byte hello1 = input.readByte(); // read hello of incoming message
                    byte hello2 = input.readByte(); // read hello of incoming message
                    byte hello3 = input.readByte(); // read hello of incoming message
                    if (hello1 != 0x01 || hello2 != 0x02 || hello3 != 0x03){
                        Log.d(TAG, "JPG stream - header broken, restarting socket");
                        break;
                    }
                    byte [] len_b = new byte[4];
                    int len = input.readInt(); // read length of incoming message, integer, so 4 bytes
                    //int len = my_bb_to_int_le(len_b);
                    System.out.println("LENGTH IS " + len);
                    if (len > 0){
                        byte[] raw_data = new byte[len];
                        input.readFully(raw_data, 0, len); // read the body
                        if (len <= 10000){ //heart beat or other information sending, not an image
                            if ((raw_data[0] == 0x19) && (raw_data[1] == 0x20)){
                                Log.d(TAG, "HEART BEAT RECEIVED");
                                outbound_heart_beats--;
                            }
                         } else if (len > 10000){ //must be an image if bigger than 10k
                            System.out.println("RECEIVED MESSAGE");
                            if (raw_data != null) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        System.out.println("TRYING TO DISPLAY");
                                        //display image
                                        Bitmap bitmap = BitmapFactory.decodeByteArray(raw_data, 0, raw_data.length);
                                        wearcam_view.setImageBitmap(bitmap); //Must.jpg present in any of your drawable folders.
                                        System.out.println("SET DISPLAYED");
                                    }
                                });

                                //ping back the client to let it know we received the message
                                byte[] ack = {0x13, 0x37};
                                new Thread(new SendThread(ack)).start();
                                //save image
                                //savePicture(raw_data);
                            }
                        }
                        byte goodbye1 = input.readByte(); // read goodbye of incoming message
                        byte goodbye2 = input.readByte(); // read goodbye of incoming message
                        byte goodbye3 = input.readByte(); // read goodbye of incoming message
                        System.out.println("GOODBYE 1 IS " + goodbye1);
                        System.out.println("GOODBYE 2 IS " + goodbye2);
                        System.out.println("GOODBYE 3 IS " + goodbye3);
                        if (goodbye1 != 0x03 || goodbye2 != 0x02 || goodbye3 != 0x01) {
                            Log.d(TAG, "JPG stream - footer broken, restarting socket");
                            break;
                        }
                    } else {
                        break;
                    }
                } catch (IOException e) {
                    Log.d(TAG, "FAILED TO RECEIVE");
                    break;
                }

            }
            mConnectState = 0;
        }
    }

    private void restartSocket(){
        Log.d(TAG, "RESARTING SOCKET");
        mConnectState = 1;

        outbound_heart_beats = 0;

        //close the previous socket now that it's broken/being restarted
        try {
            if (serverSocket != null && (!serverSocket.isClosed())) {
                output.close();
                input.close();
                serverSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        //make sure socket thread has joined before throwing off a new one
        Log.d(TAG, "IN RESTART< WAITING FOR SOCKET THREAD JOING");
        try {
            SocketThread.join();
        } catch (InterruptedException e){
            e.printStackTrace();
        }
        Log.d(TAG, "JOINED");

        //start a new socket thread
        SocketThread = new Thread(new SocketThread());
        SocketThread.start();
    }

    //this sends messages
    class SendThread implements Runnable {
        private byte [] message;
        SendThread(byte [] message) {
            this.message = message;
        }
        @Override
        public void run() {
            try {
                output.write(message);
                output.flush();
            } catch (IOException e){
                Log.d(TAG, "SOCKET DEAD - FAILED TO WRITE OUT DATA");
                mConnectState = 0; //set to "trying" state if this fails
            }
        }
    }

    private void savePicture(byte[] data){
//        byte[] data = Base64.encodeToString(ata, Base64.DEFAULT).getBytes();
        File pictureFileDir = getDir();
        System.out.println("TRYING TO SAVE AT LOCATION: " + pictureFileDir.toString());

        if (!pictureFileDir.exists() && !pictureFileDir.mkdirs()) {
            Log.d("PHOTO_HANDLER", "Can't create directory to save image.");
            return;

        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy_mm_dd_hh_mm_ss_SSS");
        String date = dateFormat.format(new Date());
        String photoFile = "Picture_" + date + ".jpg";

        String filename = pictureFileDir.getPath() + File.separator + photoFile;

        File pictureFile = new File(filename);

        try {
            FileOutputStream fos = new FileOutputStream(pictureFile);
            fos.write(data);
            fos.close();
        } catch (Exception error) {
            Log.d(TAG, "File" + filename + "not saved: "
                    + error.getMessage());
        }
    }

    private File getDir() {
        File sdDir = Environment
                .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        return sdDir; //new File(sdDir, "WearableAiMobileCompute");
    }
}

