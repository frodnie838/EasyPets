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

import com.easypets.R;
import com.easypets.models.Evento;
import com.easypets.repositories.EventoRepository;
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
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class HomeFragment extends Fragment {

    // Autenticación
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;

    // Repositorio de Eventos (para la nota rápida y el evento próximo)
    private EventoRepository eventoRepository;

    // Vistas del XML
    private CardView cardMisMascotas, cardVeterinarios, cardCalendario, cardEducacion, cardTiendas, cardEventoDestacado;
    private TextView tvEventoDia, tvEventoMes, tvEventoTitulo, tvEventoHora, tvConsejoDia;
    private EditText etNotaRapida;
    private MaterialButton btnSeleccionarFecha, btnGuardarNota;

    private String fechaNotaSeleccionada = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        eventoRepository = new EventoRepository();

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

    private void vincularVistas(View view) {
        cardMisMascotas = view.findViewById(R.id.cardMisMascotas);
        cardVeterinarios = view.findViewById(R.id.cardVeterinarios);
        cardCalendario = view.findViewById(R.id.cardCalendario);
        cardEducacion = view.findViewById(R.id.cardEducacion);
        cardTiendas = view.findViewById(R.id.cardTiendas);
        cardEventoDestacado = view.findViewById(R.id.cardEventoDestacado);

        tvEventoDia = view.findViewById(R.id.tvEventoDia);
        tvEventoMes = view.findViewById(R.id.tvEventoMes);
        tvEventoTitulo = view.findViewById(R.id.tvEventoTitulo);
        tvEventoHora = view.findViewById(R.id.tvEventoHora);
        tvConsejoDia = view.findViewById(R.id.tvConsejoDia);

        etNotaRapida = view.findViewById(R.id.etNotaRapida);
        btnSeleccionarFecha = view.findViewById(R.id.btnSeleccionarFecha);
        btnGuardarNota = view.findViewById(R.id.btnGuardarNota);
    }

    private void configurarModoInvitado() {
        tvEventoDia.setText("-");
        tvEventoMes.setText("");
        tvEventoTitulo.setText("Inicia sesión para ver eventos");
        tvEventoHora.setText("Solo para usuarios registrados");

        etNotaRapida.setEnabled(false);
        etNotaRapida.setHint("Regístrate para usar notas");
        btnSeleccionarFecha.setEnabled(false);
        btnGuardarNota.setEnabled(false);
    }

    private void configurarModoUsuario() {
        cargarProximoEvento();

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
                    cargarProximoEvento();
                }
                @Override
                public void onError(String error) {
                    Toast.makeText(getContext(), "Error al guardar", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void cargarProximoEvento() {
        eventoRepository.obtenerTodosLosEventos(currentUser.getUid(), new EventoRepository.LeerEventosCallback() {
            @Override
            public void onResultado(List<Evento> listaEventos) {
                if (listaEventos.isEmpty()) {
                    mostrarEventoVacio();
                    return;
                }

                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                Date hoy = new Date();
                Evento eventoMasProximo = null;
                long menorDiferencia = Long.MAX_VALUE;

                for (Evento e : listaEventos) {
                    try {
                        Date fechaEvento = sdf.parse(e.getFecha());
                        if (fechaEvento != null) {
                            long diferencia = fechaEvento.getTime() - hoy.getTime();
                            if (diferencia > -86400000 && diferencia < menorDiferencia) {
                                menorDiferencia = diferencia;
                                eventoMasProximo = e;
                            }
                        }
                    } catch (ParseException ex) {
                        ex.printStackTrace();
                    }
                }

                if (eventoMasProximo != null) {
                    String[] partesFecha = eventoMasProximo.getFecha().split("/");
                    tvEventoDia.setText(partesFecha[0]);
                    String[] meses = {"Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"};
                    tvEventoMes.setText(meses[Integer.parseInt(partesFecha[1]) - 1]);
                    tvEventoTitulo.setText(eventoMasProximo.getTitulo());

                    String detalle = eventoMasProximo.getTipo();
                    if (eventoMasProximo.getHora() != null && !eventoMasProximo.getHora().isEmpty()) {
                        detalle = eventoMasProximo.getHora() + " - " + detalle;
                    }
                    tvEventoHora.setText(detalle);
                } else {
                    mostrarEventoVacio();
                }
            }

            @Override
            public void onError(String error) {
                mostrarEventoVacio();
            }
        });
    }

    private void mostrarEventoVacio() {
        tvEventoDia.setText("-");
        tvEventoMes.setText("");
        tvEventoTitulo.setText("Sin eventos próximos");
        tvEventoHora.setText("Tienes tu agenda libre");
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

        // Secciones en construcción
        cardVeterinarios.setOnClickListener(v -> Toast.makeText(getContext(), "Próximamente: Veterinarios", Toast.LENGTH_SHORT).show());
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