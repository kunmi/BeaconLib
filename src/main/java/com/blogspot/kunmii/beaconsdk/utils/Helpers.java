package com.blogspot.kunmii.beaconsdk.utils;

import android.app.Application;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;


import com.blogspot.kunmii.beaconsdk.network.ServerRequest;
import com.google.gson.JsonObject;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

public class Helpers {
    public static void showDialog(Context activity, String title,String text, String okText,DialogInterface.OnClickListener listener)
    {
        AlertDialog.Builder build = new AlertDialog.Builder(activity);
        build.setTitle(title);
        build.setMessage(text);
        build.setPositiveButton(okText, listener);
        build.show();
    }

    public static void showDialog(Context activity, String title,String text)
    {
        AlertDialog.Builder build = new AlertDialog.Builder(activity);
        build.setTitle(title);
        build.setMessage(text);
        build.show();
    }


    public static void storeUserToken(String token, Application mContext)
    {
        SharedPreferences defaultPreference = PreferenceManager.getDefaultSharedPreferences(mContext);
        SharedPreferences.Editor pref = defaultPreference.edit();

        pref.putString(Config.TOKEN, token);
        pref.apply();
    }

    public static void storeBeaconLastUpdate(long lastupdate, Application mContext){

        SharedPreferences defaultPref = PreferenceManager.getDefaultSharedPreferences( mContext);
        SharedPreferences.Editor prefEdit = defaultPref.edit();

        prefEdit.putLong("beacon_update", lastupdate);
        prefEdit.apply();
    }

    public static long getBeaconLastUpdate(Application mContext){
        SharedPreferences defaultPref = PreferenceManager.getDefaultSharedPreferences(mContext);
        return defaultPref.getLong("beacon_update", -1);
    }


    public static void storeContentLastUpdate(long lastupdate, Application mContext){

        SharedPreferences defaultPref = PreferenceManager.getDefaultSharedPreferences( mContext);
        SharedPreferences.Editor prefEdit = defaultPref.edit();

        prefEdit.putLong("content_update", lastupdate);
        prefEdit.apply();
    }

    public static long getContentLastUpdate(Application mContext){
        SharedPreferences defaultPref = PreferenceManager.getDefaultSharedPreferences(mContext);
        return defaultPref.getLong("content_update", -1);
    }

    public static boolean StoreUserData(JSONObject userData, Application context)
    {
        try {
            String projectId = userData.getString("project");
            String last_project_update = userData.getString("last_project_update");
            String last_beacon_update = userData.getString("last_beacon_update");
            String last_content_update = userData.getString("last_content_update");

            SharedPreferences defaultPrefernce = PreferenceManager.getDefaultSharedPreferences(context);
            SharedPreferences.Editor pref =  defaultPrefernce.edit();

            pref.putString(Config.PROJECT_ID, projectId);
            pref.putString(Config.LAST_PROJECT_UPDATE, last_project_update);
            pref.putString(Config.LAST_BEACON_UPDATE, last_beacon_update);
            pref.putString(Config.LAST_CONTENT_UPDATE, last_content_update);

            pref.apply();
            return true;


        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static String getToken(Application mContext)
    {
        SharedPreferences defaultPreference = PreferenceManager.getDefaultSharedPreferences(mContext);
        return defaultPreference.getString(Config.TOKEN, null);

    }



    public static void clearALlData(Application application)
    {
        SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(application);
        defaultSharedPreferences.edit().clear().apply();
    }


    public static ServerRequest craftProjectUpdateRequest(Application application)
    {
        String token = Helpers.getToken(application);

        String beacon_update = String.valueOf(Helpers.getBeaconLastUpdate(application));
        String content_update =  String.valueOf(Helpers.getContentLastUpdate(application));

        ServerRequest request = new ServerRequest(application, Config.SERVER_URL);
        request.putHeader("Content-Type", "application/json");



        JSONObject jsonObject = new JSONObject();

        try {
            jsonObject.put("token", token);
            jsonObject.put("beacon_update", beacon_update);
            jsonObject.put("content_update", content_update);
        }
        catch (JSONException exp)
        {
            exp.printStackTrace();
        }


        return request;
    }

/*
    public static ServerRequest craftBeaconUpdateRequest(Application application, Beacon beacon)
    {
        String token = Helpers.getUserToken(application);

        ServerRequest request = new ServerRequest(application, Config.BEACON_UPDATE_URL + "/" +
                beacon.getProjectId() + "/" +
                beacon.getFloorPlanId() + "/" +
                beacon.getObjectId());

        request.putHeader("Authorization", token);
        request.putHeader("Content-Type", "application/json");

        request.setBody(beacon.getBeaconData());

        return request;
    }
  */
    public static JSONObject createIBeaconJSON (Application application){
        JSONObject jsonObject = null;

        try {
            StringBuilder buf=new StringBuilder();
            InputStream json = application.getAssets().open("ibeacon.json");
            BufferedReader in =
                    new BufferedReader(new InputStreamReader(json, "UTF-8"));
            String str;

            while ((str = in.readLine()) != null) {
                buf.append(str);
            }
            in.close();

            jsonObject = new JSONObject(buf.toString());
        }
        catch (IOException exp)
        {
            exp.printStackTrace();
        }
        catch (JSONException exp)
        {
            exp.printStackTrace();
        }

        return jsonObject;
    }

    public static JSONObject createEddystoneJson (Application application){
        JSONObject jsonObject = null;

        try {
            StringBuilder buf=new StringBuilder();
            InputStream json = application.getAssets().open("eddystone.json");
            BufferedReader in =
                    new BufferedReader(new InputStreamReader(json, "UTF-8"));
            String str;

            while ((str = in.readLine()) != null) {
                buf.append(str);
            }
            in.close();

            jsonObject = new JSONObject(buf.toString());
        }
        catch (IOException exp)
        {
            exp.printStackTrace();
        }
        catch (JSONException exp)
        {
            exp.printStackTrace();
        }

        return jsonObject;
    }


}
