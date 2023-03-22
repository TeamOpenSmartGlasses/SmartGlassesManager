package com.smartglassesmanager.androidsmartphone.nlp;

public class FuzzyMatch {
    private int index;
    private double similarity;
    private String incomingString;
    private String toFindString;

    public FuzzyMatch(int index, double similarity){
        this.index = index;
        this.similarity = similarity;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public double getSimilarity() {
        return similarity;
    }

    public void setSimilarity(double similarity) {
        this.similarity = similarity;
    }


    public String getToFindString() {
        return toFindString;
    }

    public void setToFindString(String toFindString) {
        this.toFindString = toFindString;
    }
}
