package com.smartglassesmanager.androidsmartphone.speechrecognition;

import java.util.Locale;

public class NaturalLanguage {

    public NaturalLanguage(String naturalLanguageName, String code, String modelLocation, Locale locale){
        this.naturalLanguageName = naturalLanguageName;
        this.code = code;
        this.modelLocation = modelLocation;
        this.locale = locale;
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
    public Locale getLocale(){return this.locale;}

    public void setLocale(Locale locale){this.locale = locale;}

    public String getModelLocation() {
        return modelLocation;
    }

    public void setModelLocation(String modelLocation) {
        this.modelLocation = modelLocation;
    }

    private String naturalLanguageName;
    private String code;
    private String modelLocation;
    private Locale locale;



}
