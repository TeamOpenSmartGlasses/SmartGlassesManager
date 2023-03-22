package com.smartglassesmanager.androidsmartphone.nlp;

import java.util.ArrayList;

import android.content.Context;
import android.util.Pair;

//rxjava internal comms
import io.reactivex.rxjava3.subjects.PublishSubject;
import org.json.JSONObject;

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

    public FuzzyMatch findNearMatches(String incomingString, String toFindString, double threshold){
        //int extraChars = 2; //some small number of chars before and after string in case the fuzzy match and what appears in trancript aren't exactly the same length
        //for (int i = 0; i < (incomingString.length() - toFindString.length() - extraChars); i++) {
        int highestIndex = -1;
        double lowestSimilarity = -1d;
        toFindString = toFindString.replaceAll("\\s+$", ""); //replace any leading/trailing spaces
        incomingString = incomingString + " "; //add a couple spaces to the end for comparison
        for (int i = 0; i <= (incomingString.length() - toFindString.length()); i++) {
            //String substring = incomingString.substring(i, i + toFindString.length() + extraChars);
            String substring = incomingString.substring(i, i + toFindString.length());
            double similarity = jw.similarity(substring, toFindString);
            if (similarity > lowestSimilarity){
                highestIndex = i;
                lowestSimilarity = similarity;
            }
        }

        //return highest match
        if (lowestSimilarity > threshold){
            FuzzyMatch match = new FuzzyMatch(Math.min(highestIndex, incomingString.length()-1), lowestSimilarity);
            match.setToFindString(toFindString);
            return match; //if the match was found in the spaces at the end, only return the last index of the input string
        } else{
            return null;
        }
    }

    //return the match and the location of the highest match in input array to Find String
    public Pair<FuzzyMatch, Integer> findBestMatch(String incomingString, ArrayList<String> toFindString, double threshold){
        FuzzyMatch bestMatch = new FuzzyMatch(-1,0);
        int bestMatchIdx = -1;
        for (int i = 0; i < toFindString.size(); i++) {
            String currToFindString = toFindString.get(i);
            FuzzyMatch currMatch = findNearMatches(incomingString, currToFindString, threshold);
            if (currMatch != null && (currMatch.getSimilarity() > bestMatch.getSimilarity())){
                bestMatch = currMatch;
                bestMatchIdx = i;
            }
        }
        if (bestMatch.getIndex() != -1) {
            return new Pair(bestMatch, bestMatchIdx);
        } else {
            return null;
        }
    }

    public int findNumMatches(String incomingString, String [] toMatch, double thresh){
        int matchCount = 0;
        for (int j = 0; j < toMatch.length; j++){
            FuzzyMatch matchLocation = findNearMatches(incomingString, toMatch[j], thresh);
            if (matchLocation != null && matchLocation.getIndex() != -1){
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
