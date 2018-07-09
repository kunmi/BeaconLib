package com.blogspot.kunmii.beaconsdk;

import android.app.Application;
import android.arch.lifecycle.LiveData;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;

import com.blogspot.kunmii.beaconsdk.data.Beacon;
import com.blogspot.kunmii.beaconsdk.data.Content;
import com.blogspot.kunmii.beaconsdk.utils.Config;
import com.blogspot.kunmii.beaconsdk.utils.Helpers;
import com.kontakt.sdk.android.ble.connection.OnServiceReadyListener;
import com.kontakt.sdk.android.ble.manager.ProximityManager;
import com.kontakt.sdk.android.ble.manager.ProximityManagerFactory;
import com.kontakt.sdk.android.ble.manager.listeners.EddystoneListener;
import com.kontakt.sdk.android.ble.manager.listeners.IBeaconListener;
import com.kontakt.sdk.android.ble.manager.listeners.simple.SimpleEddystoneListener;
import com.kontakt.sdk.android.ble.manager.listeners.simple.SimpleIBeaconListener;
import com.kontakt.sdk.android.common.KontaktSDK;
import com.kontakt.sdk.android.common.profile.IBeaconDevice;
import com.kontakt.sdk.android.common.profile.IBeaconRegion;
import com.kontakt.sdk.android.common.profile.IEddystoneDevice;
import com.kontakt.sdk.android.common.profile.IEddystoneNamespace;
import com.kontakt.sdk.android.common.profile.RemoteBluetoothDevice;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class BeaconManager {

    Application mContext = null;
    static BeaconManager instance = null;

    Beacon.Proximity mSensitivity = Beacon.Proximity.IMMEDIATE;

    private ProximityManager proximityManager;
    LibraryRepository repository;

    Object lock = new Object();

    List<Content> contents = new ArrayList<>();

    HashMap<String, IBeaconDevice> ibeacons = new HashMap<>();
    HashMap<String, IEddystoneDevice> eddystones = new HashMap<>();

    List<Beacon> projectBeacons = new ArrayList<>();
    HashMap<String, Beacon> projectBeaconsDiscovered = new HashMap<>();


    HashMap<String, Beacon> beaconsNearMe = new HashMap<>();

    List<WeakReference<OnBeaconListener>> beaconlisteners = new ArrayList<>();
    List<WeakReference<OnContentListener>> contentListeners = new ArrayList<>();

    Handler mHamdler = null;
    HandlerThread handlerThread = null;

    private BeaconManager(Application context, String token)
    {
        mContext = context;
        repository = LibraryRepository.getInstance(context);

        if(Helpers.getToken(context) == null){
            Helpers.storeUserToken(token, context);
        }
        else if(!Helpers.getToken(context).equals(token)){
            new Thread(()->{
                repository.beaconDAO.nukeAll();
                Helpers.storeUserToken(token, context);

            }).start();

            //mInstance.contentDao.nukeAll();

        }

        repository.registerUpdateListeners(beacons -> {
            projectBeacons = beacons;
        }, contents -> {

            this.contents = contents;

        });

        addBeaconListener(mainBeaconListener);
    }


    public LiveData<List<Content>> getContents(){
        return repository.contentDao.getContents();
    }


    public static BeaconManager getInstance(Application application, String token)
    {
        if (instance == null)
        {
            instance = new BeaconManager(application, token);

        }
        return instance;
    }

    public void addBeaconListener(OnBeaconListener listener){
        this.beaconlisteners.add(new WeakReference<>(listener));
    }

    public void addContentListener(OnContentListener contentListener)
    {
        this.contentListeners.add(new WeakReference<>(contentListener));
    }

    public void setSensitivity(Beacon.Proximity mSensitivity) {
        this.mSensitivity = mSensitivity;
    }



    public void startScanning()
    {

        if (mHamdler == null){
            HandlerThread handlerThread = null;

            handlerThread = new HandlerThread("Worker Thread");
            handlerThread.start();

            mHamdler = new Handler(handlerThread.getLooper());
            mHamdler.postDelayed(()->{

                KontaktSDK.initialize(mContext);

                proximityManager = ProximityManagerFactory.create(mContext);
                proximityManager.setIBeaconListener(createIBeaconListener());
                proximityManager.setEddystoneListener(createEddystoneListener());

                proximityManager.connect(new OnServiceReadyListener() {
                    @Override
                    public void onServiceReady() {
                        proximityManager.startScanning();
                    }
                });

            },0);

        }


     }


    public void stopScanning(){
        proximityManager.stopScanning();
        proximityManager = null;

        handlerThread.quit();
        mHamdler = null;
        handlerThread=null;
    }


    private IBeaconListener createIBeaconListener() {
        return new SimpleIBeaconListener() {
            @Override
            public void onIBeaconDiscovered(IBeaconDevice ibeacon, IBeaconRegion region) {
                Log.i("Sample", "IBeacon discovered: " + ibeacon.toString());

                String key = ibeacon.getAddress();
                //ibeacons.put(key, ibeacon);
                addNewBeaconDevice(ibeacon);

            }

            @Override
            public void onIBeaconsUpdated(List<IBeaconDevice> ibeacons, IBeaconRegion region) {
                Log.d("","");

                updateIbeacon(ibeacons);

            }

            @Override
            public void onIBeaconLost(IBeaconDevice ibeacon, IBeaconRegion region) {
                super.onIBeaconLost(ibeacon, region);


                String key = ibeacon.getAddress();

                removeIbeacon(ibeacon);

            }
        };
    }

    private EddystoneListener createEddystoneListener() {
        return new SimpleEddystoneListener() {
            @Override
            public void onEddystoneDiscovered(IEddystoneDevice eddystone, IEddystoneNamespace namespace) {
                Log.i("Sample", "Eddystone discovered: " + eddystone.toString());

                String key = eddystone.getAddress();

                //eddystones.put(key, eddystone);
                addNewEddystone(eddystone);
            }

            @Override
            public void onEddystonesUpdated(List<IEddystoneDevice> eddystones, IEddystoneNamespace namespace) {
                super.onEddystonesUpdated(eddystones, namespace);

                updateEddystone(eddystones);
            }

            @Override
            public void onEddystoneLost(IEddystoneDevice eddystone, IEddystoneNamespace namespace) {
                super.onEddystoneLost(eddystone, namespace);

                String key = eddystone.getAddress();

                removeEddystone(eddystone);

            }
        };
    }

    void addNewBeaconDevice(IBeaconDevice beaconDevice){
        synchronized (lock)
        {
            if(!projectBeaconsDiscovered.containsKey(beaconDevice.getAddress()))
            {
                ibeacons.put(beaconDevice.getAddress(), beaconDevice);

                for(Beacon b : projectBeacons)
                {
                    if(!b.getType().equals("iBeacon"))
                        continue;

                    try {

                        JSONObject obj = new JSONObject(b.getBeaconData());

                        if (beaconDevice.getProximityUUID().toString() != null && beaconDevice.getProximityUUID().toString().
                                equals(obj.getString(Config.NETWORK_JSON_NODE.IBEACON_UUID))) {

                            if(String.valueOf(beaconDevice.getMajor())
                                    .equals(String.valueOf(obj.getString(Config.NETWORK_JSON_NODE.IBEACON_MAJOR)))){


                                if(!(obj.getString(Config.NETWORK_JSON_NODE.IBEACON_MINOR).equals("")) &&
                                        String.valueOf(beaconDevice.getMinor()).equals(obj.getString(Config.NETWORK_JSON_NODE.IBEACON_MINOR)))
                                {
                                    projectBeaconsDiscovered.put(beaconDevice.getAddress(), b);
                                    performBeaconsNearMeUpdate(beaconDevice);
                                }


                            }


                        }
                    }
                    catch (JSONException exp)
                    {
                        exp.printStackTrace();
                    }
                }

            }
            else
                performBeaconsNearMeUpdate(beaconDevice);


        }
    }

    void addNewEddystone(IEddystoneDevice eddystoneDevice){
        synchronized (lock)
        {
            String key = eddystoneDevice.getAddress();
            if(!projectBeaconsDiscovered.containsKey(key))
            {
                eddystones.put(eddystoneDevice.getAddress(), eddystoneDevice);

                for(Beacon b : projectBeacons)
                {
                    if(b.getType().equals("iBeacon"))
                        continue;

                    try {

                        JSONObject obj = new JSONObject(b.getBeaconData());

                        if (eddystoneDevice.getNamespace() != null && eddystoneDevice.getNamespace().
                                equals(obj.getString(Config.NETWORK_JSON_NODE.EDDY_NAMESPACEID))) {


                                if(eddystoneDevice.getInstanceId()!=null && eddystoneDevice.getInstanceId()
                                        .equals(obj.getString(Config.NETWORK_JSON_NODE.EDDY_INSTANCEID)))
                                {
                                    projectBeaconsDiscovered.put(eddystoneDevice.getAddress(), b);
                                    performBeaconsNearMeUpdate(eddystoneDevice);
                                }

                            }
                    }
                    catch (JSONException exp)
                    {
                        exp.printStackTrace();
                    }
                }

            }
            else
                performBeaconsNearMeUpdate(eddystoneDevice);

        }
    }


    void updateIbeacon(List<IBeaconDevice> devices){

        for(IBeaconDevice dev : devices )
        {
            performBeaconsNearMeUpdate(dev);
        }
    }

    void removeIbeacon(IBeaconDevice device)
    {

        if(ibeacons.containsKey(device.getAddress()))
        {
            ibeacons.remove(device.getAddress());
            performBeaconsNearMeUpdate(device);
        }

    }

    void updateEddystone(List<IEddystoneDevice> devices)
    {
        for(IEddystoneDevice dev : devices)
        {
            performBeaconsNearMeUpdate(dev);
        }
    }

    void removeEddystone(IEddystoneDevice device)
    {
         if(eddystones.containsKey(device.getAddress()))
        {
            eddystones.remove(device.getAddress());
            performBeaconsNearMeUpdate(device);
        }



    }

    void performBeaconsNearMeUpdate(RemoteBluetoothDevice device){

        Beacon.Proximity proximity = Beacon.Proximity.OUT_OF_RANGE;

        switch (device.getProximity()){

            case FAR:
                proximity = Beacon.Proximity.FAR;
                break;

            case NEAR:
                proximity = Beacon.Proximity.NEAR;
                break;

            case IMMEDIATE:
                proximity = Beacon.Proximity.IMMEDIATE;
                break;

            case UNKNOWN:
                proximity = Beacon.Proximity.OUT_OF_RANGE;
        }

        if(beaconsNearMe.containsKey(device.getAddress()))
        {
            Beacon prev = beaconsNearMe.get(device.getAddress());

            if(prev.proximity != proximity)
            {
                if(proximity.getId() <= mSensitivity.getId()){

                    prev.proximity = proximity;
                    beaconsNearMe.put(device.getAddress(), prev);
                }

                else if(proximity.getId() > mSensitivity.getId())
                {
                    beaconsNearMe.remove(device.getAddress());

                    for(WeakReference<OnBeaconListener> listeners : beaconlisteners)
                    {
                        listeners.get().onLeftBeaconZoneList(prev);
                    }
                }

            }
        }
        else{

            if(proximity.getId() <= mSensitivity.getId())
            {
                if(projectBeaconsDiscovered.containsKey(device.getAddress()))
                {
                    Beacon b = projectBeaconsDiscovered.get(device.getAddress());

                    if(b.proximity.getId() == proximity.getId() && beaconsNearMe.containsKey(device.getAddress())) {
                        return;
                    }
                    else
                    {
                        b.proximity = proximity;
                        beaconsNearMe.put(device.getAddress(), b);

                        for(WeakReference<OnBeaconListener> listeners : beaconlisteners)
                        {
                            listeners.get().onReachedBeaconZone(b);
                        }
                    }


                }
            }

        }

    }


    OnBeaconListener mainBeaconListener = new OnBeaconListener() {
        @Override
        public void onReachedBeaconZone(Beacon newlySeen) {

            new Thread(()->{
                for(Content c: contents){

                    for(String objIds : c.getBeaconsAsListString()){
                        if(objIds.equals(newlySeen.getObjectId())){
                            repository.contentDao.setDiscovered(c.getObjectId());
                            break;
                        }
                    }

                }
            }).start();
            //do a beacon last seen update here


        }

        @Override
        public void onLeftBeaconZoneList(Beacon justLost) {

        }
    };

}
