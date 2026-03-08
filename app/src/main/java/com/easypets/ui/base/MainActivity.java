package com.easypets.ui.base;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.View; // Importante para View.VISIBLE y View.GONE
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

        // ✨ EL ESCUCHADOR MÁGICO (AQUÍ CENTRALIZAMOS TODO EL DISEÑO VISUAL)
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

                } else if (f instanceof MascotasFragment) {
                    cardTopProfile.setVisibility(View.VISIBLE);
                    btnTopBack.setVisibility(View.GONE);
                    ivTopLogo.setVisibility(View.VISIBLE);
                    layoutTopSearch.setVisibility(View.GONE);

                    bottomNav.getMenu().findItem(R.id.nav_pets).setChecked(true);

                } else if (f instanceof HomeFragment) {
                    cardTopProfile.setVisibility(View.VISIBLE);
                    btnTopBack.setVisibility(View.GONE);
                    ivTopLogo.setVisibility(View.VISIBLE);
                    layoutTopSearch.setVisibility(View.GONE);

                    bottomNav.getMenu().findItem(R.id.nav_home).setChecked(true);
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
}