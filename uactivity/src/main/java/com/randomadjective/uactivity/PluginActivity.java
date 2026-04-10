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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate()");
        Toast.makeText(UnityPlayer.currentActivity, "Mensaje de Entrada", Toast.LENGTH_SHORT).show();
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
        Log.i(TAG, "onResume() -> addListener");
        Wearable.getMessageClient(this).addListener(this);
    }

    @Override
    protected void onPause() {
        Log.i(TAG, "onPause() -> removeListener");
        Wearable.getMessageClient(this).removeListener(this);
        super.onPause();
    }

    public void sendMessageToSmartwatch(String message) {
        Log.i(TAG, "sendMessageToSmartwatch() payload=" + message);

        new Thread(() -> {
            Context context = UnityPlayer.currentActivity.getApplicationContext();
            List<Node> nodes;

            try {
                nodes = Tasks.await(Wearable.getNodeClient(context).getConnectedNodes());
                Log.i(TAG, "Connected nodes count=" + nodes.size());

                for (Node n : nodes) {
                    Log.i(TAG, "Sending message to node: id=" + n.getId() + ", name=" + n.getDisplayName());

                    Integer result = Tasks.await(
                            Wearable.getMessageClient(context)
                                    .sendMessage(n.getId(), PATH, message.getBytes(StandardCharsets.UTF_8))
                    );

                    Log.i(TAG, "Message sent OK. requestId=" + result + ", node=" + n.getDisplayName());
                }

                if (nodes.isEmpty()) {
                    Log.w(TAG, "No connected smartwatch nodes found.");
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
                Integer result = Tasks.await(
                        Wearable.getMessageClient(context)
                                .sendMessage(nodeId, PATH, message.getBytes(StandardCharsets.UTF_8))
                );

                Log.i(TAG, "ACK sent OK. requestId=" + result + ", nodeId=" + nodeId);
            } catch (Exception e) {
                Log.e(TAG, "Error sending ACK", e);
            }
        }).start();
    }

    @Override
    public void onMessageReceived(@NonNull MessageEvent messageEvent) {
        try {
            Log.i(TAG, "onMessageReceived() path=" + messageEvent.getPath());
            Log.i(TAG, "onMessageReceived() sourceNodeId=" + messageEvent.getSourceNodeId());

            if (!PATH.equals(messageEvent.getPath())) {
                Log.w(TAG, "Ignoring message with unexpected path: " + messageEvent.getPath());
                return;
            }

            String message = new String(messageEvent.getData(), StandardCharsets.UTF_8);
            Log.i(TAG, "Raw message received: " + message);

            boolean isTelemetry = TelemetryParser.isTelemetryPayload(message);
            Log.i(TAG, "isTelemetryPayload=" + isTelemetry);

            if (isTelemetry) {
                String enriched = TelemetryParser.enrichOnPhone(this, message);
                Log.i(TAG, "Enriched message: " + enriched);

                if (TelemetryParser.shouldAck(enriched)) {
                    String ackMessage = TelemetryParser.buildInputAck(enriched);
                    sendAckToNode(messageEvent.getSourceNodeId(), ackMessage);
                }

                String forwarded = TelemetryParser.markForwardToUnity(enriched);
                Log.i(TAG, "Forwarded message to Unity: " + forwarded);

                UnityPlayer.UnitySendMessage("UnityActivity", "OnMessageReceived", forwarded);
                Log.i(TAG, "UnitySendMessage() called with enriched telemetry payload");
            } else {
                Log.w(TAG, "Payload not recognized as telemetry. Forwarding raw message to Unity.");
                UnityPlayer.UnitySendMessage("UnityActivity", "OnMessageReceived", message);
                Log.i(TAG, "UnitySendMessage() called with raw payload");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error processing received message", e);

            try {
                String fallback = new String(messageEvent.getData(), StandardCharsets.UTF_8);
                UnityPlayer.UnitySendMessage("UnityActivity", "OnMessageReceived", fallback);
                Log.w(TAG, "Fallback raw payload forwarded to Unity after exception");
            } catch (Exception inner) {
                Log.e(TAG, "Fallback forwarding also failed", inner);
            }
        }
    }
}