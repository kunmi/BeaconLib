package com.blogspot.kunmii.beaconsdk.data;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;

import java.util.List;

@Dao
public interface ContentDao {


    @Query("SELECT * FROM content WHERE objectId = :objectId")
    Content getContentsByObjectId(String objectId);

    @Query("SELECT * FROM content")
    LiveData<List<Content>> getAllContentDebug();

    @Query("SELECT * FROM content WHERE discovered = 1")
    LiveData<List<Content>> getContents();

    @Query("SELECT * FROM content WHERE discovered = 0")
    List<Content> getUnDiscoveredContents();

    @Query("SELECT * FROM content WHERE discovered = 0 AND read = 0")
    List<Content> getAllUnreadContents();



    @Query("UPDATE content SET discovered = 1 WHERE objectId = :objectId")
    void setDiscovered(String objectId);

    @Query("UPDATE content SET read = 1 WHERE objectId = :objectId")
    void setRead(String objectId);

    @Query("UPDATE beacon SET beacon = :beaconData, type = :type, updated = :updated  WHERE objectid = :objectId")
    int UpdateBeacon(String objectId, String type, String beaconData, String updated);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertContent(Content beacon);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(Content... contents);

    @Delete
    void delete(Beacon beacon);

    @Query("DELETE FROM  beacon")
    void nukeAll();



}
