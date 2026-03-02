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
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class EducacionFragment extends Fragment {

    private RecyclerView rvArticulos;
    private TabLayout tabLayout;
    private FloatingActionButton fabAgregarArticulo;
    private ArticuloAdapter adapter;
    private List<Articulo> listaArticulos;

    private DatabaseReference comunidadRef, oficialesRef, usuariosRef;
    private FirebaseUser currentUser;
    private ValueEventListener articulosListener;
    private Query activeQuery;

    private String rolUsuario = "usuario";

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

        adapter = new ArticuloAdapter(listaArticulos, articulo -> {
            if (tabLayout.getSelectedTabPosition() == 2) {
                mostrarOpcionesMisArticulos(articulo);
            } else {
                mostrarArticuloCompleto(articulo);
            }
        });

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
        rvArticulos.setLayoutManager(new LinearLayoutManager(getContext()));
        rvArticulos.setAdapter(adapter);

        obtenerRolUsuario();
        configurarPestanas();

        fabAgregarArticulo.setOnClickListener(v -> mostrarDialogoCrearArticulo());

        galeriaLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        try {
                            Bitmap bitmap = MediaStore.Images.Media.getBitmap(requireActivity().getContentResolver(), imageUri);
                            Bitmap resized = redimensionarImagen(bitmap, 800);

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

        return view;
    }

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
        if (tabLayout.getTabCount() < 3) {
            tabLayout.addTab(tabLayout.newTab().setText("Mis Artículos"));
        }

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                listaArticulos.clear();
                adapter.notifyDataSetChanged();
                actualizarVisibilidadFab();

                if (tab.getPosition() == 0) {
                    cargarArticulos(oficialesRef, "Aún no hay artículos oficiales.");
                } else if (tab.getPosition() == 1) {
                    cargarArticulos(comunidadRef, "Aún no hay artículos en la comunidad.");
                } else if (tab.getPosition() == 2) {
                    if (currentUser != null) {
                        Query misArticulosQuery = comunidadRef.orderByChild("idAutor").equalTo(currentUser.getUid());
                        cargarArticulos(misArticulosQuery, "Aún no has escrito ningún artículo.");
                    } else {
                        Toast.makeText(getContext(), "Debes iniciar sesión para ver tus artículos.", Toast.LENGTH_SHORT).show();
                    }
                }
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        cargarArticulos(oficialesRef, "Aún no hay artículos oficiales.");
    }

    private void actualizarVisibilidadFab() {
        if (tabLayout.getSelectedTabPosition() == 0) {
            fabAgregarArticulo.setVisibility("admin".equals(rolUsuario) ? View.VISIBLE : View.GONE);
        } else if (tabLayout.getSelectedTabPosition() == 1) {
            fabAgregarArticulo.setVisibility(View.GONE);
        } else {
            fabAgregarArticulo.setVisibility(currentUser != null ? View.VISIBLE : View.GONE);
        }
    }

    private void cargarArticulos(Query query, String mensajeVacio) {
        detenerListenerArticulos();
        activeQuery = query;
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
                    Toast.makeText(getContext(), mensajeVacio, Toast.LENGTH_SHORT).show();
                }
                adapter.notifyDataSetChanged();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        };
        activeQuery.addValueEventListener(articulosListener);
    }

    private void detenerListenerArticulos() {
        if (articulosListener != null && activeQuery != null) {
            activeQuery.removeEventListener(articulosListener);
            articulosListener = null;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        detenerListenerArticulos();
    }
    private void mostrarOpcionesMisArticulos(Articulo articulo) {
        // Creamos el menú deslizable desde abajo
        BottomSheetDialog bottomSheet = new BottomSheetDialog(requireContext());
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_opciones_articulo, null);
        bottomSheet.setContentView(view);

        // Vinculamos los botones
        MaterialButton btnVer = view.findViewById(R.id.btnOpcionVer);
        MaterialButton btnEditar = view.findViewById(R.id.btnOpcionEditar);
        MaterialButton btnEliminar = view.findViewById(R.id.btnOpcionEliminar);

        // Damos función a cada botón
        btnVer.setOnClickListener(v -> {
            bottomSheet.dismiss();
            mostrarArticuloCompleto(articulo);
        });

        btnEditar.setOnClickListener(v -> {
            bottomSheet.dismiss();
            mostrarDialogoEditarArticulo(articulo);
        });

        btnEliminar.setOnClickListener(v -> {
            bottomSheet.dismiss();
            eliminarArticulo(articulo); // Esto abre la confirmación normal de borrado
        });

        // Mostramos el menú
        bottomSheet.show();
    }

    private void eliminarArticulo(Articulo articulo) {
        new AlertDialog.Builder(requireContext())
                .setTitle("¿Eliminar artículo?")
                .setMessage("Esta acción no se puede deshacer.")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    comunidadRef.child(articulo.getId()).removeValue()
                            .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Artículo eliminado", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void mostrarDialogoCrearArticulo() {
        boolean esOficial = (tabLayout.getSelectedTabPosition() == 0);
        imagenSeleccionadaBase64 = "";

        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_agregar_articulo, null);

        // Creamos el diálogo transparente (para que se vean los bordes redondeados si los pones)
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(view)
                .create();

        TextView tvTitle = view.findViewById(R.id.tvDialogTitle);
        TextInputEditText etTitulo = view.findViewById(R.id.etDialogTitulo);
        TextInputEditText etDescripcion = view.findViewById(R.id.etDialogDescripcion);
        TextInputEditText etContenido = view.findViewById(R.id.etDialogContenido);
        TextInputEditText etUrl = view.findViewById(R.id.etDialogUrl);
        MaterialButton btnImagen = view.findViewById(R.id.btnDialogImagen);
        ivVistaPreviaDialogo = view.findViewById(R.id.ivDialogVistaPrevia);

        MaterialButton btnCancelar = view.findViewById(R.id.btnCancelarDialog);
        MaterialButton btnGuardar = view.findViewById(R.id.btnGuardarDialog);

        tvTitle.setText(esOficial ? "Crear Artículo Oficial" : "Publicar en Comunidad");

        btnImagen.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            galeriaLauncher.launch(intent);
        });

        btnCancelar.setOnClickListener(v -> dialog.dismiss());

        btnGuardar.setOnClickListener(v -> {
            String titulo = etTitulo.getText().toString().trim();
            String descripcion = etDescripcion.getText().toString().trim();
            String contenido = etContenido.getText().toString().trim();
            String url = etUrl.getText().toString().trim();

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

            targetRef.child(id).setValue(nuevoArticulo).addOnSuccessListener(aVoid -> {
                Toast.makeText(getContext(), "¡Publicado correctamente!", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });
        });

        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.show();
    }

    private void mostrarDialogoEditarArticulo(Articulo articulo) {
        imagenSeleccionadaBase64 = articulo.getImagenPortadaBase64();

        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_agregar_articulo, null);
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(view)
                .create();

        TextView tvTitle = view.findViewById(R.id.tvDialogTitle);
        TextInputEditText etTitulo = view.findViewById(R.id.etDialogTitulo);
        TextInputEditText etDescripcion = view.findViewById(R.id.etDialogDescripcion);
        TextInputEditText etContenido = view.findViewById(R.id.etDialogContenido);
        TextInputEditText etUrl = view.findViewById(R.id.etDialogUrl);
        MaterialButton btnImagen = view.findViewById(R.id.btnDialogImagen);
        ivVistaPreviaDialogo = view.findViewById(R.id.ivDialogVistaPrevia);

        MaterialButton btnCancelar = view.findViewById(R.id.btnCancelarDialog);
        MaterialButton btnGuardar = view.findViewById(R.id.btnGuardarDialog);

        tvTitle.setText("Editar Mi Artículo");
        btnImagen.setText("Cambiar Imagen");
        btnGuardar.setText("Guardar cambios"); // Le cambiamos el texto al botón desde código

        etTitulo.setText(articulo.getTitulo());
        etDescripcion.setText(articulo.getDescripcionCorta());
        etContenido.setText(articulo.getContenidoCompleto());
        etUrl.setText(articulo.getUrlEnlace());

        if (imagenSeleccionadaBase64 != null && !imagenSeleccionadaBase64.isEmpty()) {
            try {
                byte[] decodedString = Base64.decode(imagenSeleccionadaBase64, Base64.DEFAULT);
                Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                ivVistaPreviaDialogo.setImageBitmap(decodedByte);
                ivVistaPreviaDialogo.setVisibility(View.VISIBLE);
            } catch (Exception e) { e.printStackTrace(); }
        }

        btnImagen.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            galeriaLauncher.launch(intent);
        });

        btnCancelar.setOnClickListener(v -> dialog.dismiss());

        btnGuardar.setOnClickListener(v -> {
            String nuevoTitulo = etTitulo.getText().toString().trim();
            String nuevaDesc = etDescripcion.getText().toString().trim();
            String nuevoContenido = etContenido.getText().toString().trim();
            String nuevaUrl = etUrl.getText().toString().trim();

            if (nuevoTitulo.isEmpty() || nuevaDesc.isEmpty() || nuevoContenido.isEmpty()) {
                Toast.makeText(getContext(), "Rellena los campos obligatorios", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> updates = new HashMap<>();
            updates.put("titulo", nuevoTitulo);
            updates.put("descripcionCorta", nuevaDesc);
            updates.put("contenidoCompleto", nuevoContenido);
            updates.put("urlEnlace", nuevaUrl);
            updates.put("imagenPortadaBase64", imagenSeleccionadaBase64);

            comunidadRef.child(articulo.getId()).updateChildren(updates)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(getContext(), "¡Artículo actualizado!", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    });
        });

        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.show();
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

        scrollView.addView(layout);
        bottomSheet.setContentView(scrollView);
        bottomSheet.getBehavior().setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED);
        bottomSheet.show();
    }

    // --------------------------------------------------------
    // ADAPTER
    // --------------------------------------------------------
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
                    holder.ivIcono.setImageTintList(null);
                } catch (Exception e) {
                    holder.ivIcono.setImageResource(articulo.isEsOficial() ? R.drawable.consejos : R.drawable.huella);
                }
            } else {
                holder.ivIcono.setImageResource(articulo.isEsOficial() ? R.drawable.consejos : R.drawable.huella);
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