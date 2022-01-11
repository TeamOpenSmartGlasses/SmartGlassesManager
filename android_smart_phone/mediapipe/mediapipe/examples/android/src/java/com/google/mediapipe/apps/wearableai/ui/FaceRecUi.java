package com.google.mediapipe.apps.wearableai.ui;

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
import android.content.Context;
import android.content.Intent;
import android.app.ActivityManager;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import android.content.ComponentName;
import android.widget.ImageView;
import java.util.List;
import android.graphics.drawable.Drawable;
import java.util.concurrent.ExecutionException;
import java.lang.InterruptedException;

import com.google.mediapipe.apps.wearableai.database.person.PersonRepository;
import com.google.mediapipe.apps.wearableai.database.person.PersonEntity;

import com.google.mediapipe.apps.wearableai.database.mediafile.MediaFileRepository;
import com.google.mediapipe.apps.wearableai.database.mediafile.MediaFileEntity;

import com.google.mediapipe.apps.wearableai.facialrecognition.FaceRecDbUtils;

import com.google.mediapipe.apps.wearableai.WearableAiAspService;

import android.app.AlertDialog;

import com.google.android.material.textfield.TextInputEditText;

//bitmap utils
import com.google.mediapipe.apps.wearableai.utils.BitmapJavaUtils;
import android.graphics.Bitmap;

//import res
import com.google.mediapipe.apps.wearableai.R;

public class FaceRecUi extends Fragment {
    private  final String TAG = "WearableAi_FaceRecUiFragment";

    private NavController navController;
    private PersonRepository mPersonRepository;
    private MediaFileRepository mMediaFileRepository;
    private List<PersonEntity> unknownPeople;

    private ImageView faceImageView;

    private FaceRecDbUtils faceRecDbUtils;
    
    private Button doKnowFace;
    private Button dontKnowFace;
    private Button badImageButton;

    public FaceRecUi() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.face_rec_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //get the image view
        faceImageView = view.findViewById(R.id.face_image_view);

        mPersonRepository = new PersonRepository(getActivity().getApplication());
        mMediaFileRepository = new MediaFileRepository(getActivity().getApplication());
        try{
            unknownPeople = mPersonRepository.getUnknownPersonsSnapshot();
        } catch (ExecutionException | InterruptedException e){
            e.printStackTrace();
        }

        //set up face rec saving utils
        faceRecDbUtils = new FaceRecDbUtils(getActivity(), mPersonRepository);

        navController = Navigation.findNavController(getActivity(), R.id.nav_host_fragment);
        
        badImageButton = view.findViewById(R.id.bad_image_face);
            badImageButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                // Code here executes on main thread after user presses button
                badImageFunc();
            }
        });

        dontKnowFace = view.findViewById(R.id.dont_know_face);
            dontKnowFace.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                // Code here executes on main thread after user presses button
                dontKnowFaceFunc();
            }
        });

        doKnowFace = view.findViewById(R.id.do_know_face);
            doKnowFace.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                // Code here executes on main thread after user presses button
                doKnowFaceFunc();
            }
        });

        //start showing unknown faces
        showFace();
    }

    public void clearFaceImage(){
        Drawable blankFacesDrawable = getActivity().getDrawable(R.drawable.outline_groups_24);
        faceImageView.setImageDrawable(blankFacesDrawable);

        //disable buttons
        badImageButton.setEnabled(false);
        doKnowFace.setEnabled(false);
        dontKnowFace.setEnabled(false);
    }

    public void showNextFace(){
        if (unknownPeople.size() <= 1){ //when no more faces to tag
            clearFaceImage();
            return;
        }

        unknownPeople.remove(0);
        showFace();
    }

    public void showFace(){
        if (unknownPeople.size() == 0){ //when no more faces to tag
            clearFaceImage();
            return;
        }

        long imageId = unknownPeople.get(0).getMediaId();
        MediaFileEntity imageRef;
        try{
             imageRef = mMediaFileRepository.getMediaFilebyId(imageId);
        } catch (ExecutionException | InterruptedException e){
            e.printStackTrace();
            return;
        }
        String imagePath = imageRef.getLocalPath();

        Log.d(TAG, "Getting image from path: "+imagePath);

        //set the image of the image view
        Bitmap imageBitmap = BitmapJavaUtils.loadImageFromStorage(imagePath);
        if (imageBitmap != null){
            faceImageView.setImageBitmap(imageBitmap);
        } else {
            Log.d(TAG, "Failed to set image, image doesn't exist.");
        }
    }

    public void badImageFunc(){
        //set in our database that this was a bad image, so we won't open it again and we will clear it from the face rec encodings object
        long id = unknownPeople.get(0).getPersonId();
        long timestamp = unknownPeople.get(0).getTimestamp();
        long mediaId = unknownPeople.get(0).getMediaId();
        faceRecDbUtils.setBadImage(id, timestamp, mediaId);
        showNextFace();
    }

    public void dontKnowFaceFunc(){
        //set in our database that we don't know this person, so we will ignore them next time we see them in our face rec api, but we still save them so if we meet them later, we have a record of them
        long id = unknownPeople.get(0).getPersonId();
        faceRecDbUtils.setConfirmedUnknown(id);
        showNextFace();
    }

    //public void doKnowFaceFunc(long knownFaceId){
    public void doKnowFaceFunc(){
        //either add a new name and update id, or set this person's id to an existing person's id
        long id = unknownPeople.get(0).getPersonId();
        createNamingDialog();
    }

    private void createNamingDialog(){
        LayoutInflater inflater = getLayoutInflater();
        View alertLayout = inflater.inflate(R.layout.face_rec_naming_modal, null);
        final TextInputEditText etPersonName = alertLayout.findViewById(R.id.tiet_personName);

        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
        alert.setTitle("Who is this?");
        // this is set the view from XML inside AlertDialog
        alert.setView(alertLayout);
        // disallow cancel of AlertDialog on click of back button and outside touch
        alert.setCancelable(false);

        alert.setNegativeButton("Cancel", (dialog, which) -> {
        });

        alert.setPositiveButton("Done", (dialog, which) -> {
            Log.d(TAG, "HIT DONE, SAVING FACE");
            String personName = etPersonName.getText().toString();
            long id = unknownPeople.get(0).getPersonId();
            Log.d(TAG, "personId in face rec ui: " + id);
            faceRecDbUtils.faceAdd(id, personName);
            showNextFace();
        });

        AlertDialog dialog = alert.create();
        dialog.show();
    }


}
