package com.teamopensmartglasses.smartglassesmanager.speechrecognition;

import android.content.Context;

import com.teamopensmartglasses.smartglassesmanager.eventbusmessages.AudioChunkNewEvent;
import com.teamopensmartglasses.smartglassesmanager.speechrecognition.deepgram.SpeechRecDeepgram;
import com.teamopensmartglasses.smartglassesmanager.speechrecognition.google.SpeechRecGoogle;
import com.teamopensmartglasses.smartglassesmanager.speechrecognition.vosk.SpeechRecVosk;

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
    }

    public void startAsrFramework(ASR_FRAMEWORKS asrFramework) {
        startAsrFramework(asrFramework, "English");
    }

    public void startAsrFramework(ASR_FRAMEWORKS asrFramework, String language) {
        //kill old asr
        EventBus.getDefault().unregister(this);
        if (speechRecFramework != null){
            speechRecFramework.destroy();
        }

        //set new asr
        this.asrFramework = asrFramework;

        //create new asr
        if (this.asrFramework == ASR_FRAMEWORKS.VOSK_ASR_FRAMEWORK){
           speechRecFramework = new SpeechRecVosk(mContext);
        } else if (this.asrFramework == ASR_FRAMEWORKS.GOOGLE_ASR_FRAMEWORK){
            speechRecFramework = new SpeechRecGoogle(mContext, language);
        } else if (this.asrFramework == ASR_FRAMEWORKS.DEEPGRAM_ASR_FRAMEWORK){
            speechRecFramework = new SpeechRecDeepgram(mContext);
        }

        //start asr
        speechRecFramework.start();
        EventBus.getDefault().register(this);
    }

    @Subscribe
    public void onAudioChunkNewEvent(AudioChunkNewEvent receivedEvent){
        //redirect audio to the currently in use ASR framework
        speechRecFramework.ingestAudioChunk(receivedEvent.thisChunk);
    }

    public void destroy(){
        if (speechRecFramework != null){
            speechRecFramework.destroy();
        }
        EventBus.getDefault().unregister(this);
    }
}

