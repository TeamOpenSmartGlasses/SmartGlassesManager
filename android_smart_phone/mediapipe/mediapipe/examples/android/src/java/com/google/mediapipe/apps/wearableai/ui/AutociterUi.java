package com.google.mediapipe.apps.wearableai.ui;

import android.widget.Button;
import android.widget.EditText;

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
import android.content.DialogInterface;
import android.app.AlertDialog;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.app.Activity;

import com.google.mediapipe.apps.wearableai.MainActivity;

//import res
import com.google.mediapipe.apps.wearableai.R;

public class AutociterUi extends Fragment {
    private  final String TAG = "WearableAi_AutociterUi";

    private boolean isActive = false;

    private NavController navController;
    
    private String phoneNumber;

    //ui
    private Button stopConvoButton;
    private Button startConvoButton;
    private Button submitNumberButton;
    private EditText phoneNumberEditText;

    public AutociterUi() {
        //required empty constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.autociter_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        navController = Navigation.findNavController(getActivity(), R.id.nav_host_fragment);

        //setup ui
        phoneNumberEditText = view.findViewById(R.id.phone_number_edit_text);

        submitNumberButton = view.findViewById(R.id.submit_number_button);
        submitNumberButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                submitNumber(view);
            }
        });

        stopConvoButton = view.findViewById(R.id.stop_convo_button);
        stopConvoButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                stopConvo();
            }
        });

        startConvoButton = view.findViewById(R.id.start_convo_button);
        startConvoButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startConvo();
            }
        });

        setupUi();
    }

    private void setupUi(){
        //disable start convo
        startConvoButton.setAlpha(.5f);
        startConvoButton.setClickable(false);
  
        //disable stop convo
        stopConvoButton.setAlpha(.5f);
        stopConvoButton.setClickable(false);

        //check if a convo is currently running
        isActive = ((MainActivity)getActivity()).mService.getAutociterStatus();
        if (isActive){
            phoneNumber = ((MainActivity)getActivity()).mService.getPhoneNumber();
            phoneNumberEditText.setText(phoneNumber); 
            showConvoStarted();
        }
    }

    private void submitNumber(View view){
        phoneNumber = phoneNumberEditText.getText().toString();
        showPhoneNumberAlert();

        //enable start convo
        startConvoButton.setAlpha(1f);
        startConvoButton.setClickable(true);

        //hide keyboard
        hideKeyboardFrom(getActivity(), view);
    }

    public static void hideKeyboardFrom(Context context, View view) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    private void showPhoneNumberAlert(){
        new AlertDialog.Builder(getActivity())
            .setTitle("Phone Number Updated")
            .setMessage("Phone number was updated to: " + phoneNumber)

            // Specifying a listener allows you to take an action before dismissing the dialog.
            // The dialog is automatically dismissed when a dialog button is clicked.
            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    // Continue with delete operation
                }
             })
            .show();
    }

    private void startConvo(){
        showConvoStarted();

        //tell autociter to start with this phone number
        ((MainActivity)getActivity()).mService.startAutociter(phoneNumber);
    }

    private void showConvoStarted(){
        //disable start convo
        startConvoButton.setAlpha(.5f);
        startConvoButton.setClickable(false);

        //enable stop convo
        stopConvoButton.setAlpha(1f);
        stopConvoButton.setClickable(true);
    }

    private void stopConvo(){
        //disable stop convo
        stopConvoButton.setAlpha(.5f);
        stopConvoButton.setClickable(false);

        //enable start convo
        startConvoButton.setAlpha(1f);
        startConvoButton.setClickable(true);

        //tell autociter to start with this phone number
        ((MainActivity)getActivity()).mService.stopAutociter();
    }

}
