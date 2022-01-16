package com.wearableintelligencesystem.androidsmartphone.ui;

import android.app.Activity;
import android.content.Context;

import androidx.appcompat.widget.Toolbar;

import com.wearableintelligencesystem.androidsmartphone.MainActivity;
import com.wearableintelligencesystem.androidsmartphone.R;

public class UiUtils {

    //set app bar title
    public static void setupTitle(Activity activity, String title) {
        Toolbar toolbar = (Toolbar) activity.findViewById(R.id.main_toolbar);
        ((MainActivity) activity).setSupportActionBar(toolbar);
        ((MainActivity) activity).getSupportActionBar().setTitle(title);
    }
}
