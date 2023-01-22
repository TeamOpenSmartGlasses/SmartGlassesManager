package com.wearableintelligencesystem.androidsmartphone.ui;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.wearableintelligencesystem.androidsmartphone.R;
import com.wearableintelligencesystem.androidsmartphone.database.mediafile.MediaFileViewModel;
import com.wearableintelligencesystem.androidsmartphone.database.phrase.Phrase;
import com.wearableintelligencesystem.androidsmartphone.database.voicecommand.VoiceCommandViewModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MemoryTimelineUi extends Fragment implements ItemClickListenerReference {
    public String TAG = "WearableAi_MemoryTimelineUI";

    private final String fragmentLabel = "Memory Timeline";

    private LiveData<List<Reference>> tagBinReferences;
    private Observer<List<Reference>> tagBinReferencesObserver;
    private ReferenceListAdapter referenceListAdapter;
    private ArrayAdapter<String> tagMenuAdapter;
    private AutoCompleteTextView tagMenu;

    private int maxDistanceBackDays = 7; //how many days in time we go backwards
    private int maxDistanceBackMilliseconds = maxDistanceBackDays * 86400 * 1000; //how many days in time we go backwards

    private VoiceCommandViewModel mVoiceCommandViewModel;

    private final int personIntervalSeconds = 60 * 15; //number seconds before and after phrase to show people you saw

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

        //setup the title
        UiUtils.setupTitle(getActivity(), fragmentLabel);

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

        //here we need to get all of the voice notes, then pull in each phrase associated with each command
        List<Phrase> phrases = mVoiceCommandViewModel.getVoiceNotesSnapshot();
        List<Reference> references = new ArrayList<Reference>();
        for (Phrase p : phrases){
            Reference new_ref = new Reference();
            new_ref.setStartTimestamp(p.getTimestamp());
            new_ref.setSummary(p.getPhrase());
            new_ref.setTitle("Voice Note");
            references.add(new_ref);
        }

        //sort references by timestamp
        Collections.sort(references, new Comparator<Reference>(){
            public int compare(Reference obj1, Reference obj2) {
                // ## Ascending order
                return Long.compare(obj2.getStartTimestamp(), obj1.getStartTimestamp()); // To compare string values
                // return Integer.valueOf(obj1.empId).compareTo(Integer.valueOf(obj2.empId)); // To compare integer values

                // ## Descending order
                // return obj2.firstName.compareToIgnoreCase(obj1.firstName); // To compare string values
                // return Integer.valueOf(obj2.empId).compareTo(Integer.valueOf(obj1.empId)); // To compare integer values
            }
        });

        //send this data to be displayed
        adapter.setReferences(references);
    }

}
