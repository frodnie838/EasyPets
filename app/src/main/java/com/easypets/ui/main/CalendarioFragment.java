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
import androidx.recyclerview.widget.RecyclerView;

import com.easypets.R;
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
    private TextView tvFechaSeleccionada;
    private MaterialButton btnIrAFecha;
    private RecyclerView rvEventos;
    private LinearLayout layoutSinEventos;
    private FloatingActionButton fabAgregarEvento;

    private Calendar calendarioActual;

    // --- FIREBASE Y REPOSITORIOS ---
    private FirebaseAuth mAuth;
    private EventoRepository eventoRepository;
    private MascotaRepository mascotaRepository;

    // Listas para manejar el desplegable y las referencias
    private List<String> nombresMascotas;
    private List<Mascota> listaMascotasUsuario;

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

        // Cargar las mascotas del usuario para el desplegable
        cargarMascotasDelUsuario();

        // Inicializar el calendario con la fecha de hoy
        calendarioActual = Calendar.getInstance();
        actualizarTextoFecha(calendarioActual.get(Calendar.DAY_OF_MONTH),
                calendarioActual.get(Calendar.MONTH),
                calendarioActual.get(Calendar.YEAR));

        configurarListeners();

        // Por ahora, simulamos que no hay eventos para mostrar el diseño vacío
        rvEventos.setVisibility(View.GONE);
        layoutSinEventos.setVisibility(View.VISIBLE);

        return view;
    }

    private void vincularVistas(View view) {
        calendarView = view.findViewById(R.id.calendarView);
        tvFechaSeleccionada = view.findViewById(R.id.tvFechaSeleccionada);
        btnIrAFecha = view.findViewById(R.id.btnIrAFecha);
        rvEventos = view.findViewById(R.id.rvEventos);
        layoutSinEventos = view.findViewById(R.id.layoutSinEventos);
        fabAgregarEvento = view.findViewById(R.id.fabAgregarEvento);
    }

    private void cargarMascotasDelUsuario() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            mascotaRepository.escucharMascotas(user.getUid(), new MascotaRepository.LeerMascotasCallback() {
                @Override
                public void onResultado(List<Mascota> listaMascotas) {
                    // Como escucharMascotas actualiza en tiempo real, limpiamos antes de rellenar
                    nombresMascotas.clear();
                    listaMascotasUsuario.clear();

                    nombresMascotas.add("General");

                    listaMascotasUsuario.addAll(listaMascotas); // Guardamos los objetos enteros

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
            actualizarTextoFecha(dayOfMonth, month, year);
            cargarEventosDeFecha(dayOfMonth, month, year);
        });

        btnIrAFecha.setOnClickListener(v -> mostrarBuscadorDeFecha());
        fabAgregarEvento.setOnClickListener(v -> mostrarDialogoAgregarEvento());
    }

    private void actualizarTextoFecha(int dia, int mes, int anio) {
        String fechaFormateada = String.format(Locale.getDefault(), "Eventos para el %02d/%02d/%d", dia, mes + 1, anio);
        tvFechaSeleccionada.setText(fechaFormateada);
    }

    private void cargarEventosDeFecha(int dia, int mes, int anio) {
        // En el futuro leeremos de Firebase aquí.
        rvEventos.setVisibility(View.GONE);
        layoutSinEventos.setVisibility(View.VISIBLE);
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
        android.widget.RadioGroup rgTipo = dialogView.findViewById(R.id.rgTipoEvento);
        MaterialButton btnCancelar = dialogView.findViewById(R.id.btnCancelarDialog);
        MaterialButton btnGuardar = dialogView.findViewById(R.id.btnGuardarDialog);
        MaterialButton btnFechaDialog = dialogView.findViewById(R.id.btnFechaDialog);
        MaterialButton btnHoraDialog = dialogView.findViewById(R.id.btnHoraDialog);
        android.widget.AutoCompleteTextView spinnerMascotas = dialogView.findViewById(R.id.spinnerMascotas);

        // CONFIGURAR EL DESPLEGABLE CON LAS MASCOTAS REALES
        ArrayAdapter<String> adapterMascotas = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                nombresMascotas
        );
        spinnerMascotas.setAdapter(adapterMascotas);
        if (!nombresMascotas.isEmpty()) {
            spinnerMascotas.setText(nombresMascotas.get(0), false);
        }

        Calendar calendarioDialog = Calendar.getInstance();
        calendarioDialog.setTimeInMillis(calendarioActual.getTimeInMillis());

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
        // ✨ NUEVO: HACER QUE EL BOTÓN DE HORA ABRA EL RELOJ
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
                    true // true para formato 24h
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
                Toast.makeText(getContext(), "Por favor, seleccione una fecha", Toast.LENGTH_SHORT).show();
                return;
            }

            // ✨ NUEVO: COMPROBAR SI HA PUESTO HORA O NO
            String horaFinal = btnHoraDialog.getText().toString();
            if (horaFinal.equals("Hora (Opcional)")) {
                horaFinal = ""; // Lo dejamos vacío porque es opcional
            }

            // (Aquí está la lógica que ya tienes de los RadioButtons para el tipo...)
            int selectedId = rgTipo.getCheckedRadioButtonId();
            String tipo = "Veterinario";
            if (selectedId == R.id.rbPeluqueria) {
                tipo = "Peluquería";
            } else if (selectedId == R.id.rbNota) {
                tipo = "Nota";
            }

            // (Aquí está la lógica que ya tienes para sacar el ID de la mascota...)
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

            // --- GUARDAR EN FIREBASE ---
            FirebaseUser user = mAuth.getCurrentUser();
            if (user != null) {
                // ✨ NUEVO: Añadimos 'horaFinal' al constructor
                Evento nuevoEvento = new Evento(null, titulo, fechaFinal, horaFinal, tipo, idMascotaSeleccionada);

                eventoRepository.guardarEvento(user.getUid(), nuevoEvento, new EventoRepository.AccionCallback() {
                    @Override
                    public void onExito() {
                        Toast.makeText(getContext(), "¡Evento guardado correctamente!", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
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
}