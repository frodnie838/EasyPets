package com.easypets.ui.servicios.veterinarios;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
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

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.easypets.R;
import com.easypets.adapters.ServicioAdapter;
import com.easypets.models.LocalServicio;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class VeterinariosFragment extends Fragment {

    private ProgressBar progressBarVeterinarios;
    private RecyclerView rvVeterinarios;
    private LinearLayout layoutSinVeterinarios;
    private TextView tvMensajeVeterinarios;

    private ServicioAdapter adapter;
    private List<LocalServicio> listaVeterinarios;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_veterinarios, container, false);

        progressBarVeterinarios = view.findViewById(R.id.progressBarVeterinarios);
        rvVeterinarios = view.findViewById(R.id.rvVeterinarios);
        layoutSinVeterinarios = view.findViewById(R.id.layoutSinVeterinarios);
        tvMensajeVeterinarios = view.findViewById(R.id.tvMensajeVeterinarios);

        listaVeterinarios = new ArrayList<>();
        adapter = new ServicioAdapter(getContext());
        rvVeterinarios.setLayoutManager(new LinearLayoutManager(getContext()));
        rvVeterinarios.setAdapter(adapter);

        // Escuchamos al mensajero global
        com.easypets.ui.servicios.BusquedaViewModel viewModel = new androidx.lifecycle.ViewModelProvider(requireActivity()).get(com.easypets.ui.servicios.BusquedaViewModel.class);
        viewModel.getCiudadBuscada().observe(getViewLifecycleOwner(), ciudad -> {
            buscarEnGoogle(ciudad);
        });

        // Bugfix visual del BottomNavigation
        if (getActivity() != null) {
            BottomNavigationView bottomNav = getActivity().findViewById(R.id.bottom_navigation);
            if (bottomNav != null) {
                android.view.Menu menu = bottomNav.getMenu();
                menu.setGroupCheckable(0, true, false);
                for (int i = 0; i < menu.size(); i++) {
                    menu.getItem(i).setChecked(false);
                }
                menu.setGroupCheckable(0, true, true);
            }
        }
        layoutSinVeterinarios.setVisibility(View.VISIBLE);
        tvMensajeVeterinarios.setText("Busca una ciudad arriba para ver los veterinarios cercanos 🐾");
        rvVeterinarios.setVisibility(View.GONE);
        return view;
    }

    private void buscarEnGoogle(String ciudad) {
        progressBarVeterinarios.setVisibility(View.VISIBLE);
        rvVeterinarios.setVisibility(View.GONE);
        layoutSinVeterinarios.setVisibility(View.GONE);

        String apiKey = getString(R.string.MAPS_API_KEY);

        if (apiKey.isEmpty()) {
            Toast.makeText(getContext(), "Error: No se encontró la API Key", Toast.LENGTH_SHORT).show();
            progressBarVeterinarios.setVisibility(View.GONE);
            return;
        }

        String query = "veterinario en " + ciudad.trim();
        String url = "https://maps.googleapis.com/maps/api/place/textsearch/json?query="
                + Uri.encode(query) + "&key=" + apiKey + "&language=es";

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    progressBarVeterinarios.setVisibility(View.GONE);
                    try {
                        String status = response.getString("status");
                        if (!status.equals("OK") && !status.equals("ZERO_RESULTS")) {
                            mostrarError("Error al conectar con Google Maps.");
                            return;
                        }

                        JSONArray results = response.getJSONArray("results");
                        listaVeterinarios.clear();

                        for (int i = 0; i < results.length(); i++) {
                            JSONObject place = results.getJSONObject(i);

                            String nombre = place.getString("name");
                            String direccion = place.optString("formatted_address", "Dirección no disponible");
                            double rating = place.optDouble("rating", 0.0);
                            int totalResenas = place.optInt("user_ratings_total", 0);

                            // Imagen por defecto de veterinario si el local no tiene foto
                            String imageUrl = "https://images.unsplash.com/photo-1629909613654-28e377c37b09?w=500&q=80";
                            if (place.has("photos")) {
                                String photoRef = place.getJSONArray("photos").getJSONObject(0).getString("photo_reference");
                                imageUrl = "https://maps.googleapis.com/maps/api/place/photo?maxwidth=500&photoreference="
                                        + photoRef + "&key=" + apiKey;
                            }

                            JSONObject location = place.getJSONObject("geometry").getJSONObject("location");
                            double lat = location.getDouble("lat");
                            double lon = location.getDouble("lng");

                            // Comprobamos si Google nos da el horario
                            boolean abierto = false;
                            boolean tieneHorario = false;
                            if (place.has("opening_hours")) {
                                tieneHorario = true;
                                abierto = place.getJSONObject("opening_hours").optBoolean("open_now", false);
                            }

                            listaVeterinarios.add(new LocalServicio(nombre, direccion, imageUrl, rating, totalResenas, lat, lon, abierto, tieneHorario));
                        }

                        if (listaVeterinarios.isEmpty()) {
                            tvMensajeVeterinarios.setText("No hemos encontrado veterinarios en '" + ciudad + "'.");
                            layoutSinVeterinarios.setVisibility(View.VISIBLE);
                        } else {
                            adapter.setServicios(listaVeterinarios);
                            rvVeterinarios.setVisibility(View.VISIBLE);
                        }

                    } catch (JSONException e) {
                        mostrarError("Error al procesar los datos recibidos.");
                    }
                },
                error -> {
                    progressBarVeterinarios.setVisibility(View.GONE);
                    mostrarError("Error de conexión. Comprueba tu internet.");
                }
        );

        Volley.newRequestQueue(requireContext()).add(request);
    }

    private void mostrarError(String mensaje) {
        tvMensajeVeterinarios.setText(mensaje);
        layoutSinVeterinarios.setVisibility(View.VISIBLE);
    }
}