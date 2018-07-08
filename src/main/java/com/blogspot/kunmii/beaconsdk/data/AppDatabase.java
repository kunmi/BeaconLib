package com.blogspot.kunmii.beaconsdk.data;


import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.content.Context;

@Database(entities = {Beacon.class, Content.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    public static final String DATABASE_NAME = "beaconsdkidb";

    private static AppDatabase mInstance;


    public abstract BeaconDAO beaconDAO();
    public abstract ContentDao contentDao();

    public static AppDatabase getInstance(Context context)
    {
        if(mInstance == null)
        {
            mInstance = Room.databaseBuilder(context, AppDatabase.class, DATABASE_NAME)
                    .fallbackToDestructiveMigration()
                    .build();
        }

        return mInstance;
    }


    public void CLEAR_ALL(){
        beaconDAO().nukeAll();
        contentDao().nukeAll();
     }



}
