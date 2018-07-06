package com.blogspot.kunmii.beaconsdk;

import android.app.Application;
import android.arch.lifecycle.LiveData;
import android.util.Log;

import com.blogspot.kunmii.beaconsdk.data.AppDatabase;
import com.blogspot.kunmii.beaconsdk.data.Beacon;
import com.blogspot.kunmii.beaconsdk.data.BeaconDAO;
import com.blogspot.kunmii.beaconsdk.data.Content;
import com.blogspot.kunmii.beaconsdk.data.ContentDao;
import com.blogspot.kunmii.beaconsdk.network.IServerRequestListener;
import com.blogspot.kunmii.beaconsdk.network.ServerRequest;
import com.blogspot.kunmii.beaconsdk.network.ServerResponse;
import com.blogspot.kunmii.beaconsdk.utils.Helpers;

import java.util.List;

public class LibraryRepository {

    static LibraryRepository mInstance = null;

    Application mContext = null;
    AppDatabase db;

    BeaconDAO beaconDAO;
    ContentDao contentDao;
    OnBeaconUpdatedListener listener = null;

    private LibraryRepository(Application application){
        mContext = application;

        db = AppDatabase.getInstance(mContext);

        beaconDAO = db.beaconDAO();
        contentDao = db.contentDao();

    }

    public static LibraryRepository getInstance(Application application, OnBeaconUpdatedListener updateListener)
    {
        if(mInstance == null) {
            mInstance = new LibraryRepository(application);

            mInstance.listener  = updateListener;

            new Thread(new Runnable() {
                @Override
                public void run() {

                    if(mInstance.beaconDAO.getBeacons().size() ==0)
                    {
                        mInstance.performBeaconUpdateCheck();
                    }

                }
            }).start();

        }

        return mInstance;
    }

    public List<Beacon> getBeacons(){
        return beaconDAO.getBeacons();
    }

    public LiveData<List<Content>> getContent(){
        return contentDao.getContents();
    }


    public interface OnBeaconUpdatedListener{
        public void onUpdate(List<Beacon> beacons);
    }


    private void performBeaconUpdateCheck(){

        ServerRequest updateRequest = Helpers.craftProjectUpdateRequest(mContext);

        updateRequest.execute(new IServerRequestListener() {
            @Override
            public void onResponse(ServerResponse response) {


                String str = response.getJsonBody();
                Log.d("kunmi", str);


            }
        });

    }

}
