package com.easypets.models;

/**
 * Modelo de datos que representa el perfil clínico e identificativo completo de una mascota.
 * Actúa como entidad principal (POJO) para la serialización y deserialización bidireccional
 * con Firebase Realtime Database. Estructura la información en diferentes dominios:
 * biográficos, físicos, identificadores legales (microchip), historial médico y multimedia.
 */
public class Mascota {

    private String idMascota;
    private String nombre;
    private String especie;
    private String raza;
    private String sexo;
    private String fechaNacimiento;
    private String color;

    private String peso;
    private String microchip;

    private boolean esterilizado;
    private String patologias;
    private String alergias;
    private String medicacionActual;

    private String fotoPerfilUrl;

    private long timestamp;

    /**
     * Constructor sin argumentos.
     * Requerido obligatoriamente por el SDK de Firebase para llevar a cabo la instanciación
     * automática del objeto al procesar los nodos (DataSnapshot) mediante reflexión.
     */
    public Mascota() {
    }

    /**
     * Constructor parametrizado para la inicialización integral de un perfil de mascota
     * antes de persistir los datos en el servidor.
     *
     * @param idMascota Identificador único generado para la mascota.
     * @param nombre Nombre de pila de la mascota.
     * @param especie Clasificación biológica o especie (ej. Perro, Gato).
     * @param raza Raza específica o variante.
     * @param sexo Género biológico de la mascota.
     * @param fechaNacimiento Cadena representativa de la fecha de nacimiento.
     * @param color Patrón o tonalidad principal del pelaje/piel.
     * @param peso Registro del peso corporal actual.
     * @param microchip Secuencia alfanumérica del identificador legal (microchip).
     * @param esterilizado Estado de la intervención de esterilización o castración.
     * @param patologias Registro de enfermedades crónicas o condiciones relevantes.
     * @param fotoPerfilUrl URL o cadena en Base64 de la fotografía principal de la mascota.
     * @param timestamp Marca temporal (UNIX epoch) de la creación del perfil.
     */
    public Mascota(String idMascota, String nombre, String especie, String raza, String sexo,
                   String fechaNacimiento, String color, String peso, String microchip,
                   boolean esterilizado, String patologias, String fotoPerfilUrl, long timestamp) {
        this.idMascota = idMascota;
        this.nombre = nombre;
        this.especie = especie;
        this.raza = raza;
        this.sexo = sexo;
        this.fechaNacimiento = fechaNacimiento;
        this.color = color;
        this.peso = peso;
        this.microchip = microchip;
        this.esterilizado = esterilizado;
        this.patologias = patologias;
        this.fotoPerfilUrl = fotoPerfilUrl;
        this.timestamp = timestamp;
    }

    public String getIdMascota() { return idMascota; }
    public void setIdMascota(String idMascota) { this.idMascota = idMascota; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getEspecie() { return especie; }
    public void setEspecie(String especie) { this.especie = especie; }

    public String getRaza() { return raza; }
    public void setRaza(String raza) { this.raza = raza; }

    public String getSexo() { return sexo; }
    public void setSexo(String sexo) { this.sexo = sexo; }

    public String getFechaNacimiento() { return fechaNacimiento; }
    public void setFechaNacimiento(String fechaNacimiento) { this.fechaNacimiento = fechaNacimiento; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public String getPeso() { return peso; }
    public void setPeso(String peso) { this.peso = peso; }

    public String getMicrochip() { return microchip; }
    public void setMicrochip(String microchip) { this.microchip = microchip; }

    public boolean isEsterilizado() { return esterilizado; }
    public void setEsterilizado(boolean esterilizado) { this.esterilizado = esterilizado; }

    public String getPatologias() { return patologias; }
    public void setPatologias(String patologias) { this.patologias = patologias; }

    public String getFotoPerfilUrl() { return fotoPerfilUrl; }
    public void setFotoPerfilUrl(String fotoPerfilUrl) { this.fotoPerfilUrl = fotoPerfilUrl; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getAlergias() { return alergias; }
    public void setAlergias(String alergias) { this.alergias = alergias; }

    public String getMedicacionActual() { return medicacionActual; }
    public void setMedicacionActual(String medicacionActual) { this.medicacionActual = medicacionActual; }
}