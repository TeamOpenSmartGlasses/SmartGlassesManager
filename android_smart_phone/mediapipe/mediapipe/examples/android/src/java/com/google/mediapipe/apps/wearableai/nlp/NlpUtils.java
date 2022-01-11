package com.google.mediapipe.apps.wearableai.nlp;

import java.util.Arrays;

import android.util.Log;

import com.google.mediapipe.apps.wearableai.comms.MessageTypes;
import android.content.Context;

//rxjava internal comms
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.subjects.PublishSubject;
import org.json.JSONObject;
import org.json.JSONException;

//fuzzy string matching
import info.debatty.java.stringsimilarity.*;

public class NlpUtils {
    private static final String TAG = "WearableIntelligenceSystem_NlpUtils";

    private static JaroWinkler jw ;
    private static Context context;
    
    private static NlpUtils myself;

    //receive/send data stream
    PublishSubject<JSONObject> dataObservable;

    public NlpUtils(Context context){
        this.context = context;
        jw = new JaroWinkler();//probably should change this to somethings else. A failure, for example, is that the strings {effective memory : affective memory} have similiarity of 0.85, but strings {fective memory : affective memory} have similirity of 0.96 ... not great
    }

    public int findNearMatches(String incomingString, String toFindString, double threshold){
        //int extraChars = 2; //some small number of chars before and after string in case the fuzzy match and what appears in trancript aren't exactly the same length
        //for (int i = 0; i < (incomingString.length() - toFindString.length() - extraChars); i++) {
        int highestIndex = -1;
        double highestDistance = -1d;
        toFindString = toFindString.replaceAll("\\s+$", ""); //replace any leading/trailing spaces
        for (int i = 0; i <= (incomingString.length() - toFindString.length()); i++) {
            //String substring = incomingString.substring(i, i + toFindString.length() + extraChars);
            String substring = incomingString.substring(i, i + toFindString.length());
            double distance = jw.similarity(substring, toFindString);
            if (distance > highestDistance){
                highestIndex = i;
                highestDistance = distance;
            }
        }

        //return highest match
        if (highestDistance > threshold){
            return highestIndex;
        } else{
            return -1;
        }
    }

    public int findNumMatches(String incomingString, String [] toMatch, double thresh){
        int matchCount = 0;
        for (int j = 0; j < toMatch.length; j++){
            int matchLocation = findNearMatches(incomingString, toMatch[j], thresh);
            if (matchLocation != -1){
                matchCount++;
            }
        }
        return matchCount;
    }

    public static NlpUtils getInstance(Context c){
        if (myself == null){
            myself = new NlpUtils(c);
        }
        return myself;
    }

}
