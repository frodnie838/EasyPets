package com.easypets.ui.main;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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
import com.google.android.material.button.MaterialButton;
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

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class EducacionFragment extends Fragment {

    private RecyclerView rvArticulos;
    private TabLayout tabLayout;
    private FloatingActionButton fabAgregarArticulo;
    private ArticuloAdapter adapter;
    private List<Articulo> listaArticulos;

    private DatabaseReference comunidadRef, oficialesRef, usuariosRef;
    private FirebaseUser currentUser;
    private ValueEventListener articulosListener;

    private String rolUsuario = "usuario";

    // ✨ Variables para la imagen
    private String imagenSeleccionadaBase64 = "";
    private ImageView ivVistaPreviaDialogo;
    private ActivityResultLauncher<Intent> galeriaLauncher;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_educacion, container, false);

        rvArticulos = view.findViewById(R.id.rvArticulos);
        tabLayout = view.findViewById(R.id.tabLayoutEducacion);
        fabAgregarArticulo = view.findViewById(R.id.fabAgregarArticulo);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        comunidadRef = FirebaseDatabase.getInstance().getReference("articulos_comunidad");
        oficialesRef = FirebaseDatabase.getInstance().getReference("articulos_oficiales");
        usuariosRef = FirebaseDatabase.getInstance().getReference("usuarios");

        listaArticulos = new ArrayList<>();
        adapter = new ArticuloAdapter(listaArticulos, this::mostrarArticuloCompleto);
        rvArticulos.setLayoutManager(new LinearLayoutManager(getContext()));
        rvArticulos.setAdapter(adapter);

        obtenerRolUsuario();
        configurarPestanas();

        fabAgregarArticulo.setOnClickListener(v -> mostrarDialogoCrearArticulo());

        // ✨ Configuramos el launcher para abrir la galería
        galeriaLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        try {
                            Bitmap bitmap = MediaStore.Images.Media.getBitmap(requireActivity().getContentResolver(), imageUri);
                            Bitmap resized = redimensionarImagen(bitmap, 800); // Reducimos tamaño para Firebase

                            if (ivVistaPreviaDialogo != null) {
                                ivVistaPreviaDialogo.setImageBitmap(resized);
                                ivVistaPreviaDialogo.setVisibility(View.VISIBLE);
                            }

                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            resized.compress(Bitmap.CompressFormat.JPEG, 70, baos);
                            imagenSeleccionadaBase64 = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
                        } catch (Exception e) {
                            Toast.makeText(getContext(), "Error al cargar la imagen", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );
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

    // ✨ Método para que la imagen no pese demasiado
    private Bitmap redimensionarImagen(Bitmap image, int maxSize) {
        int width = image.getWidth();
        int height = image.getHeight();
        float bitmapRatio = (float) width / (float) height;
        if (bitmapRatio > 1) {
            width = maxSize;
            height = (int) (width / bitmapRatio);
        } else {
            height = maxSize;
            width = (int) (height * bitmapRatio);
        }
        return Bitmap.createScaledBitmap(image, width, height, true);
    }

    private void obtenerRolUsuario() {
        if (currentUser != null) {
            usuariosRef.child(currentUser.getUid()).child("rol").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        rolUsuario = snapshot.getValue(String.class);
                    }
                    actualizarVisibilidadFab();
                }
                @Override public void onCancelled(@NonNull DatabaseError error) {}
            });
        }
    }

    private void configurarPestanas() {
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                listaArticulos.clear();
                adapter.notifyDataSetChanged();
                actualizarVisibilidadFab();

                if (tab.getPosition() == 0) {
                    cargarArticulos(oficialesRef);
                } else {
                    cargarArticulos(comunidadRef);
                }
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
        cargarArticulos(oficialesRef);
    }

    private void actualizarVisibilidadFab() {
        if (tabLayout.getSelectedTabPosition() == 0) {
            if ("admin".equals(rolUsuario)) {
                fabAgregarArticulo.setVisibility(View.VISIBLE);
            } else {
                fabAgregarArticulo.setVisibility(View.GONE);
            }
        } else {
            if (currentUser != null) {
                fabAgregarArticulo.setVisibility(View.VISIBLE);
            } else {
                fabAgregarArticulo.setVisibility(View.GONE);
            }
        }
    }

    private void cargarArticulos(DatabaseReference ref) {
        detenerListenerArticulos();
        articulosListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                listaArticulos.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    Articulo articulo = data.getValue(Articulo.class);
                    if (articulo != null) {
                        listaArticulos.add(articulo);
                    }
                }
                Collections.sort(listaArticulos, (a1, a2) -> Long.compare(a2.getTimestampCreacion(), a1.getTimestampCreacion()));
                if (listaArticulos.isEmpty() && getContext() != null) {
                    String msg = tabLayout.getSelectedTabPosition() == 0 ? "Aún no hay artículos oficiales." : "Aún no hay artículos en la comunidad.";
                    Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
                }
                adapter.notifyDataSetChanged();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        };
        ref.addValueEventListener(articulosListener);
    }

    private void detenerListenerArticulos() {
        if (articulosListener != null) {
            comunidadRef.removeEventListener(articulosListener);
            oficialesRef.removeEventListener(articulosListener);
            articulosListener = null;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        detenerListenerArticulos();
    }

    private void mostrarDialogoCrearArticulo() {
        boolean esOficial = (tabLayout.getSelectedTabPosition() == 0);
        imagenSeleccionadaBase64 = ""; // Reseteamos la imagen al abrir el diálogo

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        final TextInputEditText etTitulo = new TextInputEditText(requireContext());
        etTitulo.setHint("Título del artículo");

        // ✨ NUEVO: Campo específico para la descripción corta
        final TextInputEditText etDescripcion = new TextInputEditText(requireContext());
        etDescripcion.setHint("Breve descripción (para la lista)");
        etDescripcion.setLines(2);

        final TextInputEditText etContenido = new TextInputEditText(requireContext());
        etContenido.setHint("Escribe el contenido completo aquí...");
        etContenido.setLines(5);
        etContenido.setGravity(android.view.Gravity.TOP);

        final TextInputEditText etUrl = new TextInputEditText(requireContext());
        etUrl.setHint("Enlace a YouTube / Web (Opcional)");

        MaterialButton btnImagen = new MaterialButton(requireContext());
        btnImagen.setText("Añadir Imagen de Portada");
        btnImagen.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            galeriaLauncher.launch(intent);
        });

        ivVistaPreviaDialogo = new ImageView(requireContext());
        ivVistaPreviaDialogo.setVisibility(View.GONE);
        ivVistaPreviaDialogo.setAdjustViewBounds(true);
        ivVistaPreviaDialogo.setMaxHeight(400);
        ivVistaPreviaDialogo.setPadding(0, 20, 0, 20);

        // Añadimos los campos en orden
        layout.addView(etTitulo);
        layout.addView(etDescripcion); // Añadimos la descripción a la vista
        layout.addView(etContenido);
        layout.addView(etUrl);
        layout.addView(btnImagen);
        layout.addView(ivVistaPreviaDialogo);
        builder.setView(layout);

        builder.setTitle(esOficial ? "Crear Artículo Oficial" : "Publicar en Comunidad");
        builder.setPositiveButton("Publicar", (dialog, which) -> {
            String titulo = etTitulo.getText().toString().trim();
            String descripcion = etDescripcion.getText().toString().trim();
            String contenido = etContenido.getText().toString().trim();
            String url = etUrl.getText().toString().trim();

            // Validamos que los 3 campos de texto obligatorios estén llenos
            if (titulo.isEmpty() || descripcion.isEmpty() || contenido.isEmpty()) {
                Toast.makeText(getContext(), "Rellena título, descripción y contenido", Toast.LENGTH_SHORT).show();
                return;
            }

            String autor = esOficial ? "EasyPets Oficial" : (currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "Usuario");
            String idAutor = currentUser != null ? currentUser.getUid() : "";
            DatabaseReference targetRef = esOficial ? oficialesRef : comunidadRef;
            String id = targetRef.push().getKey();

            Articulo nuevoArticulo = new Articulo(
                    id, idAutor, titulo, descripcion, contenido, autor,
                    System.currentTimeMillis(), imagenSeleccionadaBase64, url, esOficial
            );

            targetRef.child(id).setValue(nuevoArticulo).addOnSuccessListener(aVoid ->
                    Toast.makeText(getContext(), "¡Publicado correctamente!", Toast.LENGTH_SHORT).show()
            );
        });
        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

    private void mostrarArticuloCompleto(Articulo articulo) {
        BottomSheetDialog bottomSheet = new BottomSheetDialog(requireContext());

        androidx.core.widget.NestedScrollView scrollView = new androidx.core.widget.NestedScrollView(requireContext());
        scrollView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(60, 60, 60, 80);

        if (articulo.getImagenPortadaBase64() != null && !articulo.getImagenPortadaBase64().isEmpty()) {
            ImageView ivPortada = new ImageView(requireContext());
            ivPortada.setAdjustViewBounds(true);
            ivPortada.setMaxHeight(600);
            ivPortada.setPadding(0, 0, 0, 40);
            try {
                byte[] decodedString = Base64.decode(articulo.getImagenPortadaBase64(), Base64.DEFAULT);
                Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                ivPortada.setImageBitmap(decodedByte);
                layout.addView(ivPortada);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        TextView tvTitulo = new TextView(requireContext());
        tvTitulo.setText(articulo.getTitulo());
        tvTitulo.setTextSize(22f);
        tvTitulo.setTypeface(null, android.graphics.Typeface.BOLD);
        tvTitulo.setTextColor(getResources().getColor(R.color.black, null));

        TextView tvAutorInfo = new TextView(requireContext());
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        String fecha = sdf.format(new Date(articulo.getTimestampCreacion()));

        tvAutorInfo.setText(articulo.isEsOficial() ? "✓ Oficial • " + fecha : "Por: " + articulo.getAutor() + " • " + fecha);
        tvAutorInfo.setTextSize(14f);
        tvAutorInfo.setTextColor(getResources().getColor(R.color.color_acento_primario, null));
        tvAutorInfo.setPadding(0, 10, 0, 40);

        TextView tvContenido = new TextView(requireContext());
        tvContenido.setText(articulo.getContenidoCompleto());
        tvContenido.setTextSize(16f);
        tvContenido.setTextColor(android.graphics.Color.parseColor("#424242"));
        tvContenido.setLineSpacing(10f, 1.2f);

        layout.addView(tvTitulo);
        layout.addView(tvAutorInfo);
        layout.addView(tvContenido);

        // Si hay URL, ponemos un botón debajo del todo
        if (articulo.getUrlEnlace() != null && !articulo.getUrlEnlace().isEmpty()) {
            MaterialButton btnLink = new MaterialButton(requireContext());
            btnLink.setText("Ver Vídeo / Más Info");
            btnLink.setOnClickListener(v -> {
                String link = articulo.getUrlEnlace();
                if (!link.startsWith("http://") && !link.startsWith("https://")) {
                    link = "https://" + link;
                }
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
                startActivity(browserIntent);
            });
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 40, 0, 0);
            btnLink.setLayoutParams(params);
            layout.addView(btnLink);
        }

        // ✨ NUEVO: Metemos el layout dentro del ScrollView, y el ScrollView al BottomSheet
        scrollView.addView(layout);
        bottomSheet.setContentView(scrollView);

        // ✨ Extra: Forzamos a que el BottomSheet se abra bastante para leer cómodamente
        bottomSheet.getBehavior().setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED);

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

            String fecha = new SimpleDateFormat("dd MMM", Locale.getDefault()).format(new Date(articulo.getTimestampCreacion()));
            holder.tvAutor.setText(articulo.isEsOficial() ? "✓ Oficial • " + fecha : "Por: " + articulo.getAutor());

            if (articulo.getImagenPortadaBase64() != null && !articulo.getImagenPortadaBase64().isEmpty()) {
                try {
                    byte[] decodedString = Base64.decode(articulo.getImagenPortadaBase64(), Base64.DEFAULT);
                    Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                    holder.ivIcono.setImageBitmap(decodedByte);
                    holder.ivIcono.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    holder.ivIcono.setImageTintList(null); // Quita el tinte verde si hay foto real
                } catch (Exception e) {
                    holder.ivIcono.setImageResource(articulo.isEsOficial() ? R.drawable.consejos : R.drawable.profile);
                }
            } else {
                holder.ivIcono.setImageResource(articulo.isEsOficial() ? R.drawable.consejos : R.drawable.profile);
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