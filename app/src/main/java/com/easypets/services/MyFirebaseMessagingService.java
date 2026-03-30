package com.easypets.services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.os.Build;
import android.util.Log;

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

        // ✨ EL GUARDIÁN: Miramos los Ajustes del usuario antes de hacer nada ✨
        SharedPreferences prefs = getSharedPreferences("AjustesEasyPets", Context.MODE_PRIVATE);
        boolean notificacionesActivadas = prefs.getBoolean("notificaciones", true);

        if (!notificacionesActivadas) {
            // Si el interruptor está apagado, abortamos misión de forma silenciosa.
            Log.d("FIREBASE_AVISOS", "Notificación Push (Foro/Comunidad) bloqueada por los ajustes del usuario.");
            return;
        }

        // --- A partir de aquí, el código original que lanza el aviso en el móvil ---
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
        Log.d("FIREBASE_AVISOS", "Notificación Push mostrada con éxito.");
    }
}