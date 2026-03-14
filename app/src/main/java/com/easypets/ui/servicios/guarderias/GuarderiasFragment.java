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
import com.easypets.adapters.GuarderiaAdapter;
import com.easypets.models.Guarderia;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class GuarderiasFragment extends Fragment {

    private RecyclerView rvGuarderias;
    private GuarderiaAdapter adapter;
    private List<Guarderia> listaGuarderias;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_guarderias, container, false);

        rvGuarderias = root.findViewById(R.id.rvGuarderias);
        rvGuarderias.setLayoutManager(new LinearLayoutManager(getContext()));

        listaGuarderias = new ArrayList<>();
        // Asegúrate de que GuarderiaAdapter y el modelo Guarderia coinciden con los parámetros que les pasas
        adapter = new GuarderiaAdapter(listaGuarderias, getContext());
        rvGuarderias.setAdapter(adapter);

        // Llamamos a la API de Google al abrir la pantalla
        cargarGuarderiasDeGoogle("Sevilla");

        return root;
    }

    private void cargarGuarderiasDeGoogle(String ciudad) {
        // Leemos la clave de forma segura desde el archivo local.properties
        String apiKey = getString(R.string.MAPS_API_KEY);

        // Comprobación de seguridad por si no la ha cargado bien
        if (apiKey.isEmpty() || apiKey.equals("\"\"")) {
            Log.e("GOOGLE_DEBUG", "Error: La API Key está vacía. Revisa tu local.properties");
            Toast.makeText(getContext(), "Error de configuración de API", Toast.LENGTH_SHORT).show();
            return;
        }

        // Construimos la búsqueda exacta
        String query = "guarderia canina en " + ciudad.trim();
        String url = "https://maps.googleapis.com/maps/api/place/textsearch/json?query="
                + Uri.encode(query) + "&key=" + apiKey + "&language=es";

        Log.d("GOOGLE_DEBUG", "Enviando petición a Google Places...");

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        // Google puede devolver un status antes de los resultados
                        String status = response.getString("status");
                        if (!status.equals("OK") && !status.equals("ZERO_RESULTS")) {
                            Log.e("GOOGLE_DEBUG", "Error en la API de Google. Status: " + status);
                            return;
                        }

                        JSONArray results = response.getJSONArray("results");
                        listaGuarderias.clear();

                        Log.d("GOOGLE_DEBUG", "Google encontró " + results.length() + " resultados en " + ciudad);

                        for (int i = 0; i < results.length(); i++) {
                            JSONObject place = results.getJSONObject(i);

                            // Extraemos los datos principales
                            String nombre = place.getString("name");
                            String direccion = place.optString("formatted_address", ciudad);
                            double rating = place.optDouble("rating", 4.0);

                            // --- LÓGICA DE FOTOS DE GOOGLE ---
                            // Por defecto, una imagen de Unsplash por si el local no subió foto
                            String imageUrl = "https://images.unsplash.com/photo-1541599540903-2a6a1614740e?w=500&q=80";

                            if (place.has("photos")) {
                                // Si tiene foto real, cogemos la referencia y construimos la URL de descarga
                                String photoRef = place.getJSONArray("photos").getJSONObject(0).getString("photo_reference");
                                imageUrl = "https://maps.googleapis.com/maps/api/place/photo?maxwidth=500&photoreference="
                                        + photoRef + "&key=" + apiKey;
                            }

                            // Coordenadas para tu botón de "Cómo llegar"
                            JSONObject location = place.getJSONObject("geometry").getJSONObject("location");
                            double lat = location.getDouble("lat");
                            double lon = location.getDouble("lng");

                            // Teléfono genérico ya que TextSearch no lo devuelve directamente
                            String telefono = "Consultar en Google Maps";

                            // Añadimos la guardería a la lista
                            listaGuarderias.add(new Guarderia(nombre, imageUrl, rating, direccion, telefono, lat, lon));
                        }

                        // ¡Avisamos al adaptador para que pinte la pantalla!
                        adapter.notifyDataSetChanged();

                        if (listaGuarderias.isEmpty()) {
                            Toast.makeText(getContext(), "No se encontraron guarderías en " + ciudad, Toast.LENGTH_SHORT).show();
                        }

                    } catch (JSONException e) {
                        Log.e("GOOGLE_DEBUG", "Error leyendo el JSON de Google: " + e.getMessage());
                    }
                },
                error -> {
                    Log.e("GOOGLE_DEBUG", "Error de red: " + error.toString());
                    if (error.networkResponse != null) {
                        Log.e("GOOGLE_DEBUG", "Código HTTP: " + error.networkResponse.statusCode);
                        try {
                            // Intenta leer el mensaje de error del servidor de Google
                            String body = new String(error.networkResponse.data, "UTF-8");
                            Log.e("GOOGLE_DEBUG", "Cuerpo del error: " + body);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
        );

        // Añadimos la petición a la cola de Volley
        Volley.newRequestQueue(requireContext()).add(request);
    }
}