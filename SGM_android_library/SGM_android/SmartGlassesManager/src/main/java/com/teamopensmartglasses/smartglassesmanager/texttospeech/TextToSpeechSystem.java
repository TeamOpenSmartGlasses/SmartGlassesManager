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

    public void speak(String text){
        Log.d(TAG, "Speaking TTS: " + text);
        if (this.isLoaded){
            // TTS engine is initialized successfully
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
        speak(event.text);
    }

    @Subscribe
    public void handleScoEvent(ScoStartEvent event) {
        useSco(event.scoStart);
    }
}
