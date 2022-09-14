package com.wearableintelligencesystem.androidsmartglasses.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.text.method.ScrollingMovementMethod;
import android.util.Base64;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.wearableintelligencesystemandroidsmartglasses.R;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.wearableintelligencesystem.androidsmartglasses.ASPClientSocket;
import com.wearableintelligencesystem.androidsmartglasses.ImageAdapter;
import com.wearableintelligencesystem.androidsmartglasses.MainActivity;
import com.wearableintelligencesystem.androidsmartglasses.comms.MessageTypes;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class VisualSearchUi extends ASGFragment {
    private final String TAG = "WearableAi_VisualSearchUi";

    //store information from visual search response
    List<String> thumbnailImages;

    //visual search ui
    ImageView viewfinder;

    private Handler myHandler;

    public VisualSearchUi() {
        fragmentLabel = "Visual Search";
    }

    public void captureVisualSearchImage(){
        Toast.makeText(mainActivity, "Capturing image...", Toast.LENGTH_SHORT).show();

        if (mainActivity.mBound) {
            //byte[] curr_cam_image = mService.getCurrentCameraImage();
            //mService.sendGlBoxCurrImage();
            mainActivity.mService.sendVisualSearch();
        }
    }

    private void setupVisualSearchViewfinder() {
        //live life captions mode gui setup
        viewfinder = (ImageView) getActivity().findViewById(R.id.camera_viewfinder);

        updateViewFindFrame();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.viewfinder, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.onViewCreated(view, savedInstanceState);

        //start viewfinder
        myHandler = new Handler();
        setupVisualSearchViewfinder();
    }

    private void updateViewFindFrame(){
        if (mainActivity.mBound){
            byte[] curr_cam_image = mainActivity.mService.getCurrentCameraImage();
            Bitmap curr_cam_bmp = BitmapFactory.decodeByteArray(curr_cam_image, 0, curr_cam_image.length);
            viewfinder.setImageBitmap(curr_cam_bmp);
            Log.d(TAG, "got frame");
        } else {
            Log.d(TAG, "Mbound not true");
        }

        //update the current preview frame in
        int frame_delay = 200; //milliseconds
        myHandler.postDelayed(new Runnable() {
            public void run() {
                    updateViewFindFrame();
            }
        }, frame_delay);
    }

    @Override
    protected void onBroadcastReceive(Context context, Intent intent){
        String action = intent.getAction();
        if (ASPClientSocket.ACTION_UI_DATA.equals(action)) {
            try{
                JSONObject data = new JSONObject(intent.getStringExtra(ASPClientSocket.RAW_MESSAGE_JSON_STRING));
                String typeOf = data.getString(MessageTypes.MESSAGE_TYPE_LOCAL);
                //run visual search results
                if (typeOf.equals(MessageTypes.VISUAL_SEARCH_RESULT)) {
                    showVisualSearchResults((new JSONArray(data.getString(MessageTypes.VISUAL_SEARCH_DATA))));
                }
            } catch (JSONException e){
                e.printStackTrace();
            }
        } else if (action.equals(MessageTypes.SG_TOUCHPAD_EVENT)) {
            int keyCode = intent.getIntExtra(MessageTypes.SG_TOUCHPAD_KEYCODE, -1);
            switch (keyCode) {
                case KeyEvent.KEYCODE_ENTER:
                    captureVisualSearchImage();
                    break;
            }
        }
    }

    @Override
    public void onPause(){
        super.onPause();
        myHandler.removeCallbacksAndMessages(null);
    }

    @Override
    public void onResume(){
        super.onResume();
        setupVisualSearchViewfinder();
    }

    private void showVisualSearchResults(JSONArray references){
        //show the reference
        Bundle args = new Bundle();
        args.putString("images", references.toString());
        navController.navigate(R.id.nav_image_grid, args);
    }

    @Override
    public IntentFilter makeComputeUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(MessageTypes.SG_TOUCHPAD_EVENT);
        intentFilter.addAction(ASPClientSocket.ACTION_UI_DATA);

        return intentFilter;
    }
}

