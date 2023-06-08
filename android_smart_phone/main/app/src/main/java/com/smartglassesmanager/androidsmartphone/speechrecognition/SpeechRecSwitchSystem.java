package com.smartglassesmanager.androidsmartphone.speechrecognition;

import android.content.Context;
import android.util.Log;

import com.smartglassesmanager.androidsmartphone.eventbusmessages.AudioChunkNewEvent;
import com.smartglassesmanager.androidsmartphone.speechrecognition.google.SpeechRecGoogle;
import com.smartglassesmanager.androidsmartphone.speechrecognition.vosk.SpeechRecVosk;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

//send audio to one of the built in ASR frameworks.
public class SpeechRecSwitchSystem {
    private final String TAG = "WearableAi_SpeechRecSwitchSystem";
    private ASR_FRAMEWORKS asrFramework;
    private SpeechRecFramework speechRecFramework;
    private SpeechRecVosk speechRecVosk;
    private SpeechRecGoogle speechRecGoogle;
    private Context mContext;

    public SpeechRecSwitchSystem(Context mContext) {
        this.mContext = mContext;

        EventBus.getDefault().register(this);
    }

    public void startAsrFramework(ASR_FRAMEWORKS asrFramework) {
        //kill old asr
        if (speechRecFramework != null){
            speechRecFramework.destroy();
        }

        //set new asr
        this.asrFramework = asrFramework;

        //start new asr
        if (this.asrFramework == ASR_FRAMEWORKS.VOSK_ASR_FRAMEWORK){
           speechRecFramework = new SpeechRecVosk(mContext);
        } else if (this.asrFramework == ASR_FRAMEWORKS.GOOGLE_ASR_FRAMEWORK){
            speechRecFramework = new SpeechRecGoogle(mContext);
        }
        speechRecFramework.start();
    }

    @Subscribe
    public void onAudioChunkNewEvent(AudioChunkNewEvent receivedEvent){
//        Log.d(TAG, "Got chunk");
        //redirect audio to the currently in use ASR framework
        speechRecFramework.ingestAudioChunk(receivedEvent.thisChunk);
//        if (asrFramework == ASR_FRAMEWORKS.VOSK_ASR_FRAMEWORK){
////            EventBus.getDefault().post(new VoskAudioChunkNewEvent(receivedEvent.thisChunk));
//        } else if (asrFramework == ASR_FRAMEWORKS.GOOGLE_ASR_FRAMEWORK){
//            EventBus.getDefault().post(new GoogleAudioChunkNewEvent(receivedEvent.thisChunk));
//        }
    }

    public void destroy(){
        if (speechRecFramework != null){
            speechRecFramework.destroy();
        }
        EventBus.getDefault().unregister(this);
    }
}

