package com.easypets.ui.base;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.View; // Importante para View.VISIBLE y View.GONE
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.easypets.ui.calendario.CalendarioFragment;
import com.easypets.ui.comunidad.EducacionFragment;
import com.easypets.ui.home.HomeFragment;
import com.easypets.ui.mascotas.MascotasFragment;
import com.easypets.ui.perfil.PerfilFragment;
import com.easypets.R;
import com.easypets.ui.servicios.ServiciosFragment;
import com.easypets.ui.servicios.adiestradores.AdiestradoresFragment;
import com.easypets.ui.servicios.farmacias.FarmaciasFragment;
import com.easypets.ui.servicios.guarderias.GuarderiasFragment;
import com.easypets.ui.servicios.parques.ParquesFragment;
import com.easypets.ui.servicios.paseadores.PaseadoresFragment;
import com.easypets.ui.servicios.peluquerias.PeluqueriasFragment;
import com.easypets.ui.servicios.protectoras.ProtectorasFragment;
import com.easypets.ui.servicios.tiendas.TiendasFragment;
import com.easypets.ui.servicios.veterinarios.VeterinariosFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;
    private long tiempoUltimoClicAtras = 0;

    private CardView cardTopProfile;
    private ImageView ivTopProfile, ivTopLogo;
    private android.widget.ImageButton btnTopBack;
    private android.widget.LinearLayout layoutTopSearch;
    private android.widget.EditText etTopSearch;
    private java.util.List<com.easypets.models.Notificacion> listaNotificaciones = new java.util.ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNav = findViewById(R.id.bottom_navigation);

        cardTopProfile = findViewById(R.id.cardTopProfile);
        ivTopProfile = findViewById(R.id.ivTopProfile);
        btnTopBack = findViewById(R.id.btnTopBack);
        layoutTopSearch = findViewById(R.id.layoutTopSearch);
        etTopSearch = findViewById(R.id.etTopSearch);
        ivTopLogo = findViewById(R.id.ivTopLogo);

        activarBuzonDeNotificaciones();
        guardarTokenFCM();

        btnTopBack.setOnClickListener(v -> {
            getOnBackPressedDispatcher().onBackPressed();
        });

        // Hacer que la foto de perfil funcione como botón
        cardTopProfile.setOnClickListener(v -> {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.frame_container, new PerfilFragment())
                    .addToBackStack(null)
                    .commit();
        });

        cargarFotoDePerfilEnCabecera();

        // Configurar el listener de la barra inferior
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;

            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                selectedFragment = new HomeFragment();
            } else if (itemId == R.id.nav_comunidad) {
                selectedFragment = new EducacionFragment();
            } else if (itemId == R.id.nav_pets) {
                selectedFragment = new MascotasFragment();
            } else if (itemId == R.id.nav_calendar) {
                selectedFragment = new CalendarioFragment();
            } else if (itemId == R.id.nav_service) {
                selectedFragment = new ServiciosFragment();
            }

            if (selectedFragment != null) {
                // Limpiar la pila de retroceso para que no se acumulen pantallas al navegar por el menú
                getSupportFragmentManager().popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);

                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.frame_container, selectedFragment)
                        .commit();
            }
            return true;
        });

        // --- LÓGICA DEL BUSCADOR GLOBAL ---
        android.widget.EditText etTopSearch = findViewById(R.id.etTopSearch);
        com.easypets.ui.servicios.BusquedaViewModel busquedaViewModel =
                new androidx.lifecycle.ViewModelProvider(this).get(com.easypets.ui.servicios.BusquedaViewModel.class);

        etTopSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                String ciudad = etTopSearch.getText().toString().trim();
                if (!ciudad.isEmpty()) {
                    busquedaViewModel.buscarCiudad(ciudad);

                    android.view.inputmethod.InputMethodManager imm =
                            (android.view.inputmethod.InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(etTopSearch.getWindowToken(), 0);
                    }
                }
                return true;
            }
            return false;
        });

        // Cargar el fragmento por defecto al iniciar (Home)
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.frame_container, new HomeFragment())
                    .commit();
        }

        getSupportFragmentManager().registerFragmentLifecycleCallbacks(new androidx.fragment.app.FragmentManager.FragmentLifecycleCallbacks() {
            @Override
            public void onFragmentResumed(@NonNull androidx.fragment.app.FragmentManager fm, @NonNull Fragment f) {
                super.onFragmentResumed(fm, f);

                if (f instanceof PerfilFragment) {
                    cardTopProfile.setVisibility(View.INVISIBLE);
                    btnTopBack.setVisibility(View.VISIBLE);
                    ivTopLogo.setVisibility(View.VISIBLE);
                    layoutTopSearch.setVisibility(View.GONE);

                    // Desmarcar todas las pestañas al entrar al perfil
                    int size = bottomNav.getMenu().size();
                    for (int i = 0; i < size; i++) {
                        bottomNav.getMenu().getItem(i).setChecked(false);
                    }

                }else if (f instanceof HomeFragment) {
                        cardTopProfile.setVisibility(View.VISIBLE);
                        btnTopBack.setVisibility(View.GONE);
                        ivTopLogo.setVisibility(View.VISIBLE);
                        layoutTopSearch.setVisibility(View.GONE);

                        bottomNav.getMenu().findItem(R.id.nav_home).setChecked(true);

                } else if (f instanceof EducacionFragment) {
                    cardTopProfile.setVisibility(View.VISIBLE);
                    btnTopBack.setVisibility(View.GONE);
                    ivTopLogo.setVisibility(View.GONE);
                    layoutTopSearch.setVisibility(View.VISIBLE);

                    bottomNav.getMenu().findItem(R.id.nav_comunidad).setChecked(true);

                } else if (f instanceof CalendarioFragment) {
                    cardTopProfile.setVisibility(View.VISIBLE);
                    btnTopBack.setVisibility(View.GONE);
                    ivTopLogo.setVisibility(View.VISIBLE);
                    layoutTopSearch.setVisibility(View.GONE);

                    bottomNav.getMenu().findItem(R.id.nav_calendar).setChecked(true);

                } else if (f instanceof MascotasFragment ) {
                    cardTopProfile.setVisibility(View.VISIBLE);
                    btnTopBack.setVisibility(View.GONE);
                    ivTopLogo.setVisibility(View.VISIBLE);
                    layoutTopSearch.setVisibility(View.GONE);

                    bottomNav.getMenu().findItem(R.id.nav_pets).setChecked(true);

                } else if (f instanceof ServiciosFragment) {
                    cardTopProfile.setVisibility(View.VISIBLE);
                    btnTopBack.setVisibility(View.GONE);
                    ivTopLogo.setVisibility(View.VISIBLE);
                    layoutTopSearch.setVisibility(View.GONE);

                    bottomNav.getMenu().findItem(R.id.nav_service).setChecked(true);

                } else if (f instanceof VeterinariosFragment || f instanceof ParquesFragment || f instanceof ProtectorasFragment || f instanceof PaseadoresFragment || f instanceof FarmaciasFragment || f instanceof AdiestradoresFragment || f instanceof TiendasFragment || f instanceof GuarderiasFragment || f instanceof PeluqueriasFragment) {
                    cardTopProfile.setVisibility(View.GONE);
                    btnTopBack.setVisibility(View.VISIBLE);
                    ivTopLogo.setVisibility(View.GONE);
                    layoutTopSearch.setVisibility(View.VISIBLE);

                    bottomNav.getMenu().findItem(R.id.nav_service).setChecked(true);

                }
            }
        }, true);

        // GESTOR DEL BOTÓN ATRÁS
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Si hay un fragmento en la pila (como el Perfil), simplemente lo sacamos
                if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                    getSupportFragmentManager().popBackStack();
                    // Al sacarlo, el FragmentLifecycleCallbacks se encargará de repintar el menú.
                } else {
                    // Si estamos en la base (no hay historial)
                    Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.frame_container);

                    if (!(currentFragment instanceof HomeFragment)) {
                        // Si no estamos en Home, volvemos a Home simulando un clic
                        bottomNav.setSelectedItemId(R.id.nav_home);
                    } else {
                        // Si ya estamos en Home, salimos de la app
                        if (System.currentTimeMillis() - tiempoUltimoClicAtras < 2000) {
                            finish();
                        } else {
                            Toast.makeText(MainActivity.this, "Vuelve a presionar atrás para salir", Toast.LENGTH_SHORT).show();
                            tiempoUltimoClicAtras = System.currentTimeMillis();
                        }
                    }
                }
            }
        });
        // Pedir permiso de notificaciones para Android 13+ (API 33+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) !=
                    android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
    }

    private void cargarFotoDePerfilEnCabecera() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference().child("usuarios").child(currentUser.getUid());

            userRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        String fotoBase64Actual = snapshot.child("fotoPerfil").getValue(String.class);

                        if (fotoBase64Actual != null && !fotoBase64Actual.isEmpty()) {
                            try {
                                byte[] decodedString = Base64.decode(fotoBase64Actual, Base64.DEFAULT);
                                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                                ivTopProfile.setImageBitmap(bitmap);
                                ivTopProfile.setImageTintList(null);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });
        }
    }
    // 1. Escuchar el buzón constantemente
    private void activarBuzonDeNotificaciones() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        DatabaseReference buzonRef = FirebaseDatabase.getInstance().getReference()
                .child("notificaciones").child(currentUser.getUid());

        // Usamos ValueEventListener para leer toda la lista de golpe
        buzonRef.addValueEventListener(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                listaNotificaciones.clear();

                for (DataSnapshot ds : snapshot.getChildren()) {
                    String id = ds.getKey();
                    String titulo = ds.child("titulo").getValue(String.class);
                    String mensaje = ds.child("mensaje").getValue(String.class);
                    Boolean mostrada = ds.child("mostrada").getValue(Boolean.class);

                    // Leemos los datos ocultos (usamos un fallback por si hay notificaciones antiguas sin estos datos)
                    String tipo = ds.child("tipo").getValue(String.class);
                    String hiloId = ds.child("hiloId").getValue(String.class);
                    String hiloTitulo = ds.child("hiloTitulo").getValue(String.class);
                    String hiloDescripcion = ds.child("hiloDescripcion").getValue(String.class);
                    String hiloAutor = ds.child("hiloAutor").getValue(String.class);
                    Long hiloTimestamp = ds.child("hiloTimestamp").getValue(Long.class);
                    if (hiloTimestamp == null) hiloTimestamp = 0L;

                    if (mostrada == null || !mostrada) {
                        lanzarNotificacionForo(titulo, mensaje);
                        ds.getRef().child("mostrada").setValue(true);
                    }

                    // Guardamos la notificación completa en nuestra lista
                    listaNotificaciones.add(0, new com.easypets.models.Notificacion(
                            id, titulo, mensaje, tipo, hiloId, hiloTitulo, hiloDescripcion, hiloAutor, hiloTimestamp));
                }

                // Actualizamos el número rojo de la campanita
                actualizarCampanitaUI();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        // Configurar el click en la campanita
        android.widget.FrameLayout layoutCampanita = findViewById(R.id.layoutCampanita);
        layoutCampanita.setOnClickListener(v -> mostrarDesplegableNotificaciones());
    }

    private void actualizarCampanitaUI() {
        android.widget.TextView tvBadge = findViewById(R.id.tvBadgeNotificaciones);
        android.widget.ImageView btnCampana = findViewById(R.id.btnTopNotificaciones); // Capturamos la campanita

        if (listaNotificaciones.isEmpty()) {
            // No hay notificaciones
            tvBadge.setVisibility(android.view.View.GONE);
            btnCampana.setImageResource(R.drawable.ic_notifications_outline); // Icono vacío
        } else {
            // Hay notificaciones
            tvBadge.setVisibility(android.view.View.VISIBLE);
            tvBadge.setText(String.valueOf(listaNotificaciones.size()));
            btnCampana.setImageResource(R.drawable.ic_notifications_filled); // Icono rellenado
        }
    }

    // 2. El Desplegable Flotante
    private void mostrarDesplegableNotificaciones() {
        android.view.View popupView = getLayoutInflater().inflate(R.layout.layout_popup_notificaciones, null);

        // Creamos la ventana flotante
        android.widget.PopupWindow popupWindow = new android.widget.PopupWindow(popupView,
                dpToPx(280), android.view.ViewGroup.LayoutParams.WRAP_CONTENT, true);

        popupWindow.setElevation(15f);
        popupWindow.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(androidx.core.content.ContextCompat.getColor(this, R.color.fondo_base)));

        android.widget.ListView lvNotificaciones = popupView.findViewById(R.id.lvNotificaciones);
        android.widget.TextView tvSinNotificaciones = popupView.findViewById(R.id.tvSinNotificaciones);

        if (listaNotificaciones.isEmpty()) {
            lvNotificaciones.setVisibility(android.view.View.GONE);
            tvSinNotificaciones.setVisibility(android.view.View.VISIBLE);
        } else {
            // ✨ Adaptador actualizado usando tu nueva tarjeta personalizada ✨
            android.widget.ArrayAdapter<com.easypets.models.Notificacion> adapter = new android.widget.ArrayAdapter<com.easypets.models.Notificacion>(this, R.layout.item_notificacion, listaNotificaciones) {
                @NonNull
                @Override
                public android.view.View getView(int position, @Nullable android.view.View convertView, @NonNull android.view.ViewGroup parent) {

                    // Inflamos la tarjeta si aún no existe
                    if (convertView == null) {
                        convertView = getLayoutInflater().inflate(R.layout.item_notificacion, parent, false);
                    }

                    // Vinculamos los textos de nuestra nueva tarjeta
                    android.widget.TextView tvTitulo = convertView.findViewById(R.id.tvNotificacionTitulo);
                    android.widget.TextView tvMensaje = convertView.findViewById(R.id.tvNotificacionMensaje);

                    com.easypets.models.Notificacion notif = listaNotificaciones.get(position);

                    // Ponemos los datos
                    tvTitulo.setText(notif.titulo);
                    tvMensaje.setText(notif.mensaje);

                    return convertView;
                }
            };
            lvNotificaciones.setAdapter(adapter);

            // ¿Qué pasa al pulsar una notificación?
            lvNotificaciones.setOnItemClickListener((parent, view, position, id) -> {
                com.easypets.models.Notificacion n = listaNotificaciones.get(position);

                // 1. La eliminamos de Firebase
                FirebaseDatabase.getInstance().getReference().child("notificaciones")
                        .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                        .child(n.id).removeValue();

                // 2. Cerramos el desplegable
                popupWindow.dismiss();

                // LÓGICA DE NAVEGACIÓN (DEEP LINKING)
                if ("foro_respuesta".equals(n.tipo) && n.hiloId != null) {
                    // Preparamos los datos del hilo
                    android.os.Bundle args = new android.os.Bundle();
                    args.putString("hiloId", n.hiloId);
                    args.putString("titulo", n.hiloTitulo);
                    args.putString("descripcion", n.hiloDescripcion);
                    args.putString("idAutor", n.hiloAutor);
                    args.putLong("timestamp", n.hiloTimestamp);

                    // Creamos el fragmento y le pasamos los datos
                    com.easypets.ui.comunidad.HiloDetalleFragment fragment = new com.easypets.ui.comunidad.HiloDetalleFragment();
                    fragment.setArguments(args);

                    // Marcamos el botón de la comunidad en la barra inferior (opcional para que quede bonito)
                    bottomNav.setSelectedItemId(R.id.nav_comunidad);

                    // Hacemos el salto de pantalla
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.frame_container, fragment)
                            .addToBackStack(null)
                            .commit();
                }
            });
        }

        // Mostrar el desplegable anclado justo debajo de la campanita
        android.widget.FrameLayout layoutCampanita = findViewById(R.id.layoutCampanita);
        popupWindow.showAsDropDown(layoutCampanita, -dpToPx(240), 10);
    }

    // Convertidor de medidas de pantalla
    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round((float) dp * density);
    }
    private void lanzarNotificacionForo(String titulo, String mensaje) {
        String channelId = "easypets_foro";
        android.content.Intent intent = new android.content.Intent(this, MainActivity.class);
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP);
        android.app.PendingIntent pendingIntent = android.app.PendingIntent.getActivity(this, 0, intent, android.app.PendingIntent.FLAG_ONE_SHOT | android.app.PendingIntent.FLAG_IMMUTABLE);

        android.net.Uri defaultSoundUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION);
        androidx.core.app.NotificationCompat.Builder builder = new androidx.core.app.NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.logo_sin_fondo)
                .setContentTitle(titulo)
                .setContentText(mensaje)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent)
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH);

        android.app.NotificationManager notificationManager = (android.app.NotificationManager) getSystemService(android.content.Context.NOTIFICATION_SERVICE);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            android.app.NotificationChannel channel = new android.app.NotificationChannel(channelId, "Notificaciones de la Comunidad", android.app.NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }

        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }
    private void guardarTokenFCM() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        android.util.Log.w("FCM", "No se pudo obtener el token FCM", task.getException());
                        return;
                    }

                    // Conseguimos el Token del móvil
                    String token = task.getResult();

                    // Lo guardamos en la base de datos dentro del perfil del usuario
                    FirebaseDatabase.getInstance().getReference("usuarios")
                            .child(currentUser.getUid())
                            .child("fcmToken")
                            .setValue(token);
                });
    }
}