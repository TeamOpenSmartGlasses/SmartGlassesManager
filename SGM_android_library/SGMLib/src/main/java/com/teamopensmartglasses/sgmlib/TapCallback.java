package com.teamopensmartglasses.sgmlib;

public interface TapCallback extends SubscriptionCallback {
    void call(int numTaps, boolean sideOfGlasses, long timestamp);
}
