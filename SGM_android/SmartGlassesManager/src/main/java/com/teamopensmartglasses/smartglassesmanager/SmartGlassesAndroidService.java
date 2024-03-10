package com.teamopensmartglasses.smartglassesmanager;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.lifecycle.LifecycleService;
import androidx.preference.PreferenceManager;

import com.teamopensmartglasses.smartglassesmanager.comms.MessageTypes;
import com.teamopensmartglasses.smartglassesmanager.eventbusmessages.BulletPointListViewRequestEvent;
import com.teamopensmartglasses.smartglassesmanager.eventbusmessages.CenteredTextViewRequestEvent;
import com.teamopensmartglasses.smartglassesmanager.eventbusmessages.FinalScrollingTextRequestEvent;
import com.teamopensmartglasses.smartglassesmanager.eventbusmessages.HomeScreenEvent;
import com.teamopensmartglasses.smartglassesmanager.eventbusmessages.ReferenceCardImageViewRequestEvent;
import com.teamopensmartglasses.smartglassesmanager.eventbusmessages.ReferenceCardSimpleViewRequestEvent;
import com.teamopensmartglasses.smartglassesmanager.eventbusmessages.RowsCardViewRequestEvent;
import com.teamopensmartglasses.smartglassesmanager.eventbusmessages.ScrollingTextViewStartRequestEvent;
import com.teamopensmartglasses.smartglassesmanager.eventbusmessages.ScrollingTextViewStopRequestEvent;
import com.teamopensmartglasses.smartglassesmanager.eventbusmessages.SmartGlassesConnectionEvent;
import com.teamopensmartglasses.smartglassesmanager.eventbusmessages.TextLineViewRequestEvent;
import com.teamopensmartglasses.smartglassesmanager.eventbusmessages.TextToSpeechEvent;
import com.teamopensmartglasses.smartglassesmanager.speechrecognition.ASR_FRAMEWORKS;
import com.teamopensmartglasses.smartglassesmanager.speechrecognition.SpeechRecSwitchSystem;
import com.teamopensmartglasses.smartglassesmanager.supportedglasses.AudioWearable;
import com.teamopensmartglasses.smartglassesmanager.supportedglasses.InmoAirOne;
import com.teamopensmartglasses.smartglassesmanager.supportedglasses.SmartGlassesDevice;
import com.teamopensmartglasses.smartglassesmanager.supportedglasses.TCLRayNeoXTwo;
import com.teamopensmartglasses.smartglassesmanager.supportedglasses.VuzixShield;
import com.teamopensmartglasses.smartglassesmanager.supportedglasses.VuzixUltralite;
import com.teamopensmartglasses.smartglassesmanager.texttospeech.TextToSpeechSystem;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.EventBusException;
import org.greenrobot.eventbus.Subscribe;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;

import io.reactivex.rxjava3.subjects.PublishSubject;

/** Main service of Smart Glasses Manager, that starts connections to smart glasses and talks to third party apps (3PAs) */
public abstract class SmartGlassesAndroidService extends LifecycleService {
    private static final String TAG = "SGM_ASP_Service";

    // Service Binder given to clients
    private final IBinder binder = new LocalBinder();
    public static final String ACTION_START_FOREGROUND_SERVICE = "MY_ACTION_START_FOREGROUND_SERVICE";
    public static final String ACTION_STOP_FOREGROUND_SERVICE = "MY_ACTION_STOP_FOREGROUND_SERVICE";
    private int myNotificationId;
    private Class mainActivityClass;
    private String myChannelId;
    private String notificationAppName;
    private String notificationDescription;
    private int notificationDrawable;

    //Text to Speech
    private TextToSpeechSystem textToSpeechSystem;

    //observables to send data around app
    PublishSubject<JSONObject> dataObservable;

    //representatives of the other pieces of the system
    SmartGlassesRepresentative smartGlassesRepresentative;

    //speech rec
    SpeechRecSwitchSystem speechRecSwitchSystem;

    //connection handler
    public Handler connectHandler;

    public SmartGlassesAndroidService(Class mainActivityClass, String myChannelId, int myNotificationId, String notificationAppName, String notificationDescription, int notificationDrawable){
        this.myNotificationId = myNotificationId;
        this.mainActivityClass = mainActivityClass;
        this.myChannelId = myChannelId;
        this.notificationAppName = notificationAppName;
        this.notificationDescription = notificationDescription;
        this.notificationDrawable = notificationDrawable;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        //setup connection handler
        connectHandler = new Handler();

        //start speech rec
        speechRecSwitchSystem = new SpeechRecSwitchSystem(this.getApplicationContext());
        ASR_FRAMEWORKS asrFramework = getChosenAsrFramework(this.getApplicationContext());
        String transcribeLanguage = getChosenTranscribeLanguage(this.getApplicationContext());
        speechRecSwitchSystem.startAsrFramework(asrFramework, transcribeLanguage);

        //setup data observable which passes information (transcripts, commands, etc. around our app using mutlicasting
        dataObservable = PublishSubject.create();

        //start text to speech
        textToSpeechSystem = new TextToSpeechSystem(this);
        textToSpeechSystem.setup();
    }

    protected void setupEventBusSubscribers() {
        try {
            EventBus.getDefault().register(this);
        }
        catch(EventBusException e){
            e.printStackTrace();
        }
    }

    @Subscribe
    public void handleConnectionEvent(SmartGlassesConnectionEvent event) {
        sendUiUpdate();
    }

    public void connectToSmartGlasses(SmartGlassesDevice device) {
        //this represents the smart glasses - it handles the connection, sending data to them, etc
        LifecycleService currContext = this;
        connectHandler.post(new Runnable() {
            @Override
            public void run() {
                 Log.d(TAG, "CONNECTING TO SMART GLASSES");
                smartGlassesRepresentative = new SmartGlassesRepresentative(currContext, device, currContext, dataObservable);
                smartGlassesRepresentative.connectToSmartGlasses();
            }
        });
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "WearableAiAspService killing itself and all its children");

        EventBus.getDefault().unregister(this);

        //kill asg connection
        if (smartGlassesRepresentative != null) {
            smartGlassesRepresentative.destroy();
            smartGlassesRepresentative = null;
        }

        //kill data transmitters
        if (dataObservable != null) {
            dataObservable.onComplete();
        }

        //kill speech rec
        if (speechRecSwitchSystem != null){
            speechRecSwitchSystem.destroy();
        }

        //kill textToSpeech
        textToSpeechSystem.destroy();

        //kill aioConnect
        aioRetryHandler.removeCallbacks(aioRetryConnectionTask);

        //call parent destroy
        super.onDestroy();
        Log.d(TAG, "WearableAiAspService destroy complete");
    }

    public void sendTestCard(String title, String body, String img) {
        Log.d(TAG, "SENDING TEST CARD FROM WAIService");
        EventBus.getDefault().post(new ReferenceCardSimpleViewRequestEvent(title, body));
    }

    public int getSmartGlassesConnectState() {
        if (smartGlassesRepresentative != null) {
            return smartGlassesRepresentative.getConnectionState();
        } else {
            return 0;
        }
    }

    public SmartGlassesOperatingSystem getConnectedDeviceModelName(){
        if(smartGlassesRepresentative.getConnectionState() != 2) return null;
        return smartGlassesRepresentative.smartGlassesDevice.glassesOs;
    }

    public void sendUiUpdate() {
        //connectionState = 2 means connected
        Intent intent = new Intent();
        intent.setAction(MessageTypes.GLASSES_STATUS_UPDATE);
        // Set the optional additional information in extra field.
        int connectionState;
        if (smartGlassesRepresentative != null) {
            connectionState = smartGlassesRepresentative.getConnectionState();
            intent.putExtra(MessageTypes.CONNECTION_GLASSES_GLASSES_OBJECT, smartGlassesRepresentative.smartGlassesDevice);

            // Update preferred wearable if connected
            if(connectionState == 2){
                savePreferredWearable(this, smartGlassesRepresentative.smartGlassesDevice.deviceModelName);
            }
        } else {
            connectionState = 0;
        }
        intent.putExtra(MessageTypes.CONNECTION_GLASSES_STATUS_UPDATE, connectionState);
        sendBroadcast(intent);
    }

    /** Saves the chosen ASR framework in user shared preference. */
    public static void saveChosenAsrFramework(Context context, ASR_FRAMEWORKS asrFramework) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putString(context.getResources().getString(R.string.SHARED_PREF_ASR_KEY), asrFramework.name())
                .apply();
    }

    /** Gets the chosen ASR framework from shared preference. */
    public static ASR_FRAMEWORKS getChosenAsrFramework(Context context) {
        String asrString = PreferenceManager.getDefaultSharedPreferences(context).getString(context.getResources().getString(R.string.SHARED_PREF_ASR_KEY), "");
        if (asrString.equals("")){
            saveChosenAsrFramework(context, ASR_FRAMEWORKS.VOSK_ASR_FRAMEWORK);
            asrString = ASR_FRAMEWORKS.VOSK_ASR_FRAMEWORK.name();
        }
        return ASR_FRAMEWORKS.valueOf(asrString);
    }

    public void changeChosenAsrFramework(ASR_FRAMEWORKS asrFramework){
        saveChosenAsrFramework(getApplicationContext(), asrFramework);
        if (speechRecSwitchSystem != null) {
            speechRecSwitchSystem.startAsrFramework(asrFramework);
        }
    }

    /** Gets the API key from shared preference. */
    public static String getApiKey(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(context.getResources().getString(R.string.SHARED_PREF_KEY), "");
    }

    /** Saves the API Key in user shared preference. */
    public static void saveApiKey(Context context, String key) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putString(context.getResources().getString(R.string.SHARED_PREF_KEY), key)
                .apply();
    }

    /** Gets the preferred wearable from shared preference. */
    public static String getPreferredWearable(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(context.getResources().getString(R.string.PREFERRED_WEARABLE), "");
    }

    /** Saves the preferred wearable in user shared preference. */
    public static void savePreferredWearable(Context context, String wearableName) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putString(context.getResources().getString(R.string.PREFERRED_WEARABLE), wearableName)
                .apply();
    }

    public static void saveChosenTranscribeLanguage(Context context, String targetLanguageString) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putString(context.getResources().getString(R.string.SHARED_PREF_TRANSCRIBE_LANGUAGE), targetLanguageString)
                .apply();
    }

    public static String getChosenTranscribeLanguage(Context context) {
        String targetLanguageString = PreferenceManager.getDefaultSharedPreferences(context).getString(context.getResources().getString(R.string.SHARED_PREF_TRANSCRIBE_LANGUAGE), "");
        if (targetLanguageString.equals("")){
            saveChosenTranscribeLanguage(context, "English");
            targetLanguageString = "English";
        }
        return targetLanguageString;
    }

    //service stuff
    private Notification updateNotification() {
        Context context = getApplicationContext();

        PendingIntent action = PendingIntent.getActivity(context,
                0, new Intent(context, mainActivityClass),
                PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_MUTABLE); // Flag indicating that if the described PendingIntent already exists, the current one should be canceled before generating a new one.

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder builder;

        String CHANNEL_ID = myChannelId;

        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, notificationAppName,
                NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription(notificationDescription);
        manager.createNotificationChannel(channel);

        builder = new NotificationCompat.Builder(this, CHANNEL_ID);

        return builder.setContentIntent(action)
                .setContentTitle(notificationAppName)
                .setContentText(notificationDescription)
                .setSmallIcon(notificationDrawable)
                .setTicker("...")
                .setContentIntent(action)
                .setOngoing(true).build();
    }

    public class LocalBinder extends Binder {
        public SmartGlassesAndroidService getService() {
            // Return this instance of LocalService so clients can call public methods
            return SmartGlassesAndroidService.this;
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
        if (intent != null) {
            String action = intent.getAction();
            Bundle extras = intent.getExtras();

            switch (action) {
                case ACTION_START_FOREGROUND_SERVICE:
                    // start the service in the foreground
                    Log.d("TEST", "starting foreground");
                    startForeground(myNotificationId, updateNotification());
                    break;
                case ACTION_STOP_FOREGROUND_SERVICE:
                    stopForeground(true);
                    stopSelf();
                    break;
            }
        }
        return Service.START_STICKY;
    }


    // Setup for aioConnectSmartGlasses
    ArrayList<SmartGlassesDevice> smartGlassesDevices = new ArrayList<>();
    Handler aioRetryHandler = new Handler();
    Runnable aioRetryConnectionTask = new Runnable() {
        @Override
        public void run() {
            if (smartGlassesRepresentative == null || smartGlassesRepresentative.getConnectionState() != 2) { // If still disconnected
                if(!smartGlassesDevices.isEmpty()){
                    Toast.makeText(getApplicationContext(), "Searching for glasses...", Toast.LENGTH_LONG).show();
                    Log.d(TAG, "TRYING TO CONNECT TO: " + smartGlassesDevices.get(0).deviceModelName);

                    if (smartGlassesRepresentative != null) {
                        smartGlassesRepresentative.destroy();
                        smartGlassesRepresentative = null;
                    }

                    connectToSmartGlasses(smartGlassesDevices.get(0));
                    smartGlassesDevices.add(smartGlassesDevices.remove(0));
                    aioRetryHandler.postDelayed(this, 5000); // Schedule another retry if needed
                }
                else
                {
                    aioRetryHandler.removeCallbacks(this);
                    Toast.makeText(getApplicationContext(), "No glasses found", Toast.LENGTH_LONG).show();
                }
            }
            else {
                Toast.makeText(getApplicationContext(), "Connected to " + smartGlassesRepresentative.smartGlassesDevice.deviceModelName, Toast.LENGTH_LONG).show();
            }
        }
    };
    public void aioConnectSmartGlasses(){
        if (getChosenAsrFramework(this) == ASR_FRAMEWORKS.GOOGLE_ASR_FRAMEWORK) {
            String apiKey = getApiKey(getApplicationContext());
            if (apiKey == null || apiKey.equals("")) {
                showNoGoogleAsrDialog();
                return;
            }
        }

        String preferred = getPreferredWearable(this.getApplicationContext());
        smartGlassesDevices = new ArrayList<SmartGlassesDevice>(Arrays.asList(new VuzixUltralite(), new VuzixShield(),  new InmoAirOne(), new TCLRayNeoXTwo()));
        for (int i = 0; i < smartGlassesDevices.size(); i++){
            if (smartGlassesDevices.get(i).deviceModelName.equals(preferred)){
                // Move to start for earliest search priority
                smartGlassesDevices.add(0, smartGlassesDevices.remove(i));
                break;
            }
        }

        // Check for Audio Wearable
        if (preferred.equals(new AudioWearable().deviceModelName))
            smartGlassesDevices.add(0, new AudioWearable());

        //start loop
        aioRetryConnectionTask.run();
    }

    public void showNoGoogleAsrDialog(){
        new android.app.AlertDialog.Builder(getApplicationContext()).setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle("No Google API Key Provided")
                .setMessage("You have Google ASR enabled without an API key. Please turn off Google ASR or enter a valid API key.")
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                }).show();
    }

    //show a reference card on the smart glasses with title and body text
    public void sendReferenceCard(String title, String body) {
        EventBus.getDefault().post(new ReferenceCardSimpleViewRequestEvent(title, body));
    }

    //show a reference card on the smart glasses with title and body text
    public void sendRowsCard(String[] rowStrings) {
        EventBus.getDefault().post(new RowsCardViewRequestEvent(rowStrings));
    }

    //show a bullet point list card on the smart glasses with title and bullet points
    public void sendBulletPointList(String title, String [] bullets) {
        EventBus.getDefault().post(new BulletPointListViewRequestEvent(title, bullets));
    }

    //show a list of up to 4 rows of text. Only put a few characters per line!
    public void sendBulletPointList(String[] rowStrings) {
        EventBus.getDefault().post(new RowsCardViewRequestEvent(rowStrings));
    }

    public void sendReferenceCard(String title, String body, String imgUrl) {
        EventBus.getDefault().post(new ReferenceCardImageViewRequestEvent(title, body, imgUrl));
    }

    public void startScrollingText(String title){
        EventBus.getDefault().post(new ScrollingTextViewStartRequestEvent(title));
    }

    public void pushScrollingText(String text){
        EventBus.getDefault().post(new FinalScrollingTextRequestEvent(text));
    }

    public void stopScrollingText(){
        EventBus.getDefault().post(new ScrollingTextViewStopRequestEvent());
    }

    public void sendTextLine(String text) {
        EventBus.getDefault().post(new TextLineViewRequestEvent(text));
    }

    public void sendTextToSpeech(String text, String languageString) {
        EventBus.getDefault().post(new TextToSpeechEvent(text, languageString));
    }

    public void sendCenteredText(String text){
        EventBus.getDefault().post(new CenteredTextViewRequestEvent(text));
    }

    public void sendHomeScreen(){
        EventBus.getDefault().post(new HomeScreenEvent());
    }
}
