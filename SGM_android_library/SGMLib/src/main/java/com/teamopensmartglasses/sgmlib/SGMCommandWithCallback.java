package com.teamopensmartglasses.sgmlib;

public class SGMCommandWithCallback {
    public SGMCommand command;
    public Callback callback;

    public SGMCommandWithCallback(SGMCommand command, Callback callback){
        this.command = command;
        this.callback = callback;
    }
}
