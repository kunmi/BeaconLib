package com.blogspot.kunmii.beaconsdk.fcm;

import android.util.Log;

import com.blogspot.kunmii.beaconsdk.network.ServerRequest;
import com.blogspot.kunmii.beaconsdk.network.ServerResponse;
import com.blogspot.kunmii.beaconsdk.utils.Helpers;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

public class FirebaseInstanceID extends FirebaseInstanceIdService {

    @Override
    public void onTokenRefresh() {

        String refereshedToken = FirebaseInstanceId.getInstance().getToken();

        super.onTokenRefresh();
        ServerRequest request = Helpers.sendFCMTokenToServer(getApplication(), refereshedToken);
        request.execute((ServerResponse response)->{

            try{
                Log.d(getClass().getSimpleName(), response.getJsonBody());
            }
            catch (Exception exp){
                exp.printStackTrace();
            }

        });
    }
}
