package com.randomadjective.uactivity;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.MessageClient;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;
import com.unity3d.player.UnityPlayer;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PluginActivity extends UnityPlayerActivity implements MessageClient.OnMessageReceivedListener {

    private static final String PATH = "/mensaje";
    private static final String TAG = "Phone_Main";

    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        ioExecutor.execute(() -> {
            Context context = UnityPlayer.currentActivity.getApplicationContext();

            try {
                List<Node> nodes = Tasks.await(Wearable.getNodeClient(context).getConnectedNodes());

                for (Node n : nodes) {
                    Tasks.await(
                            Wearable.getMessageClient(context)
                                    .sendMessage(n.getId(), PATH, message.getBytes(StandardCharsets.UTF_8))
                    );
                }

            } catch (Exception e) {
                Log.e(TAG, "Error sending message to smartwatch", e);
            }
        });
    }

    private void sendAckToNode(String nodeId, String eventId) {
        ioExecutor.execute(() -> {
            try {
                Context context = UnityPlayer.currentActivity.getApplicationContext();
                String phoneModel = android.os.Build.MODEL != null ? android.os.Build.MODEL : "UnknownPhone";
                String ack = InputNativeCodec.buildAck(eventId, phoneModel);

                Tasks.await(
                        Wearable.getMessageClient(context)
                                .sendMessage(nodeId, PATH, ack.getBytes(StandardCharsets.UTF_8))
                );
            } catch (Exception e) {
                Log.e(TAG, "Error sending ACK", e);
            }
        });
    }

    @Override
    public void onMessageReceived(@NonNull MessageEvent messageEvent) {
        if (!PATH.equals(messageEvent.getPath())) return;

        String message = new String(messageEvent.getData(), StandardCharsets.UTF_8);

        InputNativeCodec.MeasuredEvent measuredEvent = InputNativeCodec.parseMeasuredEvent(message);
        if (measuredEvent != null) {
            UnityPlayer.UnitySendMessage("UnityActivity", "OnMessageReceived", measuredEvent.rawMessage);
            sendAckToNode(messageEvent.getSourceNodeId(), measuredEvent.eventId);
            return;
        }

        InputNativeCodec.RawEvent rawEvent = InputNativeCodec.parseRawEvent(message);
        if (rawEvent != null) {
            UnityPlayer.UnitySendMessage("UnityActivity", "OnMessageReceived", rawEvent.rawMessage);
            return;
        }

        UnityPlayer.UnitySendMessage("UnityActivity", "OnMessageReceived", message);
    }

    public String getPhoneSessionSnapshot() {
        PhoneSessionInfo.Snapshot snapshot = PhoneSessionInfo.read(getApplicationContext());
        return snapshot.phoneModel + "|" + snapshot.batteryLevel + "|" + snapshot.temperatureC;
    }
    public void startMinigameSession(String minigameId) {
        ioExecutor.execute(() -> {
            try {
                Context context = UnityPlayer.currentActivity.getApplicationContext();

                String message = "SESSION_START|" + minigameId;

                List<Node> nodes = Tasks.await(Wearable.getNodeClient(context).getConnectedNodes());

                for (Node n : nodes) {
                    Tasks.await(
                            Wearable.getMessageClient(context)
                                    .sendMessage(n.getId(), PATH, message.getBytes(StandardCharsets.UTF_8))
                    );
                }

            } catch (Exception e) {
                Log.e(TAG, "Error sending SESSION_START", e);
            }
        });
    }

    public void endMinigameSession() {
        ioExecutor.execute(() -> {
            try {
                Context context = UnityPlayer.currentActivity.getApplicationContext();

                String message = "SESSION_END";

                List<Node> nodes = Tasks.await(Wearable.getNodeClient(context).getConnectedNodes());

                for (Node n : nodes) {
                    Tasks.await(
                            Wearable.getMessageClient(context)
                                    .sendMessage(n.getId(), PATH, message.getBytes(StandardCharsets.UTF_8))
                    );
                }

            } catch (Exception e) {
                Log.e(TAG, "Error sending SESSION_END", e);
            }
        });
    }
}