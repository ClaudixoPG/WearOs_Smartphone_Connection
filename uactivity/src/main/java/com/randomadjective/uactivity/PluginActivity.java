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

    private String path = "/mensaje";
    private String TAG = "Phone_Main";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Toast.makeText(UnityPlayer.currentActivity, "Mensaje de Entrada", Toast.LENGTH_SHORT).show();
    }

    public int Add(int i, int j) {
        return i + j;
    }

    public void ShowToast(String message) {
        Toast.makeText(UnityPlayer.currentActivity, message, Toast.LENGTH_SHORT).show();
        //create Printer object
    }

    @Override
    protected void onResume() {
        super.onResume();
        Wearable.getMessageClient(this).addListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Wearable.getMessageClient(this).removeListener(this);
    }

    /*public void sendMessageToSmartwatch(String message) {
        new Thread(() -> {
            var nodeListTask = Wearable.getNodeClient(this).getConnectedNodes();
            List<Node> nodes = null;
            try {
                nodes = Tasks.await(nodeListTask);
            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            }
            for(var n : nodes)
            {
                Wearable.getMessageClient(this).sendMessage(n.getId(),path,message.getBytes(StandardCharsets.UTF_8));
                Log.i(TAG, "Mensaje enviado a: " + n.getDisplayName());
            }
        }).start();
    }*/
    public void sendMessageToSmartwatch(String message) {
        new Thread(() -> {
            Context context = UnityPlayer.currentActivity.getApplicationContext();
            List<Node> nodes;
            try {
                nodes = Tasks.await(Wearable.getNodeClient(context).getConnectedNodes());
                for (Node n : nodes) {
                    Wearable.getMessageClient(context)
                            .sendMessage(n.getId(), path, message.getBytes(StandardCharsets.UTF_8));
                    Log.i(TAG, "Mensaje enviado a: " + n.getDisplayName());
                }
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error enviando mensaje: ", e);
            }
        }).start();
    }


    @Override
    public void onMessageReceived(@NonNull MessageEvent messageEvent) {
        //placeholder text for message event
        if (messageEvent.getPath().equals(path)) {
            String message = new String(messageEvent.getData());
            Log.i(TAG, "Mensaje recibido: " + message);
            // Aquí puedes manejar el mensaje recibido
            UnityPlayer.UnitySendMessage("UnityActivity", "OnMessageReceived", message);
        }
    }
}
