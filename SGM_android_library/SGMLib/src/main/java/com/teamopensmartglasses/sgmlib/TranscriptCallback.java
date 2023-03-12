package com.teamopensmartglasses.sgmlib;

public interface TranscriptCallback {
    void call(String transcript, long timestamp, boolean isFinal);
}
