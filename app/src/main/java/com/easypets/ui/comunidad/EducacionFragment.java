package com.easypets.ui.comunidad;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
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
import com.easypets.adapters.ArticuloAdapter;
import com.easypets.adapters.ComentarioMascotaAdapter;
import com.easypets.adapters.ForoAdapter;
import com.easypets.adapters.GaleriaAdapter;
import com.easypets.models.Articulo;
import com.easypets.models.ComentarioMascota;
import com.easypets.models.HiloForo;
import com.easypets.models.PublicacionMascota;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
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

    private ChipGroup chipGroupMisPublicaciones, chipGroupComunidad;

    private ProgressBar pbCargando;
    private EditText etBuscador;

    private TextWatcher buscadorWatcher;

    private ArticuloAdapter articuloAdapter;
    private ForoAdapter foroAdapter;
    private GaleriaAdapter galeriaAdapter;

    private List<Articulo> listaArticulos;
    private List<Articulo> listaArticulosOriginal;
    private List<HiloForo> listaHilos;
    private List<HiloForo> listaHilosOriginal;
    private List<PublicacionMascota> listaGaleria;

    private DatabaseReference comunidadRef, oficialesRef, usuariosRef, foroRef, galeriaRef, comentariosMascotasRef;
    private static FirebaseUser currentUser;
    private ValueEventListener contenidoListener;
    private Query activeQuery;

    private String rolUsuario = "usuario";
    private String miNick = "Usuario";
    private String miFotoPerfil = "";

    private String imagenSeleccionadaBase64 = "";
    private ImageView ivVistaPreviaDialogo;
    private ActivityResultLauncher<Intent> galeriaLauncher;
    private LinearLayout layoutEmptyState;

    private static int tabGuardada = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_educacion, container, false);

        rvContenido = view.findViewById(R.id.rvArticulos);
        tabLayout = view.findViewById(R.id.tabLayoutEducacion);
        fabAgregar = view.findViewById(R.id.fabAgregarArticulo);
        chipGroupMisPublicaciones = view.findViewById(R.id.chipGroupMisPublicaciones);
        chipGroupComunidad = view.findViewById(R.id.chipGroupComunidad);
        pbCargando = view.findViewById(R.id.pbCargando);
        layoutEmptyState = view.findViewById(R.id.layoutEmptyState);

        if (getActivity() != null) {
            etBuscador = getActivity().findViewById(R.id.etTopSearch);
        }

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        FirebaseDatabase db = FirebaseDatabase.getInstance();
        comunidadRef = db.getReference("articulos_comunidad");
        oficialesRef = db.getReference("articulos_oficiales");
        usuariosRef = db.getReference("usuarios");
        foroRef = db.getReference("foro_hilos");
        galeriaRef = db.getReference("mascotas_comunidad");
        comentariosMascotasRef = db.getReference("mascotas_comentarios");

        listaArticulos = new ArrayList<>();
        listaArticulosOriginal = new ArrayList<>();
        listaHilos = new ArrayList<>();
        listaHilosOriginal = new ArrayList<>();
        listaGaleria = new ArrayList<>();

        articuloAdapter = new ArticuloAdapter(listaArticulos, new ArticuloAdapter.OnArticuloClickListener() {
            @Override public void onArticuloClick(Articulo articulo) { mostrarArticuloCompleto(articulo); }
            @Override public void onEditarClick(Articulo articulo) { mostrarDialogoEditarArticulo(articulo); }
            @Override public void onBorrarClick(Articulo articulo) { confirmarBorradoArticulo(articulo); }
        });

        foroAdapter = new ForoAdapter(listaHilos, new ForoAdapter.OnHiloAccionListener() {
            @Override
            public void onHiloClick(HiloForo hilo) {
                HiloDetalleFragment detalleFragment = new HiloDetalleFragment();
                Bundle argsDetalle = new Bundle();
                argsDetalle.putString("hiloId", hilo.getId());
                argsDetalle.putString("titulo", hilo.getTitulo());
                argsDetalle.putString("descripcion", hilo.getDescripcion());
                argsDetalle.putString("idAutor", hilo.getIdAutor());
                argsDetalle.putLong("timestamp", hilo.getTimestampCreacion());
                detalleFragment.setArguments(argsDetalle);

                if (getActivity() != null) {
                    getActivity().getSupportFragmentManager().beginTransaction()
                            .replace(R.id.frame_container, detalleFragment)
                            .addToBackStack(null)
                            .commit();
                }
            }
            @Override public void onEditarClick(HiloForo hilo) { mostrarDialogoEditarHilo(hilo); }
            @Override public void onBorrarClick(HiloForo hilo) { confirmarBorradoHilo(hilo); }
        });

        galeriaAdapter = new GaleriaAdapter(listaGaleria, new GaleriaAdapter.OnGaleriaClickListener() {
            @Override
            public void onLikeClick(PublicacionMascota publicacion) {
                if (currentUser == null) {
                    Toast.makeText(getContext(), "Regístrate para dar Me gusta ❤️", Toast.LENGTH_SHORT).show();
                    return;
                }

                DatabaseReference likeRef = galeriaRef.child(publicacion.getId()).child("likes").child(currentUser.getUid());
                if (publicacion.getLikes() != null && publicacion.getLikes().containsKey(currentUser.getUid())) {
                    likeRef.removeValue();
                } else {
                    likeRef.setValue(true);
                }
            }

            @Override
            public void onComentarClick(PublicacionMascota publicacion) {
                if (currentUser == null) {
                    Toast.makeText(getContext(), "Regístrate para comentar 💬", Toast.LENGTH_SHORT).show();
                    return;
                }
                mostrarBottomSheetComentarios(publicacion);
            }

            @Override
            public void onOpcionesClick(PublicacionMascota publicacion, View anchorView) {
                CharSequence[] opciones = {"Editar descripción", "Eliminar publicación"};
                new AlertDialog.Builder(requireContext())
                        .setTitle("Opciones")
                        .setItems(opciones, (dialog, which) -> {
                            if (which == 0) mostrarDialogoEditarMascota(publicacion);
                            else confirmarBorradoMascota(publicacion);
                        })
                        .show();
            }
        });

        rvContenido.setLayoutManager(new LinearLayoutManager(getContext()));
        obtenerDatosUsuario();
        configurarPestanas();

        if (etBuscador != null) {
            buscadorWatcher = new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    aplicarFiltroActual();
                }
                @Override public void afterTextChanged(Editable s) {}
            };
            etBuscador.addTextChangedListener(buscadorWatcher);
        }

        fabAgregar.setOnClickListener(v -> {
            TabLayout.Tab tab = tabLayout.getTabAt(tabLayout.getSelectedTabPosition());
            if (tab != null && tab.getText() != null) {
                String titulo = tab.getText().toString();
                if (titulo.equals("Foro")) {
                    mostrarDialogoCrearHilo();
                } else if (titulo.equals("Mis Publicaciones")) {
                    int selectedChipId = chipGroupMisPublicaciones.getCheckedChipId();
                    if (selectedChipId == R.id.chipMisArticulos) {
                        mostrarDialogoCrearArticulo();
                    } else if (selectedChipId == R.id.chipMisHilos) {
                        mostrarDialogoCrearHilo();
                    } else if (selectedChipId == R.id.chipMisMascotas) {
                        mostrarDialogoSubirMascota();
                    }
                } else if (titulo.equals("Oficiales")) {
                    mostrarDialogoCrearArticulo();
                }
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
                                ivVistaPreviaDialogo.setPadding(0, 0, 0, 0);
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

        if (getArguments() != null && getArguments().containsKey("abrirPublicacionId")) {
            String pubId = getArguments().getString("abrirPublicacionId");

            if (pubId != null && !pubId.trim().isEmpty()) {

                TabLayout.Tab tabComunidad = tabLayout.getTabAt(1);
                if (tabComunidad != null) {
                    tabComunidad.select();
                }

                chipGroupComunidad.check(R.id.chipComunidadMascotas);

                galeriaRef.child(pubId).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        PublicacionMascota pub = snapshot.getValue(PublicacionMascota.class);
                        if (pub != null && isAdded()) {
                            mostrarBottomSheetComentarios(pub);
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
            }
        }
        return view;
    }

    private void configurarPestanas() {
        tabLayout.removeAllTabs();
        tabLayout.addTab(tabLayout.newTab().setText("Oficiales"));
        tabLayout.addTab(tabLayout.newTab().setText("Comunidad"));
        tabLayout.addTab(tabLayout.newTab().setText("Foro"));
        if (currentUser != null) tabLayout.addTab(tabLayout.newTab().setText("Mis Publicaciones"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                tabGuardada = tab.getPosition();
                detenerListener();
                actualizarVisibilidadFab();
                String titulo = tab.getText().toString();

                if (etBuscador != null) etBuscador.setText("");

                listaArticulos.clear();
                listaHilos.clear();
                listaGaleria.clear();
                articuloAdapter.notifyDataSetChanged();
                foroAdapter.notifyDataSetChanged();
                galeriaAdapter.notifyDataSetChanged();

                if (titulo.equals("Mis Publicaciones")) {
                    chipGroupMisPublicaciones.setVisibility(View.VISIBLE);
                    chipGroupComunidad.setVisibility(View.GONE);
                    actualizarListaMisPublicaciones();

                } else if (titulo.equals("Comunidad")) {
                    chipGroupMisPublicaciones.setVisibility(View.GONE);
                    chipGroupComunidad.setVisibility(View.VISIBLE);
                    actualizarListaComunidad();

                } else {
                    chipGroupMisPublicaciones.setVisibility(View.GONE);
                    chipGroupComunidad.setVisibility(View.GONE);
                    articuloAdapter.setMostrarOpciones(false);
                    foroAdapter.setMostrarOpciones(false);
                    galeriaAdapter.setMostrarOpciones(false);

                    if (titulo.equals("Oficiales")) {
                        rvContenido.setAdapter(articuloAdapter);
                        cargarArticulos(oficialesRef, "");
                    } else if (titulo.equals("Foro")) {
                        rvContenido.setAdapter(foroAdapter);
                        cargarHilos(foroRef);
                    }
                }
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {
                onTabSelected(tab);
            }
        });

        chipGroupMisPublicaciones.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!checkedIds.isEmpty()) {
                if (etBuscador != null) etBuscador.setText("");
                actualizarListaMisPublicaciones();
            }
        });

        chipGroupComunidad.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!checkedIds.isEmpty()) {
                if (etBuscador != null) etBuscador.setText("");
                actualizarListaComunidad();
            }
        });

        TabLayout.Tab tabToSelect = tabLayout.getTabAt(tabGuardada);
        if (tabToSelect != null) {
            tabToSelect.select();
        }
    }

    private void actualizarListaMisPublicaciones() {
        TabLayout.Tab tab = tabLayout.getTabAt(tabLayout.getSelectedTabPosition());
        if (tab == null || tab.getText() == null || !tab.getText().toString().equals("Mis Publicaciones")) {
            return;
        }

        detenerListener();
        if (currentUser == null) return;
        int selectedChipId = chipGroupMisPublicaciones.getCheckedChipId();

        if (selectedChipId == R.id.chipMisArticulos) {
            articuloAdapter.setMostrarOpciones(true);
            rvContenido.setAdapter(articuloAdapter);
            cargarArticulos(comunidadRef.orderByChild("idAutor").equalTo(currentUser.getUid()), "");
        } else if (selectedChipId == R.id.chipMisHilos) {
            foroAdapter.setMostrarOpciones(true);
            rvContenido.setAdapter(foroAdapter);
            cargarHilos(foroRef.orderByChild("idAutor").equalTo(currentUser.getUid()));
        } else if (selectedChipId == R.id.chipMisMascotas) {
            galeriaAdapter.setMostrarOpciones(true);
            rvContenido.setAdapter(galeriaAdapter);
            cargarMisMascotas();
        }
    }

    private void actualizarListaComunidad() {
        TabLayout.Tab tab = tabLayout.getTabAt(tabLayout.getSelectedTabPosition());
        if (tab == null || tab.getText() == null || !tab.getText().toString().equals("Comunidad")) {
            return;
        }

        detenerListener();
        int selectedChipId = chipGroupComunidad.getCheckedChipId();
        articuloAdapter.setMostrarOpciones(false);
        galeriaAdapter.setMostrarOpciones(false);

        if (selectedChipId == R.id.chipComunidadArticulos) {
            rvContenido.setAdapter(articuloAdapter);
            cargarArticulos(comunidadRef, "");
        } else if (selectedChipId == R.id.chipComunidadMascotas) {
            rvContenido.setAdapter(galeriaAdapter);
            cargarGaleria();
        }
    }

    private void aplicarFiltroActual() {
        if (etBuscador == null) return;
        String texto = etBuscador.getText().toString().toLowerCase().trim();

        if (rvContenido.getAdapter() == articuloAdapter) {
            listaArticulos.clear();
            if (texto.isEmpty()) {
                listaArticulos.addAll(listaArticulosOriginal);
            } else {
                for (Articulo a : listaArticulosOriginal) {
                    if (a.getTitulo().toLowerCase().contains(texto)) {
                        listaArticulos.add(a);
                    }
                }
            }
            articuloAdapter.notifyDataSetChanged();

        } else if (rvContenido.getAdapter() == foroAdapter) {
            listaHilos.clear();
            if (texto.isEmpty()) {
                listaHilos.addAll(listaHilosOriginal);
            } else {
                for (HiloForo h : listaHilosOriginal) {
                    if (h.getTitulo().toLowerCase().contains(texto)) {
                        listaHilos.add(h);
                    }
                }
            }
            foroAdapter.notifyDataSetChanged();
        }
    }

    private void cargarGaleria() {
        listaGaleria.clear();
        galeriaAdapter.notifyDataSetChanged();
        pbCargando.setVisibility(View.VISIBLE);

        activeQuery = galeriaRef.orderByChild("timestamp");
        contenidoListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                listaGaleria.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    PublicacionMascota p = data.getValue(PublicacionMascota.class);
                    if (p != null) listaGaleria.add(p);
                }
                Collections.reverse(listaGaleria);
                galeriaAdapter.notifyDataSetChanged();
                pbCargando.setVisibility(View.GONE);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) { pbCargando.setVisibility(View.GONE); }
        };
        activeQuery.addValueEventListener(contenidoListener);
    }

    private void cargarMisMascotas() {
        listaGaleria.clear();
        galeriaAdapter.notifyDataSetChanged();
        pbCargando.setVisibility(View.VISIBLE);

        activeQuery = galeriaRef.orderByChild("idAutor").equalTo(currentUser.getUid());
        contenidoListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                listaGaleria.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    PublicacionMascota p = data.getValue(PublicacionMascota.class);
                    if (p != null) listaGaleria.add(p);
                }
                // Como filtramos por autor, la base de datos no lo ordena por fecha. Lo hacemos nosotros:
                Collections.sort(listaGaleria, (p1, p2) -> Long.compare(p2.getTimestamp(), p1.getTimestamp()));

                galeriaAdapter.notifyDataSetChanged();
                pbCargando.setVisibility(View.GONE);
                comprobarEmptyState(listaGaleria.isEmpty());
            }
            @Override public void onCancelled(@NonNull DatabaseError error) { pbCargando.setVisibility(View.GONE); }
        };
        activeQuery.addValueEventListener(contenidoListener);
    }

    private void cargarArticulos(Query query, String mensajeVacio) {
        listaArticulosOriginal.clear();
        listaArticulos.clear();
        articuloAdapter.notifyDataSetChanged();
        pbCargando.setVisibility(View.VISIBLE);

        activeQuery = query;
        contenidoListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                listaArticulosOriginal.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    Articulo a = data.getValue(Articulo.class);
                    if (a != null) listaArticulosOriginal.add(a);
                }
                Collections.sort(listaArticulosOriginal, (a1, a2) -> Long.compare(a2.getTimestampCreacion(), a1.getTimestampCreacion()));
                aplicarFiltroActual();
                pbCargando.setVisibility(View.GONE);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) { pbCargando.setVisibility(View.GONE); }
        };
        activeQuery.addValueEventListener(contenidoListener);
    }

    private void cargarHilos(Query query) {
        listaHilosOriginal.clear();
        listaHilos.clear();
        foroAdapter.notifyDataSetChanged();
        pbCargando.setVisibility(View.VISIBLE);

        activeQuery = query;
        contenidoListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                listaHilosOriginal.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    HiloForo h = data.getValue(HiloForo.class);
                    if (h != null) listaHilosOriginal.add(h);
                }
                Collections.sort(listaHilosOriginal, (h1, h2) -> Long.compare(h2.getTimestampCreacion(), h1.getTimestampCreacion()));
                aplicarFiltroActual();
                pbCargando.setVisibility(View.GONE);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) { pbCargando.setVisibility(View.GONE); }
        };
        activeQuery.addValueEventListener(contenidoListener);
    }

    private void obtenerDatosUsuario() {
        if (currentUser != null) {
            usuariosRef.child(currentUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        if (snapshot.hasChild("rol")) rolUsuario = snapshot.child("rol").getValue(String.class);
                        if (snapshot.hasChild("nick")) miNick = snapshot.child("nick").getValue(String.class);
                        if (snapshot.hasChild("fotoPerfil")) miFotoPerfil = snapshot.child("fotoPerfil").getValue(String.class);
                    }
                    actualizarVisibilidadFab();
                }
                @Override public void onCancelled(@NonNull DatabaseError error) {}
            });
        }
    }

    private void actualizarVisibilidadFab() {
        int pos = tabLayout.getSelectedTabPosition();
        TabLayout.Tab tab = tabLayout.getTabAt(pos);
        if (tab == null || tab.getText() == null) return;
        String titulo = tab.getText().toString();

        if (titulo.equals("Oficiales")) {
            fabAgregar.setVisibility("admin".equals(rolUsuario) ? View.VISIBLE : View.GONE);
        } else if (titulo.equals("Comunidad")) {
            fabAgregar.setVisibility(View.GONE);
        } else {
            fabAgregar.setVisibility(currentUser != null ? View.VISIBLE : View.GONE);
        }
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

        if (etBuscador != null && buscadorWatcher != null) {
            etBuscador.removeTextChangedListener(buscadorWatcher);
        }
    }

    private Bitmap redimensionarImagen(Bitmap image, int maxSize) {
        int width = image.getWidth();
        int height = image.getHeight();
        float bitmapRatio = (float) width / (float) height;
        if (bitmapRatio > 1) { width = maxSize; height = (int) (width / bitmapRatio);
        } else { height = maxSize; width = (int) (height * bitmapRatio); }
        return Bitmap.createScaledBitmap(image, width, height, true);
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
            } catch (Exception e) {}
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
            if (titulo.isEmpty()) { Toast.makeText(getContext(), "El título es obligatorio", Toast.LENGTH_SHORT).show(); return; }
            String id = foroRef.push().getKey();
            HiloForo nuevo = new HiloForo(id, titulo, etDescripcion.getText().toString().trim(), currentUser.getUid(), miNick, System.currentTimeMillis());
            foroRef.child(id).setValue(nuevo).addOnSuccessListener(aVoid -> dialog.dismiss());
        });
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.show();
    }

    private void mostrarDialogoEditarHilo(HiloForo hilo) {
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_agregar_hilo, null);
        AlertDialog dialog = new AlertDialog.Builder(requireContext()).setView(view).create();
        TextInputEditText etTitulo = view.findViewById(R.id.etTituloHilo);
        TextInputEditText etDescripcion = view.findViewById(R.id.etDescripcionHilo);
        MaterialButton btnPublicar = view.findViewById(R.id.btnPublicarHilo);
        etTitulo.setText(hilo.getTitulo());
        etDescripcion.setText(hilo.getDescripcion());
        btnPublicar.setText("Guardar cambios");

        btnPublicar.setOnClickListener(v -> {
            Map<String, Object> updates = new HashMap<>();
            updates.put("titulo", etTitulo.getText().toString().trim());
            updates.put("descripcion", etDescripcion.getText().toString().trim());
            foroRef.child(hilo.getId()).updateChildren(updates).addOnSuccessListener(aVoid -> dialog.dismiss());
        });
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.show();
    }

    private void confirmarBorradoHilo(HiloForo hilo) {
        new AlertDialog.Builder(requireContext()).setTitle("Eliminar hilo").setMessage("¿Borrar hilo y respuestas?")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    foroRef.child(hilo.getId()).removeValue();
                    FirebaseDatabase.getInstance().getReference("foro_respuestas").child(hilo.getId()).removeValue();
                }).setNegativeButton("Cancelar", null).show();
    }

    private void confirmarBorradoArticulo(Articulo articulo) {
        new AlertDialog.Builder(requireContext())
                .setTitle("¿Eliminar artículo?")
                .setMessage("Esta acción no se puede deshacer.")
                .setPositiveButton("Eliminar", (d, w) -> {
                    comunidadRef.child(articulo.getId()).removeValue();
                    Toast.makeText(getContext(), "Artículo eliminado", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void confirmarBorradoMascota(PublicacionMascota pub) {
        new AlertDialog.Builder(requireContext())
                .setTitle("¿Eliminar foto?")
                .setMessage("Esta acción no se puede deshacer.")
                .setPositiveButton("Eliminar", (d, w) -> {
                    galeriaRef.child(pub.getId()).removeValue();
                    // Al borrar la foto, borramos sus comentarios para mantener Firebase limpio
                    comentariosMascotasRef.child(pub.getId()).removeValue();
                    Toast.makeText(getContext(), "Publicación eliminada", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void mostrarDialogoEditarMascota(PublicacionMascota pub) {
        EditText input = new EditText(requireContext());
        input.setText(pub.getDescripcion());
        input.setHint("Escribe una nueva descripción...");
        input.setBackgroundResource(R.drawable.bg_search_bar_top);
        input.setPadding(30, 30, 30, 30);

        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);
        layout.addView(input);

        new AlertDialog.Builder(requireContext())
                .setTitle("Editar descripción")
                .setView(layout)
                .setPositiveButton("Guardar", (dialog, which) -> {
                    String nuevaDesc = input.getText().toString().trim();
                    galeriaRef.child(pub.getId()).child("descripcion").setValue(nuevaDesc)
                            .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Descripción actualizada", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void mostrarDialogoSubirMascota() {
        if (currentUser == null) return;

        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_subir_mascota, null);
        AlertDialog dialog = new AlertDialog.Builder(requireContext()).setView(view).create();

        Spinner spinner = view.findViewById(R.id.spinnerMascotas);
        ImageView ivPreview = view.findViewById(R.id.ivDialogVistaPreviaMascota);
        TextInputEditText etDesc = view.findViewById(R.id.etDialogDescMascota);
        MaterialButton btnPublicar = view.findViewById(R.id.btnGuardarMascota);

        List<com.easypets.models.Mascota> misMascotas = new ArrayList<>();
        List<String> nombresMascotas = new ArrayList<>();

        FirebaseDatabase.getInstance().getReference("mascotas").child(currentUser.getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            com.easypets.models.Mascota m = ds.getValue(com.easypets.models.Mascota.class);
                            if (m != null) {
                                misMascotas.add(m);
                                nombresMascotas.add(m.getNombre());
                            }
                        }

                        if (misMascotas.isEmpty()) {
                            Toast.makeText(getContext(), "Primero debes registrar una mascota en 'Mis Mascotas'", Toast.LENGTH_LONG).show();
                            dialog.dismiss();
                            return;
                        }

                        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, nombresMascotas);
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        spinner.setAdapter(adapter);
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });

        spinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                com.easypets.models.Mascota seleccionada = misMascotas.get(position);
                if (seleccionada.getFotoPerfilUrl() != null && !seleccionada.getFotoPerfilUrl().isEmpty()) {
                    byte[] decodedString = Base64.decode(seleccionada.getFotoPerfilUrl(), Base64.DEFAULT);
                    ivPreview.setImageBitmap(BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length));
                    ivPreview.setPadding(0, 0, 0, 0);
                }
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        btnPublicar.setOnClickListener(v -> {
            int pos = spinner.getSelectedItemPosition();
            if (pos == -1) return;

            com.easypets.models.Mascota m = misMascotas.get(pos);
            String desc = etDesc.getText().toString().trim();

            String pubId = galeriaRef.push().getKey();
            PublicacionMascota nuevaPub = new PublicacionMascota(
                    pubId,
                    currentUser.getUid(),
                    "@" + miNick,
                    m.getNombre(),
                    desc,
                    m.getFotoPerfilUrl(),
                    System.currentTimeMillis()
            );

            galeriaRef.child(pubId).setValue(nuevaPub).addOnSuccessListener(aVoid -> {
                Toast.makeText(getContext(), "¡" + m.getNombre() + " ya está en la comunidad!", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });
        });

        view.findViewById(R.id.btnCancelarMascota).setOnClickListener(v -> dialog.dismiss());
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.show();
    }

    private void mostrarDialogoCrearArticulo() {
        boolean esOficial = (tabLayout.getSelectedTabPosition() == 0);
        imagenSeleccionadaBase64 = "";

        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_agregar_articulo, null);
        AlertDialog dialog = new AlertDialog.Builder(requireContext()).setView(view).create();

        ivVistaPreviaDialogo = view.findViewById(R.id.ivDialogVistaPrevia);
        ivVistaPreviaDialogo.setPadding(50, 50, 50, 50);

        TextInputEditText etTitulo = view.findViewById(R.id.etDialogTitulo);
        TextInputEditText etDesc = view.findViewById(R.id.etDialogDescripcion);
        TextInputEditText etCont = view.findViewById(R.id.etDialogContenido);
        TextInputEditText etUrl = view.findViewById(R.id.etDialogUrl);

        view.findViewById(R.id.btnGuardarDialog).setOnClickListener(v -> {
            String titulo = etTitulo.getText().toString().trim();
            if (titulo.isEmpty()) { Toast.makeText(getContext(), "Título obligatorio", Toast.LENGTH_SHORT).show(); return; }

            DatabaseReference targetRef = esOficial ? oficialesRef : comunidadRef;
            String id = targetRef.push().getKey();
            String autor = esOficial ? "EasyPets Oficial" : "@" + miNick;

            Articulo nuevo = new Articulo(id, currentUser.getUid(), titulo,
                    etDesc.getText().toString().trim(),
                    etCont.getText().toString().trim(),
                    autor, System.currentTimeMillis(), imagenSeleccionadaBase64,
                    etUrl.getText().toString().trim(), esOficial);

            targetRef.child(id).setValue(nuevo).addOnSuccessListener(aVoid -> {
                Toast.makeText(getContext(), "¡Publicado!", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });
        });

        view.findViewById(R.id.btnDialogImagen).setOnClickListener(v ->
                galeriaLauncher.launch(new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)));

        view.findViewById(R.id.btnCancelarDialog).setOnClickListener(v -> dialog.dismiss());

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
        ivVistaPreviaDialogo.setPadding(0, 0, 0, 0);
        MaterialButton btnGuardar = view.findViewById(R.id.btnGuardarDialog);
        MaterialButton btnCancelar = view.findViewById(R.id.btnCancelarDialog);

        tvTitle.setText("Editar Mi Artículo");
        etTitulo.setText(articulo.getTitulo());
        etDescripcion.setText(articulo.getDescripcionCorta());
        etContenido.setText(articulo.getContenidoCompleto());
        etUrl.setText(articulo.getUrlEnlace());

        if (imagenSeleccionadaBase64 != null && !imagenSeleccionadaBase64.isEmpty()) {
            try {
                byte[] decodedString = Base64.decode(imagenSeleccionadaBase64, Base64.DEFAULT);
                ivVistaPreviaDialogo.setImageBitmap(BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length));
                ivVistaPreviaDialogo.setVisibility(View.VISIBLE);
            } catch (Exception e) { e.printStackTrace(); }
        }

        btnImagen.setOnClickListener(v -> galeriaLauncher.launch(new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)));
        btnCancelar.setOnClickListener(v -> dialog.dismiss());

        btnGuardar.setOnClickListener(v -> {
            String tit = etTitulo.getText().toString().trim();
            if (tit.isEmpty()) { Toast.makeText(getContext(), "El título es obligatorio", Toast.LENGTH_SHORT).show(); return; }

            Map<String, Object> updates = new HashMap<>();
            updates.put("titulo", tit);
            updates.put("descripcionCorta", etDescripcion.getText().toString().trim());
            updates.put("contenidoCompleto", etContenido.getText().toString().trim());
            updates.put("urlEnlace", etUrl.getText().toString().trim());
            updates.put("imagenPortadaBase64", imagenSeleccionadaBase64);

            comunidadRef.child(articulo.getId()).updateChildren(updates).addOnSuccessListener(aVoid -> {
                Toast.makeText(getContext(), "Artículo actualizado", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });
        });

        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.show();
    }

    private void mostrarBottomSheetComentarios(PublicacionMascota publicacion) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.layout_bottom_sheet_comentarios, null);
        bottomSheetDialog.setContentView(view);

        RecyclerView rvComentarios = view.findViewById(R.id.rvComentariosBottomSheet);
        EditText etNuevoComentario = view.findViewById(R.id.etNuevoComentarioMascota);
        View btnEnviar = view.findViewById(R.id.btnEnviarComentarioMascota);

        List<ComentarioMascota> listaComentarios = new ArrayList<>();
        ComentarioMascotaAdapter adaptadorComentarios = new ComentarioMascotaAdapter(listaComentarios);

        rvComentarios.setLayoutManager(new LinearLayoutManager(getContext()));
        rvComentarios.setAdapter(adaptadorComentarios);

        DatabaseReference publicacionComentariosRef = comentariosMascotasRef.child(publicacion.getId());

        ValueEventListener comentariosListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                listaComentarios.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    ComentarioMascota c = ds.getValue(ComentarioMascota.class);
                    if (c != null) listaComentarios.add(c);
                }
                adaptadorComentarios.notifyDataSetChanged();
                if (!listaComentarios.isEmpty()) {
                    rvComentarios.scrollToPosition(listaComentarios.size() - 1);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        };
        publicacionComentariosRef.addValueEventListener(comentariosListener);

        btnEnviar.setOnClickListener(v -> {
            String texto = etNuevoComentario.getText().toString().trim();
            if (texto.isEmpty()) return;

            String comentarioId = publicacionComentariosRef.push().getKey();
            ComentarioMascota nuevoComentario = new ComentarioMascota(
                    comentarioId,
                    currentUser.getUid(),
                    "@" + miNick,
                    texto,
                    System.currentTimeMillis(),
                    miFotoPerfil
            );

            publicacionComentariosRef.child(comentarioId).setValue(nuevoComentario).addOnSuccessListener(aVoid -> {
                etNuevoComentario.setText("");

                galeriaRef.child(publicacion.getId()).child("comentariosCount")
                        .setValue(publicacion.getComentariosCount() + 1);

                java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("@(\\w+)").matcher(texto);
                while (matcher.find()) {
                    String nickMencionado = matcher.group(1);
                    notificarMencion(nickMencionado, publicacion);
                }
            });
        });

        bottomSheetDialog.setOnDismissListener(dialog -> {
            publicacionComentariosRef.removeEventListener(comentariosListener);
        });

        if (bottomSheetDialog.getWindow() != null) {
            bottomSheetDialog.getWindow().findViewById(com.google.android.material.R.id.design_bottom_sheet).setBackgroundResource(android.R.color.transparent);
        }

        bottomSheetDialog.show();
    }
    // MÉTODO PARA ENVIAR LA NOTIFICACIÓN DE MENCIÓN
    private void notificarMencion(String nickMencionado, PublicacionMascota publicacion) {
        usuariosRef.orderByChild("nick").equalTo(nickMencionado).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                        String idUsuarioMencionado = userSnapshot.getKey();

                        if (idUsuarioMencionado != null && !idUsuarioMencionado.equals(currentUser.getUid())) {
                            DatabaseReference notifRef = FirebaseDatabase.getInstance().getReference("notificaciones").child(idUsuarioMencionado);
                            String idNotif = notifRef.push().getKey();

                            com.easypets.models.Notificacion notificacion = new com.easypets.models.Notificacion(
                                    idNotif,
                                    "Mención en comentario",
                                    "@" + miNick + " te ha mencionado en una foto de " + publicacion.getNombreMascota(),
                                    "comunidad",
                                    publicacion.getId(),
                                    "",
                                    "",
                                    "",
                                    System.currentTimeMillis()
                            );
                            notifRef.child(idNotif).setValue(notificacion);
                        }
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
    private void comprobarEmptyState(boolean estaVacio) {
        if (estaVacio) {
            rvContenido.setVisibility(View.GONE);
            layoutEmptyState.setVisibility(View.VISIBLE);
        } else {
            rvContenido.setVisibility(View.VISIBLE);
            layoutEmptyState.setVisibility(View.GONE);
        }
    }
}