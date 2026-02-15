package com.easypets.ui.main;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.easypets.ui.mascotas.MascotasFragment;
import com.easypets.ui.perfil.PerfilFragment;
import com.easypets.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        setContentView(R.layout.activity_main);

        bottomNav = findViewById(R.id.bottom_navigation);

        // Configurar el listener de la barra
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;

            // Usamos if-else if en lugar de switch para evitar problemas con versiones nuevas de Gradle
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                selectedFragment = new HomeFragment();
            } else if (itemId == R.id.nav_pets) {
                selectedFragment = new MascotasFragment();
            } else if (itemId == R.id.nav_calendar) {
                selectedFragment = new CalendarioFragment();
            } else if (itemId == R.id.nav_profile) {
                selectedFragment = new PerfilFragment();
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.frame_container, selectedFragment)
                        .commit();
            }
            return true;
        });

        // 2. Cargar el fragmento por defecto al iniciar (Home)
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.frame_container, new HomeFragment())
                    .commit();
        }
    }
}