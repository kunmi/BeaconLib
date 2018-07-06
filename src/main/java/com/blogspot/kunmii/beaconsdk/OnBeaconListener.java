package com.blogspot.kunmii.beaconsdk;

import com.blogspot.kunmii.beaconsdk.data.Beacon;

import java.util.List;

public interface OnBeaconListener {

    void onReachedBeaconZone(Beacon newlySeen);
    void onLeftBeaconZoneList(Beacon justLost);

}
