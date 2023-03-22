package com.smartglassesmanager.androidsmartphone.ui;

//this has a few different modes of being. in general, it shows a list of transcripts, but it may exhibit different behaviour depending no what is passed in the fragment args bundle

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import java.util.List;

import com.smartglassesmanager.androidsmartphone.database.memorycache.MemoryCacheViewModel;
import com.smartglassesmanager.androidsmartphone.database.phrase.Phrase;
import com.smartglassesmanager.androidsmartphone.database.phrase.PhraseViewModel;

import com.smartglassesmanager.androidsmartphone.R;

public class AllTranscriptsUi extends Fragment implements ItemClickListenerPhrase {
    public String TAG = "WearableAi_AllTransriptsUi";

    public final String fragmentLabel = "All Transcripts";

    //cache mode
    public long cacheId;

    public static final int NEW_PHRASE_ACTIVITY_REQUEST_CODE = 1;
    public static final int VIEW_PHRASE_ACTIVITY_REQUEST_CODE = 2;
    public static final int SETTINGS_ACTIVITY_REQUEST_CODE = 2;
    public static final int LOGIN_ACTIVITY_REQUEST_CODE = 3;
    public static final int CACHE_ACTIVITY_REQUEST_CODE = 4; //I don't know what these are for yet but this looks cool

    private long startTime;
    private long stopTime;

    private PhraseViewModel mPhraseViewModel;
    private MemoryCacheViewModel mMemoryCacheViewModel;

    private NavController navController;

    @Override
    public void onClick(View view, Phrase phrase){
        Log.d(TAG, "Click on transcript");

        //open a fragment which shows in-depth, contextual view of that transcript
//        Bundle bundle = new Bundle();
//        bundle.putSerializable("phrase", phrase);
//        navController.navigate(R.id.nav_phrase_context, bundle);
    }

    public AllTranscriptsUi() {
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
        return inflater.inflate(R.layout.all_transcripts_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState){
        //get the data we passed in the bundle to create this fragment
        //next time we do this, let's get a better, more abstracted system going for this type of thing
        boolean startTimeFlag = false;
        boolean stopTimeFlag = false;
        boolean titleFlag = false;
        boolean typeFlag = false;
        if (getArguments() != null) {
            startTimeFlag = getArguments().containsKey("startTime");
            stopTimeFlag = getArguments().containsKey("stopTime");
            titleFlag = getArguments().containsKey("title");
            typeFlag = getArguments().containsKey("type");
        }
        if (startTimeFlag){
            startTime = getArguments().getLong("startTime");
        }
        if (stopTimeFlag){
            stopTime = getArguments().getLong("stopTime");
        } else if (startTimeFlag){ //if there is a start and no stop, make the stop time now
            stopTime = System.currentTimeMillis();
        }
        if (titleFlag){
            UiUtils.setupTitle(getActivity(), getArguments().getString("title"));
        } else {
            UiUtils.setupTitle(getActivity(), fragmentLabel);
        }
        if (typeFlag){
            String type = getArguments().getString("type");

            if (type.equals("cache")){
                cacheId = getArguments().getLong("cacheId");
                Log.d(TAG, "buidling menu");
                //build a menu
                setHasOptionsMenu(true);
//                Menu menu = ((MainActivity)getActivity()).actionBarMenu;
//                ((MainActivity)getActivity()).getMenuInflater().inflate(R.menu.memory_cache_menu, menu);
            }
        }

        navController = Navigation.findNavController(getActivity(), R.id.nav_host_fragment);

        RecyclerView recyclerView = view.findViewById(R.id.phrase_wall);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        linearLayoutManager.setReverseLayout(true);
        final PhraseListAdapter adapter = new PhraseListAdapter(getContext());
        adapter.setClickListener(this);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.scrollToPosition(recyclerView.getAdapter().getItemCount() -1);

        // Get a new or existing ViewModel from the ViewModelProvider.
        mPhraseViewModel = new ViewModelProvider(this).get(PhraseViewModel.class);
        mMemoryCacheViewModel = new ViewModelProvider(this).get(MemoryCacheViewModel.class);

        if (startTimeFlag) {
            mPhraseViewModel.getPhraseRange(startTime, stopTime).observe(this, new Observer<List<Phrase>>() {
                @Override
                public void onChanged(@Nullable final List<Phrase> phrases) {
                    // Update the cached copy of the words in the adapter.
                    adapter.setPhrases(phrases);
                }
            });
        } else {
            mPhraseViewModel.getAllPhrases().observe(this, new Observer<List<Phrase>>() {
                @Override
                public void onChanged(@Nullable final List<Phrase> phrases) {
                    // Update the cached copy of the words in the adapter.
                    adapter.setPhrases(phrases);
                }
            });
        }

    }

//    private void nameCache(){
//        LayoutInflater inflater = getLayoutInflater();
//        View alertLayout = inflater.inflate(R.layout.memory_cache_name_change_modal, null);
//        final TextInputEditText cacheNameEditText = (TextInputEditText) alertLayout.findViewById(R.id.memory_cache_name_edit_text);
//
//        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
//        alert.setTitle("Memory Cache Change Name");
//        // this is set the view from XML inside AlertDialog
//        alert.setView(alertLayout);
//        // disallow cancel of AlertDialog on click of back button and outside touch
//        alert.setCancelable(false);
//
//        alert.setNegativeButton("Cancel", (dialog, which) -> {
//        });
//
//        alert.setPositiveButton("Done", (dialog, which) -> {
//            String cacheName = cacheNameEditText.getText().toString();
//            mMemoryCacheViewModel.setCacheName(cacheId, cacheName);
//            Log.d(TAG, "THE NEW CACHE NAME IS: " + cacheName);
//        });
//
//        AlertDialog dialog = alert.create();
//        dialog.show();
//    }

    @Override
    public void onCreateOptionsMenu(Menu menu,MenuInflater inflater) {
        menu.clear(); // clears all menu items..
        inflater.inflate(R.menu.memory_cache_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.name_cache:
//                nameCache();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}

