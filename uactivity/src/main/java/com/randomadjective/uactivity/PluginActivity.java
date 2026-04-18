package com.randomadjective.uactivity;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.MessageClient;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;
import com.unity3d.player.UnityPlayer;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class PluginActivity extends UnityPlayerActivity implements MessageClient.OnMessageReceivedListener {

    private static final String PATH = "/mensaje";
    private static final String TAG = "Phone_Main";

    // ===== DEBUG SWITCHES =====
    private static final boolean USE_MINIMAL_FORWARD_PATH = true;

    private static final boolean ENABLE_TELEMETRY_ENRICH = false;
    private static final boolean ENABLE_ACK = false;
    private static final boolean ENABLE_FORWARD_MARK = false;
    private static final boolean ENABLE_LATENCY_LOGS = true;
    // ==========================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public int Add(int i, int j) {
        return i + j;
    }

    public void ShowToast(String message) {
        Toast.makeText(UnityPlayer.currentActivity, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Wearable.getMessageClient(this).addListener(this);
    }

    @Override
    protected void onPause() {
        Wearable.getMessageClient(this).removeListener(this);
        super.onPause();
    }

    public void sendMessageToSmartwatch(String message) {
        new Thread(() -> {
            Context context = UnityPlayer.currentActivity.getApplicationContext();

            try {
                List<Node> nodes = Tasks.await(Wearable.getNodeClient(context).getConnectedNodes());

                for (Node n : nodes) {
                    Tasks.await(
                            Wearable.getMessageClient(context)
                                    .sendMessage(n.getId(), PATH, message.getBytes(StandardCharsets.UTF_8))
                    );
                }

            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error enviando mensaje", e);
            } catch (Exception e) {
                Log.e(TAG, "Unexpected error in sendMessageToSmartwatch()", e);
            }
        }).start();
    }

    private void sendAckToNode(String nodeId, String message) {
        new Thread(() -> {
            try {
                Context context = UnityPlayer.currentActivity.getApplicationContext();
                Tasks.await(
                        Wearable.getMessageClient(context)
                                .sendMessage(nodeId, PATH, message.getBytes(StandardCharsets.UTF_8))
                );
            } catch (Exception e) {
                Log.e(TAG, "Error sending ACK", e);
            }
        }).start();
    }

    @Override
    public void onMessageReceived(@NonNull MessageEvent messageEvent) {
        long t0 = System.nanoTime();

        try {
            if (!PATH.equals(messageEvent.getPath())) {
                return;
            }

            String message = new String(messageEvent.getData(), StandardCharsets.UTF_8);

            if (ENABLE_LATENCY_LOGS) {
                Log.d(TAG, "[LATENCY] onMessageReceived start");
            }

            if (USE_MINIMAL_FORWARD_PATH) {
                forwardMessageToUnityMinimal(message, t0);
                return;
            }

            forwardMessageToUnityInstrumented(messageEvent, message, t0);

        } catch (Exception e) {
            Log.e(TAG, "Error processing received message", e);

            try {
                String fallback = new String(messageEvent.getData(), StandardCharsets.UTF_8);
                UnityPlayer.UnitySendMessage("UnityActivity", "OnMessageReceived", fallback);
            } catch (Exception inner) {
                Log.e(TAG, "Fallback forwarding also failed", inner);
            }
        }
    }

    /**
     * Ruta mínima:
     * - no parsea JSON
     * - no enrich
     * - no ack
     * - no mark
     * - solo reenvía el mensaje tal cual a Unity
     */
    private void forwardMessageToUnityMinimal(String message, long t0) {
        if (ENABLE_LATENCY_LOGS) {
            long t1 = System.nanoTime();
            Log.d(TAG, "[LATENCY] minimal before UnitySendMessage total_ns=" + (t1 - t0));
        }

        UnityPlayer.UnitySendMessage("UnityActivity", "OnMessageReceived", message);

        if (ENABLE_LATENCY_LOGS) {
            long t2 = System.nanoTime();
            Log.d(TAG, "[LATENCY] minimal full native pipeline total_ns=" + (t2 - t0));
        }
    }

    /**
     * Ruta instrumentada original:
     * mantiene enrich/ack/mark según switches.
     */
    private void forwardMessageToUnityInstrumented(@NonNull MessageEvent messageEvent, String message, long t0) {
        boolean isTelemetry = TelemetryParser.isTelemetryPayload(message);

        if (!isTelemetry) {
            if (ENABLE_LATENCY_LOGS) {
                long tEnd = System.nanoTime();
                Log.d(TAG, "[LATENCY] non-telemetry direct forward total_ns=" + (tEnd - t0));
            }

            UnityPlayer.UnitySendMessage("UnityActivity", "OnMessageReceived", message);
            return;
        }

        String processed = message;

        if (ENABLE_TELEMETRY_ENRICH) {
            processed = TelemetryParser.enrichOnPhone(this, processed);

            if (ENABLE_LATENCY_LOGS) {
                long t1 = System.nanoTime();
                Log.d(TAG, "[LATENCY] after enrich total_ns=" + (t1 - t0));
            }
        }

        if (ENABLE_ACK && TelemetryParser.shouldAck(processed)) {
            String ackMessage = TelemetryParser.buildInputAck(processed);
            if (!ackMessage.isEmpty()) {
                sendAckToNode(messageEvent.getSourceNodeId(), ackMessage);
            }

            if (ENABLE_LATENCY_LOGS) {
                long t2 = System.nanoTime();
                Log.d(TAG, "[LATENCY] after ack total_ns=" + (t2 - t0));
            }
        }

        if (ENABLE_FORWARD_MARK) {
            processed = TelemetryParser.markForwardToUnity(processed);

            if (ENABLE_LATENCY_LOGS) {
                long t3 = System.nanoTime();
                Log.d(TAG, "[LATENCY] after forward mark total_ns=" + (t3 - t0));
            }
        }

        if (ENABLE_LATENCY_LOGS) {
            long t4 = System.nanoTime();
            Log.d(TAG, "[LATENCY] before UnitySendMessage total_ns=" + (t4 - t0));
        }

        UnityPlayer.UnitySendMessage("UnityActivity", "OnMessageReceived", processed);

        if (ENABLE_LATENCY_LOGS) {
            long t5 = System.nanoTime();
            Log.d(TAG, "[LATENCY] full native pipeline total_ns=" + (t5 - t0));
        }
    }
}