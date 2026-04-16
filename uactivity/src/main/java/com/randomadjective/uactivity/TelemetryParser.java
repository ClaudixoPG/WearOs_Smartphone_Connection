package com.randomadjective.uactivity;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build;
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

    public static boolean shouldAck(String message) {
        try {
            JSONObject json = new JSONObject(message);
            String recordType = json.optString("record_type", "");
            boolean latencySampled = json.optBoolean("latency_sampled", false);

            return "input_event".equals(recordType) && latencySampled;
        } catch (Exception e) {
            return false;
        }
    }

    public static String buildInputAck(String message) {
        try {
            JSONObject inputJson = new JSONObject(message);

            JSONObject ack = new JSONObject();
            ack.put("schema_version", 1);
            ack.put("record_type", "input_ack");
            ack.put("event_type", "ack");
            ack.put("event_id", inputJson.optString("event_id", ""));
            ack.put("session_id", inputJson.optString("session_id", ""));
            ack.put("input_family", inputJson.optString("input_family", ""));
            ack.put("raw_message", inputJson.optString("raw_message", ""));
            ack.put("send_ts_watch_ns", inputJson.optLong("send_ts_watch_ns", 0L));
            ack.put("ack_ts_phone_native_ns", SystemClock.elapsedRealtimeNanos());

            return ack.toString();
        } catch (Exception e) {
            Log.e(TAG, "buildInputAck() failed", e);
            return "";
        }
    }

    public static String enrichOnPhone(Context context, String message) {
        try {
            JSONObject json = new JSONObject(message);

            long receiveTs = SystemClock.elapsedRealtimeNanos();
            json.put("receive_ts_phone_native_ns", receiveTs);

            Context appContext = context.getApplicationContext();

            Intent batteryStatus = appContext.registerReceiver(
                    null,
                    new IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            );

            int level = -1;
            int scale = -1;
            int temp = -1;
            int voltage = -1;
            int status = -1;
            int plugged = -1;
            int health = -1;

            if (batteryStatus != null) {
                level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                temp = batteryStatus.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1);
                voltage = batteryStatus.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1);
                status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
                plugged = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
                health = batteryStatus.getIntExtra(BatteryManager.EXTRA_HEALTH, -1);
            }

            double batteryPctFromIntent = (level >= 0 && scale > 0)
                    ? (level * 100.0 / scale)
                    : -1.0;

            double temperatureC = (temp >= 0)
                    ? (temp / 10.0)
                    : -1.0;

            double batteryPctFinal = batteryPctFromIntent;

            BatteryManager bm = (BatteryManager) appContext.getSystemService(Context.BATTERY_SERVICE);

            int capacityProperty = Integer.MIN_VALUE;
            long currentNowProperty = Long.MIN_VALUE;
            long currentAvgProperty = Long.MIN_VALUE;
            long energyCounterProperty = Long.MIN_VALUE;
            long chargeCounterProperty = Long.MIN_VALUE;

            if (bm != null) {
                try {
                    capacityProperty = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
                } catch (Exception ignored) { }

                try {
                    currentNowProperty = bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);
                } catch (Exception ignored) { }

                try {
                    currentAvgProperty = bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE);
                } catch (Exception ignored) { }

                try {
                    energyCounterProperty = bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_ENERGY_COUNTER);
                } catch (Exception ignored) { }

                try {
                    chargeCounterProperty = bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER);
                } catch (Exception ignored) { }

                if (capacityProperty != Integer.MIN_VALUE && capacityProperty >= 0) {
                    batteryPctFinal = capacityProperty;
                }
            }

            json.put("battery_level_phone", batteryPctFinal);
            json.put("temperature_phone_c", temperatureC);

            json.put("battery_level_phone_from_intent", batteryPctFromIntent);
            json.put("battery_level_phone_from_property", capacityProperty != Integer.MIN_VALUE ? capacityProperty : -1);

            json.put("battery_temperature_phone_raw_tenths_c", temp);
            json.put("battery_voltage_phone_mv", voltage);
            json.put("battery_status_phone", status);
            json.put("battery_plugged_phone", plugged);
            json.put("battery_health_phone", health);

            json.put("battery_current_now_phone_microa", currentNowProperty == Long.MIN_VALUE ? -1 : currentNowProperty);
            json.put("battery_current_average_phone_microa", currentAvgProperty == Long.MIN_VALUE ? -1 : currentAvgProperty);
            json.put("battery_energy_counter_phone_nwh", energyCounterProperty == Long.MIN_VALUE ? -1 : energyCounterProperty);
            json.put("battery_charge_counter_phone_uah", chargeCounterProperty == Long.MIN_VALUE ? -1 : chargeCounterProperty);

            json.put("smartphone_model", Build.MODEL != null ? Build.MODEL : "UnknownPhone");
            json.put("smartphone_manufacturer", Build.MANUFACTURER != null ? Build.MANUFACTURER : "UnknownManufacturer");
            json.put("smartphone_brand", Build.BRAND != null ? Build.BRAND : "UnknownBrand");

            return json.toString();

        } catch (Exception e) {
            Log.e(TAG, "enrichOnPhone() failed", e);
            return message;
        }
    }

    public static String markForwardToUnity(String message) {
        try {
            JSONObject json = new JSONObject(message);
            json.put("forward_ts_phone_native_ns", SystemClock.elapsedRealtimeNanos());
            return json.toString();
        } catch (Exception e) {
            Log.e(TAG, "markForwardToUnity() failed", e);
            return message;
        }
    }
}