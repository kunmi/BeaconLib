package com.blogspot.kunmii.beaconsdk.utils;

import android.app.Application;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;


import com.blogspot.kunmii.beaconsdk.data.Beacon;
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

    public static void storeFcmToken(String token, Application mContext){
        SharedPreferences defaultPref = PreferenceManager.getDefaultSharedPreferences(mContext);
        SharedPreferences.Editor editor = defaultPref.edit();

        editor.putString("fcm", token);
        editor.apply();
    }

    public static String getTokenFcm(Application mContext){
        SharedPreferences defaultPref = PreferenceManager.getDefaultSharedPreferences(mContext);
        return defaultPref.getString("fcm", null);
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
        String token = String.valueOf(Helpers.getToken(application));

        ServerRequest request = new ServerRequest(application, Config.UPDATE_URL);
        request.putHeader("Content-Type", "application/json");



        JSONObject jsonObject = new JSONObject();

        try {
            jsonObject.put("token", token==null?"":token);
            jsonObject.put("beacon_update", Helpers.getBeaconLastUpdate(application));
            jsonObject.put("content_update", Helpers.getContentLastUpdate(application));
        }
        catch (JSONException exp)
        {
            exp.printStackTrace();
        }

        request.setBody(jsonObject.toString());

        return request;
    }


    public static ServerRequest craftBeaconUpdateRequest(Application application, Beacon beacon)
    {
        String token = Helpers.getToken(application);

        ServerRequest request = new ServerRequest(application, Config.BEACON_UPDATE_URL);

        request.putHeader("Authorization", token);
        request.putHeader("Content-Type", "application/json");

        JSONObject obj = new JSONObject();
        try {
            obj.put("token", Helpers.getToken(application));
            obj.put("beacon_id", beacon.getObjectId());
            obj.put("telemetry", beacon.telemetry);
        }
        catch (JSONException exp){
            exp.printStackTrace();
        }

        request.setBody(obj.toString());

        return request;
    }

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

    public static ServerRequest sendFCMTokenToServer(Application application, String fcmToken)
    {
        String token = getToken(application);

        ServerRequest request = new ServerRequest(application, Config.PUSH_TOKEN_URL);
        request.putHeader("Content-Type", "application/json");

        JSONObject jsonObject = new JSONObject();

        try {
            jsonObject.put("token", token==null?"":token);
            jsonObject.put("fcm", fcmToken);
        }
        catch (JSONException exp)
        {
            exp.printStackTrace();
        }

        request.setBody(jsonObject.toString());

        return request;

    }


}
