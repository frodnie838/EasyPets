package com.easypets.ui.base;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.easypets.ui.calendario.CalendarioFragment;
import com.easypets.ui.comunidad.EducacionFragment;
import com.easypets.ui.home.HomeFragment;
import com.easypets.ui.mascotas.MascotasFragment;
import com.easypets.ui.perfil.PerfilFragment;
import com.easypets.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;
    private long tiempoUltimoClicAtras = 0;

    private CardView cardTopProfile;
    private ImageView ivTopProfile, ivTopLogo;
    private android.widget.ImageButton btnTopBack;
    private android.widget.LinearLayout layoutTopSearch;
    private android.widget.EditText etTopSearch;

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

        btnTopBack.setOnClickListener(v -> {
            getOnBackPressedDispatcher().onBackPressed();
        });

        // Hacer que la foto de perfil funcione como botón
        cardTopProfile.setOnClickListener(v -> {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.frame_container, new PerfilFragment())
                    .addToBackStack(null)
                    .commit();

            // "Desmarcar" visualmente todas las pestañas del menú inferior
            int size = bottomNav.getMenu().size();
            for (int i = 0; i < size; i++) {
                bottomNav.getMenu().getItem(i).setChecked(false);
            }
        });

        cargarFotoDePerfilEnCabecera();

        // Configurar el listener de la barra
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

        // Cargar el fragmento por defecto al iniciar (Home)
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.frame_container, new HomeFragment())
                    .commit();
        }

        // ✨ EL ESCUCHADOR MÁGICO
        getSupportFragmentManager().registerFragmentLifecycleCallbacks(new androidx.fragment.app.FragmentManager.FragmentLifecycleCallbacks() {
            @Override
            public void onFragmentResumed(@NonNull androidx.fragment.app.FragmentManager fm, @NonNull Fragment f) {
                super.onFragmentResumed(fm, f);

                if (f instanceof PerfilFragment) {
                    // 🔝 BARRA SUPERIOR: En Perfil: Flecha atrás, sin buscador
                    cardTopProfile.setVisibility(android.view.View.INVISIBLE);
                    btnTopBack.setVisibility(android.view.View.VISIBLE);
                    ivTopLogo.setVisibility(android.view.View.VISIBLE);
                    layoutTopSearch.setVisibility(android.view.View.GONE);

                    // ⬇️ BARRA INFERIOR: Desmarcar todas las pestañas
                    int size = bottomNav.getMenu().size();
                    for (int i = 0; i < size; i++) {
                        bottomNav.getMenu().getItem(i).setChecked(false);
                    }

                } else if (f instanceof EducacionFragment) {
                    // 🔝 BARRA SUPERIOR: Foto, SIN logo, CON buscador central
                    cardTopProfile.setVisibility(android.view.View.VISIBLE);
                    btnTopBack.setVisibility(android.view.View.GONE);
                    ivTopLogo.setVisibility(android.view.View.GONE);
                    layoutTopSearch.setVisibility(android.view.View.VISIBLE);

                    // ⬇️ BARRA INFERIOR: Marcar pestaña Educación (comprueba tu ID real)
                    bottomNav.getMenu().findItem(R.id.nav_comunidad).setChecked(true);

                } else if (f instanceof CalendarioFragment) {
                    // 🔝 BARRA SUPERIOR: Estado Normal
                    cardTopProfile.setVisibility(android.view.View.VISIBLE);
                    btnTopBack.setVisibility(android.view.View.GONE);
                    ivTopLogo.setVisibility(android.view.View.VISIBLE);
                    layoutTopSearch.setVisibility(android.view.View.GONE);

                    // ⬇️ BARRA INFERIOR: Marcar pestaña Calendario
                    bottomNav.getMenu().findItem(R.id.nav_calendar).setChecked(true);

                } else {
                    // 🔝 BARRA SUPERIOR: Estado Normal (Home, Mascotas, etc.)
                    cardTopProfile.setVisibility(android.view.View.VISIBLE);
                    btnTopBack.setVisibility(android.view.View.GONE);
                    ivTopLogo.setVisibility(android.view.View.VISIBLE);
                    layoutTopSearch.setVisibility(android.view.View.GONE);

                    // ⬇️ BARRA INFERIOR: Aquí puedes añadir más 'else if' para Mascotas o Inicio
                    // Por ejemplo, si es el Home:
                    // if (f instanceof InicioFragment) bottomNav.getMenu().findItem(R.id.nav_inicio).setChecked(true);
                }
            }
        }, true);

        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                    getSupportFragmentManager().popBackStack();

                    // Asegurarnos de marcar la pestaña correcta en el menú inferior al volver atrás
                    getSupportFragmentManager().executePendingTransactions();
                    Fragment current = getSupportFragmentManager().findFragmentById(R.id.frame_container);
                    if (current instanceof HomeFragment) bottomNav.setSelectedItemId(R.id.nav_home);
                    else if (current instanceof MascotasFragment) bottomNav.setSelectedItemId(R.id.nav_pets);
                    else if (current instanceof CalendarioFragment) bottomNav.setSelectedItemId(R.id.nav_calendar);
                    else if (current instanceof EducacionFragment) bottomNav.setSelectedItemId(R.id.nav_comunidad);

                } else {
                    Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.frame_container);

                    if (!(currentFragment instanceof HomeFragment)) {
                        bottomNav.setSelectedItemId(R.id.nav_home);
                    } else {
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
                                // Decodificar Base64 a Bitmap
                                byte[] decodedString = Base64.decode(fotoBase64Actual, Base64.DEFAULT);
                                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

                                // Poner el Bitmap
                                ivTopProfile.setImageBitmap(bitmap);
                                ivTopProfile.setImageTintList(null);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    // Si falla, se queda el icono de perfil gris por defecto
                }
            });
        }
    }
}