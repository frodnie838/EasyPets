package com.easypets.ui.servicios.parques;

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
 * Fragmento encargado de localizar y listar parques caninos y áreas de recreo.
 * Se integra con la API de Google Places (Text Search) mediante solicitudes asíncronas
 * con la librería Volley, basando sus consultas en el flujo de datos del BusquedaViewModel.
 */
public class ParquesFragment extends Fragment {

    private ProgressBar progressBarParques;
    private RecyclerView rvParques;
    private LinearLayout layoutSinParques;
    private TextView tvMensajeParques;

    private ServicioAdapter adapter;
    private List<LocalServicio> listaParques;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_parques, container, false);

        progressBarParques = root.findViewById(R.id.progressBarParques);
        rvParques = root.findViewById(R.id.rvParques);
        layoutSinParques = root.findViewById(R.id.layoutSinParques);
        tvMensajeParques = root.findViewById(R.id.tvMensajeParques);

        listaParques = new ArrayList<>();
        adapter = new ServicioAdapter(getContext());
        rvParques.setLayoutManager(new LinearLayoutManager(getContext()));
        rvParques.setAdapter(adapter);

        BusquedaViewModel viewModel = new ViewModelProvider(requireActivity()).get(BusquedaViewModel.class);
        viewModel.getCiudadBuscada().observe(getViewLifecycleOwner(), this::cargarDeGoogle);

        return root;
    }

    /**
     * Emite una solicitud HTTP GET hacia la API de Google Places.
     * Analiza el objeto JSON devuelto para instanciar modelos de tipo LocalServicio.
     * Implementa lógica de contingencia (fallback) para asignar imágenes genéricas y
     * gestionar la potencial ausencia de horarios comerciales en recintos públicos.
     *
     * @param ciudad Nombre del municipio a utilizar como filtro geográfico en la consulta.
     */
    private void cargarDeGoogle(String ciudad) {
        progressBarParques.setVisibility(View.VISIBLE);
        layoutSinParques.setVisibility(View.GONE);
        rvParques.setVisibility(View.GONE);

        String apiKey = getString(R.string.MAPS_API_KEY);

        if (apiKey.isEmpty()) {
            Toast.makeText(getContext(), "Falta API Key", Toast.LENGTH_SHORT).show();
            progressBarParques.setVisibility(View.GONE);
            return;
        }

        String query = "parque canino en " + ciudad.trim();
        String url = "https://maps.googleapis.com/maps/api/place/textsearch/json?query="
                + Uri.encode(query) + "&key=" + apiKey + "&language=es";

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    progressBarParques.setVisibility(View.GONE);
                    try {
                        String status = response.getString("status");
                        if (!status.equals("OK") && !status.equals("ZERO_RESULTS")) {
                            mostrarError("Error en servidores de Google.");
                            return;
                        }

                        JSONArray results = response.getJSONArray("results");
                        listaParques.clear();

                        for (int i = 0; i < results.length(); i++) {
                            JSONObject place = results.getJSONObject(i);

                            String nombre = place.getString("name");
                            String direccion = place.optString("formatted_address", ciudad);
                            double rating = place.optDouble("rating", 0.0);
                            int totalResenas = place.optInt("user_ratings_total", 0);

                            String imageUrl = "https://images.unsplash.com/photo-1541882672465-3cfa05d6df90?w=500&q=80";
                            if (place.has("photos")) {
                                String photoRef = place.getJSONArray("photos").getJSONObject(0).getString("photo_reference");
                                imageUrl = "https://maps.googleapis.com/maps/api/place/photo?maxwidth=500&photoreference="
                                        + photoRef + "&key=" + apiKey;
                            }

                            JSONObject location = place.getJSONObject("geometry").getJSONObject("location");
                            double lat = location.getDouble("lat");
                            double lon = location.getDouble("lng");

                            boolean abierto = true;
                            boolean tieneHorario = false;
                            if (place.has("opening_hours")) {
                                tieneHorario = true;
                                abierto = place.getJSONObject("opening_hours").optBoolean("open_now", false);
                            }

                            listaParques.add(new LocalServicio(nombre, direccion, imageUrl, rating, totalResenas, lat, lon, abierto, tieneHorario));
                        }

                        if (listaParques.isEmpty()) {
                            mostrarError("No se encontraron parques caninos en " + ciudad);
                        } else {
                            adapter.setServicios(listaParques);
                            rvParques.setVisibility(View.VISIBLE);
                        }

                    } catch (JSONException e) {
                        Log.e("GOOGLE_API", "Error parseando JSON: " + e.getMessage());
                        mostrarError("Error procesando los datos.");
                    }
                },
                error -> {
                    progressBarParques.setVisibility(View.GONE);
                    mostrarError("Comprueba tu conexión a internet.");
                }
        );

        Volley.newRequestQueue(requireContext()).add(request);
    }

    /**
     * Transiciona el estado de la vista para ocultar la lista principal
     * y notificar al usuario sobre el tipo de incidencia encontrada.
     *
     * @param mensaje Detalle del fallo en la obtención de datos o de la ausencia de resultados.
     */
    private void mostrarError(String mensaje) {
        tvMensajeParques.setText(mensaje);
        layoutSinParques.setVisibility(View.VISIBLE);
    }
}