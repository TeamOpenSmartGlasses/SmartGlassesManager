package com.wearableintelligencesystem.androidsmartphone;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.StrictMode;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.lifecycle.LifecycleService;

import com.wearableintelligencesystem.androidsmartphone.comms.MessageTypes;
import com.wearableintelligencesystem.androidsmartphone.database.WearableAiRoomDatabase;
import com.wearableintelligencesystem.androidsmartphone.database.mediafile.MediaFileRepository;
import com.wearableintelligencesystem.androidsmartphone.database.memorycache.MemoryCacheRepository;
import com.wearableintelligencesystem.androidsmartphone.database.phrase.PhraseRepository;
import com.wearableintelligencesystem.androidsmartphone.database.voicecommand.VoiceCommandRepository;
import com.wearableintelligencesystem.androidsmartphone.nlp.FuzzyMatch;
import com.wearableintelligencesystem.androidsmartphone.nlp.NlpUtils;
import com.wearableintelligencesystem.androidsmartphone.speechrecognition.NaturalLanguage;
import com.wearableintelligencesystem.androidsmartphone.speechrecognition.SpeechRecVosk;
import com.wearableintelligencesystem.androidsmartphone.supportedglasses.SmartGlassesDevice;
import com.wearableintelligencesystem.androidsmartphone.texttospeech.TextToSpeechSystem;
import com.wearableintelligencesystem.androidsmartphone.utils.NetworkUtils;
import com.wearableintelligencesystem.androidsmartphone.voicecommand.VoiceCommandServer;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;

import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.subjects.PublishSubject;

/** Main service of Smart Glasses Manager, that starts connections to smart glasses and talks to third party apps (3PAs) */
public class WearableAiAspService extends LifecycleService {
    private static final String TAG = "WearableAi_ASP_Service";

    // Service Binder given to clients
    private final IBinder binder = new LocalBinder();
    public static final String ACTION_START_FOREGROUND_SERVICE = "ACTION_START_FOREGROUND_SERVICE";
    public static final String ACTION_STOP_FOREGROUND_SERVICE = "ACTION_STOP_FOREGROUND_SERVICE";

    //our base language
    String baseLanguage = "english";

    //tools for doing NLP, used for voice command system
    NlpUtils nlpUtils;
    public static Dictionary<String, NaturalLanguage> supportedLanguages = new Hashtable<String, NaturalLanguage>();

    //speech recognition
    private SpeechRecVosk speechRecVosk;
    private SpeechRecVosk speechRecVoskForeignLanguage;

    //Text to Speech
//    private TextToSpeechSystem textToSpeechSystem;

    //voice command system
    VoiceCommandServer voiceCommandServer;

    //observables to send data around app
    PublishSubject<JSONObject> dataObservable;
    PublishSubject<byte []> audioObservable;

    //database
    private PhraseRepository mPhraseRepository = null;
    private VoiceCommandRepository mVoiceCommandRepository = null;

    //representatives of the other pieces of the system
    SmartGlassesRepresentative smartGlassesRepresentative;

    @Override
    public void onCreate() {
        super.onCreate();

        //setup NLP utils
        nlpUtils = NlpUtils.getInstance(this);

        //setup room database interfaces
        mPhraseRepository = new PhraseRepository(getApplication());
        mVoiceCommandRepository = new VoiceCommandRepository(getApplication());

        //setup data observable which passes information (transcripts, commands, etc. around our app using mutlicasting
        dataObservable = PublishSubject.create();
        audioObservable = PublishSubject.create();

        //setup languages
        supportedLanguages.put("english", new NaturalLanguage("english", "en", "model-en-us", Locale.ENGLISH)); //english
        supportedLanguages.put("french", new NaturalLanguage("french", "fr", "model-fr-small", Locale.FRENCH)); //french
        //supportedLanguages.add(new NaturalLanguage("chinese", "zh", "model-cn-small", Locale.CHINESE)); //chinese
        //supportedLanguages.add(new NaturalLanguage("italian", "it", "model-it-small", Locale.ITALIAN)); //italian
        //supportedLanguages.add(new NaturalLanguage("japanese", "ja", "model-jp-small", Locale.JAPANESE)); //japanese

        //start vosk
        speechRecVosk = new SpeechRecVosk(supportedLanguages.get(baseLanguage).getModelLocation(), true, this, audioObservable, dataObservable, mPhraseRepository);

        //start text to speech
//        textToSpeechSystem = new TextToSpeechSystem(this, dataObservable, supportedLanguages.get(baseLanguage).getLocale());

        //start voice command server to parse transcript for voice command
        voiceCommandServer = new VoiceCommandServer(dataObservable, mVoiceCommandRepository, getApplicationContext());
    }

    public void connectToSmartGlasses(SmartGlassesDevice device){
        //this represents the smart glasses - it handles the connection, sending data to them, etc
        smartGlassesRepresentative = new SmartGlassesRepresentative(this, device, dataObservable);
        smartGlassesRepresentative.connectToSmartGlasses();
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

        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Smart Glasses Manager",
                NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription("Running smart glasses middleware...");
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

    @Override
    public void onDestroy() {
        Log.d(TAG, "WearableAiAspService killing itself and all its children");

        //kill asg connection
        if (smartGlassesRepresentative != null) {
            smartGlassesRepresentative.destroy();
        }

        //kill data transmitters
        if (dataObservable != null) {
            dataObservable.onComplete();
        }
        if (audioObservable != null) {
            audioObservable.onComplete();
        }

        //kill textToSpeech
//        textToSpeechSystem.destroy();

        //kill vosk
        if (speechRecVosk != null) {
            speechRecVosk.destroy();
            if (speechRecVoskForeignLanguage != null) {
                speechRecVoskForeignLanguage.destroy();
            }
        }

        //close room database(s)
        WearableAiRoomDatabase.destroy();

        //call parent destroy
        super.onDestroy();
        Log.d(TAG, "WearableAiAspService destroy complete");
    }

    public void sendTestCard(String title, String content, String img){
        Log.d(TAG, "SENDING TEST CARD FROM WAIService");
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
            Log.d(TAG, "FAILED!!!!!!!!!");
            e.printStackTrace();
        }
    }

    public boolean areSmartGlassesConnected(){
        if (smartGlassesRepresentative != null) {
            return smartGlassesRepresentative.isConnected();
        } else {
            return false;
        }
    }
}
