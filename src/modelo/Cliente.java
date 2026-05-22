package modelo;

import java.util.ArrayList;

// ============================================================
// CLASE: Cliente (Class)
// DESCRIPCION: Guarda los datos de un cliente del taller.
//              Cada cliente puede tener varios vehiculos.
// ============================================================
public class Cliente {

    private int id;
    private String nombre;
    private String telefono;
    private ArrayList<Vehiculo> vehiculos;

    public Cliente(int id, String nombre, String telefono) {
        this.id = id;
        this.nombre = nombre;
        this.telefono = telefono;
        this.vehiculos = new ArrayList<>();
    }

    public int getId()                      { return id; }
    public String getNombre()               { return nombre; }
    public void setNombre(String nombre)    { this.nombre = nombre; }
    public String getTelefono()             { return telefono; }
    public void setTelefono(String tel)     { this.telefono = tel; }
    public ArrayList<Vehiculo> getVehiculos() { return vehiculos; }

    public void agregarVehiculo(Vehiculo v) { vehiculos.add(v); }

    @Override
    public String toString() {
        return id + " - " + nombre + " (" + telefono + ")";
    }
}
