package com.wearableintelligencesystem.androidsmartphone.ui;

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
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

//import res
import com.wearableintelligencesystem.androidsmartphone.R;

public class SocialToolsUi extends Fragment {
    private  final String TAG = "WearableAi_SocialToolsUi";

    private final String fragmentLabel = "Social Tools";

    private NavController navController;

    public SocialToolsUi() {
        //required empty constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.social_tools_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //setup the title
        UiUtils.setupTitle(getActivity(), fragmentLabel);

        navController = Navigation.findNavController(getActivity(), R.id.nav_host_fragment);

        final Button autociterButton = view.findViewById(R.id.wearable_reference_autociter_button);
            autociterButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    Log.d(TAG, "clicked autociter button");
                    navController.navigate(R.id.action_nav_social_tools_to_nav_autociter);
            }
        });

        final Button faceRecButton = view.findViewById(R.id.face_rec_button);
            faceRecButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    Log.d(TAG, "clicked face rec button");
                    navController.navigate(R.id.action_nav_social_tools_to_nav_face_rec);
            }
        });
    }


}
