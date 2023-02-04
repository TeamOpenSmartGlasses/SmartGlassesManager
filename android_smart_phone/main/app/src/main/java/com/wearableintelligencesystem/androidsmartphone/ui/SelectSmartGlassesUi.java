package com.wearableintelligencesystem.androidsmartphone.ui;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.wearableintelligencesystem.androidsmartphone.MainActivity;
import com.wearableintelligencesystem.androidsmartphone.R;
import com.wearableintelligencesystem.androidsmartphone.supportedglasses.SmartGlassesDevice;

import java.util.ArrayList;

public class SelectSmartGlassesUi extends Fragment {
    public String TAG = "WearableAi_SelectSmartGlassesUi";

    private final String fragmentLabel = "Select Glasses to Connect...";

    private NavController navController;

    //UI elements
    private ListView glassesList;

    public SelectSmartGlassesUi() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.select_glasses_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState){
        //setup titlebar
        UiUtils.setupTitle(getActivity(), fragmentLabel);

        //get the nav controller
        navController = Navigation.findNavController(getActivity(), R.id.nav_host_fragment);

        //setup list of smart glasses
        glassesList = view.findViewById(R.id.smart_glasses_list);

        // on the below line we are initializing the adapter for our list view.
        ArrayList<SmartGlassesDevice> glassesArrayList = new ArrayList<>();

        //ArrayAdapter<String> glassesListAdapter = new ArrayAdapter<>(view.getContext(), android.R.layout.simple_list_item_activated_1, glassesArrayList);
        SmartGlassesListAdapter glassesListAdapter = new SmartGlassesListAdapter(glassesArrayList, getContext());

        //listen for list presses
        glassesList.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> adapter, View v, int position,
                                    long arg3)
            {
                glassesListAdapter.setSelectedPosition(position);
            }
        });

        // on below line we are setting adapter for our list view.
        glassesList.setAdapter(glassesListAdapter);
        for (SmartGlassesDevice device : ((MainActivity)getActivity()).smartGlassesDevices){
            Log.d(TAG, device.getDeviceModelName());
            glassesListAdapter.add(device);
        }

        //setup buttons
        final Button cancelButton = view.findViewById(R.id.cancel_select_smart_glasses_button);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Code here executes on main thread after user presses button
                navController.navigate(R.id.nav_settings);
            }
        });

        final Button selectButton = view.findViewById(R.id.select_smart_glasses_button);
        selectButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Code here executes on main thread after user presses button
                SmartGlassesDevice selectedDevice = glassesListAdapter.getSelectedDevice();
                if (selectedDevice == null) {
                    Log.d(TAG, "Please choose a smart glasses device to continue.");
                } else if (!selectedDevice.getAnySupport()){
                    Log.d(TAG, "Glasses not yet supported, we're working on it.");
                } else {
                    Log.d(TAG, "Connecting to " + selectedDevice.getDeviceModelName() + "...");
                    navController.navigate(R.id.nav_settings);
                }
            }
        });

    }
}

