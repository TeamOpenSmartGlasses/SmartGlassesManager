package com.wearableintelligencesystem.androidsmartglasses;

import android.Manifest;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.renderscript.Type;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import android.util.Base64;
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
import com.wearableintelligencesystem.androidsmartglasses.archive.GlboxClientSocket;
import com.wearableintelligencesystem.androidsmartglasses.comms.MessageTypes;
import com.wearableintelligencesystem.androidsmartglasses.comms.WifiStatusCallback;
import com.wearableintelligencesystem.androidsmartglasses.comms.WifiUtils;
import com.wearableintelligencesystem.androidsmartglasses.sensors.BluetoothScanner;
import com.example.wearableintelligencesystemandroidsmartglasses.R;

import org.json.JSONException;
import org.json.JSONObject;

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
import java.text.SimpleDateFormat;
import java.util.Date;

import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.exceptions.UndeliverableException;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.subjects.PublishSubject;

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
    public static final String ACTION_START_FOREGROUND_SERVICE = "ACTION_START_FOREGROUND_SERVICE";
    public static final String ACTION_STOP_FOREGROUND_SERVICE = "ACTION_STOP_FOREGROUND_SERVICE";

    public static boolean killme = false;

    // Binder given to clients
    private final IBinder binder = new LocalBinder();

    //device status
    boolean phoneConnected = false;
    boolean wifiConnected;
    float batteryPercentage;
    boolean batteryIsCharging;

    //image stream handling
    private int img_count = 0;
    private long last_sent_time = 0l;
    private long last_proc_time = 0l;
    private long lastImageTime = 0l;
    private float fps_to_send = 0.5f; //speed we send HD frames from wearable to mobile compute unit
    private float fps_to_proc= 3f; //speed we process HD frames from wearable to mobile compute unit

    //ASP socket
    ASPClientSocket asp_client_socket;
    String asp_address;
    String asp_adv_key = "WearableAiCyborg";

    //GLBOX socket
    GlboxClientSocket glbox_client_socket;
    String glbox_address = "3.23.98.82";
    //String glbox_address = "192.168.1.34";
    //String glbox_adv_key = "WearableAiCyborgGLBOX"; //glbox no longer advertises - it retains a hardcoded domain/IP in the cloud

    //id of packet
    static final byte[] img_id = {0x01, 0x10}; //id for images

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

    byte[] curr_cam_image;

    //observables to send data around app
    PublishSubject<JSONObject> dataObservable;
    private Disposable dataSubscriber;

    //bluetooth sensors
    private BluetoothScanner mBluetoothScanner;

    //audio system
    AudioSystem audioSystem;

    @Override
    public void onCreate() {
        super.onCreate();

        //set the error handler for rxjava
        RxJavaPlugins.setErrorHandler(throwable -> {
            throwable.printStackTrace();
            if (throwable instanceof  UndeliverableException){
                Log.d(TAG, "Got RXJAVA error UndeliverableException.... carrying on.");
            }
        }); // nothing or some logging

        dataObservable = PublishSubject.create();
        dataSubscriber = dataObservable.subscribe(i -> handleDataStream(i));

        //setup our connection to the ASP
        asp_client_socket = ASPClientSocket.getInstance(this);
        asp_client_socket.setObservable(dataObservable);

        //first we have to listen for the UDP broadcast from the compute module so we know the IP address to connect to. Once we get that , we will connect to it on a socket and starting sending pictures
        //this will start the asp socket when ASP address is received and ASP is not connected. If ASP socket already running, it will update the ASP address (useful when ASP IP changes, like on new wifi)
        Thread adv_thread = new Thread(new ReceiveAdvThread());
        adv_thread.start();

        //setup audio streaming
        audioSystem = new AudioSystem(this);
        audioSystem.setObservable(dataObservable);
        audioSystem.startStreaming();

        //start glbox thread
        //then start a thread to connect to the glbox
//        Thread glbox_thread = new Thread(new StartGlbox());
//        glbox_thread.start();

        mContext = this;

        beginCamera();

        //start sensor scan
//        mBluetoothScanner = new BluetoothScanner(this);
//        mBluetoothScanner.startScan();

        //register for battery information
        this.registerReceiver(this.mBatInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

        //register for wifi information
        IntentFilter wifiFilter = new IntentFilter();
        wifiFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        WifiStatusCallback wifiConnectCallback = new WifiStatusCallback() {
            @Override
            public void onSuccess(boolean connected) {
                wifiConnected = connected;
                if (!wifiConnected){ //if wifi connects, then phone must disconnect too
                    phoneConnected = false;
                }
                updateUi();
            }
        };
        this.registerReceiver(new WifiUtils.WifiReceiver(wifiConnectCallback), wifiFilter);
    }

    class ReceiveAdvThread extends Thread {
        public void run() {
            receiveUdpBroadcast();
        }
    }

    class StartGlbox extends Thread {
        public void run() {
            glbox_starter();
        }
    }

    public byte[] getCurrentCameraImage() {
        return curr_cam_image;
    }

    public void receiveUdpBroadcast() {
        try {
            //Keep a socket open to listen to all the UDP trafic that is destined for this port
            socket = new DatagramSocket(PORT_NUM, InetAddress.getByName("0.0.0.0"));
            socket.setBroadcast(true);

            while (!killme) {
                //Receive a packet
                byte[] recvBuf = new byte[64];
                DatagramPacket packet = new DatagramPacket(recvBuf, recvBuf.length);
                socket.receive(packet);

                //Packet received
                String data = new String(packet.getData()).trim();
                if (data.equals(asp_adv_key)) {
                    asp_address = packet.getAddress().getHostAddress();
                    asp_client_socket.setIp(asp_address);
                    asp_comms_starter();
                }
                int hundos = 5; //how many hundredths of a second to wait, we split it up so we don't get stuck when the app is closed/killed
                for (int i = 0; i < hundos; i++){
                    SystemClock.sleep(100); //this is how we delay the loop from running too fast, there is probably a better way, but setting up a hanlder failed with no error, so this works for now
                    if (killme){
                        break;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.d(TAG, "Exception: " + e);
        }
    }

    public void asp_comms_starter() {
        //start the web socket to communicate with ASP, to replace TCP socket below
        if (!asp_client_socket.getWebSocketStarted()) {
            asp_client_socket.startWebSocket();
        }

        //start comms to ASP
        if (!asp_client_socket.getSocketStarted()) {
            asp_client_socket.startSocket();
        }
    }


    public void glbox_starter() {
        startGlboxSocket();
    }

    @Override
    public void onDestroy() {
        killme = true;
        dataSubscriber.dispose();
        asp_client_socket.destroy();

        try {
            adv_thread.join();
        } catch(InterruptedException e){
            e.printStackTrace();
        }

        super.onDestroy();
    }

    public class LocalBinder extends Binder {
        WearableAiService getService() {
            // Return this instance of LocalService so clients can call public methods
            return WearableAiService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    //service stuff
    private Notification updateNotification() {
        Context context = getApplicationContext();

        PendingIntent action = PendingIntent.getActivity(context,
                0, new Intent(context, MainActivity.class),
                PendingIntent.FLAG_CANCEL_CURRENT); // Flag indicating that if the described PendingIntent already exists, the current one should be canceled before generating a new one.

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        Notification notification = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.elon)
                .setContentTitle("Wearable Intelligence System")
                .setContentText("Running...")
                .setContentIntent(action)
                .setOngoing(true).build();

        return notification;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null){
            String action = intent.getAction();
            switch (action) {
                case ACTION_START_FOREGROUND_SERVICE:
                    // start the service in the foreground
                    startForeground(1234, updateNotification());
                    break;
                case ACTION_STOP_FOREGROUND_SERVICE:
                    stopForeground(true);
                    stopSelf();
                    break;
            }
        }

        return START_NOT_STICKY;
    }

    private void beginCamera() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {

            if (HiddenCameraUtils.canOverDrawOtherApps(this)) {
                CameraConfig cameraConfig = new CameraConfig()
                        .getBuilder(this)
                        .setCameraFacing(CameraFacing.REAR_FACING_CAMERA)
                        .setCameraResolution(CameraResolution.HIGH_RESOLUTION)
                        .setImageFormat(CameraImageFormat.FORMAT_JPEG)
                        .setCameraFocus(CameraFocus.NO_FOCUS)
                        .build();

                startCamera(cameraConfig);
                //startup a task that will take and send a picture every 10 seconds
                final int init_camera_delay = 2000; // 1000 milliseconds
                final int delay = 200; //5Hz
            } else {

                //Open settings to grant permission for "Draw other apps".
                HiddenCameraUtils.openDrawOverPermissionSetting(this);
            }
        } else {
            //TODO Ask your parent activity for providing runtime permission
            Toast.makeText(this, "Camera permission not available", Toast.LENGTH_SHORT).show();
        }

    }

    private void takeAndSendPicture() {
        camera_lock = true;
        System.out.println("TRYING TO TAKE PICTURE");
        takePicture(); //when succeeds, it will call the "onImageCapture" below
        System.out.println("takePicture passed");
    }

    @Override
    public void onImageCapture(@NonNull File imageFile) {
        Log.d(TAG, "SUCCESSFULLY CAPTURED IMAGE");

        //open back up camera lock
        camera_lock = false;

        //convert to bytes array
        int size = (int) imageFile.length();
        byte[] img = new byte[size];
        try {
            BufferedInputStream buf = new BufferedInputStream(new FileInputStream(imageFile));
            buf.read(img, 0, img.length);
            buf.close();
            System.arraycopy(img, 0, curr_cam_image, 0, img.length);
            //curr_cam_image = img;
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        if (router == "web") {
            uploadImage(img);
        } else if (router == "file") {
            System.out.println("SAVING IMAGE TO FILE");
            savePicture(img);
        } else {
            uploadImage(img);
        }
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

    private void startGlboxSocket() {
        //create client socket and setup socket
        System.out.println("STARTING GLBOX SOCKET");
        glbox_client_socket = GlboxClientSocket.getInstance(this);
        glbox_client_socket.setObservable(dataObservable);
        glbox_client_socket.setIp(glbox_address);
        glbox_client_socket.startSocket();
    }

    public void onPictureTaken(byte[] data, Camera camera) {
        System.out.println("ONE PICTURE TAKEN");
        if (router == "web") {
            uploadImage(data);
        } else if (router == "file") {
            System.out.println("SAVING IMAGE TO FILE");
            savePicture(data);
        } else {
            uploadImage(data);
        }
    }

    private void uploadImage(byte[] image_data) {
        //upload the jpg image
        asp_client_socket.sendBytes(img_id, image_data, "image");
        //asp_client_socket.sendImage(image_data); //this send on the websocket. The issue is that the large images make our transcript sending slow and choppy, so for now it happens on its own socket
    }

    private void savePicture(byte[] data) {
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

    private byte[] bmpToJpg(Bitmap bmp) {
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
    public void onPreviewFrame(byte[] data, Camera camera) {
        long curr_time = System.currentTimeMillis();
        float now_frame_rate = 1000f * (1f / (float)(curr_time - last_sent_time));
        float currFrameRate = 1000f * (1f / (float)(curr_time - lastImageTime));
        float now_proc_rate = 1000f * (1f / (float)(curr_time - last_proc_time));

        lastImageTime = curr_time;
        Camera.Parameters parameters = camera.getParameters();
        if (now_frame_rate <= fps_to_send) {
            int width = parameters.getPreviewSize().width;
            int height = parameters.getPreviewSize().height;

            YuvImage yuv = new YuvImage(data, parameters.getPreviewFormat(), width, height, null);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            yuv.compressToJpeg(new Rect(0, 0, width, height), 75, out);

            byte[] bytes = out.toByteArray();
            //this will slow down frame rate - need to make asynchronous in future
            curr_cam_image = new byte[bytes.length];
            System.arraycopy(bytes, 0, curr_cam_image, 0, bytes.length);
            final Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            byte[] jpg = bmpToJpg(bitmap);
            if (asp_client_socket != null && asp_client_socket.getConnectState()) {
                uploadImage(jpg);
            }
            int q_size = asp_client_socket.getImageBuf();
            //Log.d(TAG, "saving image");
            //savePicture(jpg);
            last_sent_time = curr_time;
        }

        if (now_frame_rate <= fps_to_proc) {
            last_proc_time = curr_time;
            int width = parameters.getPreviewSize().width;
            int height = parameters.getPreviewSize().height;

            YuvImage yuv = new YuvImage(data, parameters.getPreviewFormat(), width, height, null);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            yuv.compressToJpeg(new Rect(0, 0, width, height), 75, out);

            byte[] bytes = out.toByteArray();
            //this will slow down frame rate - need to make asynchronous in future
            curr_cam_image = new byte[bytes.length];
            System.arraycopy(bytes, 0, curr_cam_image, 0, bytes.length);
        }
        img_count++;
    }

    public void sendVisualSearch(){
        Log.d(TAG, "Sending visual search query");
        //encode image jpg as base64
        Bitmap bitmap = BitmapFactory.decodeByteArray(curr_cam_image, 0, curr_cam_image.length);
        byte[] jpg = bmpToJpg(bitmap);
        String imgString = Base64.encodeToString(jpg, Base64.DEFAULT);

        //send image
        try{
            JSONObject visualSearch = new JSONObject();
            visualSearch.put(MessageTypes.MESSAGE_TYPE_LOCAL, MessageTypes.VISUAL_SEARCH_QUERY);
            visualSearch.put(MessageTypes.VISUAL_SEARCH_IMAGE, imgString);
            dataObservable.onNext(visualSearch);
        } catch (JSONException e){
            e.printStackTrace();
        }
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


    public void sendGlBoxCurrImage() {
        glbox_client_socket.sendBytes(img_id, curr_cam_image, "image");
    }

    //check if service is running
    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) this.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private BroadcastReceiver mBatInfoReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context ctxt, Intent intent) {
            // Are we charging / charged?
            int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            batteryIsCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL;

            //what is the charge level?
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            batteryPercentage = level * 100 / (float)scale;
            Log.d(TAG, "BATTERY PERCENTAGE IS: " + batteryPercentage);
            Log.d(TAG, "BATTERY IS charging: " + batteryIsCharging);
            updateUi();
        }
    };

    private void updateUi() {
        //send to main ui
        final Intent intent = new Intent();
        intent.setAction(MainActivity.ACTION_UI_UPDATE);
        intent.putExtra(MainActivity.BATTERY_LEVEL_STATUS_UPDATE, batteryPercentage);
        intent.putExtra(MainActivity.BATTERY_CHARGING_STATUS_UPDATE, batteryIsCharging);
        intent.putExtra(MainActivity.WIFI_CONN_STATUS_UPDATE, wifiConnected);
        intent.putExtra(MainActivity.PHONE_CONN_STATUS_UPDATE, phoneConnected);
        mContext.sendBroadcast(intent); //eventually, we won't need to use the activity context, as our service will have its own context to send from
    }

    public void requestUiUpdate(){
        updateUi();
    }

    private void handleDataStream(JSONObject data){
        try {
            String dataType = data.getString(MessageTypes.MESSAGE_TYPE_LOCAL);
            if (dataType.equals(MessageTypes.UI_UPDATE_ACTION)){
                if (data.has(MessageTypes.PHONE_CONNECTION_STATUS)){
                    phoneConnected = data.getBoolean(MessageTypes.PHONE_CONNECTION_STATUS);
                    updateUi();
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    //wake up the screen
    private void wakeupScreen() {
        new AsyncTask<Void, Void, Exception>() {
            @Override
            protected Exception doInBackground(Void... params) {
                try {
                    Log.d(TAG, "WAITING TO TURN ON SCREEN");
                    try {
                        Thread.sleep(10000); // turn on duration
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    Log.d(TAG, "TURNING ON SCREEN");
                    PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
                    PowerManager.WakeLock fullWakeLock = powerManager.newWakeLock((PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP), "WIS:Loneworker-FULL WAKE LOCK");
                    fullWakeLock.acquire(); // turn on
                    try {
                        Thread.sleep(5000); // turn on duration
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    Log.d(TAG, "RELEASING SCREEN");
                    fullWakeLock.release();
                } catch (Exception e) {
                    return e;
                }
                return null;
            }
        }.execute();
    }

}

