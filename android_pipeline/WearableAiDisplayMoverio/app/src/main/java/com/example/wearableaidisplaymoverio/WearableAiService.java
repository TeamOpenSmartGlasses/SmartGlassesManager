package com.example.wearableaidisplaymoverio;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Environment;
import android.os.FileUtils;
import android.os.Handler;
import android.os.IBinder;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.renderscript.Type;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import android.util.Log;
import android.widget.Toast;

import android.hardware.Camera;
import com.androidhiddencamera.CameraConfig;
import com.androidhiddencamera.CameraError;
import com.androidhiddencamera.HiddenCameraService;
import com.androidhiddencamera.HiddenCameraUtils;
import com.androidhiddencamera.config.CameraFacing;
import com.androidhiddencamera.config.CameraFocus;
import com.androidhiddencamera.config.CameraImageFormat;
import com.androidhiddencamera.config.CameraResolution;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Class strongly based on app example from : https://github.com/kevalpatel2106/android-hidden-camera, Created by Keval on 11-Nov-16 @author {@link 'https://github.com/kevalpatel2106'}
 */

//handles all of the background stuff like
    //-socket connection to ASP
    //-socket connection to GLBox
    //-receiving data from both ASP and GLBox
    //-locally running the camera in the background (Android Hidden camera: https://github.com/kevalpatel2106/android-hidden-camera)
    //-whatever else we need in the background
public class WearableAiService extends HiddenCameraService {

    //image stream handling
    private int img_count = 0;
    private long last_sent_time = 0;
    private int fps_to_send = 3; //speed we send HD frames from wearable to mobile compute unit

    //ASP socket
    ASPClientSocket asp_client_socket;
    String asp_address;
    String asp_adv_key = "WearableAiCyborg";

    //GLBOX socket
    GlboxClientSocket glbox_client_socket;
    String glbox_address = "192.168.43.188";
    String glbox_adv_key = "WearableAiCyborgGLBOX";

    //id of packet
    static final byte [] img_id = {0x01, 0x10}; //id for images

    //settings
    private String router = "web";

    //lock camera when picture is being taken
    private boolean camera_lock = false;

    //tag
    private String TAG = "WearableAiDisplay";

    public int PORT_NUM = 8891;
    public DatagramSocket socket;

    private Context mContext;

    Thread adv_thread;

    @Override
    public void onCreate(){
        System.out.println("HIDDEN CAMERA SERVICE CREATED");
        //first we have to listen for the UDP broadcast from the compute module so we know the IP address to connect to. Once we get that , we will connect to it on a socket and starting sending pictures
        Thread adv_thread = new Thread(new ReceiveAdvThread());
        adv_thread.start();

        mContext = this;
    }

    class ReceiveAdvThread extends Thread {
        public void run(){
            receiveUdpBroadcast();
        }
    }

    public void receiveUdpBroadcast(){
        try {
            //Keep a socket open to listen to all the UDP trafic that is destined for this port
            socket = new DatagramSocket(PORT_NUM, InetAddress.getByName("0.0.0.0"));
            socket.setBroadcast(true);

            while (true) {
                Log.i(TAG,"Ready to receive broadcast packets!");

                //Receive a packet
                byte[] recvBuf = new byte[2600];
                DatagramPacket packet = new DatagramPacket(recvBuf, recvBuf.length);
                socket.receive(packet);

                //Packet received
                Log.i(TAG, "Packet received from: " + asp_address);
                String data = new String(packet.getData()).trim();
                Log.i(TAG, "Packet received; data: " + data);
                if (data.equals(asp_adv_key)) {
                    asp_address = packet.getAddress().getHostAddress();
                    //start socket and camera on main service thread
                    // Get a handler that can be used to post to the main thread
                    Handler mainHandler = new Handler(mContext.getMainLooper());
                    Runnable myStarterRunnable = new Runnable() {
                        @Override
                        public void run() { asp_starter(); }
                    };
                    mainHandler.post(myStarterRunnable);
                    break;
                } //if not true, listen for the next packet
            }
        } catch (IOException ex) {
            Log.d(TAG, "Exception: " + ex);
        }
    }

    public void asp_starter(){
        startAspSocket();
        startGlboxSocket();
        beginCamera();
    }


    public void glbox_starter(){
        startGlboxSocket();
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

//                handler.postDelayed(new
//                    Runnable() {
//                        public void run () {
//                            handler.postDelayed(new Runnable() {
//                                @Override
//                                public void run() {
//                                    if (!camera_lock) {
//                                        takeAndSendPicture();
//                                    }
//                                    handler.postDelayed(this, delay);
//                                }
//                            }, delay);
//                        }
//                    }, init_camera_delay);

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

    private void startAspSocket(){
        //create client socket and setup socket
        asp_client_socket = ASPClientSocket.getInstance(this);
        asp_client_socket.setIp(asp_address);
        asp_client_socket.startSocket();
    }

    private void startGlboxSocket(){
        //create client socket and setup socket
        System.out.println("STARTING GLBOX SOCKET");
        glbox_client_socket = GlboxClientSocket.getInstance(this);
        glbox_client_socket.setIp(glbox_address);
        glbox_client_socket.startSocket();
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
        asp_client_socket.sendBytes(img_id, image_data, "image");
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
            Log.d(TAG, "File saved: " + filename);
        } catch (Exception error) {
            Log.d(TAG, "File" + filename + "not saved: "
                    + error.getMessage());
        }
    }

    private byte [] bmpToJpg(Bitmap bmp){
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        byte[] byteArrayJpg = stream.toByteArray();
        return byteArrayJpg;
    }

    private File getDir() {
        File sdDir = Environment
                .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        return new File(sdDir, "CameraAPIDemo");
    }

    @Override
    public void onPreviewFrame(byte [] data, Camera camera){
        long curr_time = System.currentTimeMillis();
        float now_frame_rate = (1 / (curr_time - last_sent_time));
        if (now_frame_rate <= fps_to_send) {
            Camera.Parameters parameters = camera.getParameters();
            int width = parameters.getPreviewSize().width;
            int height = parameters.getPreviewSize().height;

            YuvImage yuv = new YuvImage(data, parameters.getPreviewFormat(), width, height, null);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            yuv.compressToJpeg(new Rect(0, 0, width, height), 50, out);

            byte[] bytes = out.toByteArray();
            final Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            byte[] jpg = bmpToJpg(bitmap);
            uploadImage(jpg);
            int q_size = asp_client_socket.getImageBuf();
            //Log.d(TAG, "saving image");
            //savePicture(jpg);
            last_sent_time = curr_time;
        }
        img_count++;
    }

    public Allocation renderScriptNV21ToRGBA8888(Context context, int width, int height, byte[] nv21) {
        RenderScript rs = RenderScript.create(context);
        ScriptIntrinsicYuvToRGB yuvToRgbIntrinsic = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs));

        Type.Builder yuvType = new Type.Builder(rs, Element.U8(rs)).setX(nv21.length);
        Allocation in = Allocation.createTyped(rs, yuvType.create(), Allocation.USAGE_SCRIPT);

        Type.Builder rgbaType = new Type.Builder(rs, Element.RGBA_8888(rs)).setX(width).setY(height);
        Allocation out = Allocation.createTyped(rs, rgbaType.create(), Allocation.USAGE_SCRIPT);

        in.copyFrom(nv21);

        yuvToRgbIntrinsic.setInput(in);
        yuvToRgbIntrinsic.forEach(out);
        return out;
    }

}

