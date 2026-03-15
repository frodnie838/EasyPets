package com.easypets.services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.easypets.R;
import com.easypets.ui.base.MainActivity;

public class NotificacionReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // 1. Recuperamos los datos que pasamos al programar la alarma
        String titulo = intent.getStringExtra("titulo");
        String mensaje = intent.getStringExtra("mensaje");

        if (titulo == null) titulo = "¡Recordatorio EasyPets!";
        if (mensaje == null) mensaje = "Tienes un evento pendiente para tu mascota.";

        // 2. Lanzamos la notificación
        mostrarNotificacionExacata(context, titulo, mensaje);
    }

    private void mostrarNotificacionExacata(Context context, String titulo, String mensaje) {
        String channelId = "easypets_alarmas";

        Intent mainIntent = new Intent(context, MainActivity.class);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, mainIntent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.huella) // Cambia por tu icono
                .setContentTitle(titulo)
                .setContentText(mensaje)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_MAX); // Máxima prioridad para despertar pantalla

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId,
                    "Alarmas de EasyPets",
                    NotificationManager.IMPORTANCE_HIGH); // Importancia ALTA
            notificationManager.createNotificationChannel(channel);
        }

        int idAleatorio = (int) System.currentTimeMillis();
        notificationManager.notify(idAleatorio, builder.build());
    }
}