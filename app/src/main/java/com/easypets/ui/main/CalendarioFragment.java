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
        // 1. Crear el constructor del Diálogo
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());

        // 2. Inflar el diseño XML que acabamos de crear
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_agregar_evento, null);
        builder.setView(dialogView);

        android.app.AlertDialog dialog = builder.create();

        // 3. Vincular las vistas del XML del diálogo
        com.google.android.material.textfield.TextInputEditText etTitulo = dialogView.findViewById(R.id.etTituloEvento);
        android.widget.RadioGroup rgTipo = dialogView.findViewById(R.id.rgTipoEvento);
        TextView tvFecha = dialogView.findViewById(R.id.tvFechaDialog);
        MaterialButton btnCancelar = dialogView.findViewById(R.id.btnCancelarDialog);
        MaterialButton btnGuardar = dialogView.findViewById(R.id.btnGuardarDialog);

        // 4. Pre-configurar la fecha elegida en el calendario
        String fechaActual = String.format(java.util.Locale.getDefault(), "%02d/%02d/%d",
                calendarioActual.get(Calendar.DAY_OF_MONTH),
                calendarioActual.get(Calendar.MONTH) + 1,
                calendarioActual.get(Calendar.YEAR));
        tvFecha.setText("Fecha seleccionada: " + fechaActual);

        // 5. Configurar Botón Cancelar
        btnCancelar.setOnClickListener(v -> dialog.dismiss());

        // 6. Configurar Botón Guardar
        btnGuardar.setOnClickListener(v -> {
            String titulo = etTitulo.getText().toString().trim();

            // Validar que no esté vacío
            if (titulo.isEmpty()) {
                etTitulo.setError("Escribe el título del evento");
                return;
            }

            // Saber qué tipo de evento ha marcado
            int selectedId = rgTipo.getCheckedRadioButtonId();
            String tipo = "Veterinario";
            if (selectedId == R.id.rbPeluqueria) {
                tipo = "Peluquería";
            } else if (selectedId == R.id.rbNota) {
                tipo = "Nota";
            }

            // TODO: Aquí guardaremos el Evento en Firebase
            // Por ahora, simulamos que se guarda con un Toast
            Toast.makeText(getContext(), "✅ " + tipo + " guardado: " + titulo, Toast.LENGTH_LONG).show();

            dialog.dismiss(); // Cerramos el diálogo
        });

        // 7. Mostrar el diálogo
        dialog.show();
    }
}