package com.example.fcm;

import android.app.Application;

import com.google.firebase.FirebaseApp;

public class MainApps extends Application {
    @Override
    public void onCreate() {
        FirebaseApp.initializeApp(this);
        super.onCreate();
    }
}

