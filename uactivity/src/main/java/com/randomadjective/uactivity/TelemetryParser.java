package com.randomadjective.uactivity;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.SystemClock;
import android.util.Log;

import org.json.JSONObject;

public class TelemetryParser {

    private static final String TAG = "TelemetryParser";

    public static boolean isTelemetryPayload(String message) {
        try {
            JSONObject json = new JSONObject(message);
            return json.has("schema_version");
        } catch (Exception e) {
            return false;
        }
    }

    public static String enrichOnPhone(Context context, String message) {
        try {
            JSONObject json = new JSONObject(message);

            long receiveTs = SystemClock.elapsedRealtimeNanos();
            json.put("receive_ts_phone_native_ns", receiveTs);

            Context appContext = context.getApplicationContext();

            // Fallback 1: sticky battery intent
            Intent batteryStatus = appContext.registerReceiver(
                    null,
                    new IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            );

            int level = batteryStatus != null
                    ? batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    : -1;

            int scale = batteryStatus != null
                    ? batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                    : -1;

            int temp = batteryStatus != null
                    ? batteryStatus.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)
                    : -1;

            double batteryPctFromIntent = (level >= 0 && scale > 0)
                    ? (level * 100.0 / scale)
                    : -1.0;

            double temperatureC = temp >= 0
                    ? temp / 10.0
                    : -1.0;

            // Fallback 2: BatteryManager property
            double batteryPct = batteryPctFromIntent;

            BatteryManager bm = (BatteryManager) appContext.getSystemService(Context.BATTERY_SERVICE);
            if (bm != null) {
                int capacity = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
                if (capacity != Integer.MIN_VALUE && capacity >= 0) {
                    batteryPct = capacity;
                }

                Log.i(TAG, "BATTERY_PROPERTY_CAPACITY=" + capacity);
            } else {
                Log.w(TAG, "BatteryManager is null");
            }

            Log.i(TAG, "batteryStatus null? " + (batteryStatus == null));
            Log.i(TAG, "EXTRA_LEVEL=" + level);
            Log.i(TAG, "EXTRA_SCALE=" + scale);
            Log.i(TAG, "EXTRA_TEMPERATURE=" + temp);
            Log.i(TAG, "batteryPctFromIntent=" + batteryPctFromIntent);
            Log.i(TAG, "batteryPctFinal=" + batteryPct);
            Log.i(TAG, "temperatureC=" + temperatureC);

            json.put("battery_level_phone", batteryPct);
            json.put("temperature_phone_c", temperatureC);

            json.put(
                    "smartphone_model",
                    android.os.Build.MODEL != null ? android.os.Build.MODEL : "UnknownPhone"
            );

            return json.toString();

        } catch (Exception e) {
            Log.e(TAG, "enrichOnPhone failed", e);
            return message;
        }
    }

    public static String markForwardToUnity(String message) {
        try {
            JSONObject json = new JSONObject(message);

            json.put(
                    "forward_ts_phone_native_ns",
                    SystemClock.elapsedRealtimeNanos()
            );

            return json.toString();

        } catch (Exception e) {
            Log.e(TAG, "markForwardToUnity failed", e);
            return message;
        }
    }
}