package com.example.wearableaimobilecompute;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

@SuppressLint("SetTextI18n")
public class MainActivity extends AppCompatActivity {
    ServerSocket serverSocket;
    Thread SocketThread = null;
    TextView tvIP, tvPort;
    TextView tvMessages;
    EditText etMessage;
    Button btnSend;
    public static String SERVER_IP = "";
    public static final int SERVER_PORT = 4567;
    String message;
    private PrintWriter output;
    private DataInputStream input;
    byte [] image_data_rcv_jpg;
    ImageView wearcam_view;

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
        SocketThread = new Thread(new SocketThread());
        SocketThread.start();
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                message = etMessage.getText().toString().trim();
                if (!message.isEmpty()) {
                    new Thread(new SendThread(message)).start();
                }
            }
        });

        //setup image view
        // New Code
        wearcam_view = (ImageView)findViewById(R.id.imageView); //Assuming an ImgView is there in your layout activity_main
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
            Socket socket;
            try {
                serverSocket = new ServerSocket(SERVER_PORT);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tvMessages.setText("Not connected");
                        tvIP.setText("IP: " + SERVER_IP);
                        tvPort.setText("Port: " + String.valueOf(SERVER_PORT));
                    }
                });
                try {
                    socket = serverSocket.accept();
                    output = new PrintWriter(socket.getOutputStream(), true);
                    input = new DataInputStream(new DataInputStream(socket.getInputStream()));
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            tvMessages.setText("Connected\n");
                        }
                    });
                    new Thread(new ReceiveThread()).start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //receives messages
    private class ReceiveThread implements Runnable {
        @Override
        public void run() {
            while (true) {
                System.out.println("LISTENING FOR MESSAGES");
                try {
                    int length = input.readInt();                    // read length of incoming message
                    byte[] raw_data = new byte[length];
                    if(length>0) {
                        input.readFully(raw_data, 0, raw_data.length); // read the message
                    }

                    if (raw_data != null) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                //display image
                                Bitmap bitmap = BitmapFactory.decodeByteArray(raw_data, 0, raw_data.length);
                                wearcam_view.setImageBitmap(bitmap); //Must.jpg present in any of your drawable folders.
                            }
                        });
                    } else {
                        SocketThread = new Thread(new SocketThread());
                        SocketThread.start();
                        return;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }
    }

    //this sends messages
    class SendThread implements Runnable {
        private String message;
        SendThread(String message) {
            this.message = message;
        }
        @Override
        public void run() {
            output.write(message + "\n");
            output.flush();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    tvMessages.append("server: " + message + "\n");
                    etMessage.setText("");
                }
            });
        }
    }
}

