package com.easypets.ui.home;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.easypets.R;
import com.easypets.adapters.EventoAdapter;
import com.easypets.models.Evento;
import com.easypets.models.Mascota;
import com.easypets.repositories.EventoRepository;
import com.easypets.repositories.MascotaRepository;
import com.easypets.ui.auth.LoginActivity;
import com.easypets.ui.comunidad.EducacionFragment;
import com.easypets.ui.veterinarios.VeterinariosFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class HomeFragment extends Fragment {

    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;

    private EventoRepository eventoRepository;
    private MascotaRepository mascotaRepository;

    // Vistas Privadas/Públicas
    private CardView cardMisMascotas, cardVeterinarios, cardCalendario, cardEducacion, cardTiendas, cardAvisoInvitado;
    private LinearLayout layoutFuncionesPrivadas;
    private TextView tvConsejoDia, tvSinEventosProximos;
    private EditText etNotaRapida;
    private MaterialButton btnSeleccionarFecha, btnGuardarNota, btnIrALoginHome;

    private RecyclerView rvProximosEventos;
    private EventoAdapter eventoAdapter;
    private String fechaNotaSeleccionada = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        eventoRepository = new EventoRepository();
        mascotaRepository = new MascotaRepository();

        vincularVistas(view);

        // Fecha por defecto para la nota
        Calendar hoy = Calendar.getInstance();
        fechaNotaSeleccionada = String.format(Locale.getDefault(), "%02d/%02d/%d", hoy.get(Calendar.DAY_OF_MONTH), hoy.get(Calendar.MONTH) + 1, hoy.get(Calendar.YEAR));

        // Lógica de visualización según el tipo de usuario
        if (currentUser == null) {
            configurarModoInvitado();
        } else {
            configurarModoUsuario();
        }

        configurarNavegacionBotones();
        cargarConsejoAleatorio();

        return view;
    }
    @Override
    public void onResume() {
        super.onResume();
        // Forzamos al menú a marcar el icono de Home al volver a esta pantalla
        if (getActivity() != null) {
            BottomNavigationView bottomNav = getActivity().findViewById(R.id.bottom_navigation);
            if (bottomNav != null) {
                bottomNav.getMenu().findItem(R.id.nav_home).setChecked(true);
            }
        }
    }
    private void vincularVistas(View view) {
        // Secciones principales
        layoutFuncionesPrivadas = view.findViewById(R.id.layoutFuncionesPrivadas);
        cardAvisoInvitado = view.findViewById(R.id.cardAvisoInvitadoHome);
        btnIrALoginHome = view.findViewById(R.id.btnIrALoginHome);

        // Cards de navegación
        cardMisMascotas = view.findViewById(R.id.cardMisMascotas);
        cardVeterinarios = view.findViewById(R.id.cardVeterinarios);
        cardCalendario = view.findViewById(R.id.cardCalendario);
        cardEducacion = view.findViewById(R.id.cardEducacion);
        cardTiendas = view.findViewById(R.id.cardTiendas);

        // Zona de consejos y eventos
        tvConsejoDia = view.findViewById(R.id.tvConsejoDia);
        tvSinEventosProximos = view.findViewById(R.id.tvSinEventosProximos);
        rvProximosEventos = view.findViewById(R.id.rvProximosEventos);

        // Nota rápida
        etNotaRapida = view.findViewById(R.id.etNotaRapida);
        btnSeleccionarFecha = view.findViewById(R.id.btnSeleccionarFecha);
        btnGuardarNota = view.findViewById(R.id.btnGuardarNota);

        // Configuración inicial RecyclerView
        eventoAdapter = new EventoAdapter();
        rvProximosEventos.setLayoutManager(new LinearLayoutManager(getContext()));
        rvProximosEventos.setAdapter(eventoAdapter);
    }

    private void configurarModoInvitado() {
        // Ocultar sección de eventos y notas personales
        layoutFuncionesPrivadas.setVisibility(View.GONE);
        // Mostrar tarjeta informativa de invitación al login
        cardAvisoInvitado.setVisibility(View.VISIBLE);

        btnIrALoginHome.setOnClickListener(v -> irALogin());
    }

    private void configurarModoUsuario() {
        layoutFuncionesPrivadas.setVisibility(View.VISIBLE);
        cardAvisoInvitado.setVisibility(View.GONE);

        // Cargar nombres de mascotas para el adaptador
        mascotaRepository.escucharMascotas(currentUser.getUid(), new MascotaRepository.LeerMascotasCallback() {
            @Override
            public void onResultado(List<Mascota> listaMascotas) {
                eventoAdapter.setMascotas(listaMascotas);
            }
            @Override
            public void onError(String error) {}
        });

        cargarProximosEventos();

        // Lógica de fecha para nota
        btnSeleccionarFecha.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new DatePickerDialog(requireContext(), (dp, year, month, day) -> {
                fechaNotaSeleccionada = String.format(Locale.getDefault(), "%02d/%02d/%d", day, month + 1, year);
                btnSeleccionarFecha.setText(String.format(Locale.getDefault(), "%02d/%02d", day, month + 1));
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });

        // Lógica de guardar nota
        btnGuardarNota.setOnClickListener(v -> {
            String texto = etNotaRapida.getText().toString().trim();
            if (texto.isEmpty()) {
                etNotaRapida.setError("Escribe algo primero");
                return;
            }
            Evento nota = new Evento(null, texto, fechaNotaSeleccionada, "", "Nota", "");
            eventoRepository.guardarEvento(currentUser.getUid(), nota, new EventoRepository.AccionCallback() {
                @Override
                public void onExito() {
                    Toast.makeText(getContext(), "Nota guardada", Toast.LENGTH_SHORT).show();
                    etNotaRapida.setText("");
                    btnSeleccionarFecha.setText("Hoy");
                    cargarProximosEventos();
                }
                @Override
                public void onError(String error) {
                    Toast.makeText(getContext(), "Error al guardar", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void configurarNavegacionBotones() {
        cardMisMascotas.setOnClickListener(v -> { cambiarTab(R.id.nav_pets); });
        cardCalendario.setOnClickListener(v -> { cambiarTab(R.id.nav_calendar); });
        cardVeterinarios.setOnClickListener(v -> abrirFragmento(new VeterinariosFragment()));
        cardEducacion.setOnClickListener(v -> abrirFragmento(new EducacionFragment()));
        cardTiendas.setOnClickListener(v -> Toast.makeText(getContext(), "Servicios cercanos próximamente", Toast.LENGTH_SHORT).show());
    }

    private void cambiarTab(int menuId) {
        if (getActivity() != null) {
            BottomNavigationView bottomNav = getActivity().findViewById(R.id.bottom_navigation);
            if (bottomNav != null) bottomNav.setSelectedItemId(menuId);
        }
    }

    private void abrirFragmento(Fragment fragment) {
        if (getActivity() != null) {
            getActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.frame_container, fragment)
                    .addToBackStack(null)
                    .commit();
        }
    }

    private void irALogin() {
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        if (getActivity() != null) getActivity().finish();
    }
    private void cargarProximosEventos() {
        eventoRepository.obtenerTodosLosEventos(currentUser.getUid(), new EventoRepository.LeerEventosCallback() {
            @Override
            public void onResultado(List<Evento> listaEventos) {
                if (listaEventos.isEmpty()) { mostrarEventosVacios(); return; }

                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                Calendar hoyCal = Calendar.getInstance();
                hoyCal.set(Calendar.HOUR_OF_DAY, 0); hoyCal.set(Calendar.MINUTE, 0); hoyCal.set(Calendar.SECOND, 0); hoyCal.set(Calendar.MILLISECOND, 0);
                long inicioHoyMs = hoyCal.getTimeInMillis();

                List<Evento> eventosFuturos = new ArrayList<>();
                for (Evento e : listaEventos) {
                    try {
                        Date fecha = sdf.parse(e.getFecha());
                        if (fecha != null && fecha.getTime() >= inicioHoyMs) eventosFuturos.add(e);
                    } catch (ParseException ex) { ex.printStackTrace(); }
                }

                Collections.sort(eventosFuturos, (e1, e2) -> {
                    try { return sdf.parse(e1.getFecha()).compareTo(sdf.parse(e2.getFecha())); }
                    catch (ParseException ex) { return 0; }
                });

                List<Evento> top3 = eventosFuturos.subList(0, Math.min(3, eventosFuturos.size()));

                if (top3.isEmpty()) {
                    mostrarEventosVacios();
                } else {
                    eventoAdapter.setEventos(top3);
                    rvProximosEventos.setVisibility(View.VISIBLE);
                    tvSinEventosProximos.setVisibility(View.GONE);
                }
            }
            @Override
            public void onError(String error) { mostrarEventosVacios(); }
        });
    }

    private void mostrarEventosVacios() {
        rvProximosEventos.setVisibility(View.GONE);
        tvSinEventosProximos.setVisibility(View.VISIBLE);
    }

    private void cargarConsejoAleatorio() {
        DatabaseReference curiosidadesRef = FirebaseDatabase.getInstance().getReference("curiosidades");
        curiosidadesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<String> lista = new ArrayList<>();
                for (DataSnapshot data : snapshot.getChildren()) {
                    String s = data.getValue(String.class);
                    if (s != null) lista.add(s);
                }
                if (!lista.isEmpty()) tvConsejoDia.setText(lista.get(new Random().nextInt(lista.size())));
                else tvConsejoDia.setText("¡Bienvenido a EasyPets! Mantén a tu mascota siempre feliz.");
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                tvConsejoDia.setText("Las 'huellas dactilares' de los perros se encuentran en su nariz. ¡No hay dos narices iguales!");
            }
        });
    }
}