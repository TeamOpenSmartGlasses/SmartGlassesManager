package com.wearableintelligencesystem.androidsmartphone.ui;

public class Reference {
    private String summary;
    private String title;
    private long startTimestamp;
    private long stopTimestamp;
    private long imagePath;
    private long id;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getStopTimestamp() {
        return stopTimestamp;
    }

    public void setStopTimestamp(long stopTimestamp) {
        this.stopTimestamp = stopTimestamp;
    }

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

    public void setStartTimestamp(long startTimestamp) {
        this.startTimestamp = startTimestamp;
    }
    public String getSummary() {
        return summary;
    }

    public String getTitle() {
        return title;
    }

    public long getStartTimestamp() {
        return startTimestamp;
    }
}
