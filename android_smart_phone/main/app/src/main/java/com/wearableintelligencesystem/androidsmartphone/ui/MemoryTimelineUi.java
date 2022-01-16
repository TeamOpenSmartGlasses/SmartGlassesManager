package com.wearableintelligencesystem.androidsmartphone.ui;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.wearableintelligencesystem.androidsmartphone.database.mediafile.MediaFileViewModel;
import com.wearableintelligencesystem.androidsmartphone.database.person.PersonEntity;
import com.wearableintelligencesystem.androidsmartphone.database.person.PersonViewModel;
import com.wearableintelligencesystem.androidsmartphone.database.phrase.Phrase;
import com.wearableintelligencesystem.androidsmartphone.database.voicecommand.VoiceCommandViewModel;

import androidx.lifecycle.LiveData;

import com.wearableintelligencesystem.androidsmartphone.R;

//menu imports:
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

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
    private PersonViewModel mPersonViewModel;

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
        mPersonViewModel = new ViewModelProvider(this).get(PersonViewModel.class);

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

        //here we need to get all of the people sightings, then pull in each phrase associated with each command
        List<PersonEntity> peopleSeen = mPersonViewModel.getAllPersonsSnapshotTimePeriod(System.currentTimeMillis() - maxDistanceBackMilliseconds, System.currentTimeMillis()); //back in time this far
        //List<PersonEntity> peopleSeen = mPersonViewModel.getAllPersonsSnapshot();
        Log.d(TAG, "peopleSEen is: " + peopleSeen.toString());
        if (peopleSeen != null){
            long last_seen = 0;
            for (PersonEntity pe : peopleSeen) {
                if (Math.abs(last_seen - pe.getTimestamp()) < (5 * 60 * 1000)){
                    continue;
                }
                Reference new_ref = new Reference();
                new_ref.setStartTimestamp(pe.getTimestamp());
                new_ref.setTitle("Conversation");
                references.add(new_ref);
                last_seen = pe.getTimestamp();
            }
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

    private String getPeopleSeenString(List<PersonEntity> peopleSeen){
        String displayString = "";
        List<Long> personIdsSeen = new ArrayList<Long>();
        for (PersonEntity pe : peopleSeen){
            Log.d(TAG, "PERSON:");
            Log.d(TAG, "---argVal: " + pe.getArgValue());
            Log.d(TAG, "---argKey: " + pe.getArgKey());
            Log.d(TAG, "---personId: " + pe.getPersonId());
            long personId = pe.getPersonId();
            if (! personIdsSeen.contains(personId)){
                String displayName = mPersonViewModel.getPersonsName(personId);
                if (! displayName.equals("deleted")) {
                    displayString = displayString + "- " + displayName + "\n";
                    personIdsSeen.add(personId);
                }
            }
        }
        return displayString;
    }
}
