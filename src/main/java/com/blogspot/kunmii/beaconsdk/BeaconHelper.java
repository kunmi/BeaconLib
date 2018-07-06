package com.blogspot.kunmii.beaconsdk;

import android.app.Application;
import android.arch.lifecycle.MutableLiveData;
import android.util.Log;

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

import java.util.HashMap;
import java.util.List;

public class BeaconHelper {

    private ProximityManager proximityManager;


    MutableLiveData<HashMap<String, IBeaconWrapper>> beaconDeviceLiveData = new MutableLiveData<>();
    MutableLiveData<HashMap<String, EddystoneWrapper>> eddystoneDeviceLiveData = new MutableLiveData<>();


    HashMap<String, IBeaconWrapper> ibeacons = new HashMap<>();
    HashMap<String, EddystoneWrapper> eddystones = new HashMap<>();

    //ForUpdating - LiveMap
    MutableLiveData<List<IBeaconDevice>> updatedIbeacon = new MutableLiveData<>();
    MutableLiveData<List<IEddystoneDevice>> updatedEddystone = new MutableLiveData<>();

    //Marked for lost
    MutableLiveData<IBeaconDevice> lostIBeacon = new MutableLiveData<>();
    MutableLiveData<IEddystoneDevice> lostEddyBeacon = new MutableLiveData<>();


    public BeaconHelper(Application context) {

        KontaktSDK.initialize(context);

        proximityManager = ProximityManagerFactory.create(context);
        proximityManager.setIBeaconListener(createIBeaconListener());
        proximityManager.setEddystoneListener(createEddystoneListener());
    }


    public void onStart(){
        startScanning();
    }

    public void onStop(){
        proximityManager.stopScanning();
    }

    public void onDestroy(){
        proximityManager.disconnect();
        proximityManager = null;
    }

    private void startScanning() {
        proximityManager.connect(new OnServiceReadyListener() {
            @Override
            public void onServiceReady() {
                proximityManager.startScanning();
            }
        });
    }

    private IBeaconListener createIBeaconListener() {
        return new SimpleIBeaconListener() {
            @Override
            public void onIBeaconDiscovered(IBeaconDevice ibeacon, IBeaconRegion region) {
                Log.i("Sample", "IBeacon discovered: " + ibeacon.toString());

                String key = ibeacon.getAddress();


                ibeacons.put(key, new IBeaconWrapper(region,ibeacon));
                beaconDeviceLiveData.setValue(ibeacons);


            }

            @Override
            public void onIBeaconsUpdated(List<IBeaconDevice> ibeacons, IBeaconRegion region) {
                Log.d("","");
                updatedIbeacon.setValue(ibeacons);
            }

            @Override
            public void onIBeaconLost(IBeaconDevice ibeacon, IBeaconRegion region) {
                super.onIBeaconLost(ibeacon, region);


                String key = ibeacon.getAddress();

                if(ibeacons.containsKey(key))
                {
                    ibeacons.remove(key);
                    beaconDeviceLiveData.setValue(ibeacons);
                }

                lostIBeacon.setValue(ibeacon);
            }
        };
    }

    private EddystoneListener createEddystoneListener() {
        return new SimpleEddystoneListener() {
            @Override
            public void onEddystoneDiscovered(IEddystoneDevice eddystone, IEddystoneNamespace namespace) {
                Log.i("Sample", "Eddystone discovered: " + eddystone.toString());

                String key = eddystone.getAddress();

                eddystones.put(key, new EddystoneWrapper(namespace,eddystone));
                eddystoneDeviceLiveData.setValue(eddystones);
            }

            @Override
            public void onEddystonesUpdated(List<IEddystoneDevice> eddystones, IEddystoneNamespace namespace) {
                super.onEddystonesUpdated(eddystones, namespace);

                updatedEddystone.setValue(eddystones);
            }

            @Override
            public void onEddystoneLost(IEddystoneDevice eddystone, IEddystoneNamespace namespace) {
                super.onEddystoneLost(eddystone, namespace);

                String key = eddystone.getAddress();

                if(eddystones.containsKey(key))
                {
                    eddystones.remove(key);
                    eddystoneDeviceLiveData.setValue(eddystones);
                }

                lostEddyBeacon.setValue(eddystone);
            }
        };
    }


    public MutableLiveData<HashMap<String, IBeaconWrapper>> getIBeaconDeviceLiveData() {
        return beaconDeviceLiveData;
    }

    public MutableLiveData<HashMap<String, EddystoneWrapper>> getEddystoneDeviceLiveData() {
        return eddystoneDeviceLiveData;
    }

    public MutableLiveData<IEddystoneDevice> getLostEddyBeacon() {
        return lostEddyBeacon;
    }

    public MutableLiveData<IBeaconDevice> getLostIBeacon() {
        return lostIBeacon;
    }

    public MutableLiveData<List<IBeaconDevice>> getUpdatedIbeacon() {
        return updatedIbeacon;
    }

    public MutableLiveData<List<IEddystoneDevice>> getUpdatedEddystone() {
        return updatedEddystone;
    }

    public interface BeaconWrapper{}


    public class IBeaconWrapper implements BeaconWrapper{

        public IBeaconRegion region;
        public IBeaconDevice device;

        public IBeaconWrapper(IBeaconRegion reg, IBeaconDevice dev)
        {
            this.region = reg;
            this.device = dev;
        }

    }

    public class EddystoneWrapper implements BeaconWrapper{

        public IEddystoneNamespace namespace;
        public IEddystoneDevice device;


        public EddystoneWrapper(IEddystoneNamespace namespace, IEddystoneDevice dev)
        {
            this.namespace = namespace;
            this.device = dev;

        }
    }



}
