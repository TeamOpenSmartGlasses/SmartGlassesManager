package com.google.mediapipe.apps.wearableai.ui;

import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.content.Context;
import android.content.Intent;
import android.app.ActivityManager;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import android.content.ComponentName;

import com.google.mediapipe.apps.wearableai.WearableAiAspService;
import com.google.mediapipe.apps.wearableai.MainActivity;

//import res
import com.google.mediapipe.apps.wearableai.R;

public class SettingsUi extends Fragment {
    private  final String TAG = "WearableAi_SettingsUIFragment";

    private NavController navController;

    public SettingsUi() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.settings_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        navController = Navigation.findNavController(getActivity(), R.id.nav_host_fragment);

        final Button killServiceButton = view.findViewById(R.id.kill_wearableai_service);
            killServiceButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                // Code here executes on main thread after user presses button
                ((MainActivity)getActivity()).stopWearableAiService();
            }
        });

        final Button startWearableAiServiceButton = view.findViewById(R.id.start_wearableai_service);
            startWearableAiServiceButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                // Code here executes on main thread after user presses button
                ((MainActivity)getActivity()).startWearableAiService();
            }
        });


        final Button startHotspotButton = view.findViewById(R.id.start_hotspot);
            startHotspotButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                // Code here executes on main thread after user presses button
                launchHotspotSettings();
            }
        });

    }


    //open hotspot settings
    private void launchHotspotSettings(){
        final Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        final ComponentName cn = new ComponentName("com.android.settings", "com.android.settings.TetherSettings");
        intent.setComponent(cn);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity( intent);
    }


}
