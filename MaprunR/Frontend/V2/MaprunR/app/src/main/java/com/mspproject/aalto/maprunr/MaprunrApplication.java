package com.mspproject.aalto.maprunr;

import android.app.Application;
import java.net.CookieManager;
import java.net.CookieHandler;

public class MaprunrApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        CookieManager manager = new CookieManager();
        CookieHandler.setDefault(manager);
    }
}