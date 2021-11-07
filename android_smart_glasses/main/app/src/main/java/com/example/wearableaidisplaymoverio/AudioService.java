package com.example.wearableaidisplaymoverio;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.wearableaidisplaymoverio.utils.AES;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.SecretKeySpec;

public class AudioService extends Service {
    //encryption key - TEMPORARILY HARD CODED - change to local storage, user can set
    private String secretKey;

    // Binder given to clients
    private final IBinder binder = new AudioService.LocalBinder();

    public static final String CHANNEL_ID = "AudioServiceChannel";

    //socket info
    static String SERVER_IP = "18.191.190.218";
    static int SERVER_PORT = 4449;
    private static int mConnectState = 0;

    private static String TAG = "WearableAi_AudioService";


    // the audio recording options
    private static final int RECORDING_RATE = 16000;
    private static final int CHANNEL = AudioFormat.CHANNEL_IN_MONO;
    private static final int FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    // the audio recorder
    private AudioRecord recorder;
    // the minimum buffer size needed for audio recording
    private static int BUFFER_SIZE = AudioRecord.getMinBufferSize(RECORDING_RATE, CHANNEL, FORMAT);
    // are we currently sending audio data
    private boolean currentlySendingAudio = false;

    //socket data
    static Thread SocketThread = null;
    static Thread SendThread = null;
    static private DataOutputStream output;

    //our actual socket connection object
    private static Socket socket;

    //remember how many packets we have in our buffer
    private static int packets_in_buf = 0;

    //queue of data to send through the socket
    private static BlockingQueue<byte []> data_queue;
//    private static BlockingQueue<String> type_queue;
    private static int queue_size = 1024;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_NOT_STICKY;
    }

    @Override
    public void onCreate(){
        super.onCreate();

        secretKey = getResources().getString(R.string.key);

        //setup data
        //create send queue and a thread to handle sending
        data_queue = new ArrayBlockingQueue<byte[]>(queue_size);



        //setup notification
//        Log.d(TAG, "STARTING AUDIO SERVICE");
//        String input = intent.getStringExtra("inputExtra");
//        createNotificationChannel();
//        Intent notificationIntent = new Intent(this, MainActivity.class);
//        PendingIntent pendingIntent = PendingIntent.getActivity(this,
//                0, notificationIntent, 0);
//
//        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
//                .setContentTitle("WearableAI Audio Service")
//                .setContentText(input)
//                .setSmallIcon(R.drawable.audio_recording_icon_small)
//                .setContentIntent(pendingIntent)
//                .build();
//
//        startForeground(1, notification);

        //start the socket thread which will send the raw audio data
        startSocket();

        // do heavy work on a background thread
        StartRecorder();

        //stopSelf();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Foreground Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    //SOCKET stuff
    public void startSocket(){
        //start first socketThread
        if (socket == null) {
            mConnectState = 1;
            Log.d(TAG, "starting AudioService socket");
            SocketThread = new Thread(new AudioService.SocketThread());
            SocketThread.start();

            //setup handler to handle keeping connection alive, all subsequent start of SocketThread
            //start a new handler thread to send heartbeats
            HandlerThread thread = new HandlerThread("HeartBeater_AudioService");
            thread.start();
            Handler handler = new Handler(thread.getLooper());
            final int delay = 3000;
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

    //heart beat checker - check if we have received a heart rate
    private void heartBeat(){
        //check if we are still connected.
        //if not , reconnect,
        //we don't need to actively send heart beats from the client, as it's assumed that we are ALWAYS streaming data. Later, if we have periods of time where no data is sent, we will want to send a heart beat perhaps. but the client doesn't really need to, we just need to check if we are still connected
        if (mConnectState == 0) {
            restartSocket();
        }

//        //or, if haven't been receiving heart beats, restart socket
//        if (mConnectState == 2) {
//            if ((System.currentTimeMillis() - lastHeartbeatTime) > (heartbeatInterval * heartbeatPanicX)) {
//                Log.d(TAG, "DIDN'T RECEIVE HEART BEATS, RESTARTING SOCKET");
//                mConnectState = 0;
//                restartSocket();
//            }
//        }
    }


    public static void restartSocket() {
        Log.d(TAG, "Restarting socket");
        mConnectState = 1;
        if (socket != null && (!socket.isClosed())) {
            try {
                output.close();
                socket.close();
            } catch (IOException e) {
                System.out.println("FAILED TO CLOSE SOCKET, SOMETHING IS WRONG");
            }
        }

        //restart socket thread
        Log.d(TAG, "starting socket");
        SocketThread = new Thread(new AudioService.SocketThread());
        SocketThread.start();
    }

    public byte [] encryptBytes(byte [] input){
        byte [] encryptedBytes = AES.encrypt(input, secretKey) ;
//        String encryptedString = AES.encrypt(originalString, secretKey) ;
//        String decryptedString = AES.decrypt(encryptedString, secretKey) ;
//
//        System.out.println(originalString);
//        System.out.println(encryptedString);
//        System.out.println(decryptedString);
        return encryptedBytes;
    }


    public byte [] decryptBytes(byte [] input) {
        byte [] decryptedBytes = AES.decrypt(input, secretKey) ;
        return decryptedBytes;
    }

    public void sendBytes(byte [] data){
        //only try to send data if the socket is connected state
        if (mConnectState != 2){
            return;
        }

        //combine those into a payload
        ByteArrayOutputStream outputStream;
        try {
            outputStream = new ByteArrayOutputStream();
            outputStream.write(data);
        } catch (IOException e){
            mConnectState = 0;
            return;
        }
        byte [] payload = outputStream.toByteArray();

        //send it in a background thread
        try {
            data_queue.add(payload);
        } catch (IllegalStateException e) {
            e.printStackTrace();
            Log.d(TAG, "Queue is full, skipping this one");
        }
    }

    static class SocketThread implements Runnable {
        public void run() {
            try {
                System.out.println("TRYING TO CONNECT AudioService server");
                socket = new Socket(SERVER_IP, SERVER_PORT);
//                lastHeartbeatTime = System.currentTimeMillis();
                System.out.println("AudioService server CONNECTED!");
                output = new DataOutputStream(socket.getOutputStream());
                mConnectState = 2;
                //make the thread that will send
                if (SendThread == null) { //if the thread is null, make a new one (the first one)
                    SendThread = new Thread(new AudioService.SendThread());
                    SendThread.start();
                } else if (!SendThread.isAlive()) { //if the thread is not null but it's dead, let it join then start a new one
                    Log.d(TAG, "IN SocketThread, WAITING FOR send THREAD JOING");
                    try {
                        SendThread.join(); //make sure socket thread has joined before throwing off a new one
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    Log.d(TAG, "send JOINED");
                    SendThread =  new Thread(new AudioService.SendThread());
                    SendThread.start();
                }
            } catch (IOException e) {
                Log.d(TAG, "Connection Refused on socket");
                e.printStackTrace();
                mConnectState = 0;
            }
        }
    }

    static class SendThread implements Runnable {
        SendThread() {
        }
        @Override
        public void run() {
            if (mConnectState != 2){
                System.out.println("MCONNECTED IS FALSE IN AudioService SendThread, returning");
                return;
            }
            //clear queue so we don't have a buildup of data
            data_queue.clear();
            while (true) {
                if (packets_in_buf > 20) { //if 20 packets in buffer (NOT QUEUE, BUF NETWORK BUFFER), restart socket
                    break;
                }
                byte[] data;
                try {
                    data = data_queue.take(); //block until there is something we can pull out to send
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
            mConnectState = 0;
        }
    }


    //AUDIO stuff
    public void StartRecorder() {
        Log.i(TAG, "Starting the audio stream");
        currentlySendingAudio = true;
        startStreaming();
    }
    public void StopRecorder() {
        Log.i(TAG, "Stopping the audio stream");
        currentlySendingAudio = false;
        recorder.release();
    }

    private void startStreaming() {
        Log.i(TAG, "Starting the background thread (in this foreground service) to read the audio data");

        Thread streamThread = new Thread(() -> {
            try {
                Log.d(TAG, "Creating the buffer of size " + BUFFER_SIZE);
                //byte[] buffer = new byte[BUFFER_SIZE];
                int rate = AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_SYSTEM);
                int bufferSize = AudioRecord.getMinBufferSize(RECORDING_RATE, CHANNEL, FORMAT);
                short[] buffer = new short[bufferSize];

                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);

                Log.d(TAG, "Creating the AudioRecord");
                recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, RECORDING_RATE, CHANNEL, FORMAT, BUFFER_SIZE * 10);
                //recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, rate, CHANNEL, FORMAT, bufferSize);

                Log.d(TAG, "AudioRecord recording...");
                recorder.startRecording();

                while (currentlySendingAudio == true) {
                    // read the data into the buffer
                    int readSize = recorder.read(buffer, 0, buffer.length);

                    //convert short array to byte array
                    ByteBuffer b_buffer = ByteBuffer.allocate(buffer.length * 2);
                    b_buffer.order(ByteOrder.LITTLE_ENDIAN);
                    b_buffer.asShortBuffer().put(buffer);
                    byte[] audio_bytes = b_buffer.array();

                    byte [] encrypted_audio_bytes = encryptBytes(audio_bytes);

                    sendBytes(encrypted_audio_bytes);

//                    double maxAmplitude = 0;
//                    for (int i = 0; i < readSize; i++) {
//                        if (Math.abs(buffer[i]) > maxAmplitude) {
//                            maxAmplitude = Math.abs(buffer[i]);
//                        }
//                    }
//
//                    double db = 0;
//                    if (maxAmplitude != 0) {
//                        db = 20.0 * Math.log10(maxAmplitude / 32767.0) + 90;
//                    }
//
//                    Log.d(TAG, "Max amplitude: " + maxAmplitude + " ; DB: " + db);
                }

                Log.d(TAG, "AudioRecord finished recording");
            } catch (Exception e) {
                Log.e(TAG, "Exception: " + e);
            }
        });



        // start the thread
        streamThread.start();
    }


    public class LocalBinder extends Binder {
        AudioService getService() {
            // Return this instance of LocalService so clients can call public methods
            return AudioService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

}