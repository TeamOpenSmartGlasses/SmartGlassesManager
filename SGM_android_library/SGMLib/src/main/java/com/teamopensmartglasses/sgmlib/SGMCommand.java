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
    private Callback callback;

    public SGMCommand(String name, UUID id, String[] phrases, String description, Callback callback){
        this.mName = name;
        this.mId = id;
        this.mPhrases = new ArrayList<>();
        this.mPhrases.addAll(Arrays.asList(phrases));
        this.mDescription = description;
    }

    public SGMCommand(String name, UUID id, String[] phrases, String description, Callback callback, boolean argRequired){
       this(name, id, phrases, description, callback);
       this.argRequired = argRequired;
    }

    public UUID getId(){
        return this.mId;
    }

    public Callback getCallback() {
        return callback;
    }
}

