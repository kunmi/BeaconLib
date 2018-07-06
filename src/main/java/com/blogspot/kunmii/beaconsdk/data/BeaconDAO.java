package com.blogspot.kunmii.beaconsdk.data;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;

import java.util.List;

@Dao
public interface BeaconDAO {

    @Query("SELECT * FROM beacon")
    List<Beacon> getBeacons();

    @Query("UPDATE beacon SET beacon = :beaconData, type = :type, updated = :updated  WHERE objectid = :objectId")
    int UpdateBeacon(String objectId, String type, String beaconData, String updated);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertBeacon(Beacon beacon);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(Beacon... beacons);

    @Delete
    void delete(Beacon beacon);

    @Query("DELETE FROM  beacon")
    void nukeAll();


}
