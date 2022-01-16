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
import com.wearableintelligencesystem.androidsmartphone.database.memorycache.MemoryCache;
import com.wearableintelligencesystem.androidsmartphone.database.memorycache.MemoryCacheViewModel;
import com.wearableintelligencesystem.androidsmartphone.database.voicecommand.VoiceCommandViewModel;

import java.util.ArrayList;
import java.util.List;

public class MemoryCachesUi extends Fragment implements ItemClickListenerReference {
    public String TAG = "WearableAi_MemoryCachesUI";

    private final String fragmentLabel = "Memory Caches";

    private LiveData<List<Reference>> tagBinReferences;
    private Observer<List<Reference>> tagBinReferencesObserver;
    private ReferenceListAdapter referenceListAdapter;
    private ArrayAdapter<String> tagMenuAdapter;
    private AutoCompleteTextView tagMenu;

    private int maxDistanceBackDays = 144; //how many days in time we go backwards
    private int maxDistanceBackMilliseconds = maxDistanceBackDays * 86400 * 1000; //how many days in time we go backwards

    private VoiceCommandViewModel mVoiceCommandViewModel;

    private final int personIntervalSeconds = 60 * 15; //number seconds before and after phrase to show people you saw

    private String currentTag;

    private NavController navController;

    private MediaFileViewModel mMediaFileViewModel;
    private MemoryCacheViewModel mMemoryCacheViewModel;

    @Override
    public void onClick(View view, Reference reference){
        Log.d(TAG, "Click on memory");

        //open a fragment which shows in-depth, contextual view of that transcript
        Bundle bundle = new Bundle();
        bundle.putLong("startTime", reference.getStartTimestamp());
        bundle.putLong("stopTime", reference.getStopTimestamp());
        bundle.putString("type", "cache");
        bundle.putLong("cacheId", reference.getId());
        bundle.putString("title", "Memory Cache");
        navController.navigate(R.id.nav_all_transcripts, bundle);
    }


    public MemoryCachesUi() {
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

        Log.d(TAG, "MemoryCachesUI onCreateView");
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.memory_cache_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState){
        super.onViewCreated(view, savedInstanceState);

        //setup the title
        UiUtils.setupTitle(getActivity(), fragmentLabel);

        Log.d(TAG, "MXT CACHES onViewCreated");

        //create NavController
        navController = Navigation.findNavController(getView());

        //get view models
        mMediaFileViewModel = new ViewModelProvider(this).get(MediaFileViewModel.class);
        mMemoryCacheViewModel = new ViewModelProvider(this).get(MemoryCacheViewModel.class);

        //setup list of phrases
        RecyclerView recyclerView = view.findViewById(R.id.reference_wall);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        linearLayoutManager.setReverseLayout(true);
        final ReferenceListAdapter adapter = new ReferenceListAdapter(getContext(), mMediaFileViewModel);
        adapter.setClickListener(this);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.scrollToPosition(recyclerView.getAdapter().getItemCount() -1);

        //here we need to get all of the voice notes, then pull in each phrase associated with each command
        List<MemoryCache> caches = mMemoryCacheViewModel.getAllCachesSnapshot();
        List<Reference> references = new ArrayList<Reference>();
        for (MemoryCache c : caches){
            Reference new_ref = new Reference();
            new_ref.setStartTimestamp(c.getStartTimestamp());
            if (c.getStopTimestamp() != null) {
                new_ref.setStopTimestamp(c.getStopTimestamp());
            } else {
                new_ref.setStopTimestamp(System.currentTimeMillis()); //still active, so set untl now
            }
            new_ref.setSummary("");
            new_ref.setId(c.getId());
            String cacheName = c.getCacheName();
            if (cacheName != null) {
                new_ref.setTitle(cacheName);
            } else {
                new_ref.setTitle("Unnamed cache");
            }
            references.add(new_ref);
        }

        //here we need to get all of the people sightings, then pull in each phrase associated with each command
//        List<PersonEntity> peopleSeen = mPersonViewModel.getAllPersonsSnapshotTimePeriod(System.currentTimeMillis() - maxDistanceBackMilliseconds, System.currentTimeMillis()); //back in time this far
//        if (peopleSeen != null){
//            for (PersonEntity pe : peopleSeen) {
//                String peopleSeenDisplayString = getPeopleSeenString(peopleSeen);
//                Reference new_ref = new Reference();
//                new_ref.setTimestamp(pe.getTimestamp());
//                new_ref.setSummary(peopleSeenDisplayString);
//                new_ref.setTitle("Conversation");
//                references.add(new_ref);
//            }
//        }

        //send this data to be displayed
        adapter.setReferences(references);
    }

//    private String getPeopleSeenString(List<PersonEntity> peopleSeen){
//        String displayString = "";
//        List<Long> personIdsSeen = new ArrayList<Long>();
//        for (PersonEntity pe : peopleSeen){
//            Log.d(TAG, "PERSON:");
//            Log.d(TAG, "---argVal: " + pe.getArgValue());
//            Log.d(TAG, "---argKey: " + pe.getArgKey());
//            Log.d(TAG, "---personId: " + pe.getPersonId());
//            long personId = pe.getPersonId();
//            if (! personIdsSeen.contains(personId)){
//                String displayName = mPersonViewModel.getPersonsName(personId);
//                if (! displayName.equals("deleted")) {
//                    displayString = displayString + "- " + displayName + "\n";
//                    personIdsSeen.add(personId);
//                }
//            }
//        }
//        return displayString;
//    }
}

