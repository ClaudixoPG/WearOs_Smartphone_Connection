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
            boolean hasSchema = json.has("schema_version");

            Log.i(TAG, "isTelemetryPayload() parsed OK, has schema_version=" + hasSchema);

            if (!hasSchema) {
                Log.w(TAG, "JSON payload received but schema_version is missing. Keys=" + json.names());
            }

            return hasSchema;

        } catch (Exception e) {
            Log.w(TAG, "isTelemetryPayload() -> invalid JSON or parse error. Raw=" + message, e);
            return false;
        }
    }

    public static String enrichOnPhone(Context context, String message) {
        try {
            JSONObject json = new JSONObject(message);

            long receiveTs = SystemClock.elapsedRealtimeNanos();
            json.put("receive_ts_phone_native_ns", receiveTs);

            Context appContext = context.getApplicationContext();

            Log.i(TAG, "----- enrichOnPhone() START -----");
            Log.i(TAG, "Phone manufacturer=" + Build.MANUFACTURER);
            Log.i(TAG, "Phone brand=" + Build.BRAND);
            Log.i(TAG, "Phone model=" + Build.MODEL);
            Log.i(TAG, "Phone device=" + Build.DEVICE);
            Log.i(TAG, "Phone product=" + Build.PRODUCT);
            Log.i(TAG, "receive_ts_phone_native_ns=" + receiveTs);

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
                } catch (Exception e) {
                    Log.w(TAG, "Failed reading BATTERY_PROPERTY_CAPACITY", e);
                }

                try {
                    currentNowProperty = bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);
                } catch (Exception e) {
                    Log.w(TAG, "Failed reading BATTERY_PROPERTY_CURRENT_NOW", e);
                }

                try {
                    currentAvgProperty = bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE);
                } catch (Exception e) {
                    Log.w(TAG, "Failed reading BATTERY_PROPERTY_CURRENT_AVERAGE", e);
                }

                try {
                    energyCounterProperty = bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_ENERGY_COUNTER);
                } catch (Exception e) {
                    Log.w(TAG, "Failed reading BATTERY_PROPERTY_ENERGY_COUNTER", e);
                }

                try {
                    chargeCounterProperty = bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER);
                } catch (Exception e) {
                    Log.w(TAG, "Failed reading BATTERY_PROPERTY_CHARGE_COUNTER", e);
                }

                if (capacityProperty != Integer.MIN_VALUE && capacityProperty >= 0) {
                    batteryPctFinal = capacityProperty;
                }
            } else {
                Log.w(TAG, "BatteryManager is null");
            }

            Log.i(TAG, "batteryStatus null? " + (batteryStatus == null));
            Log.i(TAG, "EXTRA_LEVEL=" + level);
            Log.i(TAG, "EXTRA_SCALE=" + scale);
            Log.i(TAG, "EXTRA_TEMPERATURE(raw_tenths_C)=" + temp);
            Log.i(TAG, "EXTRA_VOLTAGE(mV)=" + voltage);
            Log.i(TAG, "EXTRA_STATUS=" + status);
            Log.i(TAG, "EXTRA_PLUGGED=" + plugged);
            Log.i(TAG, "EXTRA_HEALTH=" + health);

            Log.i(TAG, "batteryPctFromIntent=" + batteryPctFromIntent);
            Log.i(TAG, "temperatureC=" + temperatureC);

            Log.i(TAG, "BATTERY_PROPERTY_CAPACITY=" + capacityProperty);
            Log.i(TAG, "BATTERY_PROPERTY_CURRENT_NOW=" + currentNowProperty);
            Log.i(TAG, "BATTERY_PROPERTY_CURRENT_AVERAGE=" + currentAvgProperty);
            Log.i(TAG, "BATTERY_PROPERTY_ENERGY_COUNTER=" + energyCounterProperty);
            Log.i(TAG, "BATTERY_PROPERTY_CHARGE_COUNTER=" + chargeCounterProperty);

            Log.i(TAG, "batteryPctFinal=" + batteryPctFinal);

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

            Log.i(TAG, "Telemetry JSON after phone enrichment: " + json.toString());
            Log.i(TAG, "----- enrichOnPhone() END -----");

            return json.toString();

        } catch (Exception e) {
            Log.e(TAG, "enrichOnPhone() failed", e);
            return message;
        }
    }

    public static String markForwardToUnity(String message) {
        try {
            JSONObject json = new JSONObject(message);

            long forwardTs = SystemClock.elapsedRealtimeNanos();
            json.put("forward_ts_phone_native_ns", forwardTs);

            Log.i(TAG, "markForwardToUnity() forward_ts_phone_native_ns=" + forwardTs);
            Log.i(TAG, "Telemetry JSON forwarded to Unity: " + json.toString());

            return json.toString();

        } catch (Exception e) {
            Log.e(TAG, "markForwardToUnity() failed", e);
            return message;
        }
    }
}