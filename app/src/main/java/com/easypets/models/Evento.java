package com.easypets.models;

/**
 * Modelo de datos que representa un evento o cita programada en la agenda del usuario.
 * Actúa como entidad (POJO) para la serialización y deserialización automática
 * con Firebase Realtime Database. Permite asociar recordatorios temporales
 * (veterinario, peluquería, paseos, etc.) a una mascota específica.
 */
public class Evento {

    private String id;
    private String titulo;
    private String fecha;
    private String hora;
    private String tipo;
    private String idMascota;

    /**
     * Constructor sin argumentos.
     * Requerido obligatoriamente por el SDK de Firebase para permitir la instanciación
     * automática del objeto al recuperar los datos (DataSnapshot) mediante reflexión.
     */
    public Evento() {
    }

    /**
     * Constructor parametrizado para inicializar un nuevo evento antes de su persistencia.
     *
     * @param id Identificador único del evento en la base de datos.
     * @param titulo Nombre descriptivo o título de la cita.
     * @param fecha Cadena de texto representativa de la fecha (formato esperado: "dd/MM/yyyy").
     * @param hora Cadena de texto representativa de la hora exacta (formato esperado: "HH:mm", o cadena vacía).
     * @param tipo Categoría o tipología del evento (ej. "Veterinario", "Peluquería", "Guardería").
     * @param idMascota Identificador (UID) de la mascota asociada a este evento.
     */
    public Evento(String id, String titulo, String fecha, String hora, String tipo, String idMascota) {
        this.id = id;
        this.titulo = titulo;
        this.fecha = fecha;
        this.hora = hora;
        this.tipo = tipo;
        this.idMascota = idMascota;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }

    public String getHora() { return hora; }
    public void setHora(String hora) { this.hora = hora; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getIdMascota() { return idMascota; }
    public void setIdMascota(String idMascota) { this.idMascota = idMascota; }
}