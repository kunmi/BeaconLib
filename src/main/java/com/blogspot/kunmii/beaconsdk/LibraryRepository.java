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
import com.blogspot.kunmii.beaconsdk.utils.Config;
import com.blogspot.kunmii.beaconsdk.utils.Helpers;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.List;

public class LibraryRepository {

    static LibraryRepository mInstance = null;

    Application mContext = null;
    AppDatabase db;

    BeaconDAO beaconDAO;
    ContentDao contentDao;
    OnBeaconUpdatedListener beaconListener = null;
    OnContentUpdateListener contentListener = null;

    private int last_seen_threshhold_in_seconds = 3600;


    private LibraryRepository(Application application){
        mContext = application;

        db = AppDatabase.getInstance(mContext);

        beaconDAO = db.beaconDAO();
        contentDao = db.contentDao();
    }

    public static LibraryRepository getInstance(Application application)
    {
        if(mInstance == null) {
            mInstance = new LibraryRepository(application);
        }

        return mInstance;
    }

    public void registerUpdateListeners(OnBeaconUpdatedListener updateListener, OnContentUpdateListener contentListener){
        mInstance.beaconListener = updateListener;
        mInstance.contentListener = contentListener;


        new Thread(new Runnable() {
            @Override
            public void run() {

                List<Beacon> beacons = mInstance.beaconDAO.getBeacons();
                updateListener.onUpdate(beacons);

                List<Content> contents = mInstance.contentDao.getUnDiscoveredContents();
                contentListener.onContentUpdate(contents);

                mInstance.performBeaconUpdateCheck();

            }
        }).start();

        new Thread(this::sendPushToken).start();

    }

    public List<Beacon> getBeacons(){
        return beaconDAO.getBeacons();
    }

    public LiveData<List<Content>> getContent(){
        return contentDao.getContents();
    }



    public void performBeaconUpdateCheck(){

        ServerRequest updateRequest = Helpers.craftProjectUpdateRequest(mContext);

        updateRequest.execute(new IServerRequestListener() {
            @Override
            public void onResponse(ServerResponse response) {

                try{
                    JSONObject jsonObject = new JSONObject(response.getJsonBody());

                    if(jsonObject.getBoolean(Config.NETWORK_JSON_NODE.SUCCESS)){

                        JSONObject data = jsonObject.getJSONObject("data");

                        JSONArray beaconJSONArray = data.getJSONArray("beacons");

                        for(int i=0; i< beaconJSONArray.length(); i++){

                            JSONObject beaconJson = beaconJSONArray.getJSONObject(i);

                            Beacon b = new Beacon();

                            b.setObjectId(beaconJson.getString(Config.NETWORK_JSON_NODE.OBJECT_ID));
                            b.setType(beaconJson.getString(Config.NETWORK_JSON_NODE.BEACON_TYPE));

                            StringBuilder sb = new StringBuilder();

                            if(b.getType().equals("iBeacon")){
                                sb.append(beaconJson.getString(Config.NETWORK_JSON_NODE.IBEACON_UUID));
                                sb.append(beaconJson.getString(Config.NETWORK_JSON_NODE.IBEACON_MAJOR));
                                sb.append(beaconJson.getString(Config.NETWORK_JSON_NODE.IBEACON_MINOR));
                            }
                            else
                            {
                                sb.append(beaconJson.getString(Config.NETWORK_JSON_NODE.EDDY_NAMESPACEID));
                                sb.append(beaconJson.getString(Config.NETWORK_JSON_NODE.EDDY_INSTANCEID));
                            }

                            b.setLookUp(sb.toString());


                            b.setRef(beaconJson.getString(Config.NETWORK_JSON_NODE.BEACON_REF));
                            b.setTxpower(beaconJson.getString(Config.NETWORK_JSON_NODE.BEACON_TXPOWER));

                            if (beaconJson.has(Config.NETWORK_JSON_NODE.UPDATED)) {
                                b.setUpdated(beaconJson.getString(Config.NETWORK_JSON_NODE.UPDATED));
                            } else {
                                b.setUpdated(beaconJson.getString(Config.NETWORK_JSON_NODE.CREATED));
                            }

                            b.setBeaconData(beaconJson.toString());
                            beaconDAO.insertBeacon(b);
                        }

                        JSONArray contentJSONArray = data.getJSONArray("contents");
                        for(int i=0; i< contentJSONArray.length(); i++){
                            JSONObject contentJson = contentJSONArray.getJSONObject(i);
                            Content c = new Content();
                            c.setObjectId(contentJson.getString(Config.NETWORK_JSON_NODE.OBJECT_ID));
                            c.setTitle(contentJson.getString(Config.NETWORK_JSON_NODE.CONTENT_TITLE));
                            c.setBody(contentJson.getString(Config.NETWORK_JSON_NODE.CONTENT_BODY));
                            c.setPublished(contentJson.getString(Config.NETWORK_JSON_NODE.CONTENT_PUBLISHED));
                            c.setBeacons(contentJson.getJSONArray(Config.NETWORK_JSON_NODE.CONTENT_BEACONS).toString());

                            Content old = contentDao.getContentsByObjectId(c.getObjectId());

                            if(old != null)
                            {
                                c.setDiscovered(old.getDiscovered());
                                c.setRead(old.getRead());
                            }
                            contentDao.insertContent(c);
                        }

                        if(data.has(Config.NETWORK_JSON_NODE.BEACON_LAST_UPDATE_TAG)){
                            Helpers.storeBeaconLastUpdate(data.getLong(Config.NETWORK_JSON_NODE.BEACON_LAST_UPDATE_TAG), mContext);
                        }

                        if(data.has(Config.NETWORK_JSON_NODE.CONTENT_LAST_UPDATE_TAG)){
                            Helpers.storeContentLastUpdate(data.getLong(Config.NETWORK_JSON_NODE.CONTENT_LAST_UPDATE_TAG), mContext);
                        }
                    }


                    if(beaconListener !=null){
                        beaconListener.onUpdate(beaconDAO.getBeacons());
                    }

                    if(contentListener != null){
                        contentListener.onContentUpdate(contentDao.getUnDiscoveredContents());
                    }


                }
                catch (JSONException exp){
                    exp.printStackTrace();
                }

                catch (NullPointerException exp){
                    exp.printStackTrace();
                }


            }
        });

    }

    public void sendPushToken(){
        String token = Helpers.getToken(mContext);

        if(token!=null){
            ServerRequest request = Helpers.sendFCMTokenToServer(mContext, token);
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


    public void sendBeaconSeen(Beacon b){

        if((new Date().getTime() - b.getLastSeen() >= last_seen_threshhold_in_seconds)) {

            ServerRequest req = Helpers.craftBeaconUpdateRequest(mContext, b);
            req.execute(new IServerRequestListener() {
                @Override
                public void onResponse(ServerResponse response) {
                    try {
                        Log.d(getClass().getSimpleName(), "sendBeaconSeen " + response.getJsonBody());

                        JSONObject obj = new JSONObject(response.getJsonBody());

                        if(obj.getBoolean(Config.NETWORK_JSON_NODE.SUCCESS)){
                            b.setLastSeen(new Date().getTime());
                        }

                    } catch (Exception exp) {
                        exp.printStackTrace();
                    }
                }
            });
        }
    }


    public interface OnBeaconUpdatedListener{
         void onUpdate(List<Beacon> beacons);
    }

    public interface OnContentUpdateListener{
        void onContentUpdate(List<Content> contents);
    }


}
