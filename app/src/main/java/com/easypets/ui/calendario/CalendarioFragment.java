package com.easypets.ui.calendario;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

// ✨ IMPORTS DE LA NUEVA LIBRERÍA DE CALENDARIO ACTUALIZADOS A LA VERSIÓN 1.9.0
import com.applandeo.materialcalendarview.CalendarView;
import com.applandeo.materialcalendarview.CalendarDay; // ✨ NUEVO
import com.applandeo.materialcalendarview.exceptions.OutOfDateRangeException;

import com.easypets.R;
import com.easypets.adapters.EventoAdapter;
import com.easypets.models.Evento;
import com.easypets.models.Mascota;
import com.easypets.repositories.EventoRepository;
import com.easypets.repositories.MascotaRepository;
import com.easypets.ui.auth.LoginActivity;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

// IMPORTS PARA FIREBASE
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CalendarioFragment extends Fragment {

    private CalendarView calendarView;
    private ImageButton btnFiltrarEventos;
    private TextView tvFechaSeleccionada;
    private MaterialButton btnIrAFecha;
    private RecyclerView rvEventos;
    private LinearLayout layoutSinEventos, layoutAvisoInvitado;
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

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        eventoRepository = new EventoRepository();
        mascotaRepository = new MascotaRepository();

        vincularVistas(view);
        inicializarComponentes();

        if (currentUser != null) {
            configurarModoUsuario(currentUser);
        } else {
            configurarModoInvitado();
        }

        return view;
    }

    private void vincularVistas(View view) {
        calendarView = view.findViewById(R.id.calendarView);
        tvFechaSeleccionada = view.findViewById(R.id.tvFechaSeleccionada);
        btnIrAFecha = view.findViewById(R.id.btnIrAFecha);
        rvEventos = view.findViewById(R.id.rvEventos);
        layoutSinEventos = view.findViewById(R.id.layoutSinEventos);
        layoutAvisoInvitado = view.findViewById(R.id.layoutAvisoInvitadoCalendario);
        fabAgregarEvento = view.findViewById(R.id.fabAgregarEvento);
        btnFiltrarEventos = view.findViewById(R.id.btnFiltrarEventos);
    }

    private void inicializarComponentes() {
        nombresMascotas = new ArrayList<>();
        listaMascotasUsuario = new ArrayList<>();
        calendarioActual = Calendar.getInstance();

        eventoAdapter = new EventoAdapter();
        rvEventos.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvEventos.setAdapter(eventoAdapter);

        actualizarTextoFecha(calendarioActual.get(Calendar.DAY_OF_MONTH),
                calendarioActual.get(Calendar.MONTH),
                calendarioActual.get(Calendar.YEAR));
    }

    private void configurarModoUsuario(final FirebaseUser user) {
        layoutAvisoInvitado.setVisibility(View.GONE);
        fabAgregarEvento.setVisibility(View.VISIBLE);

        cargarMascotasDelUsuario(user);
        configurarSwipe(requireContext());

        // ✨ NUEVO LISTENER MODERNIZADO: setOnCalendarDayClickListener (Ver. 1.9.0)
        calendarView.setOnCalendarDayClickListener(calendarDay -> {
            Calendar clickedDayCalendar = calendarDay.getCalendar();
            calendarioActual = clickedDayCalendar;

            try {
                calendarView.setDate(clickedDayCalendar);
            } catch (OutOfDateRangeException e) {
                e.printStackTrace();
            }

            int dayOfMonth = clickedDayCalendar.get(Calendar.DAY_OF_MONTH);
            int month = clickedDayCalendar.get(Calendar.MONTH);
            int year = clickedDayCalendar.get(Calendar.YEAR);

            actualizarTextoFecha(dayOfMonth, month, year);
            cargarEventosDeFecha(user.getUid(), dayOfMonth, month, year);
        });

        btnIrAFecha.setOnClickListener(v -> mostrarBuscadorDeFecha(user.getUid()));
        btnFiltrarEventos.setOnClickListener(v -> mostrarDialogoFiltros());
        fabAgregarEvento.setOnClickListener(v -> mostrarDialogoAgregarEvento(null));

        cargarEventosDeFecha(user.getUid(), calendarioActual.get(Calendar.DAY_OF_MONTH),
                calendarioActual.get(Calendar.MONTH), calendarioActual.get(Calendar.YEAR));

        cargarPuntitosEnCalendario(user.getUid());
    }

    private void configurarModoInvitado() {
        layoutAvisoInvitado.setVisibility(View.VISIBLE);
        layoutSinEventos.setVisibility(View.GONE);
        rvEventos.setVisibility(View.GONE);
        fabAgregarEvento.setVisibility(View.GONE);

        calendarView.setOnCalendarDayClickListener(calendarDay -> {
            Calendar clickedDayCalendar = calendarDay.getCalendar();
            calendarioActual = clickedDayCalendar;

            try {
                calendarView.setDate(clickedDayCalendar);
            } catch (OutOfDateRangeException e) {
                e.printStackTrace();
            }

            int dayOfMonth = clickedDayCalendar.get(Calendar.DAY_OF_MONTH);
            int month = clickedDayCalendar.get(Calendar.MONTH);
            int year = clickedDayCalendar.get(Calendar.YEAR);

            actualizarTextoFecha(dayOfMonth, month, year);
        });

        View.OnClickListener aviso = v ->
                Toast.makeText(getContext(), "Función solo para usuarios registrados", Toast.LENGTH_SHORT).show();

        btnIrAFecha.setOnClickListener(aviso);
        btnFiltrarEventos.setOnClickListener(aviso);

        MaterialButton btnLogin = layoutAvisoInvitado.findViewById(R.id.btnIrALoginCalendario);
        if (btnLogin != null) {
            btnLogin.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), LoginActivity.class);
                startActivity(intent);
            });
        }
    }

    private void cargarPuntitosEnCalendario(String uid) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference().child("eventos").child(uid);

        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;

                List<CalendarDay> eventosCalendario = new ArrayList<>();

                for (DataSnapshot ds : snapshot.getChildren()) {
                    Evento evt = ds.getValue(Evento.class);
                    if (evt != null && evt.getFecha() != null) {
                        try {
                            String[] partes = evt.getFecha().split("/");
                            if (partes.length == 3) {
                                Calendar cal = Calendar.getInstance();

                                cal.set(Calendar.HOUR_OF_DAY, 0);
                                cal.set(Calendar.MINUTE, 0);
                                cal.set(Calendar.SECOND, 0);
                                cal.set(Calendar.MILLISECOND, 0);

                                cal.set(Calendar.DAY_OF_MONTH, Integer.parseInt(partes[0]));
                                cal.set(Calendar.MONTH, Integer.parseInt(partes[1]) - 1);
                                cal.set(Calendar.YEAR, Integer.parseInt(partes[2]));

                                CalendarDay calendarDay = new CalendarDay(cal);
                                calendarDay.setImageResource(R.drawable.huella);
                                calendarDay.setLabelColor(R.color.color_acento_primario);

                                eventosCalendario.add(calendarDay);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }

                // Cargamos los eventos
                calendarView.setCalendarDays(eventosCalendario);

                calendarView.post(() -> {
                    try {
                        List<Calendar> fechasSeleccionadas = calendarView.getSelectedDates();
                        if (fechasSeleccionadas != null) {
                            calendarView.setSelectedDates(fechasSeleccionadas);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // Método definitivo y agresivo para Alarmas Exactas
    private void programarNotificacion(long tiempoEventoMillis, String titulo, String mensaje, long milisegundosAntes) {
        long tiempoActual = System.currentTimeMillis();
        long tiempoAlarma = tiempoEventoMillis - milisegundosAntes;

        if (tiempoAlarma > tiempoActual) {
            android.app.AlarmManager alarmManager = (android.app.AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(requireContext(), com.easypets.services.NotificacionReceiver.class);
            intent.putExtra("titulo", titulo);
            intent.putExtra("mensaje", mensaje);

            // ✨ NUEVO: Le pasamos el ID del usuario a la alarma para que sepa dónde guardar el mensaje
            if (mAuth.getCurrentUser() != null) {
                intent.putExtra("uid", mAuth.getCurrentUser().getUid());
            }

            int idAlarma = (int) (tiempoAlarma % Integer.MAX_VALUE);
            android.app.PendingIntent pendingIntent = android.app.PendingIntent.getBroadcast(
                    requireContext(),
                    idAlarma,
                    intent,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE
            );

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, tiempoAlarma, pendingIntent);
            } else {
                alarmManager.setExact(android.app.AlarmManager.RTC_WAKEUP, tiempoAlarma, pendingIntent);
            }

            Log.d("ALARMAS", "Alarma EXACTA programada para: " + new java.util.Date(tiempoAlarma).toString());
        }
    }

    private void configurarSwipe(Context context) {
        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) { return false; }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int posicion = viewHolder.getBindingAdapterPosition();
                Evento evento = eventoAdapter.getEventoAt(posicion);

                if (direction == ItemTouchHelper.LEFT) {
                    confirmarEliminacion(evento, posicion);
                } else {
                    mostrarDialogoAgregarEvento(evento);
                    eventoAdapter.notifyItemChanged(posicion);
                }
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                View itemView = viewHolder.itemView;
                float density = context.getResources().getDisplayMetrics().density;
                float radius = 12 * density;

                GradientDrawable background = new GradientDrawable();
                background.setCornerRadius(radius);

                if (dX > 0) { // Editar (Derecha)
                    background.setColor(Color.parseColor("#CCFFB300")); // Ámbar traslúcido
                    background.setBounds(itemView.getLeft(), itemView.getTop(), itemView.getLeft() + ((int) dX) + (int) radius, itemView.getBottom());
                } else if (dX < 0) { // Eliminar (Izquierda)
                    background.setColor(Color.parseColor("#CCE53935")); // Rojo traslúcido
                    background.setBounds(itemView.getRight() + ((int) dX) - (int) radius, itemView.getTop(), itemView.getRight(), itemView.getBottom());
                }
                background.draw(c);

                Drawable icon = ContextCompat.getDrawable(context, dX > 0 ? android.R.drawable.ic_menu_edit : android.R.drawable.ic_menu_delete);
                if (icon != null && Math.abs(dX) > (16 * density)) {
                    int margin = (int) (20 * density);
                    int iconSize = (int) (24 * density);
                    int iconTop = itemView.getTop() + (itemView.getHeight() - iconSize) / 2;
                    if (dX > 0) icon.setBounds(itemView.getLeft() + margin, iconTop, itemView.getLeft() + margin + iconSize, iconTop + iconSize);
                    else icon.setBounds(itemView.getRight() - margin - iconSize, iconTop, itemView.getRight() - margin, iconTop + iconSize);
                    icon.setTint(Color.WHITE);
                    icon.draw(c);
                }
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
        };
        new ItemTouchHelper(simpleCallback).attachToRecyclerView(rvEventos);
    }

    private void cargarEventosDeFecha(String uid, int dia, int mes, int anio) {
        String fechaBuscada = String.format(Locale.getDefault(), "%02d/%02d/%d", dia, mes + 1, anio);
        eventoRepository.obtenerEventosPorFecha(uid, fechaBuscada, new EventoRepository.LeerEventosCallback() {
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

    private void mostrarBuscadorDeFecha(String uid) {
        DatePickerDialog datePicker = new DatePickerDialog(requireContext(), R.style.TemaPickerVerde, (view, year, month, dayOfMonth) -> {
            calendarioActual.set(year, month, dayOfMonth);
            try {
                calendarView.setDate(calendarioActual);
            } catch (OutOfDateRangeException e) {
                e.printStackTrace();
            }
            actualizarTextoFecha(dayOfMonth, month, year);
            cargarEventosDeFecha(uid, dayOfMonth, month, year);
        }, calendarioActual.get(Calendar.YEAR), calendarioActual.get(Calendar.MONTH), calendarioActual.get(Calendar.DAY_OF_MONTH));

        datePicker.show();
        datePicker.getButton(DatePickerDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(requireContext(), R.color.color_acento_primario));
        datePicker.getButton(DatePickerDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(requireContext(), R.color.color_acento_primario));
    }

    private void actualizarTextoFecha(int dia, int mes, int anio) {
        tvFechaSeleccionada.setText(String.format(Locale.getDefault(), "Eventos para el %02d/%02d/%d", dia, mes + 1, anio));
    }

    private void cargarMascotasDelUsuario(FirebaseUser user) {
        mascotaRepository.escucharMascotas(user.getUid(), new MascotaRepository.LeerMascotasCallback() {
            @Override
            public void onResultado(List<Mascota> listaMascotas) {
                nombresMascotas.clear();
                listaMascotasUsuario.clear();
                nombresMascotas.add("General");
                listaMascotasUsuario.addAll(listaMascotas);
                if (eventoAdapter != null) eventoAdapter.setMascotas(listaMascotasUsuario);
                for (Mascota m : listaMascotas) nombresMascotas.add(m.getNombre());
            }
            @Override
            public void onError(String error) {}
        });
    }

    private void confirmarEliminacion(Evento evento, int posicion) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Eliminar evento")
                .setMessage("¿Estás seguro?")
                .setPositiveButton("Eliminar", (d, w) -> {
                    eventoRepository.eliminarEvento(mAuth.getCurrentUser().getUid(), evento.getId(), new EventoRepository.AccionCallback() {
                        @Override
                        public void onExito() { cargarEventosDeFecha(mAuth.getCurrentUser().getUid(), calendarioActual.get(Calendar.DAY_OF_MONTH), calendarioActual.get(Calendar.MONTH), calendarioActual.get(Calendar.YEAR)); }
                        @Override
                        public void onError(String error) { eventoAdapter.notifyItemChanged(posicion); }
                    });
                })
                .setNegativeButton("Cancelar", (d, w) -> eventoAdapter.notifyItemChanged(posicion))
                .show();
    }

    private void mostrarDialogoAgregarEvento(@Nullable Evento eventoExistente) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_agregar_evento, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextInputEditText etTitulo = dialogView.findViewById(R.id.etTituloEvento);
        AutoCompleteTextView spinnerTipo = dialogView.findViewById(R.id.spinnerTipoEvento);
        AutoCompleteTextView spinnerMascotas = dialogView.findViewById(R.id.spinnerMascotas);
        MaterialButton btnFecha = dialogView.findViewById(R.id.btnFechaDialog);
        MaterialButton btnHora = dialogView.findViewById(R.id.btnHoraDialog);
        MaterialButton btnGuardar = dialogView.findViewById(R.id.btnGuardarDialog);
        MaterialButton btnCancelar = dialogView.findViewById(R.id.btnCancelarDialog);

        // ✨ NUEVO: Enlazamos el Switch de notificaciones
        com.google.android.material.switchmaterial.SwitchMaterial switchNoti = dialogView.findViewById(R.id.switchNotificacion);

        spinnerMascotas.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, nombresMascotas));

        // He añadido más opciones al Spinner para cubrir tus casos de uso
        String[] opcionesTipo = {"Nota", "Veterinario", "Vacuna", "Peluquería", "Guardería", "Paseo", "Medicación", "Comida"};
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
                        spinnerMascotas.setText(m.getNombre(), false);
                        break;
                    }
                }
            }
        } else {
            spinnerTipo.setText(opcionesTipo[0], false);
            if (!nombresMascotas.isEmpty()) {
                spinnerMascotas.setText(nombresMascotas.get(0), false);
            }
            btnFecha.setText(String.format(Locale.getDefault(), "%02d/%02d/%d",
                    calendarioActual.get(Calendar.DAY_OF_MONTH),
                    calendarioActual.get(Calendar.MONTH) + 1,
                    calendarioActual.get(Calendar.YEAR)));
        }

        btnFecha.setOnClickListener(v -> {
            DatePickerDialog datePicker = new DatePickerDialog(requireContext(), R.style.TemaPickerVerde, (view, year, month, dayOfMonth) -> {
                btnFecha.setText(String.format(Locale.getDefault(), "%02d/%02d/%d", dayOfMonth, month + 1, year));
            }, calendarioActual.get(Calendar.YEAR), calendarioActual.get(Calendar.MONTH), calendarioActual.get(Calendar.DAY_OF_MONTH));

            datePicker.show();
            datePicker.getButton(DatePickerDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(requireContext(), R.color.color_acento_primario));
            datePicker.getButton(DatePickerDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(requireContext(), R.color.color_acento_primario));
        });

        btnHora.setOnClickListener(v -> {
            TimePickerDialog timePicker = new TimePickerDialog(requireContext(), R.style.TemaPickerVerde, (view, hour, minute) -> {
                btnHora.setText(String.format(Locale.getDefault(), "%02d:%02d", hour, minute));
            }, 12, 0, true);

            timePicker.show();
            timePicker.getButton(TimePickerDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(requireContext(), R.color.color_acento_primario));
            timePicker.getButton(TimePickerDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(requireContext(), R.color.color_acento_primario));
        });

        btnCancelar.setOnClickListener(v -> dialog.dismiss());

        btnGuardar.setOnClickListener(v -> {
            String titulo = etTitulo.getText() != null ? etTitulo.getText().toString().trim() : "";
            if (titulo.isEmpty()) {
                etTitulo.setError("Requerido");
                return;
            }

            String mascotaTxt = spinnerMascotas.getText().toString();
            String idMascota = "";
            if (!mascotaTxt.equals("General")) {
                for (Mascota m : listaMascotasUsuario) {
                    if (m.getNombre().equals(mascotaTxt)) {
                        idMascota = m.getIdMascota();
                        break;
                    }
                }
            }

            String fechaSeleccionada = btnFecha.getText().toString();
            String horaSeleccionada = btnHora.getText().toString();
            String tipoEvento = spinnerTipo.getText().toString();

            // ✨ LÓGICA DE NOTIFICACIONES INTELIGENTES ✨
            if (switchNoti.isChecked()) {
                try {
                    // 1. Convertir la fecha y la hora a Milisegundos
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

                    // Si el usuario no puso hora, asumimos por defecto las 09:00 AM
                    String fechaYHora = fechaSeleccionada + " " + (horaSeleccionada.equals("Hora (Opcional)") ? "09:00" : horaSeleccionada);
                    Date dateEvento = sdf.parse(fechaYHora);

                    if (dateEvento != null) {
                        long tiempoEventoMillis = dateEvento.getTime();

                        long UN_DIA = 24L * 60L * 60L * 1000L;
                        long UNA_HORA = 60L * 60L * 1000L;
                        long DIEZ_MINUTOS = 10L * 60L * 1000L;

                        // 2. Evaluar el Tipo de Evento
                        if (tipoEvento.equalsIgnoreCase("Veterinario") || tipoEvento.equalsIgnoreCase("Vacuna") || tipoEvento.equalsIgnoreCase("Peluquería")) {
                            // Citas importantes: Aviso 24h antes y 1h antes
                            programarNotificacion(tiempoEventoMillis, "Cita mañana: " + titulo, "Recuerda prepararlo todo para mañana.", UN_DIA);
                            programarNotificacion(tiempoEventoMillis, "¡Cita en 1 hora!", "Prepárate para salir: " + titulo, UNA_HORA);
                            Log.d("NOTIFICACIONES", "Programadas 2 alertas (24h y 1h) para: " + titulo);

                        } else if (tipoEvento.equalsIgnoreCase("Paseo") || tipoEvento.equalsIgnoreCase("Medicación") || tipoEvento.equalsIgnoreCase("Comida")) {
                            // Rutina exacta: Aviso en el momento
                            programarNotificacion(tiempoEventoMillis, "¡Es la hora! ⏰", "Toca: " + titulo, 0L);
                            Log.d("NOTIFICACIONES", "Programada 1 alerta exacta para: " + titulo);

                        } else {
                            // Otros eventos (Notas, Guardería...): Aviso 10 min antes por cortesía
                            programarNotificacion(tiempoEventoMillis, "Próximo evento: " + titulo, "Empieza en 10 minutos.", DIEZ_MINUTOS);
                            Log.d("NOTIFICACIONES", "Programada alerta genérica (-10m) para: " + titulo);
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    Log.e("NOTIFICACIONES", "Error calculando la fecha de la notificación");
                }
            }

            Evento e = new Evento(
                    eventoExistente != null ? eventoExistente.getId() : null,
                    titulo,
                    fechaSeleccionada,
                    horaSeleccionada.equals("Hora (Opcional)") ? "" : horaSeleccionada,
                    tipoEvento,
                    idMascota
            );

            FirebaseUser userC = mAuth.getCurrentUser();
            if (userC != null) {
                eventoRepository.guardarEvento(userC.getUid(), e, new EventoRepository.AccionCallback() {
                    @Override
                    public void onExito() {
                        dialog.dismiss();
                        cargarEventosDeFecha(userC.getUid(), calendarioActual.get(Calendar.DAY_OF_MONTH), calendarioActual.get(Calendar.MONTH), calendarioActual.get(Calendar.YEAR));

                        // Opcional: Avisar al usuario de que la alarma se ha guardado
                        if(switchNoti.isChecked()) {
                            Toast.makeText(getContext(), "Evento guardado con recordatorio 🔔", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getContext(), "Evento guardado (Sin alerta)", Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override
                    public void onError(String error) {
                        Toast.makeText(getContext(), "Error al guardar: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        dialog.show();
    }

    private void mostrarDialogoFiltros() {
        BottomSheetDialog bottomSheet = new BottomSheetDialog(requireContext());
        View v = getLayoutInflater().inflate(R.layout.dialog_filtros, null);
        bottomSheet.setContentView(v);
        bottomSheet.show();
    }
}