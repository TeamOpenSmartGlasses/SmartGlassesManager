package com.google.mediapipe.apps.wearableai.ui;

// some code taken from https://github.com/stairs1/memory-expansion-tools/blob/master/AndroidMXT/app/src/main/java/com/memoryexpansiontools/mxt/StreamFragment.java

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import java.util.ArrayList;
import android.widget.EditText;
import android.os.Handler;

import android.widget.LinearLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.TextView;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.io.File;

//bitmap utils
import com.google.mediapipe.apps.wearableai.utils.BitmapJavaUtils;

//date/time
//import org.threeten.bp.LocalDateTime;
//import org.threeten.bp.format.DateTimeFormatter;
//import org.threeten.bp.Instant;
//import org.threeten.bp.ZoneId;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.Instant;
import java.time.ZoneId;

import java.util.Arrays;
import java.util.List;
import java.lang.Long;

import com.google.mediapipe.apps.wearableai.database.phrase.Phrase;
import com.google.mediapipe.apps.wearableai.database.phrase.PhraseViewModel;

import com.google.mediapipe.apps.wearableai.database.voicecommand.VoiceCommandEntity;
import com.google.mediapipe.apps.wearableai.database.voicecommand.VoiceCommandViewModel;

import com.google.mediapipe.apps.wearableai.database.mediafile.MediaFileEntity;
import com.google.mediapipe.apps.wearableai.database.mediafile.MediaFileViewModel;

import androidx.lifecycle.LiveData;

import android.widget.AdapterView;

import com.google.mediapipe.apps.wearableai.ui.ItemClickListener;

import com.google.mediapipe.apps.wearableai.R;

//menu imports:
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TextView;

//shows the context that a phrase took place, including images, maps, time, surrounding transcripts, etc.
//also shows points in time before the given phrase and after the given phrase
//see MxtTagBinsUi for example on how to launch this with Phrase in Bundle for navController
public class PhraseContextUi extends Fragment {
    public String TAG = "WearableAi_PhraseContextUi";

    private VoiceCommandViewModel mVoiceCommandViewModel;
    private MediaFileViewModel mMediaFileViewModel;
    private PhraseViewModel mPhraseViewModel;

    private Phrase mainPhrase;
    private MediaFileEntity mainImage;

    private final int memInterval = 3000; //the amount of time between each memory we show, in milliseconds
    private final int numMemories = 10; //the number of memories we show before and after the main memory

    public PhraseContextUi() {
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
        return inflater.inflate(R.layout.phrase_context_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState){
        //get the phrase we passed in the bundle to create this fragment
        mainPhrase = (Phrase) getArguments().getSerializable("phrase");

        // Get a new or existing ViewModel from the ViewModelProvider.
        mVoiceCommandViewModel = new ViewModelProvider(this).get(VoiceCommandViewModel.class);
        mMediaFileViewModel = new ViewModelProvider(this).get(MediaFileViewModel.class);
        mPhraseViewModel = new ViewModelProvider(this).get(PhraseViewModel.class);

        //populate the image gallery with images
        LinearLayout imageGallery = view.findViewById(R.id.image_gallery);
        LayoutInflater localInflater = LayoutInflater.from(getActivity());

        for (int i = (-1 * numMemories); i < numMemories; i++){ //number should be odd so there is a center image, even number on both sides
            //get image
            MediaFileEntity currentImage = mMediaFileViewModel.getClosestMediaFileSnapshot("image", mainPhrase.getTimestamp() + (i * memInterval));

            //put new image into horizontally scrolling linear layout
            View imageView = localInflater.inflate(R.layout.image_item, imageGallery, false);

            //get the image view
            ImageView imageViewImage = imageView.findViewById(R.id.imageView);
            //imageViewImage.setImageResource(R.drawable.elon);
            //set the image of the image view
            String imagePath = currentImage.getLocalPath();
            Bitmap imageBitmap = BitmapJavaUtils.loadImageFromStorage(imagePath);
            if (imageBitmap != null){
                imageViewImage.setImageBitmap(imageBitmap);
            } else {
                continue;
            }

            //set text of the image item
            TextView imageViewTextView = imageView.findViewById(R.id.textView);
            String prettyTime = getPrettyDate(currentImage.getStartTimestamp());
            imageViewTextView.setText(prettyTime);

            //add the new image item to the gallery
            imageGallery.addView(imageView);
        }

        //after it's created, scroll to the center
        //must do in handler to ensure that the horizontal scroll is already created
        Handler mHandler = new Handler();
        mHandler.postDelayed(new Runnable(){
            public void run() {
                HorizontalScrollView galleryHolder = view.findViewById(R.id.gallery_holder);
                int num = imageGallery.getChildCount() / 2;
                View imageView = imageGallery.getChildAt(num);// selected child view
                final int scrollPos = imageView.getLeft() - (galleryHolder.getWidth() - imageView.getWidth()) / 2;
                galleryHolder.scrollTo(scrollPos, 0);
            }
        }, 10);
    }


    public String getPrettyDate(long timestamp){
        LocalDateTime date = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d MMM yyyy, h:mm:ssa");
        String prettyTime = date.format(formatter);
        return prettyTime;
    }

}

