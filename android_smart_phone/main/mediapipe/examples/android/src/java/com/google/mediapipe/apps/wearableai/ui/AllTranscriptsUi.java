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
import android.widget.EditText;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import java.util.List;

import com.google.mediapipe.apps.wearableai.database.phrase.Phrase;
import com.google.mediapipe.apps.wearableai.database.phrase.PhraseViewModel;

import com.google.mediapipe.apps.wearableai.ui.ItemClickListener;

import com.google.mediapipe.apps.wearableai.R;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link StreamFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class AllTranscriptsUi extends Fragment implements ItemClickListener {
    public String TAG = "WearableAi_AllTransriptsUi";

    public static final int NEW_PHRASE_ACTIVITY_REQUEST_CODE = 1;
    public static final int VIEW_PHRASE_ACTIVITY_REQUEST_CODE = 2;
    public static final int SETTINGS_ACTIVITY_REQUEST_CODE = 2;
    public static final int LOGIN_ACTIVITY_REQUEST_CODE = 3;
    public static final int CACHE_ACTIVITY_REQUEST_CODE = 4; //I don't know what these are for yet but this looks cool

    private PhraseViewModel mPhraseViewModel;
    
    private NavController navController;

    @Override
    public void onClick(View view, Phrase phrase){
        Log.d(TAG, "Click on transcript");

        //open a fragment which shows in-depth, contextual view of that transcript
        Bundle bundle = new Bundle();
        bundle.putSerializable("phrase", phrase);
        navController.navigate(R.id.nav_phrase_context, bundle);
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

        mPhraseViewModel.getAllPhrases().observe(this, new Observer<List<Phrase>>() {
            @Override
            public void onChanged(@Nullable final List<Phrase> phrases) {
                // Update the cached copy of the words in the adapter.
                adapter.setPhrases(phrases);
            }
        });

        EditText editText = view.findViewById(R.id.add_phrase_text);
        Button submitButton = view.findViewById(R.id.submit_button);
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String words = editText.getText().toString();
                if(!words.isEmpty()) {
                    editText.setText("");
                    mPhraseViewModel.addPhrase(words, getString(R.string.medium_text));
                }
            }
        });

    }
}

