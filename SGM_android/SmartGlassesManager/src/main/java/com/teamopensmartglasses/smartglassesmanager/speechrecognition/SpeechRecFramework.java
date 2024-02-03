package com.teamopensmartglasses.smartglassesmanager.speechrecognition;

import android.content.Context;

public abstract class SpeechRecFramework {
    private ASR_FRAMEWORKS asrFramework;
    private Context mContext;

    public abstract void start();
    public abstract void destroy();
    public abstract void ingestAudioChunk(byte [] audioChunk);
}
