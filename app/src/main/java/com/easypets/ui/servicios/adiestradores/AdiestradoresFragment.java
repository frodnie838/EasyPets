package com.easypets.ui.servicios.adiestradores;

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

/**
 * Fragmento encargado de listar los servicios de adiestramiento canino y educación.
 * Consume la API de Google Places (Text Search) mediante la librería Volley para
 * obtener establecimientos geolocalizados en base a la ciudad seleccionada en el ViewModel.
 */
public class AdiestradoresFragment extends Fragment {

    private ProgressBar progressBarAdiestradores;
    private RecyclerView rvAdiestradores;
    private LinearLayout layoutSinAdiestradores;
    private TextView tvMensajeAdiestradores;

    private ServicioAdapter adapter;
    private List<LocalServicio> listaAdiestradores;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_adiestradores, container, false);

        progressBarAdiestradores = root.findViewById(R.id.progressBarAdiestradores);
        rvAdiestradores = root.findViewById(R.id.rvAdiestradores);
        layoutSinAdiestradores = root.findViewById(R.id.layoutSinAdiestradores);
        tvMensajeAdiestradores = root.findViewById(R.id.tvMensajeAdiestradores);

        listaAdiestradores = new ArrayList<>();
        adapter = new ServicioAdapter(getContext());
        rvAdiestradores.setLayoutManager(new LinearLayoutManager(getContext()));
        rvAdiestradores.setAdapter(adapter);

        BusquedaViewModel viewModel = new ViewModelProvider(requireActivity()).get(BusquedaViewModel.class);
        viewModel.getCiudadBuscada().observe(getViewLifecycleOwner(), this::cargarDeGoogle);

        return root;
    }

    /**
     * Realiza una petición asíncrona GET a la API de Google Places Search.
     * Analiza el JSON de respuesta para extraer información relevante (nombre, dirección,
     * valoración, estado de apertura y referencias fotográficas) y la inyecta en el RecyclerView.
     *
     * @param ciudad Nombre de la ciudad para la cual se desean buscar adiestradores.
     */
    private void cargarDeGoogle(String ciudad) {
        progressBarAdiestradores.setVisibility(View.VISIBLE);
        layoutSinAdiestradores.setVisibility(View.GONE);
        rvAdiestradores.setVisibility(View.GONE);

        String apiKey = getString(R.string.MAPS_API_KEY);

        if (apiKey.isEmpty()) {
            Toast.makeText(getContext(), "Falta API Key", Toast.LENGTH_SHORT).show();
            progressBarAdiestradores.setVisibility(View.GONE);
            return;
        }

        String query = "adiestrador canino en " + ciudad.trim();
        String url = "https://maps.googleapis.com/maps/api/place/textsearch/json?query="
                + Uri.encode(query) + "&key=" + apiKey + "&language=es";

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    progressBarAdiestradores.setVisibility(View.GONE);
                    try {
                        String status = response.getString("status");
                        if (!status.equals("OK") && !status.equals("ZERO_RESULTS")) {
                            mostrarError("Error en servidores de Google.");
                            return;
                        }

                        JSONArray results = response.getJSONArray("results");
                        listaAdiestradores.clear();

                        for (int i = 0; i < results.length(); i++) {
                            JSONObject place = results.getJSONObject(i);

                            String nombre = place.getString("name");
                            String direccion = place.optString("formatted_address", ciudad);
                            double rating = place.optDouble("rating", 0.0);
                            int totalResenas = place.optInt("user_ratings_total", 0);

                            String imageUrl = "https://images.unsplash.com/photo-1587300003388-59208cc962cb?w=500&q=80";
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

                            listaAdiestradores.add(new LocalServicio(nombre, direccion, imageUrl, rating, totalResenas, lat, lon, abierto, tieneHorario));
                        }

                        if (listaAdiestradores.isEmpty()) {
                            mostrarError("No se encontraron adiestradores en " + ciudad);
                        } else {
                            adapter.setServicios(listaAdiestradores);
                            rvAdiestradores.setVisibility(View.VISIBLE);
                        }

                    } catch (JSONException e) {
                        Log.e("GOOGLE_API", "Error parseando JSON: " + e.getMessage());
                        mostrarError("Error procesando los datos.");
                    }
                },
                error -> {
                    progressBarAdiestradores.setVisibility(View.GONE);
                    mostrarError("Comprueba tu conexión a internet.");
                }
        );

        Volley.newRequestQueue(requireContext()).add(request);
    }

    /**
     * Actualiza la interfaz de usuario para mostrar estados de error o listas vacías.
     *
     * @param mensaje Texto descriptivo del error a presentar al usuario.
     */
    private void mostrarError(String mensaje) {
        tvMensajeAdiestradores.setText(mensaje);
        layoutSinAdiestradores.setVisibility(View.VISIBLE);
    }
}