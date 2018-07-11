package com.blogspot.kunmii.beaconsdk.utils;

import android.arch.persistence.room.ColumnInfo;

public class Config {

    public static final String SERVER = "http://192.168.0.101:3000/";
//    public static final String SERVER = "http://192.168.0.104:3000/";

    //public static final String SERVER = "http://10.0.2.2:3000/";
    public static final String SERVER_URL = SERVER+"api/sdk/";


    public static final String UPDATE_URL = "update";
    public static final String PUSH_TOKEN_URL = "push";
    public static final String BEACON_UPDATE_URL = "beaconseen";

    public static String TOKEN = "token";

    public static String LAST_PROJECT_UPDATE = "last_updated";

    public static String LAST_BEACON_UPDATE = "last_beacon_update";

    public static String LAST_CONTENT_UPDATE = "last_content_update";

    public static String PROJECT_ID = "project_id";


    public interface NETWORK_JSON_NODE{

        String SUCCESS = "success";
        String BEACONS = "beacons";
        String CONTENTS = "contents";


        String JUST_BEACON = "beacon";


        String OBJECT_ID = "_id";
        String UPDATED = "updated";
        String CREATED = "created";

        String PROJECT_NAME = "name";
        String PROJECT_EMAIL = "email";
        String PROJECT_DESCRIPTION = "description";
        String PROJECT_FLOORPLANS = "floorPlans";

        String FLOORPLAN_NAME = "name";
        String FLOORPLAN_URL = "url";
        String FLOORPLAN_BEACONS = "beacons";

        String BEACON_TYPE = "type";
        String BEACON_REF = "ref";
        String BEACON_TXPOWER = "txPower";
        String BEACON_MAP = "map";
        String BEACON_MAP_X = "x";
        String BEACON__MAP_Y = "y";

        String IBEACON_UUID = "uuid";
        String IBEACON_MAJOR = "major";
        String IBEACON_MINOR = "minor";


        String EDDY_NAMESPACEID = "nameSpaceId";
        String EDDY_INSTANCEID = "instanceId";
        String EDDY_TELEMETRY = "telemetry";

        String CONTENT_TITLE = "title";
        String CONTENT_BODY = "body";
        String CONTENT_PUBLISHED = "published";
        String CONTENT_BEACONS = "beacons";


        String BEACON_LAST_UPDATE_TAG = "beacon_update_tag";
        String CONTENT_LAST_UPDATE_TAG = "content_update_tag";

    }
}
