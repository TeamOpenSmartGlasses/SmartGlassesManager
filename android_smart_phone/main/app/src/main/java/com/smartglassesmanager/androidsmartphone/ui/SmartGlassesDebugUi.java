package com.smartglassesmanager.androidsmartphone.ui;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.smartglassesmanager.androidsmartphone.MainActivity;
import com.smartglassesmanager.androidsmartphone.R;

public class SmartGlassesDebugUi extends Fragment {
    private  final String TAG = "WearableAi_SmartGlassesDebugUIFragment";

    private final String fragmentLabel = "Smart Glasses";

    private NavController navController;

    public SmartGlassesDebugUi() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.smart_glasses_debug_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //setup the title
        UiUtils.setupTitle(getActivity(), fragmentLabel);

        navController = Navigation.findNavController(getActivity(), R.id.nav_host_fragment);

        final Button sendTestCardButton = view.findViewById(R.id.send_test_card_old);
        sendTestCardButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Code here executes on main thread after user presses button
                sendTestCard();
            }
        });

    }

    public void sendTestCard(){
        Log.d(TAG, "SENDING TEST CARD FROM SGDUI");
        ((MainActivity)getActivity()).mService.sendTestCard(testCardTitle, testCardContent, testCardImg);
    }

    public String testCardImg = "https://ichef.bbci.co.uk/news/976/cpsprodpb/7727/production/_103330503_musk3.jpg";
    public String testCardTitle = "We Out Here Testing Stuff";
    public String testCardContent = "Local man runs in circles for hours after losing cognitive control of his left leg.";
}

