package com.easypets.ui.servicios.guarderias;

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
 * Fragmento encargado de listar las guarderías y hoteles para mascotas.
 * Consume la API de Google Places (Text Search) de forma asíncrona mediante Volley
 * para recuperar datos geolocalizados en base a la ciudad emitida por el ViewModel.
 */
public class GuarderiasFragment extends Fragment {

    private ProgressBar progressBarGuarderias;
    private RecyclerView rvGuarderias;
    private LinearLayout layoutSinGuarderias;
    private TextView tvMensajeGuarderias;

    private ServicioAdapter adapter;
    private List<LocalServicio> listaGuarderias;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_guarderias, container, false);

        progressBarGuarderias = root.findViewById(R.id.progressBarGuarderias);
        rvGuarderias = root.findViewById(R.id.rvGuarderias);
        layoutSinGuarderias = root.findViewById(R.id.layoutSinGuarderias);
        tvMensajeGuarderias = root.findViewById(R.id.tvMensajeGuarderias);

        listaGuarderias = new ArrayList<>();
        adapter = new ServicioAdapter(getContext());
        rvGuarderias.setLayoutManager(new LinearLayoutManager(getContext()));
        rvGuarderias.setAdapter(adapter);

        BusquedaViewModel viewModel = new ViewModelProvider(requireActivity()).get(BusquedaViewModel.class);
        viewModel.getCiudadBuscada().observe(getViewLifecycleOwner(), this::cargarDeGoogle);

        return root;
    }

    /**
     * Realiza una petición GET a la API de Google Places Search.
     * Procesa la respuesta en formato JSON para extraer los detalles de cada establecimiento
     * (nombre, dirección, valoración, estado de apertura y referencias fotográficas)
     * y delega la actualización visual al adaptador del RecyclerView.
     *
     * @param ciudad Nombre de la ubicación geográfica sobre la cual acotar la búsqueda.
     */
    private void cargarDeGoogle(String ciudad) {
        progressBarGuarderias.setVisibility(View.VISIBLE);
        layoutSinGuarderias.setVisibility(View.GONE);
        rvGuarderias.setVisibility(View.GONE);

        String apiKey = getString(R.string.MAPS_API_KEY);

        if (apiKey.isEmpty()) {
            Toast.makeText(getContext(), "Falta API Key", Toast.LENGTH_SHORT).show();
            progressBarGuarderias.setVisibility(View.GONE);
            return;
        }

        String query = "guardería u hotel de mascotas en " + ciudad.trim();
        String url = "https://maps.googleapis.com/maps/api/place/textsearch/json?query="
                + Uri.encode(query) + "&key=" + apiKey + "&language=es";

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    progressBarGuarderias.setVisibility(View.GONE);
                    try {
                        String status = response.getString("status");
                        if (!status.equals("OK") && !status.equals("ZERO_RESULTS")) {
                            mostrarError("Error en servidores de Google.");
                            return;
                        }

                        JSONArray results = response.getJSONArray("results");
                        listaGuarderias.clear();

                        for (int i = 0; i < results.length(); i++) {
                            JSONObject place = results.getJSONObject(i);

                            String nombre = place.getString("name");
                            String direccion = place.optString("formatted_address", ciudad);
                            double rating = place.optDouble("rating", 0.0);
                            int totalResenas = place.optInt("user_ratings_total", 0);

                            String imageUrl = "https://images.unsplash.com/photo-1541599540903-2a6a1614740e?w=500&q=80";
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

                            listaGuarderias.add(new LocalServicio(nombre, direccion, imageUrl, rating, totalResenas, lat, lon, abierto, tieneHorario));
                        }

                        if (listaGuarderias.isEmpty()) {
                            mostrarError("No se encontraron servicios en " + ciudad);
                        } else {
                            adapter.setServicios(listaGuarderias);
                            rvGuarderias.setVisibility(View.VISIBLE);
                        }

                    } catch (JSONException e) {
                        Log.e("GOOGLE_API", "Error parseando JSON: " + e.getMessage());
                        mostrarError("Error procesando los datos.");
                    }
                },
                error -> {
                    progressBarGuarderias.setVisibility(View.GONE);
                    mostrarError("Comprueba tu conexión a internet.");
                }
        );

        Volley.newRequestQueue(requireContext()).add(request);
    }

    /**
     * Actualiza la interfaz de usuario para reflejar estados de error o listas vacías,
     * ocultando el RecyclerView y mostrando un mensaje descriptivo de contingencia.
     *
     * @param mensaje Texto descriptivo del error a presentar al usuario.
     */
    private void mostrarError(String mensaje) {
        tvMensajeGuarderias.setText(mensaje);
        layoutSinGuarderias.setVisibility(View.VISIBLE);
    }
}