package com.wearableintelligencesystem.androidsmartphone;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.StrictMode;
import android.util.Base64;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.app.NotificationCompat;
import androidx.lifecycle.LifecycleService;

import com.wearableintelligencesystem.androidsmartphone.comms.MessageTypes;
import com.wearableintelligencesystem.androidsmartphone.comms.SmsComms;
import com.wearableintelligencesystem.androidsmartphone.contextualsearch.ContextualSearchSystem;
import com.wearableintelligencesystem.androidsmartphone.database.WearableAiRoomDatabase;
import com.wearableintelligencesystem.androidsmartphone.database.facialemotion.FacialEmotion;
import com.wearableintelligencesystem.androidsmartphone.database.facialemotion.FacialEmotionCreator;
import com.wearableintelligencesystem.androidsmartphone.database.facialemotion.FacialEmotionRepository;
import com.wearableintelligencesystem.androidsmartphone.database.mediafile.MediaFileRepository;
import com.wearableintelligencesystem.androidsmartphone.database.memorycache.MemoryCacheRepository;
import com.wearableintelligencesystem.androidsmartphone.database.person.PersonRepository;
import com.wearableintelligencesystem.androidsmartphone.database.phrase.Phrase;
import com.wearableintelligencesystem.androidsmartphone.database.phrase.PhraseRepository;
import com.wearableintelligencesystem.androidsmartphone.database.voicecommand.VoiceCommandRepository;
import com.wearableintelligencesystem.androidsmartphone.facialrecognition.FaceRecApi;
import com.wearableintelligencesystem.androidsmartphone.nlp.FuzzyMatch;
import com.wearableintelligencesystem.androidsmartphone.nlp.NlpUtils;
import com.wearableintelligencesystem.androidsmartphone.nlp.WearableReferencerAutocite;
import com.wearableintelligencesystem.androidsmartphone.objectdetection.ObjectDetectionSystem;
import com.wearableintelligencesystem.androidsmartphone.speechrecognition.NaturalLanguage;
import com.wearableintelligencesystem.androidsmartphone.speechrecognition.SpeechRecVosk;
import com.wearableintelligencesystem.androidsmartphone.texttospeech.TextToSpeechSystem;
import com.wearableintelligencesystem.androidsmartphone.utils.NetworkUtils;
import com.wearableintelligencesystem.androidsmartphone.voicecommand.VoiceCommandServer;

import org.apache.commons.lang3.tuple.MutableTriple;
import org.apache.commons.lang3.tuple.Triple;
import org.json.JSONException;
import org.json.JSONObject;
import org.tensorflow.lite.support.image.TensorImage;

import java.io.IOException;
import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.subjects.PublishSubject;

/** Main service of WearableAI compute module android app. */
public class WearableAiAspService extends LifecycleService {
    // Service Binder given to clients
    private final IBinder binder = new LocalBinder();

    private static final String TAG = "WearableAi_ASP_Service";

    private static final String TAG_FOREGROUND_SERVICE = "FOREGROUND_SERVICE";
    public static final String ACTION_START_FOREGROUND_SERVICE = "ACTION_START_FOREGROUND_SERVICE";
    public static final String ACTION_STOP_FOREGROUND_SERVICE = "ACTION_STOP_FOREGROUND_SERVICE";
    public static final String ACTION_RUN_AFFECTIVE_MEM = "ACTION_RUN_AFFECTIVE_MEM";

    //our base language
    String baseLanguage = "english";

    NlpUtils nlpUtils;

    public static List<NaturalLanguage> supportedLanguages = new ArrayList<NaturalLanguage>();


    //mediapipe system
//    private MediaPipeSystem mediaPipeSystem;

    //handler for adv
    private Handler adv_handler;

    //speech recognition
    private SpeechRecVosk speechRecVosk;
    private SpeechRecVosk speechRecVoskForeignLanguage;

    //Text to Speech
    private TextToSpeechSystem textToSpeechSystem;

    //Object Detection system
    private ObjectDetectionSystem objectDetectionSystem;

    //contextual search system
    private ContextualSearchSystem contextualSearchSystem;

    //UI
    TextView tvIP, tvPort;
    TextView tvMessages;
    ImageView wearcam_view;

    //temp, update this on repeat and send to wearable to show connection is live
    private int count = 10;

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
    private MemoryCacheRepository mMemoryCacheRepository = null;
    private MediaFileRepository mMediaFileRepository = null;
    private PersonRepository mPersonRepository = null;

    //representatives of the other pieces of the system
    ASGRepresentative asgRep;
    GLBOXRepresentative glboxRep;

  @Override
  public void onCreate() {
      super.onCreate();

      //setup NLP utils
      nlpUtils = NlpUtils.getInstance(this);

    //setup room database interfaces
    mPhraseRepository = new PhraseRepository(getApplication());
    mFacialEmotionRepository = new FacialEmotionRepository(getApplication());
    mVoiceCommandRepository = new VoiceCommandRepository(getApplication());
    mMemoryCacheRepository = new MemoryCacheRepository(getApplication());
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

    //setup languages
    supportedLanguages.add(new NaturalLanguage("english", "en", "model-en-us", Locale.ENGLISH)); //english
    supportedLanguages.add(new NaturalLanguage("french", "fr", "model-fr-small", Locale.FRENCH)); //french

    //supportedLanguages.add(new NaturalLanguage("chinese", "zh", "model-cn-small", Locale.CHINESE)); //chinese
    //supportedLanguages.add(new NaturalLanguage("italian", "it", "model-it-small", Locale.ITALIAN)); //italian
    //supportedLanguages.add(new NaturalLanguage("japanese", "ja", "model-jp-small", Locale.JAPANESE)); //japanese

    //start vosk
    speechRecVosk = new SpeechRecVosk(getLanguageFromName(baseLanguage).getModelLocation(), true, this, audioObservable, dataObservable, mPhraseRepository);

    //start text to speech
    textToSpeechSystem = new TextToSpeechSystem(this, dataObservable, getLanguageFromName(baseLanguage).getLocale());

    //start Objectdetection system
    objectDetectionSystem = new ObjectDetectionSystem(getApplicationContext());
    objectDetectionSystem.setDataObservable(dataObservable);

    //start contextual search system
    contextualSearchSystem = new ContextualSearchSystem();
    contextualSearchSystem.setDataObservable(dataObservable);

    //start voice command server to parse transcript for voice command
    voiceCommandServer = new VoiceCommandServer(dataObservable, mVoiceCommandRepository, mMemoryCacheRepository, getApplicationContext());

    //setup mediapipe
//    mediaPipeSystem = new MediaPipeSystem(this);

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
            NetworkUtils.sendBroadcast(adv_key, adv_socket, PORT_NUM, getApplicationContext());
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
      super.onBind(intent);
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Log.d(TAG, "WearableAI Service onStartCommand");

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

        return Service.START_STICKY;
    }


    private void handleDataStream(JSONObject data){
        //first check if it's a type we should handle
        try{
            String type = data.getString(MessageTypes.MESSAGE_TYPE_LOCAL);
            if (type.equals(MessageTypes.POV_IMAGE)){
                sendImageToFaceRec(data);
            }  else if (type.equals((MessageTypes.START_FOREIGN_LANGUAGE_ASR))){
                String languageName = data.getString(MessageTypes.START_FOREIGN_LANGUAGE_SOURCE_LANGUAGE_NAME);
                speechRecVoskForeignLanguage = new SpeechRecVosk(getLanguageFromName(languageName).getModelLocation(), false, this, audioObservable, dataObservable, mPhraseRepository);
            }  else if (type.equals((MessageTypes.STOP_FOREIGN_LANGUAGE_ASR))){
                if (speechRecVoskForeignLanguage != null) {
                    speechRecVoskForeignLanguage.destroy();
                    speechRecVoskForeignLanguage = null;
                }
            }
        } catch (JSONException e){
            e.printStackTrace();
        }
    }

    private void sendImageToFaceRec(JSONObject data){
        try{
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
        FacialEmotionCreator.create(faceEmotion, "pov_camera_ASG", getApplicationContext(), mFacialEmotionRepository);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "WearableAiAspService killing itself and all its children");

        //stop advertising broadcasting IP
        adv_handler.removeCallbacksAndMessages(null);

        //kill asg connection
        asgRep.destroy();

        asgRep.destroy();

        //kill data transmitters
        dataObservable.onComplete();
        audioObservable.onComplete();

        //kill textToSpeech
        textToSpeechSystem.destroy();

        //kill vosk
        speechRecVosk.destroy();
        if (speechRecVoskForeignLanguage != null) {
            speechRecVoskForeignLanguage.destroy();
        }

        //close room database(s)
        WearableAiRoomDatabase.destroy();

        //call parent destroy
        super.onDestroy();
        Log.d(TAG, "WearableAiAspService destroy complete");
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

    //takes in a natural language name for a language and gives back the code
    public NaturalLanguage getLanguageFromName(String languageName){
      for (NaturalLanguage nl : supportedLanguages){
          FuzzyMatch modeMatchChar = nlpUtils.findNearMatches(languageName, nl.getNaturalLanguageName(), 0.8);
          if (modeMatchChar != null && modeMatchChar.getIndex() != -1){
              return nl;
          }
      }
      return null;
    }

    public void sendTestCard(String title, String content, String img){
        try{
            //build json object to send command result
            JSONObject commandResponseObject = new JSONObject();
            commandResponseObject.put(MessageTypes.MESSAGE_TYPE_LOCAL, MessageTypes.SEARCH_ENGINE_RESULT);

            JSONObject searchData = new JSONObject();
            searchData.put("title", title);
            searchData.put("body", content);
            searchData.put("image", img);

            commandResponseObject.put(MessageTypes.SEARCH_ENGINE_RESULT_DATA, searchData.toString());

            //send the command result to web socket, to send to asg
            dataObservable.onNext(commandResponseObject);
        } catch (JSONException e){
            e.printStackTrace();
        }
    }

    //takes in a natural language code for a language and gives back the NaturalLanguage
    public static NaturalLanguage getLanguageFromCode(String codeName){
        for (NaturalLanguage nl : supportedLanguages){
            if (nl.getCode().equals(codeName)){
                return nl;
            }
        }
        return null;
    }
}
