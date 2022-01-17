package com.wearableintelligencesystem.androidsmartphone.ui;

import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
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
import com.wearableintelligencesystem.androidsmartphone.MainActivity;
import com.wearableintelligencesystem.androidsmartphone.R;

public class MemoryToolsUi extends Fragment {
    private  final String TAG = "WearableAi_MemoryToolsUi";

    private final String fragmentLabel = "Memory Tools";

    private NavController navController;

    public MemoryToolsUi() {
        //required empty constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.memory_tools_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //setup the title
        UiUtils.setupTitle(getActivity(), fragmentLabel);

        //get the nav controller
        navController = Navigation.findNavController(getActivity(), R.id.nav_host_fragment);

        final Button allTranscriptButton = view.findViewById(R.id.all_transcripts);
            allTranscriptButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    Log.d(TAG, "clicked all transcripts button");
                    navController.navigate(R.id.nav_all_transcripts);
            }
        });

        final Button mxtCacheButton = view.findViewById(R.id.mxt_cache);
            mxtCacheButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    Log.d(TAG, "clicked mxt_cache button");
                    navController.navigate(R.id.nav_mxt_cache);
            }
        });

        final Button mxtTagBinsButton = view.findViewById(R.id.mxt_tag_bins);
            mxtTagBinsButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    Log.d(TAG, "clicked mxt_tag_bins button");
                    navController.navigate(R.id.nav_mxt_tag_bins);
            }
        });

        final Button memoryTimelineButton = view.findViewById(R.id.memory_timeline_button);
        memoryTimelineButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.d(TAG, "clicked mxt_tag_bins button");
                navController.navigate(R.id.nav_memory_timeline);
            }
        });

        final Button memoryCachesButton = view.findViewById(R.id.memory_cache_button);
        memoryCachesButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.d(TAG, "clicked memory_caches button");
                navController.navigate(R.id.nav_memory_caches);
            }
        });

        final Button exportDataButton = view.findViewById(R.id.export_data_button);
        exportDataButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.d(TAG, "clicked export_data_button");
                navController.navigate(R.id.nav_export_data);
            }
        });


    }



}
