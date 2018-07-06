package com.blogspot.kunmii.beaconsdk.network;

import okhttp3.Response;

public class ServerResponse {

    String jsonBody = null;
    int code;
    boolean exception;
    String reason;

    public ServerResponse(Response response)
    {
        try {
            jsonBody = response.body().string();
            code = response.code();
            exception = !response.isSuccessful();
            reason = response.message();

        }
        catch (Exception exp)
        {
            reason = "";
            code = -1;
            exception = true;
            exp.printStackTrace();
        }

    }

    public int getCode() {
        return code;
    }

    public String getJsonBody() {
        return jsonBody;
    }

    public boolean hasException() {
        return exception;
    }

    public String getReason() {
        return reason;
    }
}
