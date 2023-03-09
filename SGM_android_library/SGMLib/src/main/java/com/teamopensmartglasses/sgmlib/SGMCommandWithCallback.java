package com.teamopensmartglasses.sgmlib;

public class SGMCommandWithCallback {
    public SGMCommand command;
    public SGMCommandCallback callback;

    public SGMCommandWithCallback(SGMCommand command, SGMCommandCallback callback){
        this.command = command;
        this.callback = callback;
    }
}
