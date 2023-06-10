package com.smartglassesmanager.androidsmartphone.ui;

import android.text.Html;
import android.text.InputType;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.content.Intent;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import android.content.ComponentName;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.teamopensmartglasses.sgmlib.SmartGlassesAndroidService;
import com.smartglassesmanager.androidsmartphone.MainActivity;

//import res
import com.smartglassesmanager.androidsmartphone.R;

public class SettingsUi extends Fragment {
    private  final String TAG = "WearableAi_SettingsUIFragment";

    private final String fragmentLabel = "Smart Glasses Manager";

    private NavController navController;

    //test card raw data
    public String testCardImg = "https://ichef.bbci.co.uk/news/976/cpsprodpb/7727/production/_103330503_musk3.jpg";
    public String testCardTitle = "Reference Card";
    public String testCardContent = "This is an example of a reference card. This should appear on your glasses after pressing the 'Send Test Card' button.";

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

        //setup the title
        UiUtils.setupTitle(getActivity(), fragmentLabel);

        navController = Navigation.findNavController(getActivity(), R.id.nav_host_fragment);



        final Button killServiceButton = view.findViewById(R.id.kill_wearableai_service);
//        if (((MainActivity)getActivity()).areSmartGlassesConnected()){
//            killServiceButton.setEnabled(true);
//        } else {
//            killServiceButton.setEnabled(false);
//        }
        killServiceButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                // Code here executes on main thread after user presses button
                ((MainActivity)getActivity()).stopWearableAiService();
            }
        });

        final Button connectSmartGlassesButton = view.findViewById(R.id.connect_smart_glasses);
            connectSmartGlassesButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                // Code here executes on main thread after user presses button
                ((MainActivity)getActivity()).startWearableAiService();
                navController.navigate(R.id.nav_select_smart_glasses);
            }
        });

        final Button startHotspotButton = view.findViewById(R.id.start_hotspot);
            startHotspotButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                // Code here executes on main thread after user presses button
                launchHotspotSettings();
            }
        });

        // setup test card sender
        final Button sendTestCardButton = view.findViewById(R.id.send_test_card_old);
//        if (((MainActivity)getActivity()).areSmartGlassesConnected()){
//            sendTestCardButton.setEnabled(true);
//        } else {
//            sendTestCardButton.setEnabled(false);
//        }

        sendTestCardButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Code here executes on main thread after user presses button
                sendTestCard();
            }
        });

        final Button setGoogleApiKeyButton = view.findViewById(R.id.google_api_change);
        final Switch switchGoogleAsr = view.findViewById(R.id.google_asr_switch);
        setGoogleApiKeyButton.setEnabled(setGoogleApiKeyButton.isActivated());
        setGoogleApiKeyButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showAPIKeyDialog();
            }
        });

        switchGoogleAsr.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                setGoogleApiKeyButton.setEnabled(isChecked);
            }
        });

        //setup live captions launcher
//        final Button startLiveCaptionsButton = view.findViewById(R.id.start_live_captions);
//        startLiveCaptionsButton.setOnClickListener(new View.OnClickListener() {
//            public void onClick(View v) {
//                // Code here executes on main thread after user presses button
//                startLiveCaptions();
//            }
//        });
//
//        final Button triggerHelloWorldButton = view.findViewById(R.id.trigger_tpa_command);
//        triggerHelloWorldButton.setOnClickListener(new View.OnClickListener() {
//            public void onClick(View v) {
//                //((MainActivity)getActivity()).triggerHelloWorldTpa();
//
//                String tpaPackageName = "com.google.mlkit.samples.nl.translate";
//                String tpaServiceName = ".java.TranslationService";
//                Intent i = new Intent();
//                i.setAction(SmartGlassesAndroidService.ACTION_START_FOREGROUND_SERVICE);
//                i.setComponent(new ComponentName(tpaPackageName, tpaPackageName+tpaServiceName));
//                ComponentName c = ((MainActivity)getActivity()).startForegroundService(i);
//            }
//        });
    }

    public void sendTestCard(){
        Log.d(TAG, "SENDING TEST CARD");
        ((MainActivity)getActivity()).mService.sendTestCard(testCardTitle, testCardContent, testCardImg);
    }

    public void startLiveCaptions(){
        Log.d(TAG, "starting live cpations");
        ((MainActivity)getActivity()).mService.startLiveCaptions();
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

    /** The API won't work without a valid API key. This prompts the user to enter one. */
    private void showAPIKeyDialog() {
        LinearLayout contentLayout =
                (LinearLayout) getLayoutInflater().inflate(R.layout.api_key_message, null);
        TextView linkView = contentLayout.findViewById(R.id.api_key_link_view);
        linkView.setText(Html.fromHtml(getString(R.string.api_key_doc_link)));
        linkView.setMovementMethod(LinkMovementMethod.getInstance());
        EditText keyInput = contentLayout.findViewById(R.id.api_key_input);
        keyInput.setInputType(InputType.TYPE_CLASS_TEXT);
        keyInput.setText(((MainActivity)getActivity()).getApiKey(this.getContext()));

        AlertDialog.Builder builder = new AlertDialog.Builder(this.getContext());
        builder
                .setTitle(getString(R.string.api_key_message))
                .setView(contentLayout)
                .setPositiveButton(
                        getString(android.R.string.ok),
                        (dialog, which) -> {
                            ((MainActivity)getActivity()).saveApiKey(this.getContext(), keyInput.getText().toString().trim());
                        })
                .show();
    }
}
