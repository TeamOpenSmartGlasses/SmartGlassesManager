package com.google.mediapipe.apps.wearableai;

import java.util.concurrent.ExecutionException;
import java.lang.InterruptedException;
import com.google.mediapipe.apps.wearableai.database.WearableAiRoomDatabase;
import com.google.mediapipe.apps.wearableai.speechrecvosk.SpeechRecVosk;
import com.google.mediapipe.apps.wearableai.voicecommand.VoiceCommandServer;
import android.content.pm.PackageManager.NameNotFoundException;

import java.lang.NullPointerException;
import java.util.Arrays;

//database
import com.google.mediapipe.apps.wearableai.database.phrase.PhraseRepository;
import com.google.mediapipe.apps.wearableai.database.phrase.PhraseDao;
import com.google.mediapipe.apps.wearableai.database.phrase.PhraseViewModel;
import com.google.mediapipe.apps.wearableai.database.phrase.PhraseCreator;
import com.google.mediapipe.apps.wearableai.database.phrase.Phrase;

import com.google.mediapipe.apps.wearableai.database.facialemotion.FacialEmotionRepository;
import com.google.mediapipe.apps.wearableai.database.facialemotion.FacialEmotionDao;
import com.google.mediapipe.apps.wearableai.database.facialemotion.FacialEmotionViewModel;
import com.google.mediapipe.apps.wearableai.database.facialemotion.FacialEmotionCreator;
import com.google.mediapipe.apps.wearableai.database.facialemotion.FacialEmotion;

import com.google.mediapipe.apps.wearableai.database.voicecommand.VoiceCommandRepository;
import com.google.mediapipe.apps.wearableai.database.voicecommand.VoiceCommandDao;
import com.google.mediapipe.apps.wearableai.database.voicecommand.VoiceCommandCreator;
import com.google.mediapipe.apps.wearableai.database.voicecommand.VoiceCommandEntity;

import com.google.mediapipe.apps.wearableai.database.mediafile.MediaFileRepository;
import com.google.mediapipe.apps.wearableai.database.mediafile.MediaFileDao;
import com.google.mediapipe.apps.wearableai.database.mediafile.MediaFileCreator;
import com.google.mediapipe.apps.wearableai.database.mediafile.MediaFileEntity;

import com.google.mediapipe.apps.wearableai.database.person.PersonRepository;
import com.google.mediapipe.apps.wearableai.database.person.PersonDao;
import com.google.mediapipe.apps.wearableai.database.person.PersonCreator;
import com.google.mediapipe.apps.wearableai.database.person.PersonEntity;

//comms
import com.google.mediapipe.apps.wearableai.comms.SmsComms;

//affective computing
import com.google.mediapipe.apps.wearableai.affectivecomputing.SocialInteraction;
import com.google.mediapipe.apps.wearableai.affectivecomputing.LandmarksTranslator;

//sensors
import com.google.mediapipe.apps.wearableai.sensors.BitmapConverter;
import com.google.mediapipe.apps.wearableai.sensors.BmpProducer;

//face rec
import com.google.mediapipe.apps.wearableai.facialrecognition.FaceRecApi;

//nlp
import com.google.mediapipe.apps.wearableai.nlp.WearableReferencerAutocite;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.LifecycleService;
import androidx.annotation.Nullable;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import android.content.Intent;
import android.os.IBinder;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import android.os.Environment;
import android.net.Uri;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.util.Size;
import android.view.SurfaceHolder;
import android.os.Binder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.os.Bundle;
import android.util.Log;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.HashMap;
import java.util.Map;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.InterfaceAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.util.Enumeration;

//some repeat imports
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
import java.util.Random;

import android.content.Context;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.os.StrictMode;

import androidx.core.app.NotificationCompat;

import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.subjects.PublishSubject;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

//CUSTOM - our code

//messaging / event bus
import com.google.mediapipe.apps.wearableai.comms.MessageTypes;

//network utils
import com.google.mediapipe.apps.wearableai.utils.NetworkUtils;

/** Main service of WearableAI compute module android app. */
public class WearableAiAspService extends LifecycleService {
    // Service Binder given to clients
    private final IBinder binder = new LocalBinder();

    private static final String TAG_FOREGROUND_SERVICE = "FOREGROUND_SERVICE";
    public static final String ACTION_START_FOREGROUND_SERVICE = "ACTION_START_FOREGROUND_SERVICE";
    public static final String ACTION_STOP_FOREGROUND_SERVICE = "ACTION_STOP_FOREGROUND_SERVICE";
    public static final String ACTION_RUN_AFFECTIVE_MEM = "ACTION_RUN_AFFECTIVE_MEM";

    //mediapipe system
    private MediaPipeSystem mediaPipeSystem;

    //handler for adv
    private Handler adv_handler;

    private static final String TAG = "WearableAi_ASP_Service";

    //speech recognition
    private SpeechRecVosk speechRecVosk;

    //UI
    TextView tvIP, tvPort;
    TextView tvMessages;
    ImageView wearcam_view;

    //temp, update this on repeat and send to wearable to show connection is live
    private int count = 10;

    //keep track of current interaction - to be moved to it's own class and instantiated for each interaction/face recognition
    private SocialInteraction mSocialInteraction;

    //holds connection state
    private boolean mConnectionState = false;

    public int PORT_NUM = 8891;
    public DatagramSocket adv_socket;
    public String adv_key = "WearableAiCyborg";

  //voice command system
  VoiceCommandServer voiceCommandServer;

  //facial rec system
  private FaceRecApi faceRecApi;

  //nlp system
  private WearableReferencerAutocite mWearableReferencerAutocite;

  //sms system
  private SmsComms smsComms;

    //observables to send data around app
    PublishSubject<JSONObject> dataObservable;
    PublishSubject<byte []> audioObservable;

    //database
    private PhraseRepository mPhraseRepository = null;
    private List<Phrase> mAllPhrasesSnapshot;
    private FacialEmotionRepository mFacialEmotionRepository = null;
    private List<FacialEmotion> mAllFacialEmotionsSnapshot;
    private VoiceCommandRepository mVoiceCommandRepository = null;
    private MediaFileRepository mMediaFileRepository = null;
    private PersonRepository mPersonRepository = null;

    //representatives of the other pieces of the system
    ASGRepresentative asgRep;
    GLBOXRepresentative glboxRep;

  @Override
  public void onCreate() {
   
    //setup room database interfaces
    mPhraseRepository = new PhraseRepository(getApplication());
    mFacialEmotionRepository = new FacialEmotionRepository(getApplication());
    mVoiceCommandRepository = new VoiceCommandRepository(getApplication());
    mMediaFileRepository = new MediaFileRepository(getApplication());
    mPersonRepository = new PersonRepository(getApplication());

    //setup data observable which passes information (transcripts, commands, etc. around our app using mutlicasting
    dataObservable = PublishSubject.create();
    audioObservable = PublishSubject.create();
    Disposable s = dataObservable.subscribe(i -> handleDataStream(i));

    //our representatives - they represent the ASG and GLBOX, they hold their connection, they decide what gets sent out to them, etc
    asgRep = new ASGRepresentative(this, dataObservable, mMediaFileRepository);
    glboxRep = new GLBOXRepresentative(this, dataObservable, asgRep);
    
    //the order below is to minimize time between launch and transcription appearing on the ASG
    
    //open the UDP socket to broadcast our ip address
    openSocket();

    //send broadcast
    adv_handler = new Handler();
    final int delay = 1000; // 1000 milliseconds == 1 second
    adv_handler.postDelayed(new Runnable() {
        public void run() {
            new Thread(new SendAdvThread()).start();
            adv_handler.postDelayed(this, delay);
        }
    }, 5);

    //start connection to ASG
    asgRep.startAsgConnection();

    //start vosk
    speechRecVosk = new SpeechRecVosk(this, audioObservable, dataObservable, mPhraseRepository);

    //start voice command server to parse transcript for voice command
    voiceCommandServer = new VoiceCommandServer(dataObservable, mVoiceCommandRepository, getApplicationContext());

    //setup single interaction instance - later to be done dynamically based on seeing and recognizing a new face
    mSocialInteraction = new SocialInteraction();

    //setup mediapipe
    mediaPipeSystem = new MediaPipeSystem(this);

    //start nlp
    mWearableReferencerAutocite = new WearableReferencerAutocite(this);
    mWearableReferencerAutocite.setObservable(dataObservable);

    //start sms system
    smsComms = SmsComms.getInstance();
    smsComms.setObservable(dataObservable);

    //start face recognizer - use handler because it takes a long time to setup (if this gets optimized, maybe we won't need a handler)
    //also need handler because permissions take a second to kick in, and faceRecApi fails if file permissions aren't granted - need to look into this - cayden
    HandlerThread thread = new HandlerThread("MainWearableAiAspService");
    thread.start();
    Handler mainHandler = new Handler(thread.getLooper());
    LifecycleService mContext = this;
    mainHandler.postDelayed(new Runnable() {
        public void run() {
            faceRecApi = new FaceRecApi(mContext, mPersonRepository);
            faceRecApi.setDataObservable(dataObservable);
            faceRecApi.setup();
        }
    }, 10);
  }


    public void openSocket() {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        try {
            //Open a random port to send the package
            adv_socket = new DatagramSocket();
            adv_socket.setBroadcast(true);
        } catch (IOException e) {
            Log.e(TAG, "IOException: " + e.getMessage());
        }
    }

    class SendAdvThread extends Thread {
        public void run() {
            //send broadcast so ASG knows our address
            NetworkUtils.sendBroadcast(adv_key, adv_socket, PORT_NUM);
        }
    }

    //service stuff
    private Notification updateNotification() {
        Context context = getApplicationContext();

        PendingIntent action = PendingIntent.getActivity(context,
                0, new Intent(context, MainActivity.class),
                PendingIntent.FLAG_CANCEL_CURRENT); // Flag indicating that if the described PendingIntent already exists, the current one should be canceled before generating a new one.

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder builder;


        String CHANNEL_ID = "wearable_ai_service_channel";

        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Wearable Intelligence System",
                NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription("Running intelligence suite...");
        manager.createNotificationChannel(channel);

        builder = new NotificationCompat.Builder(this, CHANNEL_ID);

        return builder.setContentIntent(action)
                .setContentTitle("Wearable Intelligence System - Wearable AI Service")
                .setContentText("Communicating with Smart Glasses")
                .setSmallIcon(R.drawable.elon)
                .setTicker("...")
                .setContentIntent(action)
                .setOngoing(true).build();
    }

   //service stuffs
    public class LocalBinder extends Binder {
        WearableAiAspService getService() {
            // Return this instance of LocalService so clients can call public methods
            return WearableAiAspService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "WearableAI Service onStartCommand");

        if (intent != null){
            String action = intent.getAction();
            switch (action) {
                case ACTION_START_FOREGROUND_SERVICE:
                    // start the service in the foreground
                    startForeground(1234, updateNotification());
                    break;
                case ACTION_STOP_FOREGROUND_SERVICE:
                    stopSelf();
                    break;
            }
        }

        return Service.START_STICKY;
    }


    private void handleDataStream(JSONObject data){
        //first check if it's a type we should handle
        try{
            String type = data.getString(MessageTypes.MESSAGE_TYPE_LOCAL);
            if (type.equals(MessageTypes.POV_IMAGE)){
                sendImageToFaceRec(data);
            } 
        } catch (JSONException e){
            e.printStackTrace();
        }
    }

    private void sendImageToFaceRec(JSONObject data){
        try{
            Log.d(TAG, data.toString());
            String jpgImageString = data.getString(MessageTypes.JPG_BYTES_BASE64);
            byte [] jpgImage = Base64.decode(jpgImageString, Base64.DEFAULT);
            long imageTime = data.getLong(MessageTypes.TIMESTAMP);
            long imageId = data.getLong(MessageTypes.IMAGE_ID);

            //convert to bitmap
            Bitmap bitmap = BitmapFactory.decodeByteArray(jpgImage, 0, jpgImage.length);
            
            //send through facial recognition
            faceRecApi.analyze(bitmap, imageTime, imageId);

        } catch (JSONException e){
            e.printStackTrace();
        }
    }

    private void saveFacialEmotion(String faceEmotion){
        Log.d(TAG, "SAVING FACIAL EMOTION: " + faceEmotion);
        FacialEmotionCreator.create(faceEmotion, "pov_camera_ASG", getApplicationContext(), mFacialEmotionRepository);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "WearableAiAspService killing itself and all its children");

        //stop advertising broadcasting IP
        adv_handler.removeCallbacksAndMessages(null);

        //kill data transmitters
        dataObservable.onComplete();
        audioObservable.onComplete();

        //kill mediapipe
        mediaPipeSystem.destroy();

        //kill vosk
        speechRecVosk.destroy();

        //close room database(s)
        WearableAiRoomDatabase.destroy();
        stopForeground(true);
    }

    //allow ui to control Autociter/wearable referencer
    public void startAutociter(String phoneNumber){
        try{
            JSONObject startAutociterMessage = new JSONObject();
            startAutociterMessage.put(MessageTypes.MESSAGE_TYPE_LOCAL, MessageTypes.AUTOCITER_START);
            startAutociterMessage.put(MessageTypes.AUTOCITER_PHONE_NUMBER, phoneNumber);
            dataObservable.onNext(startAutociterMessage);
        } catch (JSONException e){
            e.printStackTrace();
        }
    }

    public void stopAutociter(){
        try{
            JSONObject stopAutociterMessage = new JSONObject();
            stopAutociterMessage.put(MessageTypes.MESSAGE_TYPE_LOCAL, MessageTypes.AUTOCITER_STOP);
            dataObservable.onNext(stopAutociterMessage);
        } catch (JSONException e){
            e.printStackTrace();
        }
    }

    public boolean getAutociterStatus(){
        return mWearableReferencerAutocite.getIsActive();
    }


    public String getPhoneNumber(){
        return mWearableReferencerAutocite.getPhoneNumber();
    }
    //^^^^^^allow ui to control Autociter/wearable referencer

}
