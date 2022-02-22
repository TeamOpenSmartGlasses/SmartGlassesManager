package com.wearableintelligencesystem.androidsmartglasses.ui;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.wearableintelligencesystemandroidsmartglasses.R;
import com.wearableintelligencesystem.androidsmartglasses.ImageAdapter;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

public class WakeWordPostUi extends ASGFragment {
    private final String TAG = "WearableAi_WakeWordPostUi";

    public WakeWordPostUi() {
        fragmentLabel = "Wake Word Received";
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.wake_word_post_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.onViewCreated(view, savedInstanceState);

        String commandList = getArguments().getString("command_list", null);
        TextView commandListTextView = view.findViewById(R.id.command_list);
        commandListTextView.setText("Available commands: " + commandList);
        Log.d(TAG, commandList);
//        try {
//            JSONArray data = new JSONArray(getArguments().getString("images", null));
//            Log.d(TAG, data.toString());
//        } catch (JSONException e){
//            e.printStackTrace();
//        }
    }
}



