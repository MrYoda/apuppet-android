package com.hmdm.control;

import com.hmdm.control.janus.SharingEngineJanus;

public class SharingEngineFactory {
    private static SharingEngine instance;

    public static SharingEngine getSharingEngine() {
        if (instance == null) {
            instance = new SharingEngineJanus();
        }
        return instance;
    }
}
