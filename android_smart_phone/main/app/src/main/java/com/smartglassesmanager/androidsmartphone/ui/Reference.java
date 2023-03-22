package com.smartglassesmanager.androidsmartphone.ui;

public class Reference {
    private String summary;
    private String title;
    private Long startTimestamp;
    private Long stopTimestamp;
    private long imagePath;
    private long id;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Long getStopTimestamp() {
        return stopTimestamp;
    }

    public void setStopTimestamp(Long stopTimestamp) {
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

    public void setStartTimestamp(Long startTimestamp) {
        this.startTimestamp = startTimestamp;
    }
    public String getSummary() {
        return summary;
    }

    public String getTitle() {
        return title;
    }

    public Long getStartTimestamp() {
        return startTimestamp;
    }
}
