package modelo;

// ============================================================
// CLASE: Pago
// DESCRIPCION: Representa un pago registrado contra una orden.
//              Compatible con VentanaPagos y SistemaTaller.
// ============================================================
public class Pago {

    private int          id;
    private String       fecha;    // String para compatibilidad con código existente
    private double       monto;
    private String       metodo;
    private OrdenServicio orden;   // referencia al objeto OrdenServicio
    private int          idOrden;  // ID para operaciones con BD

    // Constructor que usa VentanaPagos: new Pago("Hoy", monto, "Efectivo", orden)
    public Pago(String fecha, double monto, String metodo, OrdenServicio orden) {
        this.fecha   = fecha;
        this.monto   = monto;
        this.metodo  = metodo;
        this.orden   = orden;
        this.idOrden = orden != null ? orden.getId() : 0;
    }

    // Constructor completo para cargar desde BD
    public Pago(int id, String fecha, double monto, String metodo, int idOrden) {
        this.id      = id;
        this.fecha   = fecha;
        this.monto   = monto;
        this.metodo  = metodo;
        this.idOrden = idOrden;
    }

    // Constructor vacío
    public Pago() {}

    // ─── Getters / Setters ───
    public int           getId()                   { return id; }
    public void          setId(int id)             { this.id = id; }

    public String        getFecha()                { return fecha; }
    public void          setFecha(String f)        { this.fecha = f; }

    public double        getMonto()                { return monto; }
    public void          setMonto(double m)        { this.monto = m; }

    public String        getMetodo()               { return metodo; }
    public void          setMetodo(String m)       { this.metodo = m; }

    public OrdenServicio getOrden()                { return orden; }
    public void          setOrden(OrdenServicio o) { this.orden = o; this.idOrden = o != null ? o.getId() : 0; }

    public int           getIdOrden()              { return idOrden; }
    public void          setIdOrden(int id)        { this.idOrden = id; }

    @Override
    public String toString() {
        return "Pago{fecha=" + fecha + ", monto=$" + monto + ", metodo=" + metodo + "}";
    }
}
