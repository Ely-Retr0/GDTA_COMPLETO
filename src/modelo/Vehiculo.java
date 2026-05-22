package modelo;

// ============================================================
// CLASE: Vehiculo (Class)
// DESCRIPCION: Guarda los datos de un vehiculo.
//              Cada vehiculo pertenece a un cliente.
// ============================================================
public class Vehiculo {

    private String placas;
    private String marca;
    private String modelo;
    private int anio;
    private String color;
    private Cliente propietario;

    public Vehiculo(String placas, String marca, String modelo,
                    int anio, String color, Cliente propietario) {
        this.placas = placas;
        this.marca = marca;
        this.modelo = modelo;
        this.anio = anio;
        this.color = color;
        this.propietario = propietario;
    }

    public String getPlacas()        { return placas; }
    public String getMarca()         { return marca; }
    public String getModelo()        { return modelo; }
    public int getAnio()             { return anio; }
    public String getColor()         { return color; }
    public Cliente getPropietario()  { return propietario; }

    @Override
    public String toString() {
        return marca + " " + modelo + " " + anio + " (" + placas + ")";
    }
}
