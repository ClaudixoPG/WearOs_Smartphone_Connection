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
        try {
            if (!PATH.equals(messageEvent.getPath())) {
                return;
            }

            String message = new String(messageEvent.getData(), StandardCharsets.UTF_8);

            boolean isTelemetry = TelemetryParser.isTelemetryPayload(message);

            if (isTelemetry) {
                String enriched = TelemetryParser.enrichOnPhone(this, message);

                if (TelemetryParser.shouldAck(enriched)) {
                    String ackMessage = TelemetryParser.buildInputAck(enriched);
                    if (!ackMessage.isEmpty()) {
                        sendAckToNode(messageEvent.getSourceNodeId(), ackMessage);
                    }
                }

                String forwarded = TelemetryParser.markForwardToUnity(enriched);
                UnityPlayer.UnitySendMessage("UnityActivity", "OnMessageReceived", forwarded);
            } else {
                UnityPlayer.UnitySendMessage("UnityActivity", "OnMessageReceived", message);
            }

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
}