package com.google.mediapipe.apps.wearableai.ui;

import android.view.View;
import com.google.mediapipe.apps.wearableai.database.phrase.Phrase;

public interface ItemClickListener {
    void onClick(View view, Phrase phrase);
}
