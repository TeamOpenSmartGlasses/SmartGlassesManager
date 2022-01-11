package com.google.mediapipe.apps.wearableai;

import com.google.mediapipe.apps.wearableai.comms.RestServerComms;
import com.google.mediapipe.apps.wearableai.comms.VolleyCallback;

import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.subjects.PublishSubject;
import org.json.JSONObject;
import org.json.JSONArray;
import android.util.Log;
import org.json.JSONException;
import android.content.Context;

import com.google.mediapipe.apps.wearableai.comms.MessageTypes;

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
        try{
            String type = data.getString(MessageTypes.MESSAGE_TYPE_LOCAL);
            if (type.equals(MessageTypes.NATURAL_LANGUAGE_QUERY)){
                sendNaturalLanguageQuery(data);
            } else if (type.equals(MessageTypes.SEARCH_ENGINE_RESULT)){
                sendSearchEngineQuery(data);
            } else if (type.equals(MessageTypes.VISUAL_SEARCH_QUERY)){
                sendVisualSearchQuery(data);
            }
        } catch (JSONException e){
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
                        Log.d(TAG, "GOT visual search REST RESULT:");
                        Log.d(TAG, result.toString());
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

}
