package com.easypets.ui.servicios.guarderias;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.easypets.R;
import com.easypets.adapters.ServicioAdapter;
import com.easypets.models.LocalServicio;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class GuarderiasFragment extends Fragment {

    private RecyclerView rvGuarderias;
    private ServicioAdapter adapter;
    private List<LocalServicio> listaGuarderias;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_guarderias, container, false);

        rvGuarderias = root.findViewById(R.id.rvGuarderias);
        rvGuarderias.setLayoutManager(new LinearLayoutManager(getContext()));

        listaGuarderias = new ArrayList<>();
        adapter = new ServicioAdapter(getContext());
        rvGuarderias.setAdapter(adapter);

        cargarDeGoogle("Sevilla"); // Ciudad por defecto

        return root;
    }

    private void cargarDeGoogle(String ciudad) {
        String apiKey = getString(R.string.MAPS_API_KEY);

        if (apiKey.isEmpty()) {
            Toast.makeText(getContext(), "Error de configuración de API", Toast.LENGTH_SHORT).show();
            return;
        }

        String query = "guarderia canina en " + ciudad.trim();
        String url = "https://maps.googleapis.com/maps/api/place/textsearch/json?query="
                + Uri.encode(query) + "&key=" + apiKey + "&language=es";

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        String status = response.getString("status");
                        if (!status.equals("OK") && !status.equals("ZERO_RESULTS")) {
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

                            // Imagen por defecto de guardería
                            String imageUrl = "https://images.unsplash.com/photo-1541599540903-2a6a1614740e?w=500&q=80";
                            if (place.has("photos")) {
                                String photoRef = place.getJSONArray("photos").getJSONObject(0).getString("photo_reference");
                                imageUrl = "https://maps.googleapis.com/maps/api/place/photo?maxwidth=500&photoreference="
                                        + photoRef + "&key=" + apiKey;
                            }

                            JSONObject location = place.getJSONObject("geometry").getJSONObject("location");
                            double lat = location.getDouble("lat");
                            double lon = location.getDouble("lng");

                            listaGuarderias.add(new LocalServicio(nombre, direccion, imageUrl, rating, totalResenas, lat, lon));
                        }

                        adapter.setServicios(listaGuarderias);

                        if (listaGuarderias.isEmpty()) {
                            Toast.makeText(getContext(), "No se encontraron guarderías en " + ciudad, Toast.LENGTH_SHORT).show();
                        }

                    } catch (JSONException e) {
                        Log.e("GOOGLE_DEBUG", "Error leyendo el JSON: " + e.getMessage());
                    }
                },
                error -> Log.e("GOOGLE_DEBUG", "Error de red: " + error.toString())
        );

        Volley.newRequestQueue(requireContext()).add(request);
    }
}