package com.blogspot.kunmii.beaconsdk.fcm;

import android.app.Application;

import com.blogspot.kunmii.beaconsdk.LibraryRepository;
import com.google.firebase.messaging.RemoteMessage;

public class FirebaseMessagingService extends com.google.firebase.messaging.FirebaseMessagingService{
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);


    }


    public static void consumeFirebaseMessage(Application mcontext, RemoteMessage msg){
        LibraryRepository.getInstance(mcontext).performBeaconUpdateCheck();
    }
}
