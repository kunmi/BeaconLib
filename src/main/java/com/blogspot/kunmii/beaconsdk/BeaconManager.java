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
import com.kontakt.sdk.android.ble.device.EddystoneDevice;
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
import java.util.concurrent.ConcurrentHashMap;

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

    ConcurrentHashMap<String, Beacon> projectBeacons = new ConcurrentHashMap<>();
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

            projectBeacons.clear();

            for(Beacon b : beacons){
                projectBeacons.put(b.getLookUp(), b);
            }

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

                //ibeacons.put(key, ibeacon);
                addNewBeaconDevice(ibeacon);

            }

            @Override
            public void onIBeaconsUpdated(List<IBeaconDevice> ibeacons, IBeaconRegion region) {
                Log.d("","");

                updateIBeacons(ibeacons);

            }

            @Override
            public void onIBeaconLost(IBeaconDevice ibeacon, IBeaconRegion region) {
                super.onIBeaconLost(ibeacon, region);

                removeBeacons(ibeacon);

            }
        };
    }

    private EddystoneListener createEddystoneListener() {
        return new SimpleEddystoneListener() {
            @Override
            public void onEddystoneDiscovered(IEddystoneDevice eddystone, IEddystoneNamespace namespace) {
                Log.i("Sample", "Eddystone discovered: " + eddystone.toString());

                addNewBeaconDevice(eddystone);
            }

            @Override
            public void onEddystonesUpdated(List<IEddystoneDevice> eddystones, IEddystoneNamespace namespace) {
                super.onEddystonesUpdated(eddystones, namespace);

                updateEddyBeacons(eddystones);
            }

            @Override
            public void onEddystoneLost(IEddystoneDevice eddystone, IEddystoneNamespace namespace) {
                super.onEddystoneLost(eddystone, namespace);

                removeBeacons(eddystone);

            }
        };
    }

    void addNewBeaconDevice(RemoteBluetoothDevice beaconDevice){
        String address = resolveLookUp(beaconDevice);

        synchronized (lock)
        {
            if(!projectBeaconsDiscovered.containsKey(address)) {

                if (beaconDevice instanceof IBeaconDevice)
                    ibeacons.put(beaconDevice.getAddress(), (IBeaconDevice) beaconDevice);
                else
                    eddystones.put(beaconDevice.getAddress(), (IEddystoneDevice) beaconDevice);

                if (projectBeacons.containsKey(address)) {

                    Beacon b = projectBeacons.get(address);
                    b.proximity = resolveProximity(beaconDevice);

                    projectBeaconsDiscovered.put(address, b);
                    performBeaconsNearMeUpdate(beaconDevice);
                }
            }
            else
            {
                performBeaconsNearMeUpdate(beaconDevice);
                //Already seen and marked as discovered
            }
        }
    }


    void updateIBeacons(List<IBeaconDevice> devices){

        for(IBeaconDevice dev : devices )
        {
            performBeaconsNearMeUpdate(dev);
        }
    }
    void updateEddyBeacons(List<IEddystoneDevice> devices){

        for(IEddystoneDevice dev : devices )
        {
            performBeaconsNearMeUpdate(dev);
        }
    }

    void removeBeacons(RemoteBluetoothDevice device)
    {

        if(device instanceof IBeaconDevice){
            ibeacons.remove(device.getAddress());
        }
        else {
            eddystones.remove(device.getAddress());
        }

        String lookup = resolveLookUp(device);
        if(projectBeaconsDiscovered.containsKey(lookup))
            performBeaconsNearMeUpdate(device);
    }


    Beacon.Proximity resolveProximity(RemoteBluetoothDevice device){
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
        return proximity;

    }
    void performBeaconsNearMeUpdate(RemoteBluetoothDevice device){

        Beacon.Proximity proximity =resolveProximity(device);
        String lookUpKey = resolveLookUp(device);

        if(beaconsNearMe.containsKey(lookUpKey))
        {
            Beacon prev = beaconsNearMe.get(lookUpKey);

            if(prev.proximity != proximity)
            {
                if(proximity.getId() <= mSensitivity.getId()){

                    prev.proximity = proximity;
                    beaconsNearMe.put(lookUpKey, prev);
                    projectBeaconsDiscovered.put(lookUpKey, prev);
                }

                else if(proximity.getId() > mSensitivity.getId())
                {
                    beaconsNearMe.remove(lookUpKey);

                    prev.proximity = proximity;
                    projectBeaconsDiscovered.put(lookUpKey, prev);

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
                if(projectBeaconsDiscovered.containsKey(lookUpKey))
                {
                    Beacon b = projectBeaconsDiscovered.get(lookUpKey);

                        b.proximity = proximity;

                        if(device instanceof IEddystoneDevice){
                            if(((IEddystoneDevice) device).getTelemetry()!=null)
                               b.telemetry = ((IEddystoneDevice) device).getTelemetry().toString();
                        }
                        beaconsNearMe.put(lookUpKey, b);
                        projectBeaconsDiscovered.put(lookUpKey, b);

                        for(WeakReference<OnBeaconListener> listeners : beaconlisteners)
                        {
                            listeners.get().onReachedBeaconZone(b);
                        }
                }
            }

        }

    }


    OnBeaconListener mainBeaconListener = new OnBeaconListener() {

        @Override
        public void onReachedBeaconZone(Beacon newlySeen) {
            Log.d("onReachedBeaconZone", newlySeen.getObjectId());

            new Thread(()->{
                for(Content c: contents){

                    for(String objIds : c.getBeaconsAsListString()){
                        if(objIds.equals(newlySeen.getObjectId())){
                            repository.contentDao.setDiscovered(c.getObjectId());
                            break;
                        }
                    }
                }
                repository.sendBeaconSeen(newlySeen);

            }).start();
        }

        @Override
        public void onLeftBeaconZoneList(Beacon justLost) {
            Log.d("onLeftBeaconZoneList", justLost.getObjectId());
        }
    };


    String resolveLookUp(RemoteBluetoothDevice dev){

        StringBuilder sb = new StringBuilder();

        if(dev instanceof IBeaconDevice){
            sb.append(((IBeaconDevice) dev).getProximityUUID());
            sb.append(String.valueOf(((IBeaconDevice) dev).getMajor()));
            sb.append(String.valueOf(((IBeaconDevice) dev).getMinor()));
        }
        else {
            sb.append(((IEddystoneDevice) dev).getNamespace());
            sb.append(((IEddystoneDevice) dev).getInstanceId());
        }

        return sb.toString();

    }



}
