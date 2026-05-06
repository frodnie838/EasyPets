package com.easypets.services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.easypets.R;
import com.easypets.ui.base.MainActivity;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

/**
 * Servicio encargado de interceptar y gestionar las notificaciones Push
 * entrantes a través de Firebase Cloud Messaging (FCM).
 */
public class MyFirebaseMessagingService extends FirebaseMessagingService {

    /**
     * Método invocado automáticamente cuando el dispositivo recibe un mensaje Push desde el servidor.
     *
     * @param remoteMessage Objeto que contiene la información y carga útil (payload) del mensaje.
     */
    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        // Verificación de las preferencias del usuario para notificaciones
        SharedPreferences prefs = getSharedPreferences("AjustesEasyPets", Context.MODE_PRIVATE);
        boolean notificacionesActivadas = prefs.getBoolean("notificaciones", true);

        if (!notificacionesActivadas) {
            // Si el usuario ha deshabilitado las notificaciones en la configuración, se aborta la ejecución
            return;
        }

        // Extracción de título y cuerpo de la notificación
        String titulo = remoteMessage.getNotification() != null ? remoteMessage.getNotification().getTitle() : "EasyPets";
        String mensaje = remoteMessage.getNotification() != null ? remoteMessage.getNotification().getBody() : "";

        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        // Extracción de la carga útil (Data Payload) proveniente del Backend
        if (remoteMessage.getData().size() > 0) {
            intent.putExtra("tipoNotif", remoteMessage.getData().get("tipoNotif"));
            intent.putExtra("hiloId", remoteMessage.getData().get("hiloId"));
            intent.putExtra("hiloTitulo", remoteMessage.getData().get("hiloTitulo"));
            intent.putExtra("hiloDescripcion", remoteMessage.getData().get("hiloDescripcion"));
            intent.putExtra("hiloAutor", remoteMessage.getData().get("hiloAutor"));
            intent.putExtra("hiloTimestamp", remoteMessage.getData().get("hiloTimestamp"));
        }

        int requestID = (int) System.currentTimeMillis();
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                requestID,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

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

        // Creación del canal de notificaciones requerido para Android 8.0 (API 26) o superior
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Avisos EasyPets",
                    NotificationManager.IMPORTANCE_HIGH
            );
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }

        if (manager != null) {
            manager.notify(requestID, builder.build());
        }
    }
}