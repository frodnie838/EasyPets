package com.easypets.ui.main;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.format.DateUtils;
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
import com.easypets.models.HiloForo;
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

    private RecyclerView rvContenido;
    private TabLayout tabLayout;
    private FloatingActionButton fabAgregar;

    // Adaptadores y Listas
    private ArticuloAdapter articuloAdapter;
    private ForoAdapter foroAdapter;
    private List<Articulo> listaArticulos;
    private List<HiloForo> listaHilos;

    private DatabaseReference comunidadRef, oficialesRef, usuariosRef, foroRef;
    private static FirebaseUser currentUser;
    private ValueEventListener contenidoListener;
    private Query activeQuery;

    private String rolUsuario = "usuario";
    private String miNick = "Usuario"; // ✨ Guardaremos aquí el nick del usuario

    private String imagenSeleccionadaBase64 = "";
    private ImageView ivVistaPreviaDialogo;
    private ActivityResultLauncher<Intent> galeriaLauncher;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_educacion, container, false);

        rvContenido = view.findViewById(R.id.rvArticulos);
        tabLayout = view.findViewById(R.id.tabLayoutEducacion);
        fabAgregar = view.findViewById(R.id.fabAgregarArticulo);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        FirebaseDatabase db = FirebaseDatabase.getInstance();
        comunidadRef = db.getReference("articulos_comunidad");
        oficialesRef = db.getReference("articulos_oficiales");
        usuariosRef = db.getReference("usuarios");
        foroRef = db.getReference("foro_hilos"); // ✨ Nueva referencia al foro

        listaArticulos = new ArrayList<>();
        listaHilos = new ArrayList<>();

        // Inicializamos los dos adaptadores
        articuloAdapter = new ArticuloAdapter(listaArticulos, articulo -> {
            if (tabLayout.getSelectedTabPosition() == 3) {
                mostrarOpcionesMisArticulos(articulo);
            } else {
                mostrarArticuloCompleto(articulo);
            }
        });

        foroAdapter = new ForoAdapter(listaHilos, hilo -> {
            // TODO: En el siguiente paso abriremos la pantalla de respuestas
            Toast.makeText(getContext(), "Abriendo hilo: " + hilo.getTitulo(), Toast.LENGTH_SHORT).show();
        });

        rvContenido.setLayoutManager(new LinearLayoutManager(getContext()));

        obtenerDatosUsuario();
        configurarPestanas();

        // ✨ El botón + decide qué hacer según la pestaña
        fabAgregar.setOnClickListener(v -> {
            if (tabLayout.getSelectedTabPosition() == 2) {
                mostrarDialogoCrearHilo();
            } else {
                mostrarDialogoCrearArticulo();
            }
        });

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
                            Toast.makeText(getContext(), "Error al cargar", Toast.LENGTH_SHORT).show();
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
        if (bitmapRatio > 1) { width = maxSize; height = (int) (width / bitmapRatio);
        } else { height = maxSize; width = (int) (height * bitmapRatio); }
        return Bitmap.createScaledBitmap(image, width, height, true);
    }

    private void obtenerDatosUsuario() {
        if (currentUser != null) {
            usuariosRef.child(currentUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        if (snapshot.hasChild("rol")) rolUsuario = snapshot.child("rol").getValue(String.class);
                        if (snapshot.hasChild("nick")) miNick = snapshot.child("nick").getValue(String.class);
                    }
                    actualizarVisibilidadFab();
                }
                @Override public void onCancelled(@NonNull DatabaseError error) {}
            });
        }
    }

    private void configurarPestanas() {
        tabLayout.removeAllTabs();
        tabLayout.addTab(tabLayout.newTab().setText("Oficiales"));
        tabLayout.addTab(tabLayout.newTab().setText("Comunidad"));
        tabLayout.addTab(tabLayout.newTab().setText("Foro")); // ✨ Nueva pestaña
        tabLayout.addTab(tabLayout.newTab().setText("Mis Artículos"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                detenerListener();
                actualizarVisibilidadFab();

                int pos = tab.getPosition();
                if (pos == 0) {
                    rvContenido.setAdapter(articuloAdapter);
                    cargarArticulos(oficialesRef, "Aún no hay artículos oficiales.");
                } else if (pos == 1) {
                    rvContenido.setAdapter(articuloAdapter);
                    cargarArticulos(comunidadRef, "Aún no hay artículos en la comunidad.");
                } else if (pos == 2) {
                    // ✨ Si estamos en el foro, cambiamos el adaptador y cargamos hilos
                    rvContenido.setAdapter(foroAdapter);
                    cargarHilosForo();
                } else if (pos == 3) {
                    rvContenido.setAdapter(articuloAdapter);
                    if (currentUser != null) {
                        Query misArticulos = comunidadRef.orderByChild("idAutor").equalTo(currentUser.getUid());
                        cargarArticulos(misArticulos, "Aún no has escrito ningún artículo.");
                    }
                }
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        cargarArticulos(oficialesRef, "Aún no hay artículos oficiales.");
        rvContenido.setAdapter(articuloAdapter);
    }

    private void actualizarVisibilidadFab() {
        int pos = tabLayout.getSelectedTabPosition();
        if (pos == 0) {
            fabAgregar.setVisibility("admin".equals(rolUsuario) ? View.VISIBLE : View.GONE);
        } else if (pos == 1) {
            fabAgregar.setVisibility(View.GONE);
        } else if (pos == 2 || pos == 3) {
            fabAgregar.setVisibility(currentUser != null ? View.VISIBLE : View.GONE);
        }
    }

    // --------------------------------------------------------
    // MÉTODOS DE CARGA (ARTÍCULOS Y FORO)
    // --------------------------------------------------------

    private void cargarArticulos(Query query, String mensajeVacio) {
        listaArticulos.clear();
        articuloAdapter.notifyDataSetChanged();
        activeQuery = query;
        contenidoListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                listaArticulos.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    Articulo articulo = data.getValue(Articulo.class);
                    if (articulo != null) listaArticulos.add(articulo);
                }
                Collections.sort(listaArticulos, (a1, a2) -> Long.compare(a2.getTimestampCreacion(), a1.getTimestampCreacion()));
                articuloAdapter.notifyDataSetChanged();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) { }
        };
        activeQuery.addValueEventListener(contenidoListener);
    }

    private void cargarHilosForo() {
        listaHilos.clear();
        foroAdapter.notifyDataSetChanged();
        activeQuery = foroRef;
        contenidoListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                listaHilos.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    HiloForo hilo = data.getValue(HiloForo.class);
                    if (hilo != null) listaHilos.add(hilo);
                }
                // Los más recientes primero
                Collections.sort(listaHilos, (h1, h2) -> Long.compare(h2.getTimestampCreacion(), h1.getTimestampCreacion()));
                foroAdapter.notifyDataSetChanged();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) { }
        };
        activeQuery.addValueEventListener(contenidoListener);
    }

    private void detenerListener() {
        if (contenidoListener != null && activeQuery != null) {
            activeQuery.removeEventListener(contenidoListener);
            contenidoListener = null;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        detenerListener();
    }

    // --------------------------------------------------------
    // CREAR HILO DEL FORO
    // --------------------------------------------------------

    private void mostrarDialogoCrearHilo() {
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_agregar_hilo, null);
        AlertDialog dialog = new AlertDialog.Builder(requireContext()).setView(view).create();

        TextInputEditText etTitulo = view.findViewById(R.id.etTituloHilo);
        TextInputEditText etDescripcion = view.findViewById(R.id.etDescripcionHilo);
        MaterialButton btnCancelar = view.findViewById(R.id.btnCancelarHilo);
        MaterialButton btnPublicar = view.findViewById(R.id.btnPublicarHilo);

        btnCancelar.setOnClickListener(v -> dialog.dismiss());

        btnPublicar.setOnClickListener(v -> {
            String titulo = etTitulo.getText().toString().trim();
            String descripcion = etDescripcion.getText().toString().trim();

            if (titulo.isEmpty()) {
                Toast.makeText(getContext(), "El título es obligatorio", Toast.LENGTH_SHORT).show();
                return;
            }

            String id = foroRef.push().getKey();
            HiloForo nuevoHilo = new HiloForo(id, titulo, descripcion, currentUser.getUid(), miNick, System.currentTimeMillis());

            foroRef.child(id).setValue(nuevoHilo).addOnSuccessListener(aVoid -> {
                Toast.makeText(getContext(), "Hilo publicado en el foro", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });
        });

        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.show();
    }

    // --------------------------------------------------------
    // GESTIÓN DE ARTÍCULOS (TUS MÉTODOS ORIGINALES)
    // --------------------------------------------------------

    private void mostrarDialogoCrearArticulo() {
        boolean esOficial = (tabLayout.getSelectedTabPosition() == 0);
        imagenSeleccionadaBase64 = "";

        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_agregar_articulo, null);
        AlertDialog dialog = new AlertDialog.Builder(requireContext()).setView(view).create();

        TextView tvTitle = view.findViewById(R.id.tvDialogTitle);
        TextInputEditText etTitulo = view.findViewById(R.id.etDialogTitulo);
        TextInputEditText etDescripcion = view.findViewById(R.id.etDialogDescripcion);
        TextInputEditText etContenido = view.findViewById(R.id.etDialogContenido);
        TextInputEditText etUrl = view.findViewById(R.id.etDialogUrl);
        MaterialButton btnImagen = view.findViewById(R.id.btnDialogImagen);
        ivVistaPreviaDialogo = view.findViewById(R.id.ivDialogVistaPrevia);

        MaterialButton btnCancelar = view.findViewById(R.id.btnCancelarDialog);
        MaterialButton btnGuardar = view.findViewById(R.id.btnGuardarDialog);

        tvTitle.setText(esOficial ? "Crear Artículo Oficial" : "Publicar Artículo");

        btnImagen.setOnClickListener(v -> galeriaLauncher.launch(new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)));
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

            String autor = esOficial ? "EasyPets Oficial" : "@" + miNick;
            String idAutor = currentUser != null ? currentUser.getUid() : "";
            DatabaseReference targetRef = esOficial ? oficialesRef : comunidadRef;
            String id = targetRef.push().getKey();

            Articulo nuevoArticulo = new Articulo(id, idAutor, titulo, descripcion, contenido, autor, System.currentTimeMillis(), imagenSeleccionadaBase64, url, esOficial);

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
        AlertDialog dialog = new AlertDialog.Builder(requireContext()).setView(view).create();

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
        btnGuardar.setText("Guardar cambios");

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

        btnImagen.setOnClickListener(v -> galeriaLauncher.launch(new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)));
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

            comunidadRef.child(articulo.getId()).updateChildren(updates).addOnSuccessListener(aVoid -> {
                Toast.makeText(getContext(), "¡Artículo actualizado!", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });
        });

        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.show();
    }

    private void mostrarOpcionesMisArticulos(Articulo articulo) {
        BottomSheetDialog bottomSheet = new BottomSheetDialog(requireContext());
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_opciones_articulo, null);
        bottomSheet.setContentView(view);

        ((View) view.getParent()).setBackgroundColor(android.graphics.Color.TRANSPARENT);

        view.findViewById(R.id.btnOpcionVer).setOnClickListener(v -> {
            bottomSheet.dismiss();
            mostrarArticuloCompleto(articulo);
        });

        view.findViewById(R.id.btnOpcionEditar).setOnClickListener(v -> {
            bottomSheet.dismiss();
            mostrarDialogoEditarArticulo(articulo);
        });

        view.findViewById(R.id.btnOpcionEliminar).setOnClickListener(v -> {
            bottomSheet.dismiss();
            new AlertDialog.Builder(requireContext())
                    .setTitle("¿Eliminar artículo?")
                    .setMessage("Esta acción no se puede deshacer.")
                    .setPositiveButton("Eliminar", (d, w) -> comunidadRef.child(articulo.getId()).removeValue())
                    .setNegativeButton("Cancelar", null)
                    .show();
        });

        bottomSheet.show();
    }

    private void mostrarArticuloCompleto(Articulo articulo) {
        BottomSheetDialog bottomSheet = new BottomSheetDialog(requireContext());
        androidx.core.widget.NestedScrollView scrollView = new androidx.core.widget.NestedScrollView(requireContext());
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
                ivPortada.setImageBitmap(BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length));
                layout.addView(ivPortada);
            } catch (Exception e) { e.printStackTrace(); }
        }

        TextView tvTitulo = new TextView(requireContext());
        tvTitulo.setText(articulo.getTitulo());
        tvTitulo.setTextSize(22f);
        tvTitulo.setTypeface(null, android.graphics.Typeface.BOLD);
        tvTitulo.setTextColor(getResources().getColor(R.color.black, null));

        TextView tvAutorInfo = new TextView(requireContext());
        String fecha = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(new Date(articulo.getTimestampCreacion()));
        tvAutorInfo.setText(articulo.isEsOficial() ? "✓ Oficial • " + fecha : "Por: " + articulo.getAutor() + " • " + fecha);
        tvAutorInfo.setTextColor(getResources().getColor(R.color.color_acento_primario, null));
        tvAutorInfo.setPadding(0, 10, 0, 40);

        TextView tvContenido = new TextView(requireContext());
        tvContenido.setText(articulo.getContenidoCompleto());
        tvContenido.setTextSize(16f);

        layout.addView(tvTitulo);
        layout.addView(tvAutorInfo);
        layout.addView(tvContenido);

        if (articulo.getUrlEnlace() != null && !articulo.getUrlEnlace().isEmpty()) {
            MaterialButton btnLink = new MaterialButton(requireContext());
            btnLink.setText("Ver Vídeo / Web");
            btnLink.setOnClickListener(v -> {
                String link = articulo.getUrlEnlace();
                if (!link.startsWith("http")) link = "https://" + link;
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(link)));
            });
            layout.addView(btnLink);
        }

        scrollView.addView(layout);
        bottomSheet.setContentView(scrollView);
        bottomSheet.show();
    }

    // --------------------------------------------------------
    // ADAPTADORES (ARTÍCULOS Y FORO)
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
                    holder.ivIcono.setImageBitmap(BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length));
                    holder.ivIcono.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    holder.ivIcono.setImageTintList(null);
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

    // ✨ ADAPTADOR PARA EL FORO (Usando tu lógica de hilos secundarios)
    static class ForoAdapter extends RecyclerView.Adapter<ForoAdapter.ForoViewHolder> {
        private List<HiloForo> hilos;
        private OnHiloClickListener listener;
        public interface OnHiloClickListener { void onHiloClick(HiloForo hilo); }

        public ForoAdapter(List<HiloForo> hilos, OnHiloClickListener listener) {
            this.hilos = hilos;
            this.listener = listener;
        }

        @NonNull @Override
        public ForoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_hilo, parent, false);
            return new ForoViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ForoViewHolder holder, int position) {
            HiloForo hilo = hilos.get(position);
            holder.tvTitulo.setText(hilo.getTitulo());

            if (hilo.getDescripcion() != null && !hilo.getDescripcion().isEmpty()) {
                holder.tvDescripcion.setText(hilo.getDescripcion());
                holder.tvDescripcion.setVisibility(View.VISIBLE);
            } else {
                holder.tvDescripcion.setVisibility(View.GONE);
            }

            CharSequence tiempoTranscurrido = android.text.format.DateUtils.getRelativeTimeSpanString(
                    hilo.getTimestampCreacion(), System.currentTimeMillis(), android.text.format.DateUtils.MINUTE_IN_MILLIS);
            holder.tvFecha.setText(" • " + tiempoTranscurrido);

            // Valores por defecto mientras carga
            holder.tvAutor.setText("Cargando...");
            holder.ivAvatar.setImageResource(R.drawable.profile);
            holder.ivAvatar.setImageTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#9E9E9E")));
            holder.ivAvatar.setPadding(10, 10, 10, 10);

            FirebaseDatabase.getInstance().getReference("usuarios").child(hilo.getIdAutor())
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.exists()) {
                                // 1. Ponemos su Nick actual
                                String nick = snapshot.child("nick").getValue(String.class);
                                if (nick != null && !nick.isEmpty()) {
                                    holder.tvAutor.setText("@" + nick);
                                } else {
                                    String nombre = snapshot.child("nombre").getValue(String.class);
                                    holder.tvAutor.setText(nombre != null ? nombre : "Usuario");
                                }

                                // 2. Lógica de la Foto (Un solo campo: fotoPerfil)
                                String foto = snapshot.child("fotoPerfil").getValue(String.class);

                                if (foto != null && !foto.isEmpty()) {
                                    if (foto.startsWith("http")) {
                                        // ✨ CASO A: Es una URL (de Google). La descargamos con tu hilo secundario.
                                        cargarFotoGoogle(foto, holder.ivAvatar);
                                    } else {
                                        // ✨ CASO B: Es Base64 (de la galería). La decodificamos.
                                        try {
                                            byte[] decodedString = Base64.decode(foto, Base64.DEFAULT);
                                            Bitmap bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                                            holder.ivAvatar.setImageBitmap(bitmap);
                                            holder.ivAvatar.setPadding(0, 0, 0, 0);
                                            holder.ivAvatar.setImageTintList(null);
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                            // Si falla la decodificación, ponemos la por defecto
                                            holder.ivAvatar.setImageResource(R.drawable.profile);
                                        }
                                    }
                                } else {
                                    // ✨ CASO C: No tiene foto de ningún tipo
                                    holder.ivAvatar.setImageResource(R.drawable.profile);
                                    holder.ivAvatar.setPadding(10, 10, 10, 10);
                                    holder.ivAvatar.setImageTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#9E9E9E")));
                                }
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {}
                    });

            holder.itemView.setOnClickListener(v -> listener.onHiloClick(hilo));
        }

        @Override public int getItemCount() { return hilos.size(); }

        private void cargarFotoGoogle(String urlImagen, ImageView targetImageView) {
            new Thread(() -> {
                try {
                    java.io.InputStream in = new java.net.URL(urlImagen).openStream();
                    Bitmap foto = BitmapFactory.decodeStream(in);

                    if (targetImageView != null) {
                        targetImageView.post(() -> {
                            targetImageView.setImageBitmap(foto);
                            targetImageView.setPadding(0, 0, 0, 0);
                            targetImageView.setImageTintList(null);
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }

        class ForoViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitulo, tvDescripcion, tvAutor, tvFecha;
            ImageView ivAvatar;

            public ForoViewHolder(@NonNull View itemView) {
                super(itemView);
                tvTitulo = itemView.findViewById(R.id.tvTituloHilo);
                tvDescripcion = itemView.findViewById(R.id.tvDescripcionHilo);
                tvAutor = itemView.findViewById(R.id.tvAutorHilo);
                tvFecha = itemView.findViewById(R.id.tvFechaHilo);
                ivAvatar = itemView.findViewById(R.id.ivAvatarHilo);
            }
        }
    }
}