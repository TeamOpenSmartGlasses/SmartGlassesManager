package com.teamopensmartglasses.sgmlib;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

public class SGMCommand implements Serializable {
    private  final String TAG = "SGMLib_SGMCommand";

    private String mName; //name of the command
    private UUID mId; //unique identifier of the command
    public boolean argRequired = false;
    private ArrayList<String> mPhrases; //list of phrases, queries, questions that trigger this command (artificial limit imposed)
    private String mDescription; //a natural language description of what the command does
    private Callback mCallback;

    public SGMCommand(String name, UUID id, String[] phrases, String description){
        this.mName = name;
        this.mId = id;
        this.mPhrases = new ArrayList<>();
        this.mPhrases.addAll(Arrays.asList(phrases));
        this.mDescription = description;
    }

    public SGMCommand(String name, UUID id, String[] phrases, String description, boolean argRequired){
       this(name, id, phrases, description);
       this.argRequired = argRequired;
    }

    public UUID getId(){
        return this.mId;
    }

    public String getDescription() {
        return mDescription;
    }
}

