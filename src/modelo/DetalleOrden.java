package modelo;

// ============================================================
// CLASE: DetalleOrden (Class)
// DESCRIPCION: Un material o servicio dentro de una orden.
//              El subtotal se calcula automaticamente.
// ============================================================
public class DetalleOrden {

    private String descripcion;
    private int cantidad;
    private double precioUnitario;
    private double subtotal;

    public DetalleOrden(String descripcion, int cantidad, double precioUnitario) {
        this.descripcion = descripcion;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
        this.subtotal = cantidad * precioUnitario;
    }

    public String getDescripcion()     { return descripcion; }
    public int getCantidad()           { return cantidad; }
    public double getPrecioUnitario()  { return precioUnitario; }
    public double getSubtotal()        { return subtotal; }

    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
        this.subtotal = cantidad * precioUnitario;
    }

    public void setPrecioUnitario(double precio) {
        this.precioUnitario = precio;
        this.subtotal = cantidad * precio;
    }
}
