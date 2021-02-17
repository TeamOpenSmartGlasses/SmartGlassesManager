package com.example.wearableaidisplaymoverio;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

//singleton clientsocket class
public class ClientSocket {
    //singleton instance
    private static ClientSocket clientsocket;
    //socket data
    static Thread SocketThread = null;
    static private DataOutputStream output;
    static private BufferedReader input;
    static String SERVER_IP = "192.168.1.175"; //temporarily hardcoded
    static int SERVER_PORT = 4567;

    private ClientSocket(){
        //pass
    }

    public static ClientSocket getInstance(){
        if (clientsocket == null){
            clientsocket = new ClientSocket();
        }
        return clientsocket;
    }

    public void startSocket(){
        SocketThread = new Thread(new SocketThread());
        SocketThread.start();
    }

    public void sendBytes(byte [] data){
        new Thread(new SendThread(data)).start();
    }

    static class SocketThread implements Runnable {
        public void run() {
            Socket socket;
            try {
                System.out.println("TRYING TO CONNECT");
                socket = new Socket(SERVER_IP, SERVER_PORT);
                System.out.println("CONNECTED!");
                output = new DataOutputStream(socket.getOutputStream());
                input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                new Thread(new ReceiveThread()).start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    static class ReceiveThread implements Runnable {
        @Override
        public void run() {
            while (true) {
                try {
                    final String message = input.readLine();
                    System.out.println("MESSAGE RECEIVED");
                    System.out.println(message);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    static class SendThread implements Runnable {
        private byte [] image_data;
        SendThread(byte [] image_data) {
            this.image_data = image_data;
        }
        @Override
        public void run() {
            try {
                output.writeInt(image_data.length); // write length of the message
                output.write(image_data);           // write the message
            } catch (java.io.IOException e){
                System.out.println(e);
            }
        }
    }

}
