package com.example.wearableaidisplaymoverio;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import android.app.Activity;
import android.hardware.Camera;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.PrintWriter;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import android.view.Window;
import android.view.WindowManager;

public class MainActivity extends Activity {
    public String TAG = "WearableAiDisplay";

    //UI
    TextView messageTextView;
    TextView eyeContactMetricTextView;
    TextView facialEmotionMetricTextView;
    Button toggleCameraButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //print a bunch of stuff we can see in logcat
        for(int i = 0; i < 20; i++){
            System.out.println("WEARABLEAI");
        }

        //set full screen for Moverio
        long FLAG_SMARTFULLSCREEN = 0x80000000;
        Window win = getWindow();
        WindowManager.LayoutParams winParams = win.getAttributes();
        winParams.flags |= FLAG_SMARTFULLSCREEN;
        win.setAttributes(winParams);

        //keep the screen on throughout
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        //hint, use this to allow it to turn off:


        setContentView(R.layout.activity_main);

        //ui setup
        messageTextView = (TextView) findViewById(R.id.message);
        eyeContactMetricTextView = (TextView) findViewById(R.id.eye_contact_metric);
        facialEmotionMetricTextView = (TextView) findViewById(R.id.facial_emotion_metric);
        toggleCameraButton = (Button) findViewById(R.id.toggle_camera_button);

        //create the camera service if it isn't already running
        startService(new Intent(this, CameraService.class));

//        //setup camera preview
//        preview = (SurfaceView) findViewById(R.id.preview);
//        previewHolder = preview.getHolder();
//        previewHolder.addCallback(surfaceCallback);
//        previewHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        //setup take picture button
//        takePictureButton = (Button) findViewById(R.id.captureImage);
//        takePictureButton.setOnClickListener(new View.OnClickListener() {
//            public void onClick(View v) {
//                takeAndSendPicture();
//            }
//        });
    }

    @Override
    public void onResume() {
        super.onResume();

        registerReceiver(mComputeUpdateReceiver, makeComputeUpdateIntentFilter());
    }

    @Override
    public void onPause() {
        super.onPause();

        //unregister receiver
        unregisterReceiver(mComputeUpdateReceiver);
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
//
//    private void initPreview(int width, int height) {
//        if (camera != null && previewHolder.getSurface() != null) {
//            try {
//                camera.setPreviewDisplay(previewHolder);
//            } catch (Throwable t) {
//                Log.e("PreviewDemo",
//                        "Exception in setPreviewDisplay()", t);
//                Toast
//                        .makeText(MainActivity.this, t.getMessage(), Toast.LENGTH_LONG)
//                        .show();
//            }
//
//            if (!cameraConfigured) {
//                Camera.Parameters parameters = camera.getParameters();
//                parameters.setRecordingHint(true);
//                Camera.Size size = getBestPreviewSize(width, height,
//                        parameters);
//
//                if (size != null) {
//                    parameters.setPreviewSize(size.width, size.height);
//                    camera.setParameters(parameters);
//                    cameraConfigured = true;
//                }
//            }
//        }
//    }
//
//    private void startPreview() {
//        if (cameraConfigured && camera != null) {
//            camera.startPreview();
//            inPreview = true;
//        }
//    }
//
//    private void takeAndSendPicture() {
//        System.out.println("TAKE AND SEND PICTURE CALLED");
//        camera.takePicture(null, null, null, new PhotoHandler(getApplicationContext()));
//        startPreview();
//    }
//
//    SurfaceHolder.Callback surfaceCallback = new SurfaceHolder.Callback() {
//        public void surfaceCreated(SurfaceHolder holder) {
//            // no-op -- wait until surfaceChanged()
//        }
//
//        public void surfaceChanged(SurfaceHolder holder,
//                                   int format, int width,
//                                   int height) {
//            initPreview(width, height);
//            startPreview();
//        }
//
//        public void surfaceDestroyed(SurfaceHolder holder) {
//            // no-op
//        }
//    };
//
    public void receiveEyeContactMessage(String message){
        //see if the message is generic or one of the metrics to be displayed
        messageTextView.setText("");
        eyeContactMetricTextView.setText(message + "%");
    }

    public void receiveFacialEmotionMessage(String message){
        //see if the message is generic or one of the metrics to be displayed
        messageTextView.setText("");
        facialEmotionMetricTextView.setText(message);
    }

    private static IntentFilter makeComputeUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ClientSocket.ACTION_RECEIVE_MESSAGE);
        return intentFilter;
    }

    private final BroadcastReceiver mComputeUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (ClientSocket.ACTION_RECEIVE_MESSAGE.equals(action)) {
                if (intent.hasExtra(ClientSocket.EYE_CONTACT_MESSAGE)) {
                    String message = intent.getStringExtra(ClientSocket.EYE_CONTACT_MESSAGE);
                    receiveEyeContactMessage(message);
                } else if (intent.hasExtra(ClientSocket.FACIAL_EMOTION_MESSAGE)){
                    String message = intent.getStringExtra(ClientSocket.FACIAL_EMOTION_MESSAGE);
                    receiveFacialEmotionMessage(message);
                }
            }
        }
    };



}

