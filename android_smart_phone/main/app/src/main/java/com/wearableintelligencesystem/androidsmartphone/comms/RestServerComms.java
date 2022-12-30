package com.wearableintelligencesystem.androidsmartphone.comms;

import android.content.Context;
import android.util.Log;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

public class RestServerComms {
    private String TAG = "WearableAi_ASP_RestServerComms";

    private static RestServerComms restServerComms;

    //volley vars
    public RequestQueue mRequestQueue;
    private Context mContext;
    private String serverUrl;
    private int requestTimeoutPeriod = 10000;

    //endpoints
    public static final String NATURAL_LANGUAGE_QUERY_SEND_ENDPOINT = "/natural_language_query";
    //public static final String FINAL_TRANSCRIPT_SEND_ENDPOINT = "/final_transcript";
    public static final String FINAL_TRANSCRIPT_SEND_ENDPOINT = "/semantic_search";
    public static final String VISUAL_SEARCH_QUERY_SEND_ENDPOINT = "/visual_search_search";
    public static final String SEARCH_ENGINE_QUERY_SEND_ENDPOINT = "/search_engine_search";
    public static final String MAP_IMAGE_QUERY_SEND_ENDPOINT = "/get_static_map";
    public static final String TRANSLATE_TEXT_QUERY_ENDPOINT = "/translate_text_simple_query";
    public static final String REFERENCE_TRANSLATE_ENDPOINT = "/translate_reference_query";

    public static RestServerComms getInstance(Context c){
        if (restServerComms == null){
            restServerComms = new RestServerComms(c);
        }
        return restServerComms;
    }

    public RestServerComms(Context context) {
        // Instantiate the RequestQueue.
        mContext = context;
        mRequestQueue = Volley.newRequestQueue(mContext);
//        serverUrl = "https://wis.emexwearables.com/api";
        serverUrl = "https://wis.emexwearables.com/api";
//        serverUrl = "http://192.168.76.188:5000";
    }

    //handles requesting data, sending data
    public void restRequest(String endpoint, JSONObject data, VolleyCallback callback){
        //build the url
        String builtUrl = serverUrl + endpoint;

        //get the request type
        int requestType = Request.Method.GET;
        if (data == null){
            requestType = Request.Method.GET;
        } else { //there is data to send, send post
            requestType = Request.Method.POST;
        }

        // Request a json response from the provided URL.
        JsonObjectRequest request = new JsonObjectRequest(requestType, builtUrl, data,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        // Display the first 500 characters of the response string.
//                        Log.d(TAG, "Success requesting data, response:");
//                        Log.d(TAG, response.toString());
                        try{
                            if (response.getBoolean("success")){
                                callback.onSuccess(response);
                            } else{
                                callback.onFailure();
                            }
                        } catch (JSONException e){
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
                Log.d(TAG, "Failure sending data.");
//                if (retry < 3) {
//                    retry += 1;
//                    refresh();
//                    search(query);
//                }
            }
        });

        request.setRetryPolicy(new DefaultRetryPolicy(
                requestTimeoutPeriod,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

       mRequestQueue.add(request);
    }

}
