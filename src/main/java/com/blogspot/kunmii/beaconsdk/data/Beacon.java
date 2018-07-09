package com.blogspot.kunmii.beaconsdk.data;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;
import android.graphics.PointF;
import android.support.annotation.NonNull;


@Entity(tableName = "beacon")
public class Beacon {

    @NonNull
    @PrimaryKey()
    @ColumnInfo(name  = "objectid")
    String objectId;

    @ColumnInfo(name = "type")
    String type;

    @ColumnInfo(name = "ref")
    String ref;

    @ColumnInfo(name = "lookup")
    String lookUp;

    @ColumnInfo(name = "lastseen")
    long lastSeen = 0;

    @ColumnInfo(name = "txpower")
    String txpower;

    @ColumnInfo(name  = "beacon")
    String beaconData;

    @ColumnInfo(name = "updated")
    String updated;

    @Ignore
    public Proximity proximity = Proximity.OUT_OF_RANGE;


    public String getObjectId() {
        return objectId;
    }

    public void setObjectId(String objectId) {
        this.objectId = objectId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getRef() {
        return ref;
    }

    public void setRef(String ref) {
        this.ref = ref;
    }

    public String getTxpower() {
        return txpower;
    }

    public void setTxpower(String txpower) {
        this.txpower = txpower;
    }

    public String getBeaconData() {
        return beaconData;
    }

    public void setBeaconData(String beaconData) {
        this.beaconData = beaconData;
    }

    public String getUpdated() {
        return updated;
    }


    public void setUpdated(String updated) {
        this.updated = updated;
    }

    public String getLookUp() {
        return lookUp;
    }

    public void setLookUp(String lookup){
        this.lookUp = lookup;
    }

    public long getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(long lastSeen) {
        this.lastSeen = lastSeen;
    }

    public boolean isIbeacon(){
        if(type.equals("iBeacon"))
            return true;
        else
            return false;
    }

    @Ignore
    String getData(){
        return "";
    }

    public enum Proximity{

        // 0 - 0.5m
        IMMEDIATE (0),

        // 0.5 - 3m
        NEAR(1),

        // 3m
        FAR(2),

        // ??m
        OUT_OF_RANGE (3);

        private int id;

        Proximity(int id)
        {
            this.id = id;
        }

        public int getId(){
            return id;
        }

        }



}
