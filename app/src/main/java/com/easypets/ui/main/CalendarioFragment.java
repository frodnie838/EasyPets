package com.easypets.ui.main;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.easypets.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.Calendar;
import java.util.Locale;

public class CalendarioFragment extends Fragment {

    private android.widget.CalendarView calendarView;
    private TextView tvFechaSeleccionada;
    private MaterialButton btnIrAFecha;
    private RecyclerView rvEventos;
    private LinearLayout layoutSinEventos;
    private FloatingActionButton fabAgregarEvento;

    // Para manejar la fecha actual de forma sencilla
    private Calendar calendarioActual;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_calendario, container, false);

        vincularVistas(view);

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

    private void configurarListeners() {
        // 1. Cuando el usuario toca un día en el calendario visual
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            actualizarTextoFecha(dayOfMonth, month, year);

            // Aquí en el futuro llamaremos a Firebase para buscar eventos de este día
            cargarEventosDeFecha(dayOfMonth, month, year);
        });

        // 2. El botón mágico de "Ir a fecha..."
        btnIrAFecha.setOnClickListener(v -> mostrarBuscadorDeFecha());

        // 3. Botón flotante para añadir un evento nuevo
        fabAgregarEvento.setOnClickListener(v -> mostrarDialogoAgregarEvento());
    }

    private void actualizarTextoFecha(int dia, int mes, int anio) {
        // Los meses en Java empiezan en 0 (Enero es 0), así que le sumamos 1
        String fechaFormateada = String.format(Locale.getDefault(), "Eventos para el %02d/%02d/%d", dia, mes + 1, anio);
        tvFechaSeleccionada.setText(fechaFormateada);
    }

    private void cargarEventosDeFecha(int dia, int mes, int anio) {
        // TODO: Conectar con Firebase
        // Como aún no hay Firebase, simplemente vaciamos la lista y mostramos el icono
        rvEventos.setVisibility(View.GONE);
        layoutSinEventos.setVisibility(View.VISIBLE);
    }

    private void mostrarBuscadorDeFecha() {
        // Obtenemos el año, mes y día en el que está el calendario actualmente
        int anioActual = calendarioActual.get(Calendar.YEAR);
        int mesActual = calendarioActual.get(Calendar.MONTH);
        int diaActual = calendarioActual.get(Calendar.DAY_OF_MONTH);

        // Creamos el diálogo nativo de Android
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    // Cuando el usuario elige una fecha y le da a "Aceptar"
                    calendarioActual.set(year, month, dayOfMonth);

                    // Hacemos que el calendario pegue el salto a esa fecha en milisegundos
                    calendarView.setDate(calendarioActual.getTimeInMillis(), true, true);

                    // Actualizamos el texto de abajo
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

        // Vincular las vistas del XML del diálogo
        com.google.android.material.textfield.TextInputEditText etTitulo = dialogView.findViewById(R.id.etTituloEvento);
        android.widget.RadioGroup rgTipo = dialogView.findViewById(R.id.rgTipoEvento);
        MaterialButton btnFechaDialog = dialogView.findViewById(R.id.btnFechaDialog);
        MaterialButton btnCancelar = dialogView.findViewById(R.id.btnCancelarDialog);
        MaterialButton btnGuardar = dialogView.findViewById(R.id.btnGuardarDialog);

        // --- NUEVO: CONFIGURAR EL DESPLEGABLE DE MASCOTAS ---
        android.widget.AutoCompleteTextView spinnerMascotas = dialogView.findViewById(R.id.spinnerMascotas);

        // Creamos una lista falsa por ahora. La primera opción es la "Por defecto".
        String[] opcionesMascotas = {"General", "Max (Perro)", "Luna (Gato)"};

        // Creamos un adaptador para que el menú lea este array
        android.widget.ArrayAdapter<String> adapterMascotas = new android.widget.ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                opcionesMascotas
        );
        spinnerMascotas.setAdapter(adapterMascotas);

        // Seleccionamos la primera por defecto para que no salga vacío
        spinnerMascotas.setText(opcionesMascotas[0], false);
        // ----------------------------------------------------

        // Calendario temporal para este diálogo
        Calendar calendarioDialog = Calendar.getInstance();
        calendarioDialog.setTimeInMillis(calendarioActual.getTimeInMillis());

        // HACER QUE EL BOTÓN DE FECHA ABRA EL SELECTOR
        btnFechaDialog.setOnClickListener(v -> {
            DatePickerDialog picker = new DatePickerDialog(
                    requireContext(),
                    (view, year, month, dayOfMonth) -> {
                        calendarioDialog.set(year, month, dayOfMonth);
                        String fechaFormat = String.format(java.util.Locale.getDefault(), "%02d/%02d/%d", dayOfMonth, month + 1, year);
                        btnFechaDialog.setText(fechaFormat);
                    },
                    calendarioDialog.get(Calendar.YEAR),
                    calendarioDialog.get(Calendar.MONTH),
                    calendarioDialog.get(Calendar.DAY_OF_MONTH)
            );
            picker.show();
        });

        // Configurar Botón Cancelar
        btnCancelar.setOnClickListener(v -> dialog.dismiss());

        // Configurar Botón Guardar
        btnGuardar.setOnClickListener(v -> {
            String titulo = etTitulo.getText().toString().trim();

            if (titulo.isEmpty()) {
                etTitulo.setError("Escribe el título del evento");
                return;
            }

            // Validar que se haya elegido una fecha
            String fechaFinal = btnFechaDialog.getText().toString();
            if (fechaFinal.equals("Seleccione una fecha")) {
                Toast.makeText(getContext(), "Por favor, elige una fecha", Toast.LENGTH_SHORT).show();
                return; // Detenemos el guardado si no hay fecha
            }

            // Saber qué tipo de evento ha marcado
            int selectedId = rgTipo.getCheckedRadioButtonId();
            String tipo = "Veterinario";
            if (selectedId == R.id.rbPeluqueria) {
                tipo = "Peluquería";
            } else if (selectedId == R.id.rbNota) {
                tipo = "Nota";
            }

            // --- NUEVO: OBTENER QUÉ MASCOTA SE HA SELECCIONADO ---
            String mascotaSeleccionada = spinnerMascotas.getText().toString();

            // TODO: Guardar en Firebase
            Toast.makeText(getContext(), "Guardando: " + titulo + " para " + mascotaSeleccionada + " (" + tipo + ") el " + fechaFinal, Toast.LENGTH_LONG).show();

            dialog.dismiss();
        });

        dialog.show();
    }
}