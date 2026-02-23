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

        // Inicializar Firebase y Repositorios
        mAuth = FirebaseAuth.getInstance();
        eventoRepository = new EventoRepository();
        mascotaRepository = new MascotaRepository();

        // Inicializar listas
        nombresMascotas = new ArrayList<>();
        listaMascotasUsuario = new ArrayList<>();

        eventoAdapter = new EventoAdapter();
        rvEventos.setLayoutManager(new LinearLayoutManager(getContext()));
        rvEventos.setAdapter(eventoAdapter);

        // Cargar las mascotas del usuario para el desplegable
        cargarMascotasDelUsuario();

        // Inicializar el calendario con la fecha de hoy
        calendarioActual = Calendar.getInstance();
        actualizarTextoFecha(calendarioActual.get(Calendar.DAY_OF_MONTH),
                calendarioActual.get(Calendar.MONTH),
                calendarioActual.get(Calendar.YEAR));

        configurarListeners();

        cargarEventosDeFecha(calendarioActual.get(Calendar.DAY_OF_MONTH),
                calendarioActual.get(Calendar.MONTH),
                calendarioActual.get(Calendar.YEAR));

        return view;
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

                    if (eventoAdapter != null) {
                        eventoAdapter.setMascotas(listaMascotasUsuario);
                    }

                    for (Mascota m : listaMascotas) {
                        nombresMascotas.add(m.getNombre() + " (" + m.getEspecie() + ")");
                    }
                }

                @Override
                public void onError(String error) {
                    if (nombresMascotas.isEmpty()) {
                        nombresMascotas.add("General");
                    }
                }
            });
        }
    }

    private void configurarListeners() {
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            // Actualizamos la variable global calendarioActual para saber en qué día estamos
            calendarioActual.set(year, month, dayOfMonth);

            actualizarTextoFecha(dayOfMonth, month, year);
            cargarEventosDeFecha(dayOfMonth, month, year);
        });

        btnIrAFecha.setOnClickListener(v -> mostrarBuscadorDeFecha());
        btnFiltrarEventos.setOnClickListener(v -> mostrarDialogoFiltros());
        fabAgregarEvento.setOnClickListener(v -> mostrarDialogoAgregarEvento());
    }

    private void actualizarTextoFecha(int dia, int mes, int anio) {
        String fechaFormateada = String.format(Locale.getDefault(), "Eventos para el %02d/%02d/%d", dia, mes + 1, anio);
        tvFechaSeleccionada.setText(fechaFormateada);
    }

    private void cargarEventosDeFecha(int dia, int mes, int anio) {
        String fechaBuscada = String.format(Locale.getDefault(), "%02d/%02d/%d", dia, mes + 1, anio);

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            eventoRepository.obtenerEventosPorFecha(user.getUid(), fechaBuscada, new EventoRepository.LeerEventosCallback() {
                @Override
                public void onResultado(List<Evento> listaEventos) {
                    if (listaEventos.isEmpty()) {
                        // Si no hay eventos, ocultamos la lista y mostramos la huella
                        rvEventos.setVisibility(View.GONE);
                        layoutSinEventos.setVisibility(View.VISIBLE);
                    } else {
                        // Si hay eventos, los pasamos al adaptador y mostramos la lista
                        eventoAdapter.setEventos(listaEventos);
                        rvEventos.setVisibility(View.VISIBLE);
                        layoutSinEventos.setVisibility(View.GONE);
                    }
                }

                @Override
                public void onError(String error) {
                    Toast.makeText(getContext(), "Error al cargar eventos", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void mostrarBuscadorDeFecha() {
        int anioActual = calendarioActual.get(Calendar.YEAR);
        int mesActual = calendarioActual.get(Calendar.MONTH);
        int diaActual = calendarioActual.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    calendarioActual.set(year, month, dayOfMonth);
                    calendarView.setDate(calendarioActual.getTimeInMillis(), true, true);
                    actualizarTextoFecha(dayOfMonth, month, year);

                    cargarEventosDeFecha(dayOfMonth, month, year);
                },
                anioActual, mesActual, diaActual
        );

        datePickerDialog.show();
    }

    private void mostrarDialogoAgregarEvento() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_agregar_evento, null);
        builder.setView(dialogView);

        android.app.AlertDialog dialog = builder.create();

        com.google.android.material.textfield.TextInputEditText etTitulo = dialogView.findViewById(R.id.etTituloEvento);
        android.widget.AutoCompleteTextView spinnerTipoEvento = dialogView.findViewById(R.id.spinnerTipoEvento);
        MaterialButton btnCancelar = dialogView.findViewById(R.id.btnCancelarDialog);
        MaterialButton btnGuardar = dialogView.findViewById(R.id.btnGuardarDialog);
        MaterialButton btnFechaDialog = dialogView.findViewById(R.id.btnFechaDialog);
        MaterialButton btnHoraDialog = dialogView.findViewById(R.id.btnHoraDialog);
        android.widget.AutoCompleteTextView spinnerMascotas = dialogView.findViewById(R.id.spinnerMascotas);

        ArrayAdapter<String> adapterMascotas = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                nombresMascotas
        );
        spinnerMascotas.setAdapter(adapterMascotas);
        if (!nombresMascotas.isEmpty()) {
            spinnerMascotas.setText(nombresMascotas.get(0), false);
        }

        String[] opcionesTipo = {"Nota", "Veterinario", "Peluquería", "Guardería"};
        ArrayAdapter<String> adapterTipo = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                opcionesTipo
        );
        spinnerTipoEvento.setAdapter(adapterTipo);
        spinnerTipoEvento.setText(opcionesTipo[0], false); // "Nota" por defecto

        Calendar calendarioDialog = Calendar.getInstance();
        calendarioDialog.setTimeInMillis(calendarioActual.getTimeInMillis());

        // Poner la fecha actual en el botón al abrir el diálogo
        String fechaFormatInit = String.format(Locale.getDefault(), "%02d/%02d/%d",
                calendarioDialog.get(Calendar.DAY_OF_MONTH),
                calendarioDialog.get(Calendar.MONTH) + 1,
                calendarioDialog.get(Calendar.YEAR));
        btnFechaDialog.setText(fechaFormatInit);

        btnFechaDialog.setOnClickListener(v -> {
            DatePickerDialog picker = new DatePickerDialog(
                    requireContext(),
                    (view, year, month, dayOfMonth) -> {
                        calendarioDialog.set(year, month, dayOfMonth);
                        String fechaFormat = String.format(Locale.getDefault(), "%02d/%02d/%d", dayOfMonth, month + 1, year);
                        btnFechaDialog.setText(fechaFormat);
                    },
                    calendarioDialog.get(Calendar.YEAR),
                    calendarioDialog.get(Calendar.MONTH),
                    calendarioDialog.get(Calendar.DAY_OF_MONTH)
            );
            picker.show();
        });

        btnHoraDialog.setOnClickListener(v -> {
            int horaActual = calendarioDialog.get(Calendar.HOUR_OF_DAY);
            int minutoActual = calendarioDialog.get(Calendar.MINUTE);

            android.app.TimePickerDialog timePicker = new android.app.TimePickerDialog(
                    requireContext(),
                    (view1, hourOfDay, minute) -> {
                        calendarioDialog.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        calendarioDialog.set(Calendar.MINUTE, minute);
                        String horaFormat = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute);
                        btnHoraDialog.setText(horaFormat);
                    },
                    horaActual,
                    minutoActual,
                    true
            );
            timePicker.show();
        });

        btnCancelar.setOnClickListener(v -> dialog.dismiss());

        btnGuardar.setOnClickListener(v -> {
            String titulo = etTitulo.getText().toString().trim();
            if (titulo.isEmpty()) {
                etTitulo.setError("Escribe el título del evento");
                return;
            }

            String fechaFinal = btnFechaDialog.getText().toString();
            if (fechaFinal.equals("Seleccione una fecha")) {
                Toast.makeText(getContext(), "Por favor, elige una fecha", Toast.LENGTH_SHORT).show();
                return;
            }

            String horaFinal = btnHoraDialog.getText().toString();
            if (horaFinal.equals("Hora (Opcional)")) {
                horaFinal = "";
            }

            String mascotaSeleccionadaTexto = spinnerMascotas.getText().toString();
            String idMascotaSeleccionada = "";

            if (!mascotaSeleccionadaTexto.equals("General")) {
                for (Mascota m : listaMascotasUsuario) {
                    String nombreMostrado = m.getNombre() + " (" + m.getEspecie() + ")";
                    if (nombreMostrado.equals(mascotaSeleccionadaTexto)) {
                        idMascotaSeleccionada = m.getIdMascota();
                        break;
                    }
                }
            }

            String tipo = spinnerTipoEvento.getText().toString();

            FirebaseUser user = mAuth.getCurrentUser();
            if (user != null) {
                Evento nuevoEvento = new Evento(null, titulo, fechaFinal, horaFinal, tipo, idMascotaSeleccionada);

                eventoRepository.guardarEvento(user.getUid(), nuevoEvento, new EventoRepository.AccionCallback() {
                    @Override
                    public void onExito() {
                        Toast.makeText(getContext(), "¡Evento guardado correctamente!", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();

                        String fechaActualViendo = String.format(Locale.getDefault(), "%02d/%02d/%d",
                                calendarioActual.get(Calendar.DAY_OF_MONTH),
                                calendarioActual.get(Calendar.MONTH) + 1,
                                calendarioActual.get(Calendar.YEAR));

                        if (fechaFinal.equals(fechaActualViendo)) {
                            cargarEventosDeFecha(calendarioActual.get(Calendar.DAY_OF_MONTH),
                                    calendarioActual.get(Calendar.MONTH),
                                    calendarioActual.get(Calendar.YEAR));
                        }
                    }

                    @Override
                    public void onError(String error) {
                        Toast.makeText(getContext(), "Error al guardar: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Toast.makeText(getContext(), "Debes iniciar sesión para guardar eventos", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }
    private void mostrarDialogoFiltros() {
        // Usamos BottomSheetDialog para que salga desde abajo de la pantalla
        com.google.android.material.bottomsheet.BottomSheetDialog bottomSheet = new com.google.android.material.bottomsheet.BottomSheetDialog(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_filtros, null);
        bottomSheet.setContentView(dialogView);

        com.google.android.material.switchmaterial.SwitchMaterial switchVerTodos = dialogView.findViewById(R.id.switchVerTodos);
        android.widget.AutoCompleteTextView spinnerTipo = dialogView.findViewById(R.id.spinnerFiltroTipo);
        android.widget.AutoCompleteTextView spinnerMascota = dialogView.findViewById(R.id.spinnerFiltroMascota);
        MaterialButton btnLimpiar = dialogView.findViewById(R.id.btnLimpiarFiltros);
        MaterialButton btnAplicar = dialogView.findViewById(R.id.btnAplicarFiltros);

        // Opciones del Spinner de Tipo
        String[] opcionesTipo = {"Todos", "Veterinario", "Peluquería", "Guardería", "Nota"};
        spinnerTipo.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, opcionesTipo));
        spinnerTipo.setText("Todos", false);

        // Opciones del Spinner de Mascota
        List<String> opcionesMascota = new ArrayList<>();
        opcionesMascota.add("Todas");
        for (Mascota m : listaMascotasUsuario) {
            opcionesMascota.add(m.getNombre());
        }
        spinnerMascota.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, opcionesMascota));
        spinnerMascota.setText("Todas", false);

        btnLimpiar.setOnClickListener(v -> {
            switchVerTodos.setChecked(false);
            spinnerTipo.setText("Todos", false);
            spinnerMascota.setText("Todas", false);
        });

        btnAplicar.setOnClickListener(v -> {
            boolean verTodos = switchVerTodos.isChecked();
            String tipoFiltro = spinnerTipo.getText().toString();
            String mascotaFiltro = spinnerMascota.getText().toString();

            // Buscar el ID de la mascota si eligió una específica
            String idMascotaFiltro = "";
            if (!mascotaFiltro.equals("Todas")) {
                for (Mascota m : listaMascotasUsuario) {
                    if (m.getNombre().equals(mascotaFiltro)) {
                        idMascotaFiltro = m.getIdMascota();
                        break;
                    }
                }
            }

            aplicarFiltrosA_Firebase(verTodos, tipoFiltro, idMascotaFiltro);
            bottomSheet.dismiss();
        });

        bottomSheet.show();
    }

    private void aplicarFiltrosA_Firebase(boolean verTodos, String tipoFiltro, String idMascotaFiltro) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        if (verTodos) {
            tvFechaSeleccionada.setText("Todos tus eventos");
            eventoRepository.obtenerTodosLosEventos(user.getUid(), new EventoRepository.LeerEventosCallback() {
                @Override
                public void onResultado(List<Evento> listaEventos) {
                    procesarYMostrarLista(listaEventos, tipoFiltro, idMascotaFiltro);
                }

                @Override
                public void onError(String error) { }
            });
        } else {
            String fechaBuscada = String.format(Locale.getDefault(), "%02d/%02d/%d",
                    calendarioActual.get(Calendar.DAY_OF_MONTH),
                    calendarioActual.get(Calendar.MONTH) + 1,
                    calendarioActual.get(Calendar.YEAR));
            tvFechaSeleccionada.setText("Eventos para el " + fechaBuscada);

            eventoRepository.obtenerEventosPorFecha(user.getUid(), fechaBuscada, new EventoRepository.LeerEventosCallback() {
                @Override
                public void onResultado(List<Evento> listaEventos) {
                    procesarYMostrarLista(listaEventos, tipoFiltro, idMascotaFiltro);
                }

                @Override
                public void onError(String error) { }
            });
        }
    }

    private void procesarYMostrarLista(List<Evento> listaOriginal, String tipoFiltro, String idMascotaFiltro) {
        List<Evento> listaFiltrada = new ArrayList<>();

        for (Evento e : listaOriginal) {
            boolean pasaFiltroTipo = tipoFiltro.equals("Todos") || tipoFiltro.equals(e.getTipo());

            // Si la mascota del evento es nula (General), hay que manejarlo para que no dé error
            String idMascotaEvento = e.getIdMascota() == null ? "" : e.getIdMascota();
            boolean pasaFiltroMascota = idMascotaFiltro.isEmpty() || idMascotaFiltro.equals(idMascotaEvento);

            if (pasaFiltroTipo && pasaFiltroMascota) {
                listaFiltrada.add(e);
            }
        }

        if (listaFiltrada.isEmpty()) {
            rvEventos.setVisibility(View.GONE);
            layoutSinEventos.setVisibility(View.VISIBLE);
        } else {
            eventoAdapter.setEventos(listaFiltrada);
            rvEventos.setVisibility(View.VISIBLE);
            layoutSinEventos.setVisibility(View.GONE);
        }
    }
}