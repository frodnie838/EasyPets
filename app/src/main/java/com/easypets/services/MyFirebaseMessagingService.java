package com.easypets.services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.easypets.R;
import com.easypets.ui.base.MainActivity;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        String titulo = remoteMessage.getNotification() != null ? remoteMessage.getNotification().getTitle() : "EasyPets";
        String mensaje = remoteMessage.getNotification() != null ? remoteMessage.getNotification().getBody() : "Nueva notificación";

        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        // Rescatamos los datos ocultos que nos manda index.js
        if (remoteMessage.getData().size() > 0) {
            intent.putExtra("tipoNotif", remoteMessage.getData().get("tipoNotif"));
            intent.putExtra("hiloId", remoteMessage.getData().get("hiloId"));
            intent.putExtra("hiloTitulo", remoteMessage.getData().get("hiloTitulo"));
            intent.putExtra("hiloDescripcion", remoteMessage.getData().get("hiloDescripcion"));
            intent.putExtra("hiloAutor", remoteMessage.getData().get("hiloAutor"));
            intent.putExtra("hiloTimestamp", remoteMessage.getData().get("hiloTimestamp"));
        }

        int requestID = (int) System.currentTimeMillis();
        PendingIntent pendingIntent = PendingIntent.getActivity(this, requestID, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String channelId = "easypets_avisos";
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.huella)
                .setContentTitle(titulo)
                .setContentText(mensaje)
                .setAutoCancel(true)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "Avisos EasyPets", NotificationManager.IMPORTANCE_HIGH);
            manager.createNotificationChannel(channel);
        }
        manager.notify(requestID, builder.build());
    }
}