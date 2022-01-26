package com.wearableintelligencesystem.androidsmartphone.ui;

// some code taken from https://github.com/stairs1/memory-expansion-tools/blob/master/AndroidMXT/app/src/main/java/com/memoryexpansiontools/mxt/StreamFragment.java

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.os.Handler;

import android.speech.tts.Voice;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.wearableintelligencesystem.androidsmartphone.database.phrase.Phrase;
import com.wearableintelligencesystem.androidsmartphone.database.voicecommand.VoiceCommandEntity;
import com.wearableintelligencesystem.androidsmartphone.database.voicecommand.VoiceCommandViewModel;
import com.wearableintelligencesystem.androidsmartphone.database.phrase.PhraseViewModel;

import androidx.lifecycle.LiveData;

import android.widget.AdapterView;
import android.widget.TextView;

import com.wearableintelligencesystem.androidsmartphone.R;

//menu imports:
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.AdapterView.OnItemClickListener;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link StreamFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MxtTagBinsUi extends Fragment implements ItemClickListenerVoiceCommandEntity {
    public String TAG = "WearableAi_MxtTagBinsUi";

    private final String fragmentLabel = "Tag Bins";

    private final String CURRENT_TAG_KEY = "CURRENT_TAG_KEY";

    private LiveData<List<VoiceCommandEntity>> tagBinPhrases;
    private Observer<List<VoiceCommandEntity>> tagBinPhrasesObserver;
    private VoiceCommandEntityListAdapter voiceEntityListAdapter;
    private ArrayAdapter<String> tagMenuAdapter;
    private AutoCompleteTextView tagMenu;

    private VoiceCommandViewModel mVoiceCommandViewModel;
    private PhraseViewModel mPhraseViewModel;

    private String currentTag;
    
    private NavController navController;

    @Override
    public void onClick(View view, VoiceCommandEntity vc){
        Log.d(TAG, "Click on transcript");

        Handler handler = new Handler();

        mPhraseViewModel.getPhrase(vc.getTranscriptId()).observe(this, new Observer<Phrase>() {
            @Override
            public void onChanged(@Nullable final Phrase phrase) {
                //open a fragment which shows in-depth, contextual view of that transcript
                Bundle bundle = new Bundle();
                bundle.putSerializable("phrase", phrase);
                navController.navigate(R.id.nav_phrase_context, bundle);
            }
        });
    }

    public MxtTagBinsUi() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Log.d(TAG, "MXT TAG BINS onCreate");

        if (savedInstanceState != null) {
            currentTag = savedInstanceState.getString(CURRENT_TAG_KEY);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(CURRENT_TAG_KEY, currentTag);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Log.d(TAG, "MXT TAG BINS onCreateView");
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.mxt_tag_bins_fragment, container, false);
    }

//    @Override
//    public void onSaveInstanceState(Bundle outState) {
//        Log.d(TAG, "called on save instance state");
////        outState.putParcelableArrayList(HEALTH_ITEMS, (ArrayList) mList);
////        outState.putString(TITLE, mTitle);
//    }
//
//    @Override
//    public void onCreate(@Nullable Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        if(savedInstanceState != null){
////            mList = savedInstanceState.getParcelableArrayList(HEALTH_ITEMS);
////            mTitle = savedInstanceState.getString(TITLE);
////            itemId = savedInstanceState.getInt(ID);
////            mChoiceMode = savedInstanceState.getInt(CHOICE_MODE);
//        }
////        getActivity().setTitle(mTitle);
////        adapter = (ListAdapter) new HealthAdapter(mList, getContext()).load(itemId);
////
//        //populate dropdown with tags
//        populateTopMenu(view);
//    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState){
        super.onViewCreated(view, savedInstanceState);

        //setup the title
        UiUtils.setupTitle(getActivity(), fragmentLabel);

        Log.d(TAG, "MXT TAG BINS onViewCreated");

        //create NavController
//        navController = Navigation.findNavController(getActivity(), R.id.nav_host_fragment);
        navController = Navigation.findNavController(getView());

        // Get a new or existing ViewModel from the ViewModelProvider.
        mVoiceCommandViewModel = new ViewModelProvider(this).get(VoiceCommandViewModel.class);
        mPhraseViewModel = new ViewModelProvider(this).get(PhraseViewModel.class);

        //populate dropdown with tags
        populateTopMenu(view);
        
        //setup list of phrases
        RecyclerView recyclerView = view.findViewById(R.id.phrase_wall);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        linearLayoutManager.setReverseLayout(true);
        voiceEntityListAdapter = new VoiceCommandEntityListAdapter(getContext());
        voiceEntityListAdapter.setClickListener(this);
        recyclerView.setAdapter(voiceEntityListAdapter);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.scrollToPosition(recyclerView.getAdapter().getItemCount() -1);

    }

    //WARNING - this is a BROKEN system - this bug : https://github.com/material-components/material-components-android/issues/2012
    //is huge and fundamental - it's not just this, but even after clearing the filter, other things don't work well. below is a hack and we should absolutly NOT use this menu in the future - someone PLEASE change to to a different type of menu
    //FURTHER - we are basically using this bug to save the last value of the menu - probably when you update it, you will have to use instance state to save the state of the menu so we can reinstantiate upon the back button returning us to this fragment
    public void populateTopMenu(View v){
        
        tagMenuAdapter = new ArrayAdapter<String>(getActivity(), R.layout.menu_item_exposed_dropdown, getResources().getStringArray(R.array.exposed_dropdown_content));

        tagMenu = v.findViewById(R.id.tag_menu_tv);
        tagMenu.setAdapter(tagMenuAdapter);

        //setup listener
        tagMenu.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapter, View v, int pos, long id) {
                String newTag = ((TextView) v).getText().toString();
                //populate view with phrases matching tag
                updateTag(newTag);
            }
        });


        //we have to do this because of this bug: https://github.com/material-components/material-components-android/issues/2012
        //test to clear filter after 1 second
        Handler mHandler = new Handler();
        mHandler.postDelayed(new Runnable(){
            public void run() {
                //clear the filter so all can be seen
                tagMenuAdapter.getFilter().filter(null);

                //set menu to first option
                String startTag;
                if (currentTag == null){
                    startTag = tagMenu.getAdapter().getItem(0).toString();
                } else {
                    startTag = currentTag;
                }

                tagMenu.setText(startTag, false);

                //update tag to the last set option
                updateTag(startTag);
            }
        }, 1);
    }

    public void updateTag(String newTag){
        //set the fragment's memory of the currentTag so we can load it when we return
        currentTag = newTag;
        newTag = newTag.toLowerCase();

        if (tagBinPhrases != null){
            tagBinPhrases.removeObserver(tagBinPhrasesObserver);
        }

        //here we need to get all of the MXT commands, and then pull in each phrase associated with each command
        tagBinPhrases = mVoiceCommandViewModel.getTagBin(newTag);
        tagBinPhrasesObserver = new Observer<List<VoiceCommandEntity>>() {
            @Override
            public void onChanged(@Nullable final List<VoiceCommandEntity> commands) {
                voiceEntityListAdapter.setVoiceCommandEntitys(commands);
            }
        };
        tagBinPhrases.observe(getActivity(), tagBinPhrasesObserver);
    }
}

