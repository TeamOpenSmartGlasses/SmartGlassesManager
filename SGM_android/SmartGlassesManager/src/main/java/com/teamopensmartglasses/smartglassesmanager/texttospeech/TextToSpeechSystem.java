package com.teamopensmartglasses.smartglassesmanager.texttospeech;

import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import com.teamopensmartglasses.smartglassesmanager.eventbusmessages.ScoStartEvent;
import com.teamopensmartglasses.smartglassesmanager.eventbusmessages.TextToSpeechEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.Locale;

public class TextToSpeechSystem {
    private  final String TAG = "WearableAi_TextToSpeechSystem";

    private Context mContext;
    public boolean isLoaded = false;
    private TextToSpeech ttsModel;
    private boolean sco;

    public TextToSpeechSystem(Context context){
        this.mContext = context;
        this.sco = false;
        EventBus.getDefault().register(this);
    }

    public void useSco(boolean useSco){
        this.sco = useSco;
    }

//    public void setup(Locale language){
    public void setup(){
        Locale language = Locale.ENGLISH;
        ttsModel = new TextToSpeech(mContext, status -> {
            if (status == TextToSpeech.SUCCESS) {
                ttsModel.setLanguage(language);
                ttsModel.setSpeechRate(1.6f);
                ttsModel.setPitch(0.8f);
//                ttsModel.setAudioAttributes(new AudioAttributes.Builder()
//                        .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
//                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
//                        .build());
                Log.d("TextToSpeech","TTS Model initialized");
                this.isLoaded = true;
                Log.d(TAG, ttsModel.getVoices().toString());
                Log.d(TAG, ttsModel.getDefaultEngine());
            } else {
                Log.d(TAG, "TTS failed with code: " + status);
            }
        });
    }

    //default speak (english)
    public void speak(String text){
        speak(text, Locale.ENGLISH) ;
    }

    public void speak(String text, Locale locale){
        Log.d(TAG, "Speaking TTS: " + text + " --- In language: " + locale.toString());
        if (this.isLoaded){
            // TTS engine is initialized successfully
            int result = ttsModel.setLanguage(locale);
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e(TAG, "Language is not supported or missing data: " + locale);
                return;
            }

            if (sco) {
                Bundle ttsParams = new Bundle();
                ttsParams.putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_VOICE_CALL);
                ttsParams.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f);
                ttsModel.speak(text, TextToSpeech.QUEUE_FLUSH, ttsParams, null);
            } else {
                ttsModel.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
            }
        } else {
            Log.d(TAG, "TTS failed because not loaded.");
        }
    }

    public void destroy(){
        EventBus.getDefault().unregister(this);
        if (ttsModel != null) {
            ttsModel.shutdown();
        }

        Log.d("TextToSpeech","TTS destroyed");
    }


    @Subscribe
    public void handleTtsEvent(TextToSpeechEvent event) {
        String languageString = event.language;
        Locale language = Locale.ENGLISH; // Default to English
        switch (languageString.toLowerCase()) {
            case "english":
                language = Locale.ENGLISH;
                break;
            case "chinese":
                language = Locale.CHINESE; // or Locale.SIMPLIFIED_CHINESE for more specificity
                break;
            case "italian":
                language = Locale.ITALIAN;
                break;
            case "japanese":
                language = Locale.JAPANESE;
                break;
            case "spanish":
                language = new Locale("es", "ES");
                break;
            case "russian":
                language = new Locale("ru", "RU");
                break;
            case "dutch":
                language = new Locale("nl", "NL");
                break;
            case "hebrew":
                language = new Locale("iw", "IL");
                break;
            default:
                // Log or alert the user that the language is not supported for direct Locale constants
                Log.d(TAG, "Language not supported by TTS: " + languageString);
                break;
        }
        speak(event.text, language);
    }

    @Subscribe
    public void handleScoEvent(ScoStartEvent event) {
        useSco(event.scoStart);
    }
}
