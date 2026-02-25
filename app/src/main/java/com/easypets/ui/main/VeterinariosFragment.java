package com.easypets.ui.main;

import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.easypets.R;
import com.easypets.adapters.VeterinarioAdapter;
import com.easypets.models.Veterinario;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VeterinariosFragment extends Fragment {

    private EditText etCiudadBuscador;
    private MaterialButton btnBuscarVeterinarios;
    private ProgressBar progressBarVeterinarios;
    private RecyclerView rvVeterinarios;
    private LinearLayout layoutSinVeterinarios;
    private TextView tvMensajeVeterinarios;

    private VeterinarioAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_veterinarios, container, false);

        // 1. Vincular Vistas
        etCiudadBuscador = view.findViewById(R.id.etCiudadBuscador);
        btnBuscarVeterinarios = view.findViewById(R.id.btnBuscarVeterinarios);
        progressBarVeterinarios = view.findViewById(R.id.progressBarVeterinarios);
        rvVeterinarios = view.findViewById(R.id.rvVeterinarios);
        layoutSinVeterinarios = view.findViewById(R.id.layoutSinVeterinarios);
        tvMensajeVeterinarios = view.findViewById(R.id.tvMensajeVeterinarios);

        // 2. Configurar la Lista (RecyclerView)
        adapter = new VeterinarioAdapter();
        rvVeterinarios.setLayoutManager(new LinearLayoutManager(getContext()));
        rvVeterinarios.setAdapter(adapter);

        // 3. Programar el Botón de Búsqueda
        btnBuscarVeterinarios.setOnClickListener(v -> {
            String ciudad = etCiudadBuscador.getText().toString().trim();
            if (ciudad.isEmpty()) {
                Toast.makeText(getContext(), "Por favor, escribe el nombre de una ciudad", Toast.LENGTH_SHORT).show();
                return;
            }
            buscarVeterinariosEnOSM(ciudad);
        });

        if (getActivity() != null) {
            BottomNavigationView bottomNav = getActivity().findViewById(R.id.bottom_navigation);
            if (bottomNav != null) {
                android.view.Menu menu = bottomNav.getMenu();
                // Le quitamos la obligación de tener uno seleccionado
                menu.setGroupCheckable(0, true, false);
                for (int i = 0; i < menu.size(); i++) {
                    menu.getItem(i).setChecked(false); // Apagamos todos
                }
                // Le volvemos a poner la protección
                menu.setGroupCheckable(0, true, true);
            }
        }

        return view;
    }

    private void buscarVeterinariosEnOSM(String ciudad) {
        // Ocultar resultados y mostrar la ruedecita de carga
        progressBarVeterinarios.setVisibility(View.VISIBLE);
        rvVeterinarios.setVisibility(View.GONE);
        layoutSinVeterinarios.setVisibility(View.GONE);

        // Creamos un hilo secundario para no bloquear la pantalla del usuario
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            List<Veterinario> resultados = new ArrayList<>();
            boolean exito = false;

            try {
                // Crear la consulta (Query) en Overpass QL
                String query = "[out:json][timeout:25];" +
                        "area[name=\"" + ciudad + "\"]->.searchArea;" +
                        "nwr[\"amenity\"=\"veterinary\"](area.searchArea);" +
                        "out center;";

                // Conectarse a la API pública
                String urlString = "https://overpass-api.de/api/interpreter?data=" + URLEncoder.encode(query, "UTF-8");
                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                // Añadimos identificador y tiempos de espera para evitar bloqueos del servidor
                conn.setRequestProperty("User-Agent", "EasyPets_App_TFG/1.0");
                conn.setRequestProperty("Accept", "application/json");
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);

                if (conn.getResponseCode() == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    JSONObject jsonResponse = new JSONObject(response.toString());
                    JSONArray elements = jsonResponse.getJSONArray("elements");

                    for (int i = 0; i < elements.length(); i++) {
                        JSONObject element = elements.getJSONObject(i);

                        double lat = 0.0;
                        double lon = 0.0;

                        // Coordenadas
                        if (element.has("lat") && element.has("lon")) {
                            lat = element.getDouble("lat");
                            lon = element.getDouble("lon");
                        } else if (element.has("center")) {
                            JSONObject center = element.getJSONObject("center");
                            lat = center.getDouble("lat");
                            lon = center.getDouble("lon");
                        }

                        // Datos de la clínica
                        if (element.has("tags")) {
                            JSONObject tags = element.getJSONObject("tags");

                            // Nombre y teléfono directo de OSM
                            String nombre = tags.optString("name", "Clínica Veterinaria (Sin Nombre)");
                            String telefono = tags.optString("phone", tags.optString("contact:phone", "Sin teléfono"));

                            String direccionFinal = "Buscando dirección...";

                            try {
                                Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
                                List<Address> direccionesMap = geocoder.getFromLocation(lat, lon, 1);

                                if (direccionesMap != null && !direccionesMap.isEmpty()) {
                                    direccionFinal = direccionesMap.get(0).getAddressLine(0);
                                } else {
                                    direccionFinal = "Dirección no encontrada en el mapa";
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                                direccionFinal = "Dirección no disponible (Fallo de conexión)";
                            }

                            resultados.add(new Veterinario(nombre, direccionFinal, telefono, lat, lon));
                        }
                    }
                    exito = true;
                } else {
                    System.out.println("Error del servidor OSM. Código HTTP: " + conn.getResponseCode());
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

            final boolean finalExito = exito;
            handler.post(() -> {
                progressBarVeterinarios.setVisibility(View.GONE);

                if (finalExito) {
                    if (resultados.isEmpty()) {
                        tvMensajeVeterinarios.setText("No hemos encontrado veterinarios en '" + ciudad + "'.\nComprueba que esté bien escrito o prueba con una ciudad más grande.");
                        layoutSinVeterinarios.setVisibility(View.VISIBLE);
                    } else {
                        adapter.setVeterinarios(resultados);
                        rvVeterinarios.setVisibility(View.VISIBLE);
                    }
                } else {
                    tvMensajeVeterinarios.setText("Error al conectar con el servidor de mapas.\nComprueba tu conexión a Internet e inténtalo de nuevo.");
                    layoutSinVeterinarios.setVisibility(View.VISIBLE);
                }
            });
        });
    }
}