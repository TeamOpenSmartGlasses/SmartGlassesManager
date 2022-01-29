package com.wearableintelligencesystem.androidsmartglasses.ui;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.wearableintelligencesystemandroidsmartglasses.R;
import com.wearableintelligencesystem.androidsmartglasses.MainActivity;

public class ConvoModeUi extends Fragment {
    private  final String TAG = "WearableAi_ConvoModeUIFragment";

    private final String fragmentLabel = "ConvoMode";

    private NavController navController;

    public ConvoModeUi() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.convo_mode_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //navController = Navigation.findNavController(getActivity(), R.id.nav_host_fragment);
    }

}

