package com.easypets.ui.main;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.easypets.R;
import com.easypets.models.Articulo;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EducacionFragment extends Fragment {

    private RecyclerView rvArticulos;
    private TabLayout tabLayout;
    private FloatingActionButton fabAgregarArticulo;
    private ArticuloAdapter adapter;
    private List<Articulo> listaArticulos;

    private DatabaseReference comunidadRef;
    private FirebaseUser currentUser;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_educacion, container, false);

        rvArticulos = view.findViewById(R.id.rvArticulos);
        tabLayout = view.findViewById(R.id.tabLayoutEducacion);
        fabAgregarArticulo = view.findViewById(R.id.fabAgregarArticulo);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        comunidadRef = FirebaseDatabase.getInstance().getReference("articulos_comunidad");

        listaArticulos = new ArrayList<>();
        adapter = new ArticuloAdapter(listaArticulos, this::mostrarArticuloCompleto);
        rvArticulos.setLayoutManager(new LinearLayoutManager(getContext()));
        rvArticulos.setAdapter(adapter);

        configurarPestanas();
        cargarArticulosOficiales(); // Carga por defecto

        fabAgregarArticulo.setOnClickListener(v -> mostrarDialogoCrearArticulo());

        if (getActivity() != null) {
            BottomNavigationView bottomNav = getActivity().findViewById(R.id.bottom_navigation);
            if (bottomNav != null) {
                android.view.Menu menu = bottomNav.getMenu();
                // Le quitamos la obligación de tener uno seleccionado
                menu.setGroupCheckable(0, true, false);
                for (int i = 0; i < menu.size(); i++) {
                    menu.getItem(i).setChecked(false); // Apagamos todos
                }
                // Le volvemos a poner la protección
                menu.setGroupCheckable(0, true, true);
            }
        }
        return view;
    }

    private void configurarPestanas() {
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    fabAgregarArticulo.setVisibility(View.GONE);
                    cargarArticulosOficiales();
                } else {
                    if (currentUser != null) {
                        fabAgregarArticulo.setVisibility(View.VISIBLE);
                    }
                    cargarArticulosComunidad();
                }
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void cargarArticulosOficiales() {
        listaArticulos.clear();
        listaArticulos.add(new Articulo("Enseña a tu perro a sentarse", "El comando más básico y útil.", "1. Ponte frente a tu perro...\n2. Mueve el premio hacia atrás...", R.drawable.consejos));
        listaArticulos.add(new Articulo("La importancia de la socialización", "Evita miedos exponiendo a tu mascota.", "La ventana principal es entre 3 y 14 semanas...", R.drawable.huella));
        listaArticulos.add(new Articulo("Alimentos peligrosos", "Lo que nunca debes darle a tu mascota.", "- Chocolate\n- Cebolla\n- Uvas...", R.drawable.veterinario));
        adapter.notifyDataSetChanged();
    }

    private void cargarArticulosComunidad() {
        comunidadRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Solo recargamos si seguimos en la pestaña de comunidad
                if (tabLayout.getSelectedTabPosition() == 1) {
                    listaArticulos.clear();
                    for (DataSnapshot data : snapshot.getChildren()) {
                        Articulo articulo = data.getValue(Articulo.class);
                        if (articulo != null) {
                            listaArticulos.add(articulo);
                        }
                    }
                    Collections.reverse(listaArticulos); // Los más nuevos arriba
                    adapter.notifyDataSetChanged();
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Error al cargar la comunidad", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void mostrarDialogoCrearArticulo() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());

        // Creamos un diseño básico por código para el diálogo
        android.widget.LinearLayout layout = new android.widget.LinearLayout(requireContext());
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        final TextInputEditText etTitulo = new TextInputEditText(requireContext());
        etTitulo.setHint("Título del artículo");

        final TextInputEditText etContenido = new TextInputEditText(requireContext());
        etContenido.setHint("Escribe tu consejo aquí...");
        etContenido.setLines(5);
        etContenido.setGravity(android.view.Gravity.TOP);

        layout.addView(etTitulo);
        layout.addView(etContenido);
        builder.setView(layout);

        builder.setTitle("Publicar en la Comunidad");
        builder.setPositiveButton("Publicar", (dialog, which) -> {
            String titulo = etTitulo.getText().toString().trim();
            String contenido = etContenido.getText().toString().trim();

            if (titulo.isEmpty() || contenido.isEmpty()) {
                Toast.makeText(getContext(), "Rellena todos los campos", Toast.LENGTH_SHORT).show();
                return;
            }

            // Obtener el nombre del autor (usamos el email antes de la @ si no hay nombre)
            String autor = currentUser.getDisplayName();
            if (autor == null || autor.isEmpty()) {
                autor = currentUser.getEmail().split("@")[0];
            }

            String id = comunidadRef.push().getKey();
            String descripcionCorta = contenido.length() > 50 ? contenido.substring(0, 50) + "..." : contenido;

            Articulo nuevoArticulo = new Articulo(id, titulo, descripcionCorta, contenido, autor);
            comunidadRef.child(id).setValue(nuevoArticulo).addOnSuccessListener(aVoid ->
                    Toast.makeText(getContext(), "¡Artículo publicado!", Toast.LENGTH_SHORT).show()
            );
        });
        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

    private void mostrarArticuloCompleto(Articulo articulo) {
        BottomSheetDialog bottomSheet = new BottomSheetDialog(requireContext());
        android.widget.LinearLayout layout = new android.widget.LinearLayout(requireContext());
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(60, 60, 60, 80);

        TextView tvTitulo = new TextView(requireContext());
        tvTitulo.setText(articulo.getTitulo());
        tvTitulo.setTextSize(22f);
        tvTitulo.setTypeface(null, android.graphics.Typeface.BOLD);
        tvTitulo.setTextColor(getResources().getColor(R.color.black, null));

        TextView tvAutor = new TextView(requireContext());
        tvAutor.setText("Por: " + articulo.getAutor());
        tvAutor.setTextSize(14f);
        tvAutor.setTextColor(getResources().getColor(R.color.color_acento_primario, null));
        tvAutor.setPadding(0, 10, 0, 40);

        TextView tvContenido = new TextView(requireContext());
        tvContenido.setText(articulo.getContenidoCompleto());
        tvContenido.setTextSize(16f);
        tvContenido.setTextColor(android.graphics.Color.parseColor("#424242"));
        tvContenido.setLineSpacing(10f, 1.2f);

        layout.addView(tvTitulo);
        layout.addView(tvAutor);
        layout.addView(tvContenido);

        bottomSheet.setContentView(layout);
        bottomSheet.show();
    }

    static class ArticuloAdapter extends RecyclerView.Adapter<ArticuloAdapter.ArticuloViewHolder> {
        private List<Articulo> articulos;
        private OnArticuloClickListener listener;

        public interface OnArticuloClickListener { void onArticuloClick(Articulo articulo); }

        public ArticuloAdapter(List<Articulo> articulos, OnArticuloClickListener listener) {
            this.articulos = articulos;
            this.listener = listener;
        }

        @NonNull @Override
        public ArticuloViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_articulo, parent, false);
            return new ArticuloViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ArticuloViewHolder holder, int position) {
            Articulo articulo = articulos.get(position);
            holder.tvTitulo.setText(articulo.getTitulo());
            holder.tvDescripcion.setText(articulo.getDescripcionCorta());
            holder.tvAutor.setText("Por: " + articulo.getAutor());

            // Si es de la comunidad, le ponemos un icono genérico distinto
            if (articulo.getImagenResId() == 0) {
                holder.ivIcono.setImageResource(R.drawable.profile); // Icono por defecto para usuarios
            } else {
                holder.ivIcono.setImageResource(articulo.getImagenResId());
            }

            holder.itemView.setOnClickListener(v -> listener.onArticuloClick(articulo));
        }

        @Override public int getItemCount() { return articulos.size(); }

        class ArticuloViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitulo, tvDescripcion, tvAutor;
            ImageView ivIcono;

            public ArticuloViewHolder(@NonNull View itemView) {
                super(itemView);
                tvTitulo = itemView.findViewById(R.id.tvTituloArticulo);
                tvDescripcion = itemView.findViewById(R.id.tvDescripcionArticulo);
                tvAutor = itemView.findViewById(R.id.tvAutorArticulo);
                ivIcono = itemView.findViewById(R.id.ivIconoArticulo);
            }
        }
    }
}