package com.smartglassesmanager.androidsmartphone.texttospeech;

import android.content.Context;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import com.smartglassesmanager.androidsmartphone.comms.MessageTypes;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.subjects.PublishSubject;

public class TextToSpeechSystem {

    private Context mContext;

    public boolean isLoaded = false;
    private TextToSpeech ttsModel;
    private PublishSubject<JSONObject> dataObservable;
    public Locale language = Locale.ENGLISH;
    private Disposable dataSubscriber;
    final Handler main_handler;

    public TextToSpeechSystem(Context context, PublishSubject<JSONObject> dataObservable, Locale language){
        this.mContext = context;
        this.language = language;
        this.setup(language);
        main_handler = new Handler();
        this.dataObservable = dataObservable;
        this.dataSubscriber = dataObservable.subscribe(i -> handleDataStream(i));

    }

    public void setup(Locale language){
        ttsModel = new TextToSpeech(mContext, status -> {
            if (status != -1) {
                ttsModel.setLanguage(language);
                Log.d("TextToSpeech","TTL Model initialized");
                this.isLoaded = true;
            }
        });
    }

    public void speak(String text, Locale locale){
        if(this.isLoaded){
            if(locale != language){
                ttsModel.setLanguage(locale);
                ttsModel.speak(text,TextToSpeech.QUEUE_FLUSH, null ,null);
                ttsModel.setLanguage(language);
                Log.d("TextToSpeech",text + " spoken");
            } else {
                ttsModel.speak(text,TextToSpeech.QUEUE_FLUSH, null ,null);
            }
        }
    }

    private void handleDataStream(JSONObject data){
        try {
            String dataType = data.getString(MessageTypes.MESSAGE_TYPE_LOCAL);
            if (dataType.equals(MessageTypes.TEXT_TO_SPEECH_SPEAK)) {
//                handleNewTextToSpeak(data);
            }
        } catch (JSONException e){
            e.printStackTrace();
        }
    }

//    private void handleNewTextToSpeak(JSONObject data){
//        String toSpeak;
//        String languageCodeFromData;
//
//        try {
//            //extract text to speak
//            toSpeak = data.getString(MessageTypes.TEXT_TO_SPEECH_SPEAK_DATA);
//            //extract the target language accent
//            Locale newLocale;
//            if (data.has(MessageTypes.TEXT_TO_SPEECH_TARGET_LANGUAGE_CODE)) {
//                languageCodeFromData = data.getString(MessageTypes.TEXT_TO_SPEECH_TARGET_LANGUAGE_CODE);
//
//                //get the local from the code
//                newLocale = WearableAiAspService.getLanguageFromCode(languageCodeFromData).getLocale();
//            } else {
//                newLocale = language;
//            }
//
//            // now say it
//            main_handler.post(new Runnable() {
//                public void run() {
//                    speak(toSpeak, newLocale);
//                }
//            });
//        }
//        catch (JSONException e){
//            e.printStackTrace();
//        }
//    }

    public void destroy(){
        ttsModel.shutdown();
        Log.d("TextToSpeech","TTS destroyed");
    }




}
