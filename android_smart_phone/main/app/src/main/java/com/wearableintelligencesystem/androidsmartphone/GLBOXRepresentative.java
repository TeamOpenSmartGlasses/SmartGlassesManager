package com.wearableintelligencesystem.androidsmartphone;

import com.wearableintelligencesystem.androidsmartphone.comms.RestServerComms;
import com.wearableintelligencesystem.androidsmartphone.comms.VolleyCallback;

import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.subjects.PublishSubject;
import org.json.JSONObject;
import org.json.JSONArray;
import android.util.Log;
import org.json.JSONException;
import android.content.Context;

import com.wearableintelligencesystem.androidsmartphone.comms.MessageTypes;

import java.util.Locale;

class GLBOXRepresentative {
    private static final String TAG = "WearableAi_GLBOXRepresentative";

    //REST server
    private RestServerComms restServerComms;

    //receive/send data stream
    PublishSubject<JSONObject> dataObservable;
    Disposable dataSub;

    Context context;
    ASGRepresentative asgRep;

    //for now, we have access to asgRep, but later all messages should be sent on event bus
    GLBOXRepresentative(Context context, PublishSubject<JSONObject> dataObservable, ASGRepresentative asgRep){
        this.context = context;
        this.asgRep = asgRep;

        //start rest server connection handler
        restServerComms = RestServerComms.getInstance(context);

        //receive/send data
        this.dataObservable = dataObservable;
        dataSub = this.dataObservable.subscribe(i -> handleDataStream(i));
    }

    //receive audio and send to vosk
    public void handleDataStream(JSONObject data){
        //first check if it's a type we should handle
        try {
            String type = data.getString(MessageTypes.MESSAGE_TYPE_LOCAL);
            if (type.equals(MessageTypes.NATURAL_LANGUAGE_QUERY)) {
                sendNaturalLanguageQuery(data);
            } else if (type.equals(MessageTypes.SEARCH_ENGINE_QUERY)) {
                sendSearchEngineQuery(data);
            } else if (type.equals(MessageTypes.VISUAL_SEARCH_QUERY)) {
                sendVisualSearchQuery(data);
            } else if (type.equals(MessageTypes.FINAL_TRANSCRIPT_FOREIGN)) {
                sendTranslateRequest(data);
            } else if (type.equals(MessageTypes.REFERENCE_TRANSLATE_SEARCH_QUERY)) {
                sendReferenceTranslateQuery(data);
            }else if (type.equals(MessageTypes.OBJECT_TRANSLATION_REQUEST)) {
                sendObjectTranslateRequest(data);
            }else if (type.equals(MessageTypes.CONTEXTUAL_SEARCH_REQUEST)) {
                sendContextualSearch(data);
            } else if (type.equals(MessageTypes.FINAL_TRANSCRIPT)) {
                Log.d(TAG, "GOT FINAL TRANSCRIPT");
                //sendFinalTranscript(data);
            }

    }
            catch (JSONException e){
            e.printStackTrace();
        }

    }

    public void sendVisualSearchQuery(JSONObject data){
        Log.d(TAG, "Running sendVisualSearchQuery");
        try{
            JSONObject restMessage = new JSONObject();
            restMessage.put("image", data.getString(MessageTypes.VISUAL_SEARCH_IMAGE));
            restServerComms.restRequest(RestServerComms.VISUAL_SEARCH_QUERY_SEND_ENDPOINT, restMessage, new VolleyCallback(){
                @Override
                    public void onSuccess(JSONObject result){
                        asgRep.sendCommandResponse("Search success, displaying results.");
                        try{
                            asgRep.sendVisualSearchResults(result.getJSONArray("response"));
                        } catch (JSONException e){
                            e.printStackTrace();
                        }
                    }
                @Override
                public void onFailure(){
                    asgRep.sendCommandResponse("Search failed, please try again.");
                }

                });
        } catch (JSONException e){
            e.printStackTrace();
        }
    }

    public void sendContextualSearch(JSONObject data){
        Log.d(TAG, "Running sendContextualSearch");
        try{
            JSONObject restMessage = new JSONObject();
            restMessage.put("transcript", data.getString(MessageTypes.TRANSCRIPT_TEXT).toLowerCase());
            restMessage.put("timestamp", data.getString(MessageTypes.TIMESTAMP));
            restMessage.put("id", data.getString(MessageTypes.TRANSCRIPT_ID));

            restServerComms.restRequest(RestServerComms.FINAL_TRANSCRIPT_SEND_ENDPOINT, restMessage, new VolleyCallback(){
                @Override
                public void onSuccess(JSONObject result){
                    asgRep.sendCommandResponse("Final transcript to contextual search send success, displaying results.");
                    //check if there was a result at all
                    try {
                        boolean search_result = result.getBoolean("result");
                        if (search_result) {
                            asgRep.sendSearchEngineResults(result);
                        }
                    } catch (JSONException e){
                        e.printStackTrace();
                    }
                }
                @Override
                public void onFailure(){
                    asgRep.sendCommandResponse("Contextual search failed, please try again.");
                }
            });
        } catch (JSONException e){
            e.printStackTrace();
        }
    }

//
//    public void sendFinalTranscript(JSONObject data){
//        Log.d(TAG, "Running sendFinalTranscript");
//        try{
//            JSONObject restMessage = new JSONObject();
//            restMessage.put("transcript", data.getString(MessageTypes.TRANSCRIPT_TEXT).toLowerCase());
//            restMessage.put("timestamp", data.getString(MessageTypes.TIMESTAMP));
//            restMessage.put("id", data.getString(MessageTypes.TRANSCRIPT_ID));
//
//            restServerComms.restRequest(RestServerComms.FINAL_TRANSCRIPT_SEND_ENDPOINT, restMessage, new VolleyCallback(){
//                @Override
//                public void onSuccess(JSONObject result){
//                    asgRep.sendCommandResponse("Final transcript send success, displaying results.");
//                    //check if there was a result at all
//                    try {
//                        boolean search_result = result.getBoolean("result");
//                        if (search_result) {
//                            asgRep.sendSearchEngineResults(result);
//                        }
//                    } catch (JSONException e){
//                        e.printStackTrace();
//                    }
//                }
//                @Override
//                public void onFailure(){
//                    asgRep.sendCommandResponse("Semantic search failed, please try again.");
//                }
//            });
//        } catch (JSONException e){
//            e.printStackTrace();
//        }
//    }

    private void sendNaturalLanguageQuery(JSONObject data){
        Log.d(TAG, "Running sendNaturalLanguageQuery");
        try{
            JSONObject restMessage = new JSONObject();
            restMessage.put("query", data.get(MessageTypes.TEXT_QUERY));
            restServerComms.restRequest(RestServerComms.NATURAL_LANGUAGE_QUERY_SEND_ENDPOINT, restMessage, new VolleyCallback(){
                @Override
                    public void onSuccess(JSONObject result){
                        Log.d(TAG, "GOT natural language REST RESULT:");
                        Log.d(TAG, result.toString());
                        try{
                            asgRep.sendCommandResponse("Answer: " + result.getString("response"));
                        } catch (JSONException e){
                            e.printStackTrace();
                        }
                    }
                @Override
                public void onFailure(){
                    asgRep.sendCommandResponse("Query failed, please try again.");
                }

                });
        } catch (JSONException e){
            e.printStackTrace();
        }
    }

    private void sendSearchEngineQuery(JSONObject data){
        Log.d(TAG, "Running sendSearchEngineQuery");
        try{
            JSONObject restMessage = new JSONObject();
            restMessage.put("query", data.get(MessageTypes.TEXT_QUERY));
            restServerComms.restRequest(RestServerComms.SEARCH_ENGINE_QUERY_SEND_ENDPOINT, restMessage, new VolleyCallback(){
                @Override
                public void onSuccess(JSONObject result){
                    Log.d(TAG, "GOT search engine REST RESULT:");
                    Log.d(TAG, result.toString());
                    asgRep.sendCommandResponse("Search success, displaying results.");
                    try{
                        asgRep.sendSearchEngineResults(result.getJSONObject("response"));
                    } catch (JSONException e){
                        e.printStackTrace();
                    }
                }
                @Override
                public void onFailure(){
                    asgRep.sendCommandResponse("Search failed, please try again.");
                }

            });
        } catch (JSONException e){
            e.printStackTrace();
        }
    }
    private void sendTranslateRequest(JSONObject data){
        Log.d(TAG, "Running sendTranslateRequest");
        try{
            JSONObject restMessage = new JSONObject();
            restMessage.put("query", data.get(MessageTypes.TRANSCRIPT_TEXT));
            restMessage.put("source_language", "fr");
            restMessage.put("target_language", "en");
            restServerComms.restRequest(RestServerComms.TRANSLATE_TEXT_QUERY_ENDPOINT, restMessage, new VolleyCallback(){
                @Override
                public void onSuccess(JSONObject result){
//                    Log.d(TAG, "GOT translate TEXT RESULT:");
//                    Log.d(TAG, result.toString());
                    try{
                        asgRep.sendTranslateResults(result.getString("response"));
                    } catch (JSONException e){
                        e.printStackTrace();
                    }
                }
                @Override
                public void onFailure() {
                }
            });
        } catch (JSONException e){
            e.printStackTrace();
        }
    }
    public void sendObjectTranslateRequest(JSONObject data){
//        Log.d(TAG, "Running sendObjectTranslateRequest");
        try{
            JSONObject restMessage = new JSONObject();
            restMessage.put("query", data.get(MessageTypes.TRANSCRIPT_TEXT));
            restMessage.put("source_language", "en");
            restMessage.put("target_language", "fr");
            restServerComms.restRequest(RestServerComms.TRANSLATE_TEXT_QUERY_ENDPOINT, restMessage, new VolleyCallback(){
                @Override
                public void onSuccess(JSONObject result){
//                    Log.d(TAG, "GOT translated TEXT RESULT:");
//                    Log.d(TAG, result.toString());
                    JSONObject resultToSend = new JSONObject();
                    try {
                        resultToSend.put("source",data.get(MessageTypes.TRANSCRIPT_TEXT));
                        resultToSend.put("target",result.getString("response"));
                        asgRep.sendObjectTranslationResults(resultToSend);
//                        Log.d(TAG, resultToSend.toString());
                    } catch (JSONException e) {
                        asgRep.sendCommandResponse("Response Falied");
                        e.printStackTrace();
                    }
                }
                @Override
                public void onFailure() {

                }
            });
        } catch (JSONException e){
            e.printStackTrace();
        }
    }

    private void sendReferenceTranslateQuery(JSONObject data){
        Log.d(TAG, "Running sendReferenceTranslateQuery");
        try{
            JSONObject restMessage = new JSONObject();
            restMessage.put("query", data.get(MessageTypes.REFERENCE_TRANSLATE_DATA));
            restMessage.put("source_language", "en");
            restMessage.put("target_language", data.get(MessageTypes.REFERENCE_TRANSLATE_TARGET_LANGUAGE_CODE));
            restServerComms.restRequest(RestServerComms.REFERENCE_TRANSLATE_ENDPOINT, restMessage, new VolleyCallback(){
                @Override
                public void onSuccess(JSONObject result){
                    Log.d(TAG, "GOT reference Translate REST RESULT:");
                    Log.d(TAG, result.toString());
                    asgRep.sendCommandResponse("Reference translate success, displaying results.");
                    try{
                        asgRep.sendReferenceTranslateResults(result.getJSONObject("response"));
                    } catch (JSONException e){
                        e.printStackTrace();
                    }
                }
                @Override
                public void onFailure(){
                    asgRep.sendCommandResponse("Reference translate failed, please try again.");
                }
            });
        } catch (JSONException e){
            e.printStackTrace();
        }
    }


}
