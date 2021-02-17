package com.example.wearableaidisplaymoverio;

import android.app.Service;
import android.content.Intent;
import android.hardware.Camera;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import java.io.IOException;

public class CameraService extends Service {
    public String TAG = "WearableAiDisplay";
    //camera
    private Camera camera = null;
    private boolean camera_lock = false;

    //socket class instance
    ClientSocket clientsocket;

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    void sendPictures() {
        //start camera preview, must be done even though we are not showing a preview
        camera.startPreview();

        //startup a task that will take and send a picture every 10 seconds
        final Handler handler = new Handler();
        final int init_camera_delay = 5000; // 1000 milliseconds
        final int delay = 5000; // = 2Hz

        handler.postDelayed(new
            Runnable() {
                public void run () {
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (!camera_lock) {
                                camera_lock = true;
                                takeAndSendPicture();
                            }
                            handler.postDelayed(this, delay);
                        }
                    }, delay);
                }
            }, init_camera_delay);
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "FUUUUUUUUUUUUUUUUUUUUUUUUCCCCCCCCCCCCKKKKKKKKKKYYYYYYYYYYEEEEAAAAAAHHHHHHHh");
        System.out.println("CAAAAAYYYYYYYYYYYYYYYY");
        startSocket();
        startCamera();
    }

    @Override
    public void onDestroy() {
        closeCamera();
        //closeSocket();
    }

    private void startSocket(){
        //create client socket and setup socket
        clientsocket = ClientSocket.getInstance();
        clientsocket.startSocket();
    }


    private void takeAndSendPicture() {
        System.out.println("TAKE AND SEND PICTURE CALLED");
        camera.takePicture(null, null, null, new PhotoHandler(null));
    }


    void startCamera(){
        camera = Camera.open();
        Camera.Parameters parameters = camera.getParameters();
        parameters.setRecordingHint(true);
        parameters.setPreviewSize(640, 480);
        camera.setParameters(parameters);
        sendPictures();
    }

    void closeCamera(){
            camera.release();
            camera = null;
        }

    private Camera.Size getBestPreviewSize(int width, int height,
                                           Camera.Parameters parameters) {
        Camera.Size result = null;

        for (Camera.Size size : parameters.getSupportedPreviewSizes()) {
            if (size.width <= width && size.height <= height) {
                if (result == null) {
                    result = size;
                } else {
                    int resultArea = result.width * result.height;
                    int newArea = size.width * size.height;

                    if (newArea > resultArea) {
                        result = size;
                    }
                }
            }
        }

        return (result);
    }

}
