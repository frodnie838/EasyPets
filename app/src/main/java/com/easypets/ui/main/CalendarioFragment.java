package com.easypets.ui.main;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.easypets.R;
import com.easypets.adapters.EventoAdapter;
import com.easypets.models.Evento;
import com.easypets.models.Mascota;
import com.easypets.repositories.EventoRepository;
import com.easypets.repositories.MascotaRepository;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class CalendarioFragment extends Fragment {

    private android.widget.CalendarView calendarView;
    private android.widget.ImageButton btnFiltrarEventos;
    private TextView tvFechaSeleccionada;
    private MaterialButton btnIrAFecha;
    private RecyclerView rvEventos;
    private LinearLayout layoutSinEventos;
    private FloatingActionButton fabAgregarEvento;

    private Calendar calendarioActual;

    private FirebaseAuth mAuth;
    private EventoRepository eventoRepository;
    private MascotaRepository mascotaRepository;

    private List<String> nombresMascotas;
    private List<Mascota> listaMascotasUsuario;

    private EventoAdapter eventoAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_calendario, container, false);

        vincularVistas(view);

        mAuth = FirebaseAuth.getInstance();
        eventoRepository = new EventoRepository();
        mascotaRepository = new MascotaRepository();

        nombresMascotas = new ArrayList<>();
        listaMascotasUsuario = new ArrayList<>();

        eventoAdapter = new EventoAdapter();
        rvEventos.setLayoutManager(new LinearLayoutManager(getContext()));
        rvEventos.setAdapter(eventoAdapter);

        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int posicion = viewHolder.getAdapterPosition();
                Evento evento = eventoAdapter.getEventoAt(posicion);

                if (direction == ItemTouchHelper.LEFT) {
                    confirmarEliminacion(evento, posicion);
                } else if (direction == ItemTouchHelper.RIGHT) {
                    mostrarDialogoAgregarEvento(evento);
                    eventoAdapter.notifyItemChanged(posicion);
                }
            }

            @Override
            public void onChildDraw(@NonNull android.graphics.Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                View itemView = viewHolder.itemView;
                float density = recyclerView.getContext().getResources().getDisplayMetrics().density;
                float radius = 12 * density;

                android.graphics.drawable.GradientDrawable background = new android.graphics.drawable.GradientDrawable();
                background.setCornerRadius(radius);

                if (dX > 0) {
                    // Ámbar sólido (Editar)
                    background.setColor(android.graphics.Color.parseColor("#BBFBD04F"));
                    background.setBounds(itemView.getLeft(), itemView.getTop(), itemView.getLeft() + ((int) dX) + (int) radius, itemView.getBottom());
                } else if (dX < 0) {
                    // Rojo sólido (Eliminar)
                    background.setColor(android.graphics.Color.parseColor("#FA594F"));
                    background.setBounds(itemView.getRight() + ((int) dX) - (int) radius, itemView.getTop(), itemView.getRight(), itemView.getBottom());
                }

                background.draw(c);

                android.graphics.drawable.Drawable icon;
                if (dX > 0) {
                    icon = androidx.core.content.ContextCompat.getDrawable(requireContext(), android.R.drawable.ic_menu_edit);
                } else {
                    icon = androidx.core.content.ContextCompat.getDrawable(requireContext(), android.R.drawable.ic_menu_delete);
                }

                if (icon != null && Math.abs(dX) > (16 * density)) {
                    int margin = (int) (20 * density);
                    int iconSize = (int) (24 * density);
                    int iconTop = itemView.getTop() + (itemView.getHeight() - iconSize) / 2;

                    if (dX > 0) {
                        icon.setBounds(itemView.getLeft() + margin, iconTop, itemView.getLeft() + margin + iconSize, iconTop + iconSize);
                    } else {
                        icon.setBounds(itemView.getRight() - margin - iconSize, iconTop, itemView.getRight() - margin, iconTop + iconSize);
                    }

                    icon.setTint(android.graphics.Color.WHITE);
                    icon.setAlpha(255);
                    icon.draw(c);
                }

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
        };

        new ItemTouchHelper(simpleCallback).attachToRecyclerView(rvEventos);
        cargarMascotasDelUsuario();
        calendarioActual = Calendar.getInstance();
        actualizarTextoFecha(calendarioActual.get(Calendar.DAY_OF_MONTH), calendarioActual.get(Calendar.MONTH), calendarioActual.get(Calendar.YEAR));
        configurarListeners();
        cargarEventosDeFecha(calendarioActual.get(Calendar.DAY_OF_MONTH), calendarioActual.get(Calendar.MONTH), calendarioActual.get(Calendar.YEAR));

        return view;
    }

    private void confirmarEliminacion(Evento evento, int posicion) {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Eliminar evento")
                .setMessage("¿Estás seguro de que quieres eliminar '" + evento.getTitulo() + "'?")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    FirebaseUser user = mAuth.getCurrentUser();
                    if (user != null && evento.getId() != null) {
                        eventoRepository.eliminarEvento(user.getUid(), evento.getId(), new EventoRepository.AccionCallback() {
                            @Override
                            public void onExito() {
                                Toast.makeText(getContext(), "Evento eliminado", Toast.LENGTH_SHORT).show();
                                cargarEventosDeFecha(calendarioActual.get(Calendar.DAY_OF_MONTH), calendarioActual.get(Calendar.MONTH), calendarioActual.get(Calendar.YEAR));
                            }
                            @Override
                            public void onError(String error) {
                                Toast.makeText(getContext(), "Error: " + error, Toast.LENGTH_SHORT).show();
                                eventoAdapter.notifyItemChanged(posicion);
                            }
                        });
                    }
                })
                .setNegativeButton("Cancelar", (dialog, which) -> eventoAdapter.notifyItemChanged(posicion))
                .show();
    }

    private void vincularVistas(View view) {
        calendarView = view.findViewById(R.id.calendarView);
        tvFechaSeleccionada = view.findViewById(R.id.tvFechaSeleccionada);
        btnIrAFecha = view.findViewById(R.id.btnIrAFecha);
        rvEventos = view.findViewById(R.id.rvEventos);
        layoutSinEventos = view.findViewById(R.id.layoutSinEventos);
        fabAgregarEvento = view.findViewById(R.id.fabAgregarEvento);
        btnFiltrarEventos = view.findViewById(R.id.btnFiltrarEventos);
    }

    private void cargarMascotasDelUsuario() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            mascotaRepository.escucharMascotas(user.getUid(), new MascotaRepository.LeerMascotasCallback() {
                @Override
                public void onResultado(List<Mascota> listaMascotas) {
                    nombresMascotas.clear();
                    listaMascotasUsuario.clear();
                    nombresMascotas.add("General");
                    listaMascotasUsuario.addAll(listaMascotas);
                    if (eventoAdapter != null) eventoAdapter.setMascotas(listaMascotasUsuario);
                    for (Mascota m : listaMascotas) {
                        nombresMascotas.add(m.getNombre() + " (" + m.getEspecie() + ")");
                    }
                }
                @Override
                public void onError(String error) {}
            });
        }
    }

    private void configurarListeners() {
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            calendarioActual.set(year, month, dayOfMonth);
            actualizarTextoFecha(dayOfMonth, month, year);
            cargarEventosDeFecha(dayOfMonth, month, year);
        });
        btnIrAFecha.setOnClickListener(v -> mostrarBuscadorDeFecha());
        btnFiltrarEventos.setOnClickListener(v -> mostrarDialogoFiltros());
        fabAgregarEvento.setOnClickListener(v -> mostrarDialogoAgregarEvento(null));
    }

    private void actualizarTextoFecha(int dia, int mes, int anio) {
        tvFechaSeleccionada.setText(String.format(Locale.getDefault(), "Eventos para el %02d/%02d/%d", dia, mes + 1, anio));
    }

    private void cargarEventosDeFecha(int dia, int mes, int anio) {
        String fechaBuscada = String.format(Locale.getDefault(), "%02d/%02d/%d", dia, mes + 1, anio);
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            eventoRepository.obtenerEventosPorFecha(user.getUid(), fechaBuscada, new EventoRepository.LeerEventosCallback() {
                @Override
                public void onResultado(List<Evento> listaEventos) {
                    if (listaEventos.isEmpty()) {
                        rvEventos.setVisibility(View.GONE);
                        layoutSinEventos.setVisibility(View.VISIBLE);
                    } else {
                        eventoAdapter.setEventos(listaEventos);
                        rvEventos.setVisibility(View.VISIBLE);
                        layoutSinEventos.setVisibility(View.GONE);
                    }
                }
                @Override
                public void onError(String error) {}
            });
        }
    }

    private void mostrarBuscadorDeFecha() {
        new DatePickerDialog(requireContext(), (view, year, month, dayOfMonth) -> {
            calendarioActual.set(year, month, dayOfMonth);
            calendarView.setDate(calendarioActual.getTimeInMillis(), true, true);
            actualizarTextoFecha(dayOfMonth, month, year);
            cargarEventosDeFecha(dayOfMonth, month, year);
        }, calendarioActual.get(Calendar.YEAR), calendarioActual.get(Calendar.MONTH), calendarioActual.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void mostrarDialogoAgregarEvento(@Nullable Evento eventoExistente) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_agregar_evento, null);
        builder.setView(dialogView);
        android.app.AlertDialog dialog = builder.create();

        com.google.android.material.textfield.TextInputEditText etTitulo = dialogView.findViewById(R.id.etTituloEvento);
        android.widget.AutoCompleteTextView spinnerTipo = dialogView.findViewById(R.id.spinnerTipoEvento);
        android.widget.AutoCompleteTextView spinnerMascotas = dialogView.findViewById(R.id.spinnerMascotas);
        MaterialButton btnFecha = dialogView.findViewById(R.id.btnFechaDialog);
        MaterialButton btnHora = dialogView.findViewById(R.id.btnHoraDialog);
        MaterialButton btnGuardar = dialogView.findViewById(R.id.btnGuardarDialog);
        MaterialButton btnCancelar = dialogView.findViewById(R.id.btnCancelarDialog);

        spinnerMascotas.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, nombresMascotas));
        String[] opcionesTipo = {"Nota", "Veterinario", "Peluquería", "Guardería"};
        spinnerTipo.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, opcionesTipo));

        if (eventoExistente != null) {
            etTitulo.setText(eventoExistente.getTitulo());
            spinnerTipo.setText(eventoExistente.getTipo(), false);
            btnFecha.setText(eventoExistente.getFecha());
            btnHora.setText(eventoExistente.getHora().isEmpty() ? "Hora (Opcional)" : eventoExistente.getHora());
            btnGuardar.setText("Actualizar");

            if (eventoExistente.getIdMascota() == null || eventoExistente.getIdMascota().isEmpty()) {
                spinnerMascotas.setText("General", false);
            } else {
                for (Mascota m : listaMascotasUsuario) {
                    if (m.getIdMascota().equals(eventoExistente.getIdMascota())) {
                        spinnerMascotas.setText(m.getNombre() + " (" + m.getEspecie() + ")", false);
                        break;
                    }
                }
            }
        } else {
            spinnerTipo.setText(opcionesTipo[0], false);
            spinnerMascotas.setText(nombresMascotas.get(0), false);
            btnFecha.setText(String.format(Locale.getDefault(), "%02d/%02d/%d", calendarioActual.get(Calendar.DAY_OF_MONTH), calendarioActual.get(Calendar.MONTH) + 1, calendarioActual.get(Calendar.YEAR)));
        }

        btnFecha.setOnClickListener(v -> {
            new DatePickerDialog(requireContext(), (view, year, month, dayOfMonth) -> {
                btnFecha.setText(String.format(Locale.getDefault(), "%02d/%02d/%d", dayOfMonth, month + 1, year));
            }, calendarioActual.get(Calendar.YEAR), calendarioActual.get(Calendar.MONTH), calendarioActual.get(Calendar.DAY_OF_MONTH)).show();
        });

        btnHora.setOnClickListener(v -> {
            new android.app.TimePickerDialog(requireContext(), (view, hour, minute) -> {
                btnHora.setText(String.format(Locale.getDefault(), "%02d:%02d", hour, minute));
            }, 12, 0, true).show();
        });

        btnCancelar.setOnClickListener(v -> dialog.dismiss());

        btnGuardar.setOnClickListener(v -> {
            String titulo = etTitulo.getText().toString().trim();
            if (titulo.isEmpty()) { etTitulo.setError("Requerido"); return; }

            String mascotaTxt = spinnerMascotas.getText().toString();
            String idMascota = "";
            if (!mascotaTxt.equals("General")) {
                for (Mascota m : listaMascotasUsuario) {
                    if ((m.getNombre() + " (" + m.getEspecie() + ")").equals(mascotaTxt)) {
                        idMascota = m.getIdMascota();
                        break;
                    }
                }
            }

            Evento e = new Evento(eventoExistente != null ? eventoExistente.getId() : null,
                    titulo, btnFecha.getText().toString(),
                    btnHora.getText().toString().equals("Hora (Opcional)") ? "" : btnHora.getText().toString(),
                    spinnerTipo.getText().toString(), idMascota);

            FirebaseUser user = mAuth.getCurrentUser();
            if (user != null) {
                eventoRepository.guardarEvento(user.getUid(), e, new EventoRepository.AccionCallback() {
                    @Override
                    public void onExito() {
                        dialog.dismiss();
                        cargarEventosDeFecha(calendarioActual.get(Calendar.DAY_OF_MONTH), calendarioActual.get(Calendar.MONTH), calendarioActual.get(Calendar.YEAR));
                    }
                    @Override
                    public void onError(String error) {}
                });
            }
        });
        dialog.show();
    }

    private void mostrarDialogoFiltros() {
        com.google.android.material.bottomsheet.BottomSheetDialog bottomSheet = new com.google.android.material.bottomsheet.BottomSheetDialog(requireContext());
        View v = getLayoutInflater().inflate(R.layout.dialog_filtros, null);
        bottomSheet.setContentView(v);
        v.findViewById(R.id.btnAplicarFiltros).setOnClickListener(view -> bottomSheet.dismiss());
        bottomSheet.show();
    }

    private void aplicarFiltrosA_Firebase(boolean verTodos, String tipo, String idMascota) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;
        eventoRepository.obtenerTodosLosEventos(user.getUid(), new EventoRepository.LeerEventosCallback() {
            @Override
            public void onResultado(List<Evento> lista) { procesarYMostrarLista(lista, tipo, idMascota); }
            @Override
            public void onError(String error) {}
        });
    }

    private void procesarYMostrarLista(List<Evento> lista, String tipo, String idMascota) {
        List<Evento> filtrada = new ArrayList<>();
        for (Evento e : lista) {
            if ((tipo.equals("Todos") || e.getTipo().equals(tipo)) && (idMascota.isEmpty() || idMascota.equals(e.getIdMascota()))) {
                filtrada.add(e);
            }
        }
        eventoAdapter.setEventos(filtrada);
    }
}