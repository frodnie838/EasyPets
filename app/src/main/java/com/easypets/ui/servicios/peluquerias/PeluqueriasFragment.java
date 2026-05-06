package com.easypets.ui.servicios.peluquerias;

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
 * Fragmento encargado de listar los establecimientos de peluquería canina y felina.
 * Implementa el patrón MVVM observando un BusquedaViewModel compartido para reaccionar
 * a los cambios de ubicación, y consume la API de Google Places de forma asíncrona.
 */
public class PeluqueriasFragment extends Fragment {

    private ProgressBar progressBarPeluquerias;
    private RecyclerView rvPeluquerias;
    private LinearLayout layoutSinPeluquerias;
    private TextView tvMensajePeluquerias;

    private ServicioAdapter adapter;
    private List<LocalServicio> listaPeluquerias;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_peluquerias, container, false);

        progressBarPeluquerias = root.findViewById(R.id.progressBarPeluquerias);
        rvPeluquerias = root.findViewById(R.id.rvPeluquerias);
        layoutSinPeluquerias = root.findViewById(R.id.layoutSinPeluquerias);
        tvMensajePeluquerias = root.findViewById(R.id.tvMensajePeluquerias);

        listaPeluquerias = new ArrayList<>();
        adapter = new ServicioAdapter(getContext());
        rvPeluquerias.setLayoutManager(new LinearLayoutManager(getContext()));
        rvPeluquerias.setAdapter(adapter);

        BusquedaViewModel viewModel = new ViewModelProvider(requireActivity()).get(BusquedaViewModel.class);
        viewModel.getCiudadBuscada().observe(getViewLifecycleOwner(), this::cargarDeGoogle);

        return root;
    }

    /**
     * Construye y ejecuta una petición HTTP GET mediante Volley dirigida a la API de Google Places.
     * Analiza el JSON resultante para poblar la lista de establecimientos, implementando
     * asignación dinámica de imágenes con fallback a fotografías de stock (Unsplash) en caso
     * de no existir referencias multimedia en la base de datos de Google.
     *
     * @param ciudad Localidad empleada como parámetro de filtro en la consulta de Places API.
     */
    private void cargarDeGoogle(String ciudad) {
        progressBarPeluquerias.setVisibility(View.VISIBLE);
        layoutSinPeluquerias.setVisibility(View.GONE);
        rvPeluquerias.setVisibility(View.GONE);

        String apiKey = getString(R.string.MAPS_API_KEY);

        if (apiKey.isEmpty()) {
            Toast.makeText(getContext(), "Falta API Key", Toast.LENGTH_SHORT).show();
            progressBarPeluquerias.setVisibility(View.GONE);
            return;
        }

        String query = "peluquería de mascotas en " + ciudad.trim();
        String url = "https://maps.googleapis.com/maps/api/place/textsearch/json?query="
                + Uri.encode(query) + "&key=" + apiKey + "&language=es";

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    progressBarPeluquerias.setVisibility(View.GONE);
                    try {
                        String status = response.getString("status");
                        if (!status.equals("OK") && !status.equals("ZERO_RESULTS")) {
                            mostrarError("Error en servidores de Google.");
                            return;
                        }

                        JSONArray results = response.getJSONArray("results");
                        listaPeluquerias.clear();

                        for (int i = 0; i < results.length(); i++) {
                            JSONObject place = results.getJSONObject(i);

                            String nombre = place.getString("name");
                            String direccion = place.optString("formatted_address", ciudad);
                            double rating = place.optDouble("rating", 0.0);
                            int totalResenas = place.optInt("user_ratings_total", 0);

                            String imageUrl = "https://images.unsplash.com/photo-1516734212186-a967f81ad0d7?w=500&q=80";
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

                            listaPeluquerias.add(new LocalServicio(nombre, direccion, imageUrl, rating, totalResenas, lat, lon, abierto, tieneHorario));
                        }

                        if (listaPeluquerias.isEmpty()) {
                            mostrarError("No se encontraron peluquerías en " + ciudad);
                        } else {
                            adapter.setServicios(listaPeluquerias);
                            rvPeluquerias.setVisibility(View.VISIBLE);
                        }

                    } catch (JSONException e) {
                        Log.e("GOOGLE_API", "Error parseando JSON: " + e.getMessage());
                        mostrarError("Error procesando los datos.");
                    }
                },
                error -> {
                    progressBarPeluquerias.setVisibility(View.GONE);
                    mostrarError("Comprueba tu conexión a internet.");
                }
        );

        Volley.newRequestQueue(requireContext()).add(request);
    }

    /**
     * Alterna la visibilidad de los componentes de la interfaz para desplegar
     * un mensaje de aviso cuando ocurre una anomalía en la red o no hay resultados.
     *
     * @param mensaje Información descriptiva del estado de error a mostrar.
     */
    private void mostrarError(String mensaje) {
        tvMensajePeluquerias.setText(mensaje);
        layoutSinPeluquerias.setVisibility(View.VISIBLE);
    }
}