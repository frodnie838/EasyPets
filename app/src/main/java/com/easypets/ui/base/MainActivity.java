package com.easypets.ui.base;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;

import com.easypets.R;
import com.easypets.models.Notificacion;
import com.easypets.ui.auth.LoginActivity;
import com.easypets.ui.calendario.CalendarioFragment;
import com.easypets.ui.comunidad.EducacionFragment;
import com.easypets.ui.comunidad.HiloDetalleFragment;
import com.easypets.ui.home.HomeFragment;
import com.easypets.ui.mascotas.MascotasFragment;
import com.easypets.ui.perfil.PerfilFragment;
import com.easypets.ui.servicios.BusquedaViewModel;
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

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Actividad principal de la aplicación.
 * Actúa como contenedor (Host) para la navegación por fragmentos mediante BottomNavigationView.
 * Gestiona el ciclo de vida global, el buzón de notificaciones en tiempo real,
 * y la cabecera dinámica de la interfaz.
 */
public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;
    private long tiempoUltimoClicAtras = 0;
    private CardView cardTopProfile;
    private ImageView ivTopProfile, ivTopLogo;
    private ImageButton btnTopBack;
    private LinearLayout layoutTopSearch;
    private EditText etTopSearch;

    private List<Notificacion> listaNotificaciones = new ArrayList<>();
    private HashSet<String> notificacionesProcesadas = new HashSet<>();
    private DatabaseReference buzonRef;
    private ValueEventListener notificacionesListener;

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

        ivTopLogo.setOnClickListener(v -> {
            if (bottomNav.getSelectedItemId() != R.id.nav_home) {
                bottomNav.setSelectedItemId(R.id.nav_home);
            }
        });

        activarBuzonDeNotificaciones();
        guardarTokenFCM();

        btnTopBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        cardTopProfile.setOnClickListener(v -> {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.frame_container, new PerfilFragment())
                    .addToBackStack(null)
                    .commit();
        });

        cargarFotoDePerfilEnCabecera();

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) selectedFragment = new HomeFragment();
            else if (itemId == R.id.nav_comunidad) selectedFragment = new EducacionFragment();
            else if (itemId == R.id.nav_pets) selectedFragment = new MascotasFragment();
            else if (itemId == R.id.nav_calendar) selectedFragment = new CalendarioFragment();
            else if (itemId == R.id.nav_service) selectedFragment = new ServiciosFragment();

            if (selectedFragment != null) {
                getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.frame_container, selectedFragment)
                        .commit();
            }
            return true;
        });

        BusquedaViewModel busquedaViewModel = new ViewModelProvider(this).get(BusquedaViewModel.class);
        etTopSearch.setOnEditorActionListener((v, actionId, event) -> {
            String ciudad = etTopSearch.getText().toString().trim();
            if (!ciudad.isEmpty()) {
                busquedaViewModel.buscarCiudad(ciudad);
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) imm.hideSoftInputFromWindow(etTopSearch.getWindowToken(), 0);
            }
            return true;
        });

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().replace(R.id.frame_container, new HomeFragment()).commit();
        }

        configurarNavegacionSuperior();
        manejarDeepLinkDePush(getIntent());
    }

    /**
     * Inicializa un listener en tiempo real sobre Firebase Database para capturar
     * y almacenar temporalmente las notificaciones dirigidas al usuario en sesión.
     */
    private void activarBuzonDeNotificaciones() {
        FrameLayout layoutCampanita = findViewById(R.id.layoutCampanita);
        layoutCampanita.setOnClickListener(v -> mostrarDesplegableNotificaciones());

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null || currentUser.isAnonymous()) {
            actualizarCampanitaUI();
            return;
        }

        buzonRef = FirebaseDatabase.getInstance().getReference().child("notificaciones").child(currentUser.getUid());
        notificacionesListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                listaNotificaciones.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Notificacion n = new Notificacion(
                            ds.getKey(),
                            ds.child("titulo").getValue(String.class),
                            ds.child("mensaje").getValue(String.class),
                            ds.child("tipo").getValue(String.class),
                            ds.child("hiloId").getValue(String.class),
                            ds.child("hiloTitulo").getValue(String.class),
                            ds.child("hiloDescripcion").getValue(String.class),
                            ds.child("hiloAutor").getValue(String.class),
                            ds.child("hiloTimestamp").getValue(Long.class) != null ? ds.child("hiloTimestamp").getValue(Long.class) : 0L
                    );
                    listaNotificaciones.add(0, n);
                }
                actualizarCampanitaUI();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        };
        buzonRef.addValueEventListener(notificacionesListener);
    }

    /**
     * Instancia y muestra una ventana emergente (PopupWindow) con el listado
     * de notificaciones pendientes del usuario.
     */
    private void mostrarDesplegableNotificaciones() {
        View popupView = getLayoutInflater().inflate(R.layout.layout_popup_notificaciones, null);
        PopupWindow popupWindow = new PopupWindow(popupView, dpToPx(280), ViewGroup.LayoutParams.WRAP_CONTENT, true);
        popupWindow.setElevation(15f);
        popupWindow.setBackgroundDrawable(new ColorDrawable(ContextCompat.getColor(this, R.color.fondo_base)));

        ListView lvNotificaciones = popupView.findViewById(R.id.lvNotificaciones);
        TextView tvSinNotificaciones = popupView.findViewById(R.id.tvSinNotificaciones);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null || user.isAnonymous()) {
            lvNotificaciones.setVisibility(View.GONE);
            tvSinNotificaciones.setVisibility(View.VISIBLE);
            tvSinNotificaciones.setText("Inicia sesión para gestionar tus notificaciones");
            tvSinNotificaciones.setOnClickListener(v -> {
                popupWindow.dismiss();
                startActivity(new Intent(this, LoginActivity.class));
                finish();
            });
        } else if (listaNotificaciones.isEmpty()) {
            lvNotificaciones.setVisibility(View.GONE);
            tvSinNotificaciones.setVisibility(View.VISIBLE);
            tvSinNotificaciones.setText("No tienes notificaciones pendientes");
        } else {
            lvNotificaciones.setVisibility(View.VISIBLE);
            tvSinNotificaciones.setVisibility(View.GONE);
            lvNotificaciones.setAdapter(new ArrayAdapter<Notificacion>(this, R.layout.item_notificacion, listaNotificaciones) {
                @NonNull
                @Override
                public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                    if (convertView == null) convertView = getLayoutInflater().inflate(R.layout.item_notificacion, parent, false);
                    Notificacion n = getItem(position);
                    ((TextView) convertView.findViewById(R.id.tvNotificacionTitulo)).setText(n.titulo);
                    ((TextView) convertView.findViewById(R.id.tvNotificacionMensaje)).setText(n.mensaje);
                    return convertView;
                }
            });

            lvNotificaciones.setOnItemClickListener((parent, view, position, id) -> {
                Notificacion n = listaNotificaciones.get(position);
                buzonRef.child(n.id).removeValue();
                popupWindow.dismiss();
                navegarDesdeNotificacion(n);
            });
        }
        popupWindow.showAsDropDown(findViewById(R.id.layoutCampanita), -dpToPx(240), 10);
    }

    /**
     * Enruta al usuario hacia el fragmento correspondiente según el origen de la notificación.
     *
     * @param n Objeto Notificacion seleccionado.
     */
    private void navegarDesdeNotificacion(Notificacion n) {
        if ("foro_respuesta".equals(n.tipo) && n.hiloId != null) {
            bottomNav.getMenu().findItem(R.id.nav_comunidad).setChecked(true);
            Bundle args = new Bundle();
            args.putString("hiloId", n.hiloId);
            args.putString("titulo", n.hiloTitulo);
            args.putString("descripcion", n.hiloDescripcion);
            args.putString("idAutor", n.hiloAutor);
            args.putLong("timestamp", n.hiloTimestamp);
            HiloDetalleFragment f = new HiloDetalleFragment();
            f.setArguments(args);
            getSupportFragmentManager().beginTransaction().replace(R.id.frame_container, f).addToBackStack(null).commit();
        } else if ("evento_calendario".equals(n.tipo)) {
            bottomNav.setSelectedItemId(R.id.nav_calendar);
        }
    }

    private void actualizarCampanitaUI() {
        TextView tvBadge = findViewById(R.id.tvBadgeNotificaciones);
        ImageView btnCampana = findViewById(R.id.btnTopNotificaciones);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null || user.isAnonymous() || listaNotificaciones.isEmpty()) {
            tvBadge.setVisibility(View.GONE);
            btnCampana.setImageResource(R.drawable.ic_notifications_outline);
        } else {
            tvBadge.setVisibility(View.VISIBLE);
            tvBadge.setText(String.valueOf(listaNotificaciones.size()));
            btnCampana.setImageResource(R.drawable.ic_notifications_filled);
        }
    }

    /**
     * Registra un callback en el FragmentManager para actualizar dinámicamente
     * la cabecera (TopBar) dependiendo del Fragment actualmente visible.
     * También configura el comportamiento del botón físico 'Atrás' del sistema.
     */
    private void configurarNavegacionSuperior() {
        getSupportFragmentManager().registerFragmentLifecycleCallbacks(new FragmentManager.FragmentLifecycleCallbacks() {
            @Override
            public void onFragmentResumed(@NonNull FragmentManager fm, @NonNull Fragment f) {
                super.onFragmentResumed(fm, f);
                boolean isServiceSub = f instanceof VeterinariosFragment || f instanceof ParquesFragment || f instanceof ProtectorasFragment || f instanceof PaseadoresFragment || f instanceof FarmaciasFragment || f instanceof AdiestradoresFragment || f instanceof TiendasFragment || f instanceof GuarderiasFragment || f instanceof PeluqueriasFragment;

                cardTopProfile.setVisibility(f instanceof PerfilFragment || isServiceSub ? View.GONE : View.VISIBLE);
                btnTopBack.setVisibility(f instanceof PerfilFragment || isServiceSub ? View.VISIBLE : View.GONE);
                ivTopLogo.setVisibility(f instanceof EducacionFragment || isServiceSub ? View.GONE : View.VISIBLE);
                layoutTopSearch.setVisibility(f instanceof EducacionFragment || isServiceSub ? View.VISIBLE : View.GONE);

                if (f instanceof HomeFragment) bottomNav.getMenu().findItem(R.id.nav_home).setChecked(true);
                else if (f instanceof EducacionFragment) bottomNav.getMenu().findItem(R.id.nav_comunidad).setChecked(true);
                else if (f instanceof CalendarioFragment) bottomNav.getMenu().findItem(R.id.nav_calendar).setChecked(true);
                else if (f instanceof MascotasFragment) bottomNav.getMenu().findItem(R.id.nav_pets).setChecked(true);
                else if (f instanceof ServiciosFragment) bottomNav.getMenu().findItem(R.id.nav_service).setChecked(true);
            }
        }, true);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (getSupportFragmentManager().getBackStackEntryCount() > 0) getSupportFragmentManager().popBackStack();
                else if (bottomNav.getSelectedItemId() != R.id.nav_home) bottomNav.setSelectedItemId(R.id.nav_home);
                else {
                    if (System.currentTimeMillis() - tiempoUltimoClicAtras < 2000) finish();
                    else {
                        Toast.makeText(MainActivity.this, "Presiona atrás de nuevo para salir", Toast.LENGTH_SHORT).show();
                        tiempoUltimoClicAtras = System.currentTimeMillis();
                    }
                }
            }
        });
    }

    private void cargarFotoDePerfilEnCabecera() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && !user.isAnonymous()) {
            FirebaseDatabase.getInstance().getReference("usuarios").child(user.getUid())
                    .addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            String foto = snapshot.child("fotoPerfil").getValue(String.class);
                            if (foto != null && !foto.isEmpty()) {
                                if (foto.startsWith("http")) cargarFotoUrl(foto);
                                else {
                                    byte[] decoded = Base64.decode(foto, Base64.DEFAULT);
                                    ivTopProfile.setImageBitmap(BitmapFactory.decodeByteArray(decoded, 0, decoded.length));
                                    ivTopProfile.setImageTintList(null);
                                }
                            }
                        }
                        @Override public void onCancelled(@NonNull DatabaseError error) {}
                    });
        }
    }

    private void cargarFotoUrl(String url) {
        new Thread(() -> {
            try {
                InputStream in = new URL(url).openStream();
                Bitmap b = BitmapFactory.decodeStream(in);
                runOnUiThread(() -> {
                    ivTopProfile.setImageBitmap(b);
                    ivTopProfile.setImageTintList(null);
                });
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    private void manejarDeepLinkDePush(Intent intent) {
        if (intent != null && intent.hasExtra("tipoNotif")) {
            String tipo = intent.getStringExtra("tipoNotif");
            if ("foro_respuesta".equals(tipo)) {
                Notificacion n = new Notificacion(null, null, null, tipo, intent.getStringExtra("hiloId"), intent.getStringExtra("hiloTitulo"), intent.getStringExtra("hiloDescripcion"), intent.getStringExtra("hiloAutor"), intent.getLongExtra("hiloTimestamp", 0L));
                navegarDesdeNotificacion(n);
            }
        }
    }

    /**
     * Genera y almacena el token de Firebase Cloud Messaging necesario para
     * enviar notificaciones Push individuales a este dispositivo.
     */
    private void guardarTokenFCM() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && !user.isAnonymous()) {
            FirebaseMessaging.getInstance().getToken().addOnSuccessListener(token ->
                    FirebaseDatabase.getInstance().getReference("usuarios").child(user.getUid()).child("fcmToken").setValue(token));
        }
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (buzonRef != null && notificacionesListener != null) buzonRef.removeEventListener(notificacionesListener);
    }
}