package modelo;

import java.util.ArrayList;

// ============================================================
// CLASE: SistemaTaller (Class)
// DESCRIPCION: Almacen central del taller.
//              Guarda las listas globales de clientes,
//              ordenes y pagos. Al ser "static", cualquier
//              clase puede acceder sin crear un objeto.
//
//              PARA MySQL: Al conectar, estas listas se
//              llenaran con datos de la base de datos.
// ============================================================
public class SistemaTaller {

    // TODO: Llenar desde MySQL al iniciar el programa
    public static ArrayList<Cliente>       clientes = new ArrayList<>();
    public static ArrayList<OrdenServicio> ordenes  = new ArrayList<>();
    public static ArrayList<Pago>          pagos    = new ArrayList<>();
	public static Object inventario;
    
    // Busca un cliente por su ID
    public static Cliente buscarClientePorId(int id) {
        for (Cliente c : clientes) {
            if (c.getId() == id) return c;
        }
        return null;
    }

    // Busca una orden por su ID
    public static OrdenServicio buscarOrdenPorId(int id) {
        for (OrdenServicio o : ordenes) {
            if (o.getId() == id) return o;
        }
        return null;
    }

    // Calcula cuanto debe un cliente en total
    public static double calcularDeudaCliente(Cliente cliente) {
        double deuda = 0;
        for (OrdenServicio o : ordenes) {
            if (o.getCliente().equals(cliente)) {
                double pagado = 0;
                for (Pago p : pagos) {
                    if (p.getOrden().equals(o)) pagado += p.getMonto();
                }
                deuda += (o.getTotal() - pagado);
            }
        }
        return deuda;
    }
}
