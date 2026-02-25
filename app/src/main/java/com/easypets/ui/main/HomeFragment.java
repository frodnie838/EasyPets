package com.easypets.ui.main;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.easypets.R;
import com.easypets.adapters.EventoAdapter;
import com.easypets.models.Evento;
import com.easypets.models.Mascota;
import com.easypets.repositories.EventoRepository;
import com.easypets.repositories.MascotaRepository;
import com.easypets.ui.auth.LoginActivity;
import com.easypets.ui.mascotas.MascotasFragment;
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
    private CardView cardMisMascotas, cardVeterinarios, cardCalendario, cardEducacion, cardTiendas;
    private TextView tvConsejoDia, tvSinEventosProximos;
    private EditText etNotaRapida;
    private MaterialButton btnSeleccionarFecha, btnGuardarNota;
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

        Calendar hoy = Calendar.getInstance();
        fechaNotaSeleccionada = String.format(Locale.getDefault(), "%02d/%02d/%d", hoy.get(Calendar.DAY_OF_MONTH), hoy.get(Calendar.MONTH) + 1, hoy.get(Calendar.YEAR));

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
        if (getActivity() != null) {
            BottomNavigationView bottomNav = getActivity().findViewById(R.id.bottom_navigation);
            if (bottomNav != null) {
                bottomNav.getMenu().findItem(R.id.nav_home).setChecked(true);
            }
        }
    }
    private void vincularVistas(View view) {
        cardMisMascotas = view.findViewById(R.id.cardMisMascotas);
        cardVeterinarios = view.findViewById(R.id.cardVeterinarios);
        cardCalendario = view.findViewById(R.id.cardCalendario);
        cardEducacion = view.findViewById(R.id.cardEducacion);
        cardTiendas = view.findViewById(R.id.cardTiendas);
        tvConsejoDia = view.findViewById(R.id.tvConsejoDia);

        etNotaRapida = view.findViewById(R.id.etNotaRapida);
        btnSeleccionarFecha = view.findViewById(R.id.btnSeleccionarFecha);
        btnGuardarNota = view.findViewById(R.id.btnGuardarNota);

        rvProximosEventos = view.findViewById(R.id.rvProximosEventos);
        tvSinEventosProximos = view.findViewById(R.id.tvSinEventosProximos);

        eventoAdapter = new EventoAdapter();
        rvProximosEventos.setLayoutManager(new LinearLayoutManager(getContext()));
        rvProximosEventos.setAdapter(eventoAdapter);
    }

    private void configurarModoInvitado() {
        etNotaRapida.setEnabled(false);
        etNotaRapida.setHint("Regístrate para usar notas");
        btnSeleccionarFecha.setEnabled(false);
        btnGuardarNota.setEnabled(false);
        tvSinEventosProximos.setText("Inicia sesión para ver tus eventos");
        tvSinEventosProximos.setVisibility(View.VISIBLE);
        rvProximosEventos.setVisibility(View.GONE);
    }

    private void configurarModoUsuario() {
        // Cargar las mascotas para que el adaptador sepa sus nombres
        mascotaRepository.escucharMascotas(currentUser.getUid(), new MascotaRepository.LeerMascotasCallback() {
            @Override
            public void onResultado(List<Mascota> listaMascotas) {
                eventoAdapter.setMascotas(listaMascotas);
            }
            @Override
            public void onError(String error) {}
        });
        cargarProximosEventos();

        // Configurar Guardado de Nota Rápida
        btnSeleccionarFecha.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new DatePickerDialog(requireContext(), (view, year, month, dayOfMonth) -> {
                fechaNotaSeleccionada = String.format(Locale.getDefault(), "%02d/%02d/%d", dayOfMonth, month + 1, year);
                btnSeleccionarFecha.setText(String.format(Locale.getDefault(), "%02d/%02d", dayOfMonth, month + 1));
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });

        btnGuardarNota.setOnClickListener(v -> {
            String textoNota = etNotaRapida.getText().toString().trim();
            if (textoNota.isEmpty()) {
                etNotaRapida.setError("Escribe algo primero");
                return;
            }

            Evento notaEvento = new Evento(null, textoNota, fechaNotaSeleccionada, "", "Nota", "");
            eventoRepository.guardarEvento(currentUser.getUid(), notaEvento, new EventoRepository.AccionCallback() {
                @Override
                public void onExito() {
                    Toast.makeText(getContext(), "Nota guardada con éxito", Toast.LENGTH_SHORT).show();
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

    private void cargarProximosEventos() {
        eventoRepository.obtenerTodosLosEventos(currentUser.getUid(), new EventoRepository.LeerEventosCallback() {
            @Override
            public void onResultado(List<Evento> listaEventos) {
                if (listaEventos.isEmpty()) {
                    mostrarEventosVacios();
                    return;
                }

                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                Calendar hoyCal = Calendar.getInstance();
                // Ponemos las horas a 0 para que cuenten los eventos de hoy
                hoyCal.set(Calendar.HOUR_OF_DAY, 0);
                hoyCal.set(Calendar.MINUTE, 0);
                hoyCal.set(Calendar.SECOND, 0);
                hoyCal.set(Calendar.MILLISECOND, 0);
                long inicioHoyMs = hoyCal.getTimeInMillis();

                List<Evento> eventosFuturos = new ArrayList<>();

                for (Evento e : listaEventos) {
                    try {
                        Date fechaEvento = sdf.parse(e.getFecha());
                        if (fechaEvento != null && fechaEvento.getTime() >= inicioHoyMs) {
                            eventosFuturos.add(e);
                        }
                    } catch (ParseException ex) {
                        ex.printStackTrace();
                    }
                }

                Collections.sort(eventosFuturos, (e1, e2) -> {
                    try {
                        return sdf.parse(e1.getFecha()).compareTo(sdf.parse(e2.getFecha()));
                    } catch (ParseException ex) {
                        return 0;
                    }
                });

                List<Evento> top3 = new ArrayList<>();
                for (int i = 0; i < Math.min(3, eventosFuturos.size()); i++) {
                    top3.add(eventosFuturos.get(i));
                }

                // Mostrar en la lista
                if (top3.isEmpty()) {
                    mostrarEventosVacios();
                } else {
                    eventoAdapter.setEventos(top3);
                    rvProximosEventos.setVisibility(View.VISIBLE);
                    tvSinEventosProximos.setVisibility(View.GONE);
                }
            }

            @Override
            public void onError(String error) {
                mostrarEventosVacios();
            }
        });
    }

    private void mostrarEventosVacios() {
        rvProximosEventos.setVisibility(View.GONE);
        tvSinEventosProximos.setVisibility(View.VISIBLE);
    }

    private void configurarNavegacionBotones() {
        // Enlazar Tarjeta de Mascotas
        cardMisMascotas.setOnClickListener(v -> {
            if (currentUser == null) {
                mostrarAvisoRegistro("Mis Mascotas");
                return;
            }
            BottomNavigationView bottomNav = getActivity().findViewById(R.id.bottom_navigation);
            if (bottomNav != null) {
                bottomNav.setSelectedItemId(R.id.nav_pets);
            }
        });

        cardCalendario.setOnClickListener(v -> {
            if (currentUser == null) {
                mostrarAvisoRegistro("Calendario");
                return;
            }
            BottomNavigationView bottomNav = getActivity().findViewById(R.id.bottom_navigation);
            if (bottomNav != null) {
                bottomNav.setSelectedItemId(R.id.nav_calendar);
            }
        });

        // Abrir el buscador de Veterinarios al pulsar la tarjeta
        cardVeterinarios.setOnClickListener(v -> {
            if (getActivity() != null) {
                VeterinariosFragment veterinariosFragment = new VeterinariosFragment();
                getActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.frame_container, veterinariosFragment) // Sustituye la pantalla actual
                        .addToBackStack(null) // ¡Súper importante! Para que el botón "Atrás" del móvil te devuelva al Home
                        .commit();
            }
        });
        // Secciones en construcción
        cardEducacion.setOnClickListener(v -> Toast.makeText(getContext(), "Próximamente: Educación", Toast.LENGTH_SHORT).show());
        cardTiendas.setOnClickListener(v -> Toast.makeText(getContext(), "Próximamente: Tiendas", Toast.LENGTH_SHORT).show());
    }

    private void mostrarAvisoRegistro(String funcion) {
        new AlertDialog.Builder(getContext())
                .setTitle("¡Crea una cuenta gratuita!")
                .setMessage("La función '" + funcion + "' requiere que inicies sesión.")
                .setPositiveButton("Registrarme", (dialog, which) -> {
                    mAuth.signOut();
                    Intent intent = new Intent(getActivity(), LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    getActivity().finish();
                })
                .setNegativeButton("Más tarde", null)
                .show();
    }

    private void cargarConsejoAleatorio() {
        DatabaseReference curiosidadesRef = FirebaseDatabase.getInstance().getReference("curiosidades");

        curiosidadesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<String> listaCuriosidades = new ArrayList<>();

                // Recoger todas las curiosidades de la base de datos
                for (DataSnapshot data : snapshot.getChildren()) {
                    String curiosidad = data.getValue(String.class);
                    if (curiosidad != null) {
                        listaCuriosidades.add(curiosidad);
                    }
                }

                // Elegir una al azar si hay alguna en la lista
                if (!listaCuriosidades.isEmpty()) {
                    int indiceAleatorio = new Random().nextInt(listaCuriosidades.size());
                    tvConsejoDia.setText(listaCuriosidades.get(indiceAleatorio));
                } else {
                    // Texto por defecto si la base de datos está vacía
                    tvConsejoDia.setText("¡Bienvenido a EasyPets! Mantén a tu mascota siempre feliz y sana.");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Texto por defecto si falla la conexión
                tvConsejoDia.setText("El chocolate y la cebolla son muy tóxicos para perros y gatos. ¡Mantenlos fuera de su alcance!");
            }
        });
    }
}