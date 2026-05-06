package com.easypets.ui.servicios;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.easypets.R;
import com.easypets.ui.servicios.adiestradores.AdiestradoresFragment;
import com.easypets.ui.servicios.farmacias.FarmaciasFragment;
import com.easypets.ui.servicios.guarderias.GuarderiasFragment;
import com.easypets.ui.servicios.parques.ParquesFragment;
import com.easypets.ui.servicios.paseadores.PaseadoresFragment;
import com.easypets.ui.servicios.peluquerias.PeluqueriasFragment;
import com.easypets.ui.servicios.protectoras.ProtectorasFragment;
import com.easypets.ui.servicios.tiendas.TiendasFragment;
import com.easypets.ui.servicios.veterinarios.VeterinariosFragment;

/**
 * Fragmento contenedor que actúa como panel principal (Dashboard) para el módulo de servicios.
 * Orquesta la navegación hacia los diferentes sub-fragmentos especializados mediante
 * un sistema de tarjetas interactivas clasificadas por tipología de servicio.
 */
public class ServiciosFragment extends Fragment {

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_servicios, container, false);

        configurarTarjeta(root.findViewById(R.id.cardVeterinarios), "Veterinarios", R.drawable.veterinario, v ->
                abrirFragmento(new VeterinariosFragment())
        );
        configurarTarjeta(root.findViewById(R.id.cardFarmacias), "Farmacias", R.drawable.farmacia, v ->
                abrirFragmento(new FarmaciasFragment())
        );

        configurarTarjeta(root.findViewById(R.id.cardGuarderias), "Guarderías", R.drawable.guarderia, v ->
                abrirFragmento(new GuarderiasFragment())
        );
        configurarTarjeta(root.findViewById(R.id.cardPeluquerias), "Peluquerías", R.drawable.peluqueria, v ->
                abrirFragmento(new PeluqueriasFragment())
        );

        configurarTarjeta(root.findViewById(R.id.cardAdiestradores), "Adiestradores", R.drawable.adiestrador, v ->
                abrirFragmento(new AdiestradoresFragment())
        );
        configurarTarjeta(root.findViewById(R.id.cardPaseadores), "Paseadores", R.drawable.paseador, v ->
                abrirFragmento(new PaseadoresFragment())
        );

        configurarTarjeta(root.findViewById(R.id.cardTiendas), "Tiendas", R.drawable.tienda, v ->
                abrirFragmento(new TiendasFragment())
        );
        configurarTarjeta(root.findViewById(R.id.cardParques), "Parques Caninos", R.drawable.parque, v ->
                abrirFragmento(new ParquesFragment())
        );

        configurarTarjeta(root.findViewById(R.id.cardProtectoras), "Protectoras y Adopción", R.drawable.protectora, v ->
                abrirFragmento(new ProtectorasFragment())
        );

        return root;
    }

    /**
     * Inicializa los componentes visuales de una tarjeta interactiva, inyectando el
     * icono, el título y el manejador de eventos correspondiente.
     *
     * @param cardView Contenedor principal de la tarjeta a configurar.
     * @param titulo Cadena de texto a mostrar en la tarjeta.
     * @param iconoRes Identificador del recurso gráfico a inyectar.
     * @param listener Manejador de clics para ejecutar la navegación.
     */
    private void configurarTarjeta(View cardView, String titulo, int iconoRes, View.OnClickListener listener) {
        if (cardView == null) return;
        ImageView icono = cardView.findViewById(R.id.ivIconoDashboard);
        TextView texto = cardView.findViewById(R.id.tvTituloDashboard);

        icono.setImageResource(iconoRes);
        texto.setText(titulo);
        cardView.setOnClickListener(listener);
    }

    /**
     * Gestiona la transacción del gestor de fragmentos para apilar la nueva vista
     * en la pila de navegación (BackStack), permitiendo el retorno a este menú principal.
     *
     * @param fragment Instancia del nuevo fragmento a cargar en el contenedor principal.
     */
    private void abrirFragmento(Fragment fragment) {
        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.frame_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    private void mostrarMensaje(String mensaje) {
        Toast.makeText(getContext(), mensaje, Toast.LENGTH_SHORT).show();
    }
}