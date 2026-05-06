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

import com.applandeo.materialcalendarview.CalendarView;
import com.applandeo.materialcalendarview.CalendarDay;
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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Fragmento que implementa la agenda interactiva del usuario.
 * Gestiona la visualización del calendario mensual, la creación, edición y eliminación
 * de eventos, y programa notificaciones locales mediante AlarmManager en base a
 * reglas de tiempo específicas para cada tipo de evento.
 */
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

    private List<Evento> listaEventosMemoria = new ArrayList<>();
    private String filtroTipoActual = "Todos";
    private String filtroMascotaActual = "Todas";
    private boolean verTodosLosEventos = false;

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

    /**
     * Habilita las funcionalidades interactivas del calendario para usuarios autenticados.
     * Vincula listeners de selección de días, activa gestos de swipe y solicita la
     * sincronización inicial de los datos estructurados en Firebase.
     *
     * @param user Instancia activa del usuario autenticado.
     */
    private void configurarModoUsuario(final FirebaseUser user) {
        layoutAvisoInvitado.setVisibility(View.GONE);
        fabAgregarEvento.setVisibility(View.VISIBLE);

        cargarMascotasDelUsuario(user);
        configurarSwipe(requireContext());

        calendarView.setOnCalendarDayClickListener(calendarDay -> {
            calendarioActual = (Calendar) calendarDay.getCalendar().clone();

            try {
                calendarView.setDate(calendarioActual);
            } catch (OutOfDateRangeException e) {
                e.printStackTrace();
            }

            verTodosLosEventos = false;
            filtroTipoActual = "Todos";
            filtroMascotaActual = "Todas";

            int dayOfMonth = calendarioActual.get(Calendar.DAY_OF_MONTH);
            int month = calendarioActual.get(Calendar.MONTH);
            int year = calendarioActual.get(Calendar.YEAR);

            actualizarTextoFecha(dayOfMonth, month, year);
            refrescarEventos(user.getUid());
        });

        btnIrAFecha.setOnClickListener(v -> mostrarBuscadorDeFecha(user.getUid()));
        btnFiltrarEventos.setOnClickListener(v -> mostrarDialogoFiltros());
        fabAgregarEvento.setOnClickListener(v -> mostrarDialogoAgregarEvento(null));

        refrescarEventos(user.getUid());
        cargarPuntitosEnCalendario(user.getUid());
    }

    /**
     * Restringe el acceso a la gestión de eventos para usuarios no registrados.
     * Mantiene el componente de calendario visual pero desactiva interacciones clave
     * informando al usuario de la necesidad de inicio de sesión.
     */
    private void configurarModoInvitado() {
        layoutAvisoInvitado.setVisibility(View.VISIBLE);
        layoutSinEventos.setVisibility(View.GONE);
        rvEventos.setVisibility(View.GONE);
        fabAgregarEvento.setVisibility(View.GONE);

        calendarView.setOnCalendarDayClickListener(calendarDay -> {
            calendarioActual = (Calendar) calendarDay.getCalendar().clone();
            try {
                calendarView.setDate(calendarioActual);
            } catch (OutOfDateRangeException e) {
                e.printStackTrace();
            }
            actualizarTextoFecha(calendarioActual.get(Calendar.DAY_OF_MONTH), calendarioActual.get(Calendar.MONTH), calendarioActual.get(Calendar.YEAR));
        });

        View.OnClickListener aviso = v -> Toast.makeText(getContext(), "Función solo para usuarios registrados", Toast.LENGTH_SHORT).show();

        btnIrAFecha.setOnClickListener(aviso);
        btnFiltrarEventos.setOnClickListener(aviso);

        MaterialButton btnLogin = layoutAvisoInvitado.findViewById(R.id.btnIrALoginCalendario);
        if (btnLogin != null) {
            btnLogin.setOnClickListener(v -> startActivity(new Intent(getActivity(), LoginActivity.class)));
        }
    }

    /**
     * Coordina la consulta al repositorio de eventos basándose en el estado
     * del conmutador "verTodosLosEventos". Determina si se requiere una consulta
     * completa o filtrada por fecha exacta.
     *
     * @param uid Identificador del usuario propietario de los eventos.
     */
    private void refrescarEventos(String uid) {
        if (verTodosLosEventos) {
            eventoRepository.obtenerTodosLosEventos(uid, new EventoRepository.LeerEventosCallback() {
                @Override
                public void onResultado(List<Evento> listaEventos) {
                    listaEventosMemoria.clear();
                    listaEventosMemoria.addAll(listaEventos);
                    ejecutarFiltroEnMemoria();
                }
                @Override
                public void onError(String error) {}
            });
        } else {
            int dia = calendarioActual.get(Calendar.DAY_OF_MONTH);
            int mes = calendarioActual.get(Calendar.MONTH);
            int anio = calendarioActual.get(Calendar.YEAR);
            String fechaBuscada = String.format(Locale.getDefault(), "%02d/%02d/%d", dia, mes + 1, anio);

            eventoRepository.obtenerEventosPorFecha(uid, fechaBuscada, new EventoRepository.LeerEventosCallback() {
                @Override
                public void onResultado(List<Evento> listaEventos) {
                    listaEventosMemoria.clear();
                    listaEventosMemoria.addAll(listaEventos);
                    ejecutarFiltroEnMemoria();
                }
                @Override
                public void onError(String error) {}
            });
        }
    }

    /**
     * Aplica reglas lógicas iterativas sobre la colección de eventos cargada en memoria.
     * Evalúa condiciones paramétricas (Tipo de evento, Vinculación de mascota) para
     * construir una lista final que es delegada al adaptador de la vista.
     */
    private void ejecutarFiltroEnMemoria() {
        List<Evento> listaFiltrada = new ArrayList<>();

        for (Evento e : listaEventosMemoria) {
            String tipoEv = (e.getTipo() != null) ? e.getTipo().trim() : "";
            String idMascEv = (e.getIdMascota() != null) ? e.getIdMascota().trim() : "";

            boolean pasaTipo = filtroTipoActual.equalsIgnoreCase("Todos") || tipoEv.equalsIgnoreCase(filtroTipoActual);
            boolean pasaMascota = false;

            if (filtroMascotaActual.equalsIgnoreCase("Todas")) {
                pasaMascota = true;
            } else if (filtroMascotaActual.equalsIgnoreCase("General")) {
                if (idMascEv.isEmpty() || idMascEv.equalsIgnoreCase("General")) pasaMascota = true;
            } else {
                for (Mascota m : listaMascotasUsuario) {
                    String nomMasc = m.getNombre() != null ? m.getNombre().trim() : "";
                    String idMasc = m.getIdMascota() != null ? m.getIdMascota().trim() : "";

                    if (nomMasc.equalsIgnoreCase(filtroMascotaActual)) {
                        if (idMascEv.equalsIgnoreCase(idMasc) || idMascEv.equalsIgnoreCase(nomMasc)) {
                            pasaMascota = true;
                        }
                        break;
                    }
                }
            }

            if (pasaTipo && pasaMascota) {
                listaFiltrada.add(e);
            }
        }

        if (listaFiltrada.isEmpty()) {
            rvEventos.setVisibility(View.GONE);
            layoutSinEventos.setVisibility(View.VISIBLE);
        } else {
            eventoAdapter.setEventos(new ArrayList<>(listaFiltrada));
            eventoAdapter.notifyDataSetChanged();
            rvEventos.setVisibility(View.VISIBLE);
            layoutSinEventos.setVisibility(View.GONE);
        }
    }

    private void mostrarDialogoFiltros() {
        BottomSheetDialog bottomSheet = new BottomSheetDialog(requireContext());
        View v = getLayoutInflater().inflate(R.layout.dialog_filtros, null);
        bottomSheet.setContentView(v);

        AutoCompleteTextView spinnerTipo = v.findViewById(R.id.spinnerFiltroTipo);
        AutoCompleteTextView spinnerMascotas = v.findViewById(R.id.spinnerFiltroMascota);
        MaterialButton btnAplicar = v.findViewById(R.id.btnAplicarFiltros);
        MaterialButton btnLimpiar = v.findViewById(R.id.btnLimpiarFiltros);
        com.google.android.material.switchmaterial.SwitchMaterial switchVerTodos = v.findViewById(R.id.switchVerTodos);

        if (spinnerTipo == null || spinnerMascotas == null || btnAplicar == null) return;

        if (switchVerTodos != null) switchVerTodos.setChecked(verTodosLosEventos);

        List<String> opcionesTipo = new ArrayList<>();
        opcionesTipo.add("Todos");
        opcionesTipo.addAll(Arrays.asList(com.easypets.models.TipoEvento.obtenerTodosLosNombres()));
        ArrayAdapter<String> adapterTipo = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, opcionesTipo);
        spinnerTipo.setAdapter(adapterTipo);

        List<String> opcionesMascotas = new ArrayList<>();
        opcionesMascotas.add("Todas");
        opcionesMascotas.add("General");
        if (nombresMascotas != null) {
            for (String nombre : nombresMascotas) {
                if (!opcionesMascotas.contains(nombre)) opcionesMascotas.add(nombre);
            }
        }
        ArrayAdapter<String> adapterMascotas = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, opcionesMascotas);
        spinnerMascotas.setAdapter(adapterMascotas);

        spinnerTipo.setText(filtroTipoActual, false);
        spinnerMascotas.setText(filtroMascotaActual, false);

        btnAplicar.setOnClickListener(view -> {
            String tipoSeleccionado = spinnerTipo.getText().toString().trim();
            String mascotaSeleccionada = spinnerMascotas.getText().toString().trim();

            filtroTipoActual = tipoSeleccionado.isEmpty() ? "Todos" : tipoSeleccionado;
            filtroMascotaActual = mascotaSeleccionada.isEmpty() ? "Todas" : mascotaSeleccionada;

            if (switchVerTodos != null) {
                verTodosLosEventos = switchVerTodos.isChecked();
            }

            if (verTodosLosEventos) {
                tvFechaSeleccionada.setText("Eventos filtrados (Todos los días)");
            } else {
                actualizarTextoFecha(calendarioActual.get(Calendar.DAY_OF_MONTH), calendarioActual.get(Calendar.MONTH), calendarioActual.get(Calendar.YEAR));
            }

            refrescarEventos(mAuth.getCurrentUser().getUid());
            bottomSheet.dismiss();
        });

        btnLimpiar.setOnClickListener(view -> {
            verTodosLosEventos = false;
            filtroTipoActual = "Todos";
            filtroMascotaActual = "Todas";

            actualizarTextoFecha(calendarioActual.get(Calendar.DAY_OF_MONTH), calendarioActual.get(Calendar.MONTH), calendarioActual.get(Calendar.YEAR));
            refrescarEventos(mAuth.getCurrentUser().getUid());
            bottomSheet.dismiss();
        });

        bottomSheet.show();
    }

    private void mostrarBuscadorDeFecha(String uid) {
        DatePickerDialog datePicker = new DatePickerDialog(requireContext(), R.style.TemaPickerVerde, (view, year, month, dayOfMonth) -> {
            Calendar nuevaFecha = Calendar.getInstance();
            nuevaFecha.set(Calendar.YEAR, year);
            nuevaFecha.set(Calendar.MONTH, month);
            nuevaFecha.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            nuevaFecha.set(Calendar.HOUR_OF_DAY, 0);
            nuevaFecha.set(Calendar.MINUTE, 0);
            nuevaFecha.set(Calendar.SECOND, 0);
            nuevaFecha.set(Calendar.MILLISECOND, 0);

            calendarioActual = nuevaFecha;

            try {
                calendarView.setDate(calendarioActual);
            } catch (OutOfDateRangeException e) {
                e.printStackTrace();
            }

            verTodosLosEventos = false;
            filtroTipoActual = "Todos";
            filtroMascotaActual = "Todas";

            actualizarTextoFecha(dayOfMonth, month, year);
            refrescarEventos(uid);

        }, calendarioActual.get(Calendar.YEAR), calendarioActual.get(Calendar.MONTH), calendarioActual.get(Calendar.DAY_OF_MONTH));

        datePicker.show();
        datePicker.getButton(DatePickerDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(requireContext(), R.color.color_acento_primario));
        datePicker.getButton(DatePickerDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(requireContext(), R.color.color_acento_primario));
    }

    private void actualizarTextoFecha(int dia, int mes, int anio) {
        tvFechaSeleccionada.setText(String.format(Locale.getDefault(), "Eventos para el %02d/%02d/%d", dia, mes + 1, anio));
    }

    /**
     * Mantiene un canal abierto con la base de datos para mapear todos los eventos
     * del usuario y asignar un indicador visual (huella) en los días correspondientes
     * dentro de la vista del componente de calendario.
     *
     * @param uid Identificador del usuario autenticado.
     */
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
                        } catch (Exception e) {}
                    }
                }

                calendarView.setCalendarDays(eventosCalendario);
                calendarView.post(() -> {
                    try {
                        List<Calendar> fechasSeleccionadas = calendarView.getSelectedDates();
                        if (fechasSeleccionadas != null) {
                            calendarView.setSelectedDates(fechasSeleccionadas);
                        }
                    } catch (Exception e) {}
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    /**
     * Genera una intención vinculada a un PendingIntent para programar el
     * disparo de un BroadcastReceiver en un punto de tiempo futuro establecido.
     *
     * @param tiempoEventoMillis Instante objetivo del evento en milisegundos.
     * @param titulo Título de la notificación.
     * @param mensaje Cuerpo de la notificación.
     * @param milisegundosAntes Decalaje de tiempo para el disparo prematuro de la alerta.
     */
    private void programarNotificacion(long tiempoEventoMillis, String titulo, String mensaje, long milisegundosAntes) {
        long tiempoActual = System.currentTimeMillis();
        long tiempoAlarma = tiempoEventoMillis - milisegundosAntes;

        if (tiempoAlarma > tiempoActual) {
            android.app.AlarmManager alarmManager = (android.app.AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(requireContext(), com.easypets.services.NotificacionReceiver.class);
            intent.putExtra("titulo", titulo);
            intent.putExtra("mensaje", mensaje);

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
        }
    }

    /**
     * Adjunta un manejador de gestos al RecyclerView de eventos para interceptar
     * desplazamientos laterales e implementar un borrado contextual o la apertura
     * de diálogos de edición mediante acciones rápidas.
     */
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

                if (dX > 0) {
                    background.setColor(Color.parseColor("#CCFFB300"));
                    background.setBounds(itemView.getLeft(), itemView.getTop(), itemView.getLeft() + ((int) dX) + (int) radius, itemView.getBottom());
                } else if (dX < 0) {
                    background.setColor(Color.parseColor("#CCE53935"));
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

    private void cargarMascotasDelUsuario(FirebaseUser user) {
        mascotaRepository.escucharMascotas(user.getUid(), new MascotaRepository.LeerMascotasCallback() {
            @Override
            public void onResultado(List<Mascota> listaMascotas) {
                nombresMascotas.clear();
                listaMascotasUsuario.clear();
                nombresMascotas.add("General");
                listaMascotasUsuario.addAll(listaMascotas);
                if (eventoAdapter != null) eventoAdapter.setMascotas(listaMascotasUsuario);
                for (Mascota m : listaMascotas) {
                    if (m.getNombre() != null) {
                        nombresMascotas.add(m.getNombre());
                    }
                }
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
                        public void onExito() {
                            refrescarEventos(mAuth.getCurrentUser().getUid());
                        }
                        @Override
                        public void onError(String error) {
                            eventoAdapter.notifyItemChanged(posicion);
                        }
                    });
                })
                .setNegativeButton("Cancelar", (d, w) -> eventoAdapter.notifyItemChanged(posicion))
                .show();
    }

    /**
     * Levanta un cuadro de diálogo para la estructuración de la agenda, con selectores
     * de fecha, hora y listas desplegables. Infiere programáticamente múltiples alarmas
     * según la tipología del objeto Evento instanciado.
     */
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

        com.google.android.material.switchmaterial.SwitchMaterial switchNoti = dialogView.findViewById(R.id.switchNotificacion);

        spinnerMascotas.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, nombresMascotas));

        String[] opcionesTipo = com.easypets.models.TipoEvento.obtenerTodosLosNombres();
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

            if (switchNoti.isChecked()) {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                    String fechaYHora = fechaSeleccionada + " " + (horaSeleccionada.equals("Hora (Opcional)") ? "09:00" : horaSeleccionada);
                    Date dateEvento = sdf.parse(fechaYHora);

                    if (dateEvento != null) {
                        long tiempoEventoMillis = dateEvento.getTime();
                        long UN_DIA = 24L * 60L * 60L * 1000L;
                        long DOS_HORAS = 2L * 60L * 60L * 1000L;
                        long UNA_HORA = 60L * 60L * 1000L;
                        long QUINCE_MIN = 15L * 60L * 1000L;

                        boolean esGeneral = mascotaTxt.equals("General") || mascotaTxt.isEmpty();
                        String deMascota = esGeneral ? "" : " de " + mascotaTxt;
                        String paraMascota = esGeneral ? "" : " para " + mascotaTxt;
                        String conMascota = esGeneral ? "" : " con " + mascotaTxt;
                        String aMascota = esGeneral ? "" : " a " + mascotaTxt;

                        com.easypets.models.TipoEvento tipoEnum = com.easypets.models.TipoEvento.desdeString(tipoEvento);

                        switch (tipoEnum) {
                            case VETERINARIO:
                                programarNotificacion(tiempoEventoMillis, "🩺 Mañana: " + titulo, "Recuerda la cita médica" + deMascota + ".", UN_DIA);
                                programarNotificacion(tiempoEventoMillis, "🚨 ¡Cita Veterinaria hoy!", "Tienes cita en 2 horas" + paraMascota + ": " + titulo, DOS_HORAS);
                                break;
                            case PELUQUERIA:
                            case GUARDERIA:
                                programarNotificacion(tiempoEventoMillis, "✂️ Mañana: " + titulo, "Recuerda la cita" + deMascota + ".", UN_DIA);
                                programarNotificacion(tiempoEventoMillis, "⏰ ¡Cita en 1 hora!", "Prepárate para salir" + conMascota + ": " + titulo, UNA_HORA);
                                break;
                            case PASEO:
                                programarNotificacion(tiempoEventoMillis, "🦮 ¡Hora del paseo!", "Ve cogiendo la correa" + deMascota + ".", QUINCE_MIN);
                                break;
                            case MEDICACION:
                                String tituloMed = esGeneral ? "💊 ¡Hora de su medicina!" : "💊 ¡Medicina para " + mascotaTxt + "!";
                                programarNotificacion(tiempoEventoMillis, tituloMed, "Toca administrar: " + titulo, 0L);
                                break;
                            case COMIDA:
                                programarNotificacion(tiempoEventoMillis, "🦴 ¡Hora de comer!", "Toca servir la comida" + aMascota + ".", 0L);
                                break;
                            case NOTA:
                            default:
                                String tituloNota = esGeneral ? "📌 Recordatorio" : "📌 Recordatorio para " + mascotaTxt;
                                programarNotificacion(tiempoEventoMillis, tituloNota, titulo, 0L);
                                break;
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
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
                        refrescarEventos(userC.getUid());
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
}