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

//import res
import com.google.mediapipe.apps.wearableai.R;

public class MainUi extends Fragment {
    private  final String TAG = "WearableAi_MainUIFragment";

    private NavController navController;

    public MainUi() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.main_ui_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        navController = Navigation.findNavController(getActivity(), R.id.nav_host_fragment);

        final Button memoryToolsButton = view.findViewById(R.id.memory_tools);
            memoryToolsButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    Log.d(TAG, "clicked memory tools button");
                    navController.navigate(R.id.nav_memory_tools);
            }
        });

        final Button killServiceButton = view.findViewById(R.id.kill_wearableai_service);
            killServiceButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                // Code here executes on main thread after user presses button
                stopWearableAiService();
            }
        });

        final Button startWearableAiServiceButton = view.findViewById(R.id.start_wearableai_service);
            startWearableAiServiceButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                // Code here executes on main thread after user presses button
                startWearableAiService();
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

   public void stopWearableAiService() {
        if (!isMyServiceRunning(WearableAiAspService.class)) return;
        Intent stopIntent = new Intent(getActivity(), WearableAiAspService.class);
        stopIntent.setAction(WearableAiAspService.ACTION_STOP_FOREGROUND_SERVICE);
        Log.d(TAG, "MainActivity stopping WearableAI service");
        getActivity().startService(stopIntent);
   }

//   public void sendWearableAiServiceMessage(String message) {
//        if (!isMyServiceRunning(WearableAiAspService.class)) return;
//        Intent messageIntent = new Intent(getActivity(), WearableAiAspService.class);
//        messageIntent.setAction(message);
//        Log.d(TAG, "Sending WearableAi Service this message: " + message);
//        getActivity().startService(messageIntent);
//   }

   public void startWearableAiService() {
        if (isMyServiceRunning(WearableAiAspService.class)) return;
        Intent startIntent = new Intent(getActivity(), WearableAiAspService.class);
        startIntent.setAction(WearableAiAspService.ACTION_START_FOREGROUND_SERVICE);
        Log.d(TAG, "MainActivity starting WearableAI service");
        getActivity().startService(startIntent);
    }

    //check if service is running
    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getActivity().getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
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
