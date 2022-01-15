package com.wearableintelligencesystem.androidsmartphone.ui;

public class Reference {
    private String summary = "This is the reference that I like.";
    private String title = "Reference Man";
    private long timestamp = 1642197491863l;
    private long imagePath;

    public long getImagePath() {
        return imagePath;
    }

    public void setImagePath(long imagePath) {
        this.imagePath = imagePath;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    public String getSummary() {
        return summary;
    }

    public String getTitle() {
        return title;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
