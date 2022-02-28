package com.wearableintelligencesystem.androidsmartglasses.ui;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.wearableintelligencesystemandroidsmartglasses.R;

import org.json.JSONArray;
import org.json.JSONException;

public class CommandResolveUi extends ASGFragment {
    private final String TAG = "WearableAi_CommandResolveUi";

    public CommandResolveUi() {
        fragmentLabel = "Command Resolved";
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.command_resolve_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.onViewCreated(view, savedInstanceState);

        boolean success = getArguments().getBoolean("success", false);
        String message = getArguments().getString("message", null);
        TextView successTextView = view.findViewById(R.id.success_message);
        ImageView successImageView = view.findViewById(R.id.command_success_icon);

        if(success){
            successImageView.setImageResource(R.drawable.ic_command_success);
            successTextView.setText("Success");
        }
        else{
            successImageView.setImageResource(R.drawable.ic_command_failed);
            successTextView.setText("Failed");
        }

//        try {
//            JSONArray data = new JSONArray(getArguments().getString("images", null));

//            String data = getArguments().getString("", null);
//            Log.d(TAG, "OPEN");
//        } catch (JSONException e){
//            e.printStackTrace();
//        }
    }
}




