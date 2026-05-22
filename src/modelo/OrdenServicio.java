package modelo;

import java.util.ArrayList;

// ============================================================
// CLASE: OrdenServicio
// DESCRIPCION: Orden de trabajo del taller.
//              Versión corregida: agrega setId, setDescripcion
//              y constructor vacío que necesitan otras clases.
// ============================================================
public class OrdenServicio {

    private int    id;
    private String diagnostico;
    private String descripcion; // alias de diagnostico para compatibilidad
    private String estado;
    private String fechaIngreso;
    private String fechaEntrega;
    private Cliente  cliente;
    private Vehiculo vehiculo;
    private ArrayList<DetalleOrden> detalles;
    private double total;

    // Constructor completo (el que ya tenían)
    public OrdenServicio(int id, String diagnostico, String estado,
                         String fechaIngreso, String fechaEntrega,
                         Cliente cliente, Vehiculo vehiculo) {
        this.id           = id;
        this.diagnostico  = diagnostico;
        this.descripcion  = diagnostico;
        this.estado       = estado;
        this.fechaIngreso = fechaIngreso;
        this.fechaEntrega = fechaEntrega;
        this.cliente      = cliente;
        this.vehiculo     = vehiculo;
        this.detalles     = new ArrayList<>();
        this.total        = 0.0;
    }

    // Constructor vacío (necesario para cargar desde BD)
    public OrdenServicio() {
        this.detalles = new ArrayList<>();
    }

    // ─── Getters / Setters ───
    public int    getId()                    { return id; }
    public void   setId(int id)              { this.id = id; }

    public String getDiagnostico()           { return diagnostico; }
    public void   setDiagnostico(String v)   { this.diagnostico = v; this.descripcion = v; }

    // alias para compatibilidad con código que llama getDescripcion/setDescripcion
    public String getDescripcion()           { return diagnostico; }
    public void   setDescripcion(String v)   { this.diagnostico = v; this.descripcion = v; }

    public String getEstado()                { return estado; }
    public void   setEstado(String v)        { this.estado = v; }

    public String getFechaIngreso()          { return fechaIngreso; }
    public void   setFechaIngreso(String v)  { this.fechaIngreso = v; }

    public String getFechaEntrega()          { return fechaEntrega; }
    public void   setFechaEntrega(String v)  { this.fechaEntrega = v; }

    public Cliente  getCliente()             { return cliente; }
    public void     setCliente(Cliente c)    { this.cliente = c; }

    public Vehiculo getVehiculo()            { return vehiculo; }
    public void     setVehiculo(Vehiculo v)  { this.vehiculo = v; }

    public ArrayList<DetalleOrden> getDetalles() { return detalles; }
    public double   getTotal()               { return total; }
    public void     setTotal(double t)       { this.total = t; }

    public void agregarDetalle(DetalleOrden d) {
        detalles.add(d);
        calcularTotal();
    }

    public void eliminarDetalle(int index) {
        if (index >= 0 && index < detalles.size()) {
            detalles.remove(index);
            calcularTotal();
        }
    }

    private void calcularTotal() {
        total = 0;
        for (DetalleOrden d : detalles) total += d.getSubtotal();
    }

    @Override
    public String toString() {
        String nombreCliente = cliente != null ? cliente.getNombre() : "Sin cliente";
        String infoVehiculo  = vehiculo != null ? vehiculo.toString() : "Sin vehículo";
        return "Orden #" + id + " - " + nombreCliente + " - " + infoVehiculo;
    }
}
