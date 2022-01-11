//NOTES:
//there is a race condition in how the sockets are setup
//if we restartSocket() then SendThread or ReceiveThread fails, it will set the mConnectState back to 0 even though we are currently trying to connect
//need to have some handler that only send mConnectState = 0 if it's currently = 2. If it's =1, then don't change it


// Copyright 2019 The MediaPipe Authors.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.mediapipe.apps.wearableai.voicecommand;

import com.google.mediapipe.apps.wearableai.comms.MessageTypes;

import java.util.concurrent.ExecutionException;
import java.lang.InterruptedException;
import com.google.mediapipe.apps.wearableai.database.WearableAiRoomDatabase;

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

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.LifecycleService;
import androidx.annotation.Nullable;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.ArrayList;
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
import android.media.RingtoneManager;
import android.media.Ringtone;
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
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import com.google.mediapipe.components.FrameProcessor;
import com.google.mediapipe.components.PermissionHelper;
import com.google.mediapipe.framework.AndroidAssetUtil;
import com.google.mediapipe.framework.AndroidPacketCreator;
import com.google.mediapipe.glutil.EglManager;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.util.Log;
import com.google.mediapipe.formats.proto.LandmarkProto.NormalizedLandmark;
import com.google.mediapipe.formats.proto.LandmarkProto.NormalizedLandmarkList;
import com.google.mediapipe.framework.Packet;
import com.google.mediapipe.framework.PacketGetter;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.HashMap;
import java.util.Map;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
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

//lots of repeat imports...
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

//import android.support.v4.app.NotificationCompat;
import androidx.core.app.NotificationCompat;

import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.subjects.PublishSubject;
import org.json.JSONObject;
import org.json.JSONException;

import java.util.Arrays;

//nlp
import com.google.mediapipe.apps.wearableai.nlp.NlpUtils;

//parse interpret
public class VoiceCommandServer {
    private  final String TAG = "WearableAi_VoiceCommand";
    //one stream to receive transcripts, data, etc.
    public PublishSubject<JSONObject> dataObservable;
    private Disposable dataSubscriber;

    //nlp
    private NlpUtils nlpUtils;

    //voice command stuff
    private ArrayList<VoiceCommand> voiceCommands;
    private ArrayList<String> wakeWords;

    //private ArrayList<String> voiceCommands;
    private Context mContext;

    //voice command fuzzy search threshold
    private final double wakeWordThreshold = 0.90;
    private final double commandThreshold = 0.90;

    //database to save voice commmands to
    public VoiceCommandRepository mVoiceCommandRepository;

    public VoiceCommandServer(PublishSubject<JSONObject> observable, VoiceCommandRepository voiceCommandRepository, Context context){
        //setup nlp
        nlpUtils = NlpUtils.getInstance(context);

        mVoiceCommandRepository = voiceCommandRepository;

        mContext = context;

        dataObservable = observable;
        dataSubscriber = dataObservable.subscribe(i -> handleDataStream(i));

        //get all voice commands
        voiceCommands = new ArrayList<VoiceCommand>();
        voiceCommands.add(new MxtVoiceCommand(context));
        voiceCommands.add(new NaturalLanguageQueryVoiceCommand(context));
        voiceCommands.add(new SearchEngineVoiceCommand(context));
        voiceCommands.add(new SwitchModesVoiceCommand(context));

        wakeWords = new ArrayList<>(Arrays.asList(new String [] {"hey computer", "hey google", "alexa", "licklider", "lickliter", "mind extension", "mind expansion", "wearable AI", "ask wolfram"}));
    }
    
    public void setObservable(PublishSubject observable){
        dataObservable = observable;
    }

    private void handleDataStream(JSONObject data){
        try {
            String dataType = data.getString(MessageTypes.MESSAGE_TYPE_LOCAL);
            if (dataType.equals(MessageTypes.FINAL_TRANSCRIPT)){
                parseTranscript(data);
            }
        } catch (JSONException e){
            e.printStackTrace();
        }
    }

    private void parseTranscript(JSONObject data){
        String transcript = "";
        long transcriptTime = 0l;
        long transcriptId = 0l;
        try{
            transcript = data.getString(MessageTypes.TRANSCRIPT_TEXT).toLowerCase();
            transcriptTime = data.getLong(MessageTypes.TIMESTAMP);
            transcriptId = data.getLong(MessageTypes.TRANSCRIPT_ID);
        } catch (JSONException e){
            e.printStackTrace();
        }

        //loop through all voice commands to see if any of their wake words match
        for (int i = 0; i < voiceCommands.size(); i++){
            //check if any of the wake words match
            for (int j = 0; j < voiceCommands.get(i).getWakeWords().size(); j++){
                String currWakeWord = voiceCommands.get(i).getWakeWords().get(j);
                int wakeWordLocation = nlpUtils.findNearMatches(transcript, currWakeWord, wakeWordThreshold); //transcript.indexOf(currWakeWord);
                if (wakeWordLocation != -1){ //if the substring "wake word" is in the larger string "transcript"
                    //we found a command, now get its arguments and run it
                    String preArgs = transcript.substring(0, wakeWordLocation);
                    String postArgs = transcript.substring(wakeWordLocation + currWakeWord.length());
                    voiceCommands.get(i).runCommand(this, preArgs, currWakeWord, j, postArgs, transcriptTime, transcriptId);
                    return;
                }
            }
        }

        //loop through all global wake words to see if any match
        for (int i = 0; i < wakeWords.size(); i++){
            String currWakeWord = wakeWords.get(i);
            int wakeWordLocation = nlpUtils.findNearMatches(transcript, currWakeWord, wakeWordThreshold); //transcript.indexOf(currWakeWord);
            if (wakeWordLocation != -1){ //if the substring "wake word" is in the larger string "transcript"
                //we found a command, now get its arguments and run it
                String preArgs = transcript.substring(0, wakeWordLocation);
                String rest = transcript.substring(wakeWordLocation);
                parseCommand(preArgs, currWakeWord, rest, transcriptTime, transcriptId);
                return;
            }

        }
    }

    private void parseCommand(String preArgs, String wakeWord, String rest, long transcriptTime, long transcriptId){
        for (int i = 0; i < voiceCommands.size(); i++){
            for (int j = 0; j < voiceCommands.get(i).getCommands().size(); j++){
                String currCommand = voiceCommands.get(i).getCommands().get(j);
                int commandLocation = nlpUtils.findNearMatches(rest, currCommand, commandThreshold);
                if (commandLocation != -1){ //if the substring "wake word" is in the larger string "transcript"
                    String postArgs = rest.substring(commandLocation + currCommand.length());
                    voiceCommands.get(i).runCommand(this, preArgs, wakeWord, j, postArgs, transcriptTime, transcriptId);
                    return;
                }
            }
        }
    }


//    private void runCommand(String preArgs, String wakeWord, String command, String postArgs){
//        Log.d(TAG, "RUN COMMAND with postARgs:" + postArgs);
//        //below is how we WANT to call commands, by creating command classes and calling their 'run' functions here... but for sake of time I am doing hack below
////        for (int i = 0; i < commandFuncs.size(); i++){
////            if (command.equals(commandFuncs[i])){ //if the substring "wake word" is in the larger string "transcript"
////                //commandFuncs[i].runCommand(wakeWord, voiceCommands[i], commandLocation + voiceCommands[i].length());
////            }
////        }
////
//        //save command
//        VoiceCommandCreator.create(preArgs, wakeWord, command, postArgs, "transcript_ASG", mContext, mVoiceCommandRepository);
//         try{
//            VoiceCommandEntity latestCommand = mVoiceCommandRepository.getLatestCommand("start conversation");
//            Log.d(TAG, "getLatestCommand output on 'start conversation' command");
//            Log.d(TAG, latestCommand.getCommand());
//            Log.d(TAG, latestCommand.getWakeWord());
//            Log.d(TAG, latestCommand.getPostArgs());
//         } catch (ExecutionException | InterruptedException e) {
//             e.printStackTrace();
//         }
//
//
//
//        //run command finder
//        if (command.contains("start conversation")){ //if the substring "wake word" is in the larger string "transcript"
//            convoTag(true); //this is precicely where it gets hacky, this should be generic and running "run"function in voice command class
//        } else if (command.contains("stop conversation")){
//            convoTag(false);
//        } else if ((command.contains("effective search")) || (command.contains("affective search"))){
//            affectiveSearch(postArgs);
//        }
//    }

    private void convoTag(boolean start){
        if (start){
            Log.d(TAG, "********** starting conversation");
        } else {
            Log.d(TAG, "********** stopping conversation");
        }
    }

    private void affectiveSearch(String postArgs){
        try{
            postArgs = postArgs.trim();
            int idxSpace = postArgs.indexOf(' ');
            String emotion = "";
            if (idxSpace != -1){
                emotion = postArgs.trim().substring(0, postArgs.indexOf(' '));
            } else{
                emotion = postArgs;
            }
            Log.d(TAG, "emotion is: " + emotion);
            JSONObject affective_search_query = new JSONObject();
            affective_search_query.put(MessageTypes.MESSAGE_TYPE_LOCAL, "affective_search_query");
            affective_search_query.put("emotion", emotion);
            dataObservable.onNext(affective_search_query);
        } catch (JSONException e){
            e.printStackTrace();
        }
    }

}






