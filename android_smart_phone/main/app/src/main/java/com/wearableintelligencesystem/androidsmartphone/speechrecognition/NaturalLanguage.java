package com.wearableintelligencesystem.androidsmartphone.speechrecognition;

public class NaturalLanguage {

    public NaturalLanguage(String naturalLanguageName, String code, String modelLocation){
        this.naturalLanguageName = naturalLanguageName;
        this.code = code;
        this.modelLocation = modelLocation;
    }

    public String getNaturalLanguageName() {
        return naturalLanguageName;
    }

    public void setNaturalLanguageName(String naturalLanguageName) {
        this.naturalLanguageName = naturalLanguageName;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getModelLocation() {
        return modelLocation;
    }

    public void setModelLocation(String modelLocation) {
        this.modelLocation = modelLocation;
    }

    private String naturalLanguageName;
    private String code;
    private String modelLocation;
}
