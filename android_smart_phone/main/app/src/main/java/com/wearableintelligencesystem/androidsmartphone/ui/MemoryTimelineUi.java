package com.wearableintelligencesystem.androidsmartphone.ui;

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

import java.sql.Ref;
import java.util.ArrayList;
import java.util.List;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.wearableintelligencesystem.androidsmartphone.database.mediafile.MediaFileViewModel;
import com.wearableintelligencesystem.androidsmartphone.database.phrase.Phrase;
import com.wearableintelligencesystem.androidsmartphone.database.voicecommand.VoiceCommandViewModel;

import androidx.lifecycle.LiveData;

import com.wearableintelligencesystem.androidsmartphone.R;

//menu imports:
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

public class MemoryTimelineUi extends Fragment implements ItemClickListenerReference {
    public String TAG = "WearableAi_MemoryTimelineUI";

    private LiveData<List<Reference>> tagBinReferences;
    private Observer<List<Reference>> tagBinReferencesObserver;
    private ReferenceListAdapter referenceListAdapter;
    private ArrayAdapter<String> tagMenuAdapter;
    private AutoCompleteTextView tagMenu;

    private VoiceCommandViewModel mVoiceCommandViewModel;

    private String currentTag;

    private NavController navController;

    private MediaFileViewModel mMediaFileViewModel;

    @Override
    public void onClick(View view, Reference reference){
        Log.d(TAG, "Click on memory");

//        //open a fragment which shows in-depth, contextual view of that transcript
//        Bundle bundle = new Bundle();
//        bundle.putSerializable("reference", reference);
//        navController.navigate(R.id.action_nav_mxt_tag_bins_to_nav_reference_context, bundle);
    }


    public MemoryTimelineUi() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Log.d(TAG, "MemoryTimelineUI onCreateView");
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.memory_timeline_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState){
        super.onViewCreated(view, savedInstanceState);

        Log.d(TAG, "MXT TAG BINS onViewCreated");

        //create NavController
        navController = Navigation.findNavController(getView());

        //get view models
        mMediaFileViewModel = new ViewModelProvider(this).get(MediaFileViewModel.class);

        //setup list of phrases
        RecyclerView recyclerView = view.findViewById(R.id.reference_wall);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        linearLayoutManager.setReverseLayout(true);
        final ReferenceListAdapter adapter = new ReferenceListAdapter(getContext(), mMediaFileViewModel);
        adapter.setClickListener(this);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.scrollToPosition(recyclerView.getAdapter().getItemCount() -1);

        // Get a new or existing ViewModel from the ViewModelProvider.
        mVoiceCommandViewModel = new ViewModelProvider(this).get(VoiceCommandViewModel.class);

        //here we need to get all of the MXT commands, and then pull in each phrase associated with each command
        mVoiceCommandViewModel.getMxtCache().observe(getActivity(), new Observer<List<Phrase>>() {
            @Override
            public void onChanged(@Nullable final List<Phrase> phrases) {
                //get all phrases associated with latest mxt commands, then
                // Update the cached copy of the words in the adapter.
//                Log.d(TAG, "mxt cache");
//                Log.d(TAG, Long.toString(voiceCommands.get(0).getTranscriptId()));
//                List<Long> phraseIds = new ArrayList<Long>();
//                voiceCommands.forEach( (voiceCommand) -> phraseIds.add(voiceCommand.getTranscriptId()) );
//                Log.d(TAG, Arrays.toString(phraseIds.toArray()));
//                List<Reference> phrases = mReferenceViewModel.getReferences(phraseIds);
                List<Reference> references = new ArrayList<Reference>();
                for (Phrase p : phrases){
                    Reference new_ref = new Reference();
                    new_ref.setTimestamp(p.getTimestamp());
                    new_ref.setSummary(p.getPhrase());
                    new_ref.setTitle("Voice Note");
                    references.add(new_ref);
                }
                adapter.setReferences(references);
            }
        });

    }
}
