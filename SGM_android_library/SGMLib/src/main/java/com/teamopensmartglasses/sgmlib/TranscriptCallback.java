package com.teamopensmartglasses.sgmlib;

public interface TranscriptCallback extends SubscriptionCallback {
    void call(String transcript, long timestamp, boolean isFinal);
}
