package com.easypets.ui.servicios.farmacias;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.easypets.R;
import com.easypets.adapters.ServicioAdapter;
import com.easypets.models.LocalServicio;
import com.easypets.ui.servicios.BusquedaViewModel;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class FarmaciasFragment extends Fragment {

    private ProgressBar progressBarFarmacias;
    private RecyclerView rvFarmacias;
    private LinearLayout layoutSinFarmacias;
    private TextView tvMensajeFarmacias;

    private ServicioAdapter adapter;
    private List<LocalServicio> listaFarmacias;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_farmacias, container, false);

        progressBarFarmacias = root.findViewById(R.id.progressBarFarmacias);
        rvFarmacias = root.findViewById(R.id.rvFarmacias);
        layoutSinFarmacias = root.findViewById(R.id.layoutSinFarmacias);
        tvMensajeFarmacias = root.findViewById(R.id.tvMensajeFarmacias);

        listaFarmacias = new ArrayList<>();
        adapter = new ServicioAdapter(getContext());
        rvFarmacias.setLayoutManager(new LinearLayoutManager(getContext()));
        rvFarmacias.setAdapter(adapter);

        BusquedaViewModel viewModel = new ViewModelProvider(requireActivity()).get(BusquedaViewModel.class);
        viewModel.getCiudadBuscada().observe(getViewLifecycleOwner(), this::cargarDeGoogle);

        return root;
    }

    private void cargarDeGoogle(String ciudad) {
        progressBarFarmacias.setVisibility(View.VISIBLE);
        layoutSinFarmacias.setVisibility(View.GONE);
        rvFarmacias.setVisibility(View.GONE);

        String apiKey = getString(R.string.MAPS_API_KEY);

        if (apiKey.isEmpty()) {
            Toast.makeText(getContext(), "Falta API Key", Toast.LENGTH_SHORT).show();
            progressBarFarmacias.setVisibility(View.GONE);
            return;
        }

        // Búsqueda orientada a farmacias
        String query = "farmacia veterinaria en " + ciudad.trim();
        String url = "https://maps.googleapis.com/maps/api/place/textsearch/json?query="
                + Uri.encode(query) + "&key=" + apiKey + "&language=es";

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    progressBarFarmacias.setVisibility(View.GONE);
                    try {
                        String status = response.getString("status");
                        if (!status.equals("OK") && !status.equals("ZERO_RESULTS")) {
                            mostrarError("Error en servidores de Google.");
                            return;
                        }

                        JSONArray results = response.getJSONArray("results");
                        listaFarmacias.clear();

                        for (int i = 0; i < results.length(); i++) {
                            JSONObject place = results.getJSONObject(i);

                            String nombre = place.getString("name");
                            String direccion = place.optString("formatted_address", ciudad);
                            double rating = place.optDouble("rating", 0.0);
                            int totalResenas = place.optInt("user_ratings_total", 0);

                            // Foto genérica de farmacia
                            String imageUrl = "https://images.unsplash.com/photo-1584308666744-24d5c474f2ae?w=500&q=80";
                            if (place.has("photos")) {
                                String photoRef = place.getJSONArray("photos").getJSONObject(0).getString("photo_reference");
                                imageUrl = "https://maps.googleapis.com/maps/api/place/photo?maxwidth=500&photoreference="
                                        + photoRef + "&key=" + apiKey;
                            }

                            JSONObject location = place.getJSONObject("geometry").getJSONObject("location");
                            double lat = location.getDouble("lat");
                            double lon = location.getDouble("lng");

                            boolean abierto = false;
                            boolean tieneHorario = false;
                            if (place.has("opening_hours")) {
                                tieneHorario = true;
                                abierto = place.getJSONObject("opening_hours").optBoolean("open_now", false);
                            }

                            listaFarmacias.add(new LocalServicio(nombre, direccion, imageUrl, rating, totalResenas, lat, lon, abierto, tieneHorario));
                        }

                        if (listaFarmacias.isEmpty()) {
                            mostrarError("No se encontraron farmacias en " + ciudad);
                        } else {
                            adapter.setServicios(listaFarmacias);
                            rvFarmacias.setVisibility(View.VISIBLE);
                        }

                    } catch (JSONException e) {
                        Log.e("GOOGLE_API", "Error parseando JSON: " + e.getMessage());
                        mostrarError("Error procesando los datos.");
                    }
                },
                error -> {
                    progressBarFarmacias.setVisibility(View.GONE);
                    mostrarError("Comprueba tu conexión a internet.");
                }
        );

        Volley.newRequestQueue(requireContext()).add(request);
    }

    private void mostrarError(String mensaje) {
        tvMensajeFarmacias.setText(mensaje);
        layoutSinFarmacias.setVisibility(View.VISIBLE);
    }
}