package com.easypets.services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.easypets.R;
import com.easypets.ui.base.MainActivity;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class NotificacionReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("ALARMAS", "¡BroadcastReceiver ejecutado!");

        // 1. Recuperamos los datos que programamos
        String titulo = intent.getStringExtra("titulo");
        String mensaje = intent.getStringExtra("mensaje");
        String uid = intent.getStringExtra("uid"); // Recuperamos el ID del usuario

        if (titulo == null) titulo = "¡Recordatorio EasyPets!";
        if (mensaje == null) mensaje = "Tienes un evento pendiente para tu mascota.";

        // ✨ 2. EL GUARDIÁN: Leemos los Ajustes del usuario ✨
        SharedPreferences prefs = context.getSharedPreferences("AjustesEasyPets", Context.MODE_PRIVATE);
        boolean notificacionesActivadas = prefs.getBoolean("notificaciones", true);

        // Si están activadas, despertamos la pantalla y hacemos ruido
        if (notificacionesActivadas) {
            despertarPantalla(context);
            mostrarNotificacionExacata(context, titulo, mensaje);
            Log.d("ALARMAS", "Notificación Push mostrada al usuario.");
        } else {
            // Si están desactivadas, no hacemos nada en el móvil (modo silencio)
            Log.d("ALARMAS", "Notificación Push silenciada por los ajustes.");
        }

        // ✨ 3. LA CAMPANITA (Se ejecuta SIEMPRE, esté silenciado o no) ✨
        // Guardamos una copia en el buzón de Firebase para el desplegable de la app
        if (uid != null && !uid.isEmpty()) {
            DatabaseReference buzonRef = FirebaseDatabase.getInstance().getReference()
                    .child("notificaciones")
                    .child(uid);

            String idNotificacion = buzonRef.push().getKey();

            // Preparamos la carta igual que en el foro
            java.util.HashMap<String, Object> carta = new java.util.HashMap<>();
            carta.put("titulo", "📅 " + titulo); // Le pongo un emoji de calendario para diferenciarlo del foro
            carta.put("mensaje", mensaje);
            carta.put("mostrada", false); // Esto encenderá el puntito rojo de la campana
            carta.put("tipo", "evento_calendario");

            if (idNotificacion != null) {
                buzonRef.child(idNotificacion).setValue(carta);
                Log.d("ALARMAS", "Notificación guardada en el buzón de Firebase de forma silenciosa.");
            }
        }
    }

    private void despertarPantalla(Context context) {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (pm != null && !pm.isInteractive()) {
            PowerManager.WakeLock wakeLock = pm.newWakeLock(
                    PowerManager.FULL_WAKE_LOCK |
                            PowerManager.ACQUIRE_CAUSES_WAKEUP |
                            PowerManager.ON_AFTER_RELEASE,
                    "EasyPets:AlarmaDespertador"
            );
            wakeLock.acquire(4000);
        }
    }

    private void mostrarNotificacionExacata(Context context, String titulo, String mensaje) {
        String channelId = "easypets_alarmas";

        Intent mainIntent = new Intent(context, MainActivity.class);
        mainIntent.putExtra("abrirFragment", "calendario");
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, (int)System.currentTimeMillis(), mainIntent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        Uri sonidoFuerte = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.logo_sin_fondo)
                .setContentTitle(titulo)
                .setContentText(mensaje)
                .setAutoCancel(true)
                .setSound(sonidoFuerte)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM);

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId,
                    "Alarmas de EasyPets",
                    NotificationManager.IMPORTANCE_HIGH);
            channel.enableVibration(true);
            notificationManager.createNotificationChannel(channel);
        }

        int idAleatorio = (int) System.currentTimeMillis();
        notificationManager.notify(idAleatorio, builder.build());
    }
}