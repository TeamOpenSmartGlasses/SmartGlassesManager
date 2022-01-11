package com.wearableintelligencesystem.androidsmartphone.ui;

import android.view.View;
import com.wearableintelligencesystem.androidsmartphone.database.phrase.Phrase;

public interface ItemClickListener {
    void onClick(View view, Phrase phrase);
}
