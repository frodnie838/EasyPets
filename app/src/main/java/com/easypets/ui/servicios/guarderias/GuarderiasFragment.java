package com.easypets.ui.servicios.guarderias;

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

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.easypets.R;
import com.easypets.adapters.GuarderiaAdapter;
import com.easypets.models.Guarderia;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GuarderiasFragment extends Fragment {

    private RecyclerView rvGuarderias;
    private GuarderiaAdapter adapter;
    private List<Guarderia> listaGuarderias;

    // TOKEN DE YELP
    private static final String YELP_API_KEY = "Bearer gmk6IU6S0Jxz7cp82Wbv0yvqcEd8afQCmKO-fsd64SejouF-ePYB6WWSLc1SVq9Tqq1w7EO5SvtudpiM7XizjC7Fe4CfdR8SJL53tNi9TvYobN0Nv1ciBs8vwE20aXYx";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_guarderias, container, false);

        rvGuarderias = root.findViewById(R.id.rvGuarderias);
        rvGuarderias.setLayoutManager(new LinearLayoutManager(getContext()));

        listaGuarderias = new ArrayList<>();
        adapter = new GuarderiaAdapter(listaGuarderias, getContext());
        rvGuarderias.setAdapter(adapter);

        // Llamamos a la API al abrir la pantalla
        cargarGuarderiasDeYelp("Madrid"); // Cambia la ciudad si quieres

        return root;
    }

    private void cargarGuarderiasDeYelp(String ciudad) {
        // La URL de búsqueda de Yelp (petboarding = guarderías de mascotas)
        String url = "https://api.yelp.com/v3/businesses/search?term=petboarding&location=" + ciudad;

        RequestQueue queue = Volley.newRequestQueue(requireContext());

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONArray businesses = response.getJSONArray("businesses");
                        listaGuarderias.clear();

                        for (int i = 0; i < businesses.length(); i++) {
                            JSONObject negocio = businesses.getJSONObject(i);

                            String nombre = negocio.getString("name");
                            String imageUrl = negocio.optString("image_url", "");
                            double rating = negocio.optDouble("rating", 0.0);

                            JSONObject location = negocio.getJSONObject("location");
                            String ubicacion = location.optString("city", ciudad);

                            String telefono = negocio.optString("display_phone", "Teléfono no disponible");

                            double lat = 0.0;
                            double lon = 0.0;
                            JSONObject coordinates = negocio.optJSONObject("coordinates");
                            if (coordinates != null) {
                                lat = coordinates.optDouble("latitude", 0.0);
                                lon = coordinates.optDouble("longitude", 0.0);
                            }

                            listaGuarderias.add(new Guarderia(nombre, imageUrl, rating, ubicacion, telefono, lat, lon));
                        }

                        adapter.notifyDataSetChanged();

                    } catch (Exception e) {
                        Log.e("YELP_API", "Error leyendo JSON: " + e.getMessage());
                    }
                },
                error -> {
                    Toast.makeText(getContext(), "Error al cargar guarderías. Revisa tu API Key.", Toast.LENGTH_SHORT).show();
                    Log.e("YELP_API", "Error de red: " + error.toString());
                }
        ) {
            // ✨ Este bloque inyecta la clave secreta en la cabecera de la petición (Headers)
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", YELP_API_KEY);
                return headers;
            }
        };

        queue.add(request);
    }
}