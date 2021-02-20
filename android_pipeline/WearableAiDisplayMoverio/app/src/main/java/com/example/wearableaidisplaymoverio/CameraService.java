package com.example.wearableaidisplaymoverio;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Environment;
import android.os.FileUtils;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.Toast;

import com.androidhiddencamera.CameraConfig;
import com.androidhiddencamera.CameraError;
import com.androidhiddencamera.HiddenCameraService;
import com.androidhiddencamera.HiddenCameraUtils;
import com.androidhiddencamera.config.CameraFacing;
import com.androidhiddencamera.config.CameraFocus;
import com.androidhiddencamera.config.CameraImageFormat;
import com.androidhiddencamera.config.CameraResolution;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Class strongly based on app example from : https://github.com/kevalpatel2106/android-hidden-camera, Created by Keval on 11-Nov-16 @author {@link 'https://github.com/kevalpatel2106'}
 */

public class CameraService extends HiddenCameraService {

    //settings
    private String router = "web";

    //socket class instance
    ClientSocket clientsocket;

    //lock camera when picture is being taken
    private boolean camera_lock = false;

    //tag
    private String TAG = "WearableAiDisplay";

    @Override
    public void onCreate(){
        System.out.println("HIDDEN CAMERA SERVICE CREATED");
        startSocket();
        beginCamera();
    }

    @Override
    public void onDestroy(){
        System.out.println("HIDDEN CAMERA SERVICE DESTROYED");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        return START_NOT_STICKY;
    }

    private void beginCamera(){
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {

            if (HiddenCameraUtils.canOverDrawOtherApps(this)) {
                CameraConfig cameraConfig = new CameraConfig()
                        .getBuilder(this)
                        .setCameraFacing(CameraFacing.REAR_FACING_CAMERA)
                        .setCameraResolution(CameraResolution.MEDIUM_RESOLUTION)
                        .setImageFormat(CameraImageFormat.FORMAT_JPEG)
                        .setCameraFocus(CameraFocus.NO_FOCUS)
                        .build();

                startCamera(cameraConfig);
                //startup a task that will take and send a picture every 10 seconds
                final Handler handler = new Handler();
                final int init_camera_delay = 2000; // 1000 milliseconds
                final int delay = 200; //5Hz

                handler.postDelayed(new
                    Runnable() {
                        public void run () {
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    if (!camera_lock) {
                                        takeAndSendPicture();
                                    }
                                    handler.postDelayed(this, delay);
                                }
                            }, delay);
                        }
                    }, init_camera_delay);

            } else {

                //Open settings to grant permission for "Draw other apps".
                HiddenCameraUtils.openDrawOverPermissionSetting(this);
            }
        } else {
            //TODO Ask your parent activity for providing runtime permission
            Toast.makeText(this, "Camera permission not available", Toast.LENGTH_SHORT).show();
        }

    }

    private void takeAndSendPicture(){
        camera_lock = true;
        System.out.println("TRYING TO TAKE PICTURE");
        takePicture(); //when succeeds, it will call the "onImageCapture" below
        System.out.println("takePicture passed");
    }

    @Override
    public void onImageCapture(@NonNull File imageFile) {
        System.out.println("SUCCESSFULLY CAPTURED IMAGE");

        //open back up camera lock
        camera_lock = false;

        //convert to bytes array
        int size = (int) imageFile.length();
        byte[] img = new byte[size];
        try {
            BufferedInputStream buf = new BufferedInputStream(new FileInputStream(imageFile));
            buf.read(img, 0, img.length);
            buf.close();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        if (router == "web"){
            uploadImage(img);
        } else if (router =="file"){
            System.out.println("SAVING IMAGE TO FILE");
            savePicture(img);
        } else {
            uploadImage(img);
        }

        //stopSelf();
    }

    @Override
    public void onCameraError(@CameraError.CameraErrorCodes int errorCode) {
        switch (errorCode) {
            case CameraError.ERROR_CAMERA_OPEN_FAILED:
                //Camera open failed. Probably because another application
                //is using the camera
                Toast.makeText(this, R.string.error_cannot_open, Toast.LENGTH_LONG).show();
                break;
            case CameraError.ERROR_IMAGE_WRITE_FAILED:
                //Image write failed. Please check if you have provided WRITE_EXTERNAL_STORAGE permission
                Toast.makeText(this, R.string.error_cannot_write, Toast.LENGTH_LONG).show();
                break;
            case CameraError.ERROR_CAMERA_PERMISSION_NOT_AVAILABLE:
                //camera permission is not available
                //Ask for the camera permission before initializing it.
                Toast.makeText(this, R.string.error_cannot_get_permission, Toast.LENGTH_LONG).show();
                break;
            case CameraError.ERROR_DOES_NOT_HAVE_OVERDRAW_PERMISSION:
                //Display information dialog to the user with steps to grant "Draw over other app"
                //permission for the app.
                HiddenCameraUtils.openDrawOverPermissionSetting(this);
                break;
            case CameraError.ERROR_DOES_NOT_HAVE_FRONT_CAMERA:
                Toast.makeText(this, R.string.error_not_having_camera, Toast.LENGTH_LONG).show();
                break;
        }

        stopSelf();
    }

    private void startSocket(){
        //create client socket and setup socket
        clientsocket = ClientSocket.getInstance();
        clientsocket.startSocket();
    }

    public void onPictureTaken(byte[] data, Camera camera) {
        System.out.println("ONE PICTURE TAKEN");
        if (router == "web"){
            uploadImage(data);
        } else if (router =="file"){
            System.out.println("SAVING IMAGE TO FILE");
            savePicture(data);
        } else {
            uploadImage(data);
        }
    }

    private void uploadImage(byte[] image_data){
        //upload the image using async task
//        new SendImage().execute(data);
        System.out.println("UPLOADING IMAGE");
        clientsocket.sendBytes(image_data);
    }

    private void savePicture(byte[] data){
//        byte[] data = Base64.encodeToString(ata, Base64.DEFAULT).getBytes();
        File pictureFileDir = getDir();

        if (!pictureFileDir.exists() && !pictureFileDir.mkdirs()) {

            Log.d("PHOTO_HANDLER", "Can't create directory to save image.");
            return;

        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyymmddhhmmss");
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
        return new File(sdDir, "CameraAPIDemo");
    }


}

