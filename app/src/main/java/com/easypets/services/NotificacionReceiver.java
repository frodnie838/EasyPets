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

import androidx.core.app.NotificationCompat;

import com.easypets.R;
import com.easypets.ui.base.MainActivity;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

/**
 * Receptor de difusiones (BroadcastReceiver) encargado de gestionar las alarmas y recordatorios locales.
 * Evalúa las preferencias de privacidad del usuario antes de emitir notificaciones push
 * y sincroniza el historial de notificaciones con Firebase Realtime Database.
 */
public class NotificacionReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "easypets_alarmas";
    private static final String PREFS_NAME = "AjustesEasyPets";
    private static final String PREF_NOTIFICACIONES = "notificaciones";

    @Override
    public void onReceive(Context context, Intent intent) {
        String titulo = intent.getStringExtra("titulo");
        String mensaje = intent.getStringExtra("mensaje");
        String uid = intent.getStringExtra("uid");

        if (titulo == null) titulo = "¡Recordatorio EasyPets!";
        if (mensaje == null) mensaje = "Tienes un evento pendiente para tu mascota.";

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean notificacionesActivadas = prefs.getBoolean(PREF_NOTIFICACIONES, true);

        if (notificacionesActivadas) {
            despertarPantalla(context);
            mostrarNotificacionExacta(context, titulo, mensaje);
        }

        sincronizarNotificacionFirebase(uid, titulo, mensaje);
    }

    /**
     * Enciende temporalmente la pantalla del dispositivo utilizando WakeLock para
     * asegurar que el usuario visualice alertas críticas programadas.
     *
     * @param context Contexto de la aplicación.
     */
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

    /**
     * Construye y despliega la notificación local en el sistema operativo Android.
     *
     * @param context Contexto de la aplicación.
     * @param titulo  Título de la notificación.
     * @param mensaje Cuerpo del mensaje de la notificación.
     */
    private void mostrarNotificacionExacta(Context context, String titulo, String mensaje) {
        Intent mainIntent = new Intent(context, MainActivity.class);
        mainIntent.putExtra("abrirFragment", "calendario");
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                (int) System.currentTimeMillis(),
                mainIntent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE
        );

        Uri sonidoFuerte = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.logo_sin_fondo)
                .setContentTitle(titulo)
                .setContentText(mensaje)
                .setAutoCancel(true)
                .setSound(sonidoFuerte)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM);

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (notificationManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID,
                        "Alarmas de EasyPets",
                        NotificationManager.IMPORTANCE_HIGH
                );
                channel.enableVibration(true);
                notificationManager.createNotificationChannel(channel);
            }

            int idAleatorio = (int) System.currentTimeMillis();
            notificationManager.notify(idAleatorio, builder.build());
        }
    }

    /**
     * Registra la notificación en la base de datos en tiempo real para que esté disponible
     * en el historial (campana) dentro de la aplicación de manera persistente.
     *
     * @param uid     Identificador del usuario.
     * @param titulo  Título del evento.
     * @param mensaje Detalle del evento.
     */
    private void sincronizarNotificacionFirebase(String uid, String titulo, String mensaje) {
        if (uid != null && !uid.isEmpty()) {
            DatabaseReference buzonRef = FirebaseDatabase.getInstance().getReference()
                    .child("notificaciones")
                    .child(uid);

            String idNotificacion = buzonRef.push().getKey();

            if (idNotificacion != null) {
                Map<String, Object> notificacionData = new HashMap<>();
                notificacionData.put("titulo", "📅 " + titulo);
                notificacionData.put("mensaje", mensaje);
                notificacionData.put("mostrada", false);
                notificacionData.put("tipo", "evento_calendario");

                buzonRef.child(idNotificacion).setValue(notificacionData);
            }
        }
    }
}