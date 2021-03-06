package com.blogspot.kunmii.beaconsdk.data;


import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

@Entity(tableName = "content")
public class Content  {

    @NonNull
    @PrimaryKey()
    String objectId;

    @ColumnInfo(name = "title")
    String title;

    @ColumnInfo(name = "body")
    String body;

    @ColumnInfo(name = "beacons")
    String  beacons;

    @ColumnInfo(name = "published")
    String published;

    @ColumnInfo(name = "discovered")
    Boolean discovered = false;

    @ColumnInfo(name = "read")
    Boolean read = false;

    @NonNull
    public String getObjectId() {
        return objectId;
    }

    public void setObjectId(@NonNull String objectId) {
        this.objectId = objectId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getBeacons() {
        return beacons;
    }

    public void setBeacons(String beacons) {
        this.beacons = beacons;
    }

    public String getPublished() {
        return published;
    }

    public void setPublished(String published) {
        this.published = published;
    }

    public Boolean getDiscovered() {
        return discovered;
    }

    public void setDiscovered(Boolean discovered) {
        this.discovered = discovered;
    }

    public Boolean getRead() {
        return read;
    }

    public void setRead(Boolean read) {
        this.read = read;
    }


    @Ignore
    public List<String> getBeaconsAsListString(){
        List<String> beacons = new ArrayList<>();

        try {
            JSONArray array = new JSONArray(getBeacons());

            for(int i=0; i< array.length(); i++)
            {
                beacons.add(array.getString(i));
            }


        }
        catch (JSONException exp)
        {
            exp.printStackTrace();
        }

        return beacons;
    }

    @Ignore
    public List<Beacon> getBeaconsAsList(BeaconDAO beaconDAO){
        List<Beacon> beacons = new ArrayList<>();

        try {
            JSONArray array = new JSONArray(getBeacons());

            for(int i=0; i< array.length(); i++)
            {
                Beacon b = beaconDAO.getBeaconWithObjectId(array.getString(i));
                beacons.add(b);
            }


        }
        catch (JSONException exp)
        {
            exp.printStackTrace();
        }

        return beacons;
    }

}

