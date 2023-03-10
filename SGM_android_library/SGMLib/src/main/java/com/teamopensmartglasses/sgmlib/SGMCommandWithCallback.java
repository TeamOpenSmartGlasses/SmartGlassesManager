package com.teamopensmartglasses.sgmlib;

public class SGMCommandWithCallback {
    public SGMCommand command;
    public SGMCallback callback;

    public SGMCommandWithCallback(SGMCommand command, SGMCallback callback){
        this.command = command;
        this.callback = callback;
    }
}
