package com.teamopensmartglasses.smartglassesmanager.speechrecognition.deepgram;

import android.content.Context;
import android.util.Log;

import com.teamopensmartglasses.smartglassesmanager.eventbusmessages.SpeechRecOutputEvent;
import com.teamopensmartglasses.smartglassesmanager.speechrecognition.SpeechRecFramework;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.OkHttpClient;
        import okhttp3.Request;
        import okhttp3.Response;
        import okhttp3.WebSocket;
        import okhttp3.WebSocketListener;
        import okio.ByteString;

public class SpeechRecDeepgram extends SpeechRecFramework {
    private static final String SERVER_URL = "wss://api.deepgram.com/v1/listen?encoding=linear16&sample_rate=16000&smart_format=true&punctuate=true&utterances=true&diarize=true&filler_words=true&sentiment=true&language=en&model=nova-2";
    private static final String API_KEY = "";
    private WebSocket webSocket;
    public String TAG = "WearableAi_SpeechRecDeepgram";

    public SpeechRecDeepgram(Context context) {
    }

    @Override
    public void start() {
        Log.d(TAG, "starting Deepgram");
        initializeWebSocket();
    }

    @Override
    public void destroy() {
        Log.d(TAG, "destroying Deepgram");
        if (webSocket != null) {
            webSocket.close(1000, "Closing Connection");
        }
    }

    @Override
    public void ingestAudioChunk(byte[] audioChunk) {
        Log.d(TAG, "inject audio Deepgram");
        if (webSocket != null) {
            webSocket.send(ByteString.of(audioChunk));
        }
    }

    private void initializeWebSocket() {
        Log.d(TAG, "init socket Deepgram");
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(SERVER_URL)
                .addHeader("Authorization", "Token " + API_KEY)
                .build();

        webSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                // Connection opened
                Log.d(TAG, "opened Deepgram");
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                // Handle incoming messages (transcriptions)
                // Use text JSON parsing to extract transcription and update your UI accordingly
                Log.d(TAG, "got message Deepgram");
                Log.d(TAG, text);

                parseDeepgramMessage(text);
            }

            @Override
            public void onClosing(WebSocket webSocket, int code, String reason) {
                webSocket.close(1000, null);
                Log.d(TAG, "got closing Deepgram");
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                // Handle failure
                Log.d(TAG, "failure in Deepgram");
            }
        });

        client.dispatcher().executorService().shutdown();
    }

    public void parseDeepgramMessage(String jsonString) {
        try {
            JSONObject obj = new JSONObject(jsonString);

            // Extracting fields from the JSON string
            String type = obj.getString("type");
            JSONArray channelIndex = obj.getJSONArray("channel_index");
            double duration = obj.getDouble("duration");
            double start = obj.getDouble("start");
            boolean isFinal = obj.getBoolean("is_final");
            boolean speechFinal = obj.getBoolean("speech_final");

            // Parsing nested JSON objects
            JSONObject channel = obj.getJSONObject("channel");
            JSONArray alternatives = channel.getJSONArray("alternatives");
            JSONObject firstAlternative = alternatives.getJSONObject(0);
            String transcript = firstAlternative.getString("transcript");
            double confidence = firstAlternative.getDouble("confidence");

            // Parsing metadata
            JSONObject metadata = obj.getJSONObject("metadata");
            String requestId = metadata.getString("request_id");
            JSONObject modelInfo = metadata.getJSONObject("model_info");
            String modelName = modelInfo.getString("name");
            String modelVersion = modelInfo.getString("version");
            String modelArch = modelInfo.getString("arch");
            String modelUuid = metadata.getString("model_uuid");

            boolean fromFinalize = obj.getBoolean("from_finalize");

            // Output extracted data to verify
            System.out.println("Type: " + type);
            System.out.println("Channel Index: " + channelIndex.toString());
            System.out.println("Duration: " + duration);
            System.out.println("Start: " + start);
            System.out.println("Is Final: " + isFinal);
            System.out.println("Speech Final: " + speechFinal);
            System.out.println("Transcript: " + transcript);
            System.out.println("Confidence: " + confidence);
            System.out.println("Request ID: " + requestId);
            System.out.println("Model Name: " + modelName);
            System.out.println("Model Version: " + modelVersion);
            System.out.println("Model Arch: " + modelArch);
            System.out.println("Model UUID: " + modelUuid);
            System.out.println("From Finalize: " + fromFinalize);

            //send it to the system
            EventBus.getDefault().post(new SpeechRecOutputEvent(transcript, (long) start, false));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}