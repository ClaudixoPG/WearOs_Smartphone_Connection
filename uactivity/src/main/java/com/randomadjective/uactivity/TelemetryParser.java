package com.randomadjective.uactivity;

import android.os.Build;
import android.os.SystemClock;

import org.json.JSONException;
import org.json.JSONObject;

public final class TelemetryParser {

    private TelemetryParser() {}

    public static boolean isTelemetryPayload(String raw) {
        try {
            JSONObject json = new JSONObject(raw);
            return json.has("schema_version") && json.has("raw_message");
        } catch (Exception e) {
            return false;
        }
    }

    public static String enrichOnPhone(String raw) throws JSONException {
        JSONObject json = new JSONObject(raw);
        json.put("receive_ts_phone_native_ns", SystemClock.elapsedRealtimeNanos());
        json.put("smartphone_model", Build.MODEL != null ? Build.MODEL : "UnknownPhone");
        return json.toString();
    }

    public static String markForwardToUnity(String raw) throws JSONException {
        JSONObject json = new JSONObject(raw);
        json.put("forward_ts_phone_native_ns", SystemClock.elapsedRealtimeNanos());
        return json.toString();
    }

    public static String extractRawMessage(String raw) throws JSONException {
        JSONObject json = new JSONObject(raw);
        return json.getString("raw_message");
    }
}