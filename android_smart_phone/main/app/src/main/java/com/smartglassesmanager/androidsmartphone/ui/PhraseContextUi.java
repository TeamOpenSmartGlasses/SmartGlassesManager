package com.smartglassesmanager.androidsmartphone.ui;

// some code taken from https://github.com/stairs1/memory-expansion-tools/blob/master/AndroidMXT/app/src/main/java/com/memoryexpansiontools/mxt/StreamFragment.java

import android.location.Location;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.os.Handler;

import android.widget.LinearLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.TextView;
import android.graphics.Bitmap;

//bitmap utils
import com.smartglassesmanager.androidsmartphone.utils.BitmapJavaUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.Instant;
import java.time.ZoneId;

import com.smartglassesmanager.androidsmartphone.database.phrase.Phrase;
import com.smartglassesmanager.androidsmartphone.database.phrase.PhraseViewModel;

import com.smartglassesmanager.androidsmartphone.database.voicecommand.VoiceCommandViewModel;

import com.smartglassesmanager.androidsmartphone.database.mediafile.MediaFileEntity;
import com.smartglassesmanager.androidsmartphone.database.mediafile.MediaFileViewModel;

import com.smartglassesmanager.androidsmartphone.R;

//menu imports:


//shows the context that a phrase took place, including images, maps, time, surrounding transcripts, etc.
//also shows points in time before the given phrase and after the given phrase
//see MxtTagBinsUi for example on how to launch this with Phrase in Bundle for navController
public class PhraseContextUi extends Fragment {
    public String TAG = "WearableAi_PhraseContextUi";

    private final String fragmentLabel = "Egocentric Context";

    private VoiceCommandViewModel mVoiceCommandViewModel;
    private MediaFileViewModel mMediaFileViewModel;
    private PhraseViewModel mPhraseViewModel;

    private Phrase mainPhrase;
    private MediaFileEntity mainImage;

    private final int memInterval = 3000; //the amount of time between each memory we show, in milliseconds
    private final int numMemories = 10; //the number of memories we show before and after the main memory
    private final int personIntervalSeconds = 60 * 15; //number seconds before and after phrase to show people you saw

    private ImageView mapImageView;
    private TextView personListTextView;

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
        //setup titlebar
        UiUtils.setupTitle(getActivity(), fragmentLabel);

        //get the phrase we passed in the bundle to create this fragment
        mainPhrase = (Phrase) getArguments().getSerializable("phrase");

        // Get a new or existing ViewModel from the ViewModelProvider.
        mVoiceCommandViewModel = new ViewModelProvider(this).get(VoiceCommandViewModel.class);
        mMediaFileViewModel = new ViewModelProvider(this).get(MediaFileViewModel.class);
        mPhraseViewModel = new ViewModelProvider(this).get(PhraseViewModel.class);

        //populate the image gallery with images
        LinearLayout imageGallery = view.findViewById(R.id.image_gallery);
        LayoutInflater localInflater = LayoutInflater.from(getActivity());

        //get the map image view
        mapImageView = (ImageView) view.findViewById(R.id.map_image_view);

        //set image view to location
        Location currentLocation = mainPhrase.getLocation();
        if (currentLocation != null) {
            Log.d(TAG, "GOT CURRET LOCATION");
            double longitude = currentLocation.getLongitude();
            double latitude = currentLocation.getLatitude();
//            updateMapImage(latitude, longitude);
        } else {
            Log.d(TAG, "CURRENT LOCATION FOR PHRASE IS NULL");
        }

        //get the person list text view
        personListTextView = (TextView) view.findViewById(R.id.people_list);
        //get all people that were seen at that time
        long phraseTime = mainPhrase.getTimestamp();
        long startTime = phraseTime - (personIntervalSeconds * 1000);
        long endTime = phraseTime + (personIntervalSeconds * 1000);

        for (int i = (-1 * numMemories); i < numMemories; i++){ //number should be odd so there is a center image, even number on both sides
            //get image
            MediaFileEntity currentImage = mMediaFileViewModel.getClosestMediaFileSnapshot("image", mainPhrase.getTimestamp() + (i * memInterval));

            if (currentImage != null) {
                //put new image into horizontally scrolling linear layout
                View imageView = localInflater.inflate(R.layout.image_item, imageGallery, false);

                //get the image view
                ImageView imageViewImage = imageView.findViewById(R.id.imageView);
                //imageViewImage.setImageResource(R.drawable.elon);
                //set the image of the image view
                String imagePath = currentImage.getLocalPath();
                Bitmap imageBitmap = BitmapJavaUtils.loadImageFromStorage(imagePath);
                if (imageBitmap != null) {
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

