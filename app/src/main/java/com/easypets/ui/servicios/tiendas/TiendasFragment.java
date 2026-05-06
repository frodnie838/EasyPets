package com.easypets.ui.servicios.tiendas;

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
 * Fragmento encargado de listar los establecimientos de venta de productos para mascotas.
 * Consume la API de Google Places (Text Search) de forma asíncrona mediante Volley
 * para recuperar datos geolocalizados en base a la ciudad emitida por el ViewModel.
 */
public class TiendasFragment extends Fragment {

    private ProgressBar progressBarTiendas;
    private RecyclerView rvTiendas;
    private LinearLayout layoutSinTiendas;
    private TextView tvMensajeTiendas;

    private ServicioAdapter adapter;
    private List<LocalServicio> listaTiendas;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_tiendas, container, false);

        progressBarTiendas = root.findViewById(R.id.progressBarTiendas);
        rvTiendas = root.findViewById(R.id.rvTiendas);
        layoutSinTiendas = root.findViewById(R.id.layoutSinTiendas);
        tvMensajeTiendas = root.findViewById(R.id.tvMensajeTiendas);

        listaTiendas = new ArrayList<>();
        adapter = new ServicioAdapter(getContext());
        rvTiendas.setLayoutManager(new LinearLayoutManager(getContext()));
        rvTiendas.setAdapter(adapter);

        BusquedaViewModel viewModel = new ViewModelProvider(requireActivity()).get(BusquedaViewModel.class);
        viewModel.getCiudadBuscada().observe(getViewLifecycleOwner(), this::cargarDeGoogle);

        return root;
    }

    /**
     * Realiza una petición GET a la API de Google Places Search.
     * Procesa la respuesta en formato JSON para extraer los detalles de cada establecimiento comercial
     * (nombre, dirección, valoración, estado de apertura y referencias fotográficas)
     * e inyecta los resultados en el adaptador del RecyclerView.
     *
     * @param ciudad Nombre de la ubicación geográfica sobre la cual acotar la búsqueda.
     */
    private void cargarDeGoogle(String ciudad) {
        progressBarTiendas.setVisibility(View.VISIBLE);
        layoutSinTiendas.setVisibility(View.GONE);
        rvTiendas.setVisibility(View.GONE);

        String apiKey = getString(R.string.MAPS_API_KEY);

        if (apiKey.isEmpty()) {
            Toast.makeText(getContext(), "Falta API Key", Toast.LENGTH_SHORT).show();
            progressBarTiendas.setVisibility(View.GONE);
            return;
        }

        String query = "tienda de mascotas en " + ciudad.trim();
        String url = "https://maps.googleapis.com/maps/api/place/textsearch/json?query="
                + Uri.encode(query) + "&key=" + apiKey + "&language=es";

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    progressBarTiendas.setVisibility(View.GONE);
                    try {
                        String status = response.getString("status");
                        if (!status.equals("OK") && !status.equals("ZERO_RESULTS")) {
                            mostrarError("Error en servidores de Google.");
                            return;
                        }

                        JSONArray results = response.getJSONArray("results");
                        listaTiendas.clear();

                        for (int i = 0; i < results.length(); i++) {
                            JSONObject place = results.getJSONObject(i);

                            String nombre = place.getString("name");
                            String direccion = place.optString("formatted_address", ciudad);
                            double rating = place.optDouble("rating", 0.0);
                            int totalResenas = place.optInt("user_ratings_total", 0);

                            String imageUrl = "https://images.unsplash.com/photo-1583337130417-3346a1be7dee?w=500&q=80";
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

                            listaTiendas.add(new LocalServicio(nombre, direccion, imageUrl, rating, totalResenas, lat, lon, abierto, tieneHorario));
                        }

                        if (listaTiendas.isEmpty()) {
                            mostrarError("No se encontraron tiendas en " + ciudad);
                        } else {
                            adapter.setServicios(listaTiendas);
                            rvTiendas.setVisibility(View.VISIBLE);
                        }

                    } catch (JSONException e) {
                        Log.e("GOOGLE_API", "Error parseando JSON: " + e.getMessage());
                        mostrarError("Error procesando los datos.");
                    }
                },
                error -> {
                    progressBarTiendas.setVisibility(View.GONE);
                    mostrarError("Comprueba tu conexión a internet.");
                }
        );

        Volley.newRequestQueue(requireContext()).add(request);
    }

    /**
     * Actualiza la interfaz de usuario para reflejar estados de error o listas vacías,
     * ocultando el RecyclerView y desplegando un mensaje de contingencia.
     *
     * @param mensaje Texto descriptivo del error a presentar al usuario.
     */
    private void mostrarError(String mensaje) {
        tvMensajeTiendas.setText(mensaje);
        layoutSinTiendas.setVisibility(View.VISIBLE);
    }
}