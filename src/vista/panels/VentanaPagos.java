package vista.panels;

import modelo.ConexionMySQL;
import modelo.OrdenServicio;
import modelo.Pago;
import package_sistemaTR.MenuRefaccionaria;
import seguridad.Seguridad;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.sql.*;

// ============================================================
// CLASE: VentanaPagos (corregida)
// Fix: guarda en MySQL, descarga lista desde BD,
//      usa modelo.SistemaTaller con ruta absoluta,
//      estilo visual consistente con el resto del sistema.
// ============================================================
public class VentanaPagos extends JPanel {

    private MenuRefaccionaria menu;
    private DefaultTableModel modeloPagos;
    private JTable            tabla;

    public VentanaPagos(MenuRefaccionaria menu) {
        this.menu = menu;
        setLayout(new BorderLayout(0, 15));
        setBackground(MenuRefaccionaria.BG_DARK);
        setBorder(new EmptyBorder(25, 30, 25, 30));

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel titulo = new JLabel("Registro de Pagos");
        titulo.setFont(new Font("SansSerif", Font.BOLD, 20));
        titulo.setForeground(MenuRefaccionaria.TEXT_WHITE);
        JButton btnRefresh = MenuRefaccionaria.btnPrimario("↻ Actualizar");
        btnRefresh.addActionListener(e -> cargarPagos());
        header.add(titulo,     BorderLayout.WEST);
        header.add(btnRefresh, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        // Tabla
        modeloPagos = new DefaultTableModel(
            new String[]{"ID", "Fecha", "Monto", "Método", "Orden #"}, 0
        ) { @Override public boolean isCellEditable(int r, int c) { return false; } };

        tabla = new JTable(modeloPagos);
        tabla.setBackground(MenuRefaccionaria.BG_CARD);
        tabla.setForeground(MenuRefaccionaria.TEXT_WHITE);
        tabla.setFont(new Font("SansSerif", Font.PLAIN, 13));
        tabla.setRowHeight(32);
        tabla.getTableHeader().setBackground(MenuRefaccionaria.BG_CARD2);
        tabla.getTableHeader().setForeground(MenuRefaccionaria.TEXT_MUTED);
        tabla.setSelectionBackground(MenuRefaccionaria.ACCENT_BLUE);
        tabla.setGridColor(new Color(40, 48, 80));

        JScrollPane sc = new JScrollPane(tabla) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(MenuRefaccionaria.BG_CARD);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 18, 18));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        sc.getViewport().setBackground(MenuRefaccionaria.BG_CARD);
        sc.setBorder(null);
        add(sc, BorderLayout.CENTER);

        add(crearFormPago(), BorderLayout.SOUTH);
        cargarPagos();
    }

    // ─── CARGAR DESDE MYSQL ───
    private void cargarPagos() {
        modeloPagos.setRowCount(0);
        modelo.SistemaTaller.pagos.clear();

        try (Connection con = ConexionMySQL.conectar()) {
            if (con == null) return;
            ResultSet rs = con.createStatement().executeQuery(
                "SELECT * FROM pagos ORDER BY id DESC"
            );
            while (rs.next()) {
                Pago p = new Pago(
                    rs.getInt("id"),
                    rs.getString("fecha"),
                    rs.getDouble("monto"),
                    rs.getString("metodo"),
                    rs.getInt("id_orden")
                );
                modelo.SistemaTaller.pagos.add(p);
                modeloPagos.addRow(new Object[]{
                    p.getId(),
                    p.getFecha(),
                    String.format("$%.2f", p.getMonto()),
                    p.getMetodo().toUpperCase(),
                    p.getIdOrden()
                });
            }
        } catch (Exception ex) {
            System.err.println("Error cargando pagos: " + ex.getMessage());
        }
    }

    // ─── FORMULARIO ───
    private JPanel crearFormPago() {
        JPanel form = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(MenuRefaccionaria.BG_CARD);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 18, 18));
                g2.dispose();
            }
        };
        form.setOpaque(false);
        form.setLayout(new FlowLayout(FlowLayout.LEFT, 12, 12));

        JTextField txtOrden = MenuRefaccionaria.textField("Nº Orden");
        txtOrden.setPreferredSize(new Dimension(100, 36));

        JTextField txtMonto = MenuRefaccionaria.textField("Monto");
        txtMonto.setPreferredSize(new Dimension(120, 36));

        String[] metodos = {"EFECTIVO", "TARJETA", "TRANSFERENCIA"};
        JComboBox<String> cbMetodo = new JComboBox<>(metodos);
        cbMetodo.setBackground(MenuRefaccionaria.BG_CARD2);
        cbMetodo.setForeground(MenuRefaccionaria.TEXT_WHITE);
        cbMetodo.setFont(new Font("SansSerif", Font.PLAIN, 13));
        cbMetodo.setPreferredSize(new Dimension(150, 36));

        JButton btnGuardar = MenuRefaccionaria.btnPrimario("Registrar Pago");
        btnGuardar.addActionListener(e -> registrar(txtOrden, txtMonto, cbMetodo));

        JLabel lbl = new JLabel("Nuevo pago:");
        lbl.setForeground(MenuRefaccionaria.TEXT_MUTED);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 13));

        form.add(lbl);
        form.add(txtOrden);
        form.add(txtMonto);
        form.add(cbMetodo);
        form.add(btnGuardar);
        return form;
    }

    private void registrar(JTextField txtOrden, JTextField txtMonto, JComboBox<String> cbMetodo) {
        String ordenStr = txtOrden.getText().trim();
        String montoStr = txtMonto.getText().trim();

        if (ordenStr.isEmpty() || ordenStr.equals("Nº Orden")) {
            JOptionPane.showMessageDialog(this, "Ingresa el número de orden."); return;
        }
        if (montoStr.isEmpty() || montoStr.equals("Monto")) {
            JOptionPane.showMessageDialog(this, "Ingresa el monto."); return;
        }

        try {
            int    idOrden = Integer.parseInt(ordenStr);
            double monto   = Double.parseDouble(montoStr.replace(",", "."));
            String metodo  = (String) cbMetodo.getSelectedItem();

            try (Connection con = ConexionMySQL.conectar()) {
                if (con == null) { JOptionPane.showMessageDialog(this, "Sin conexión a BD."); return; }

                // Verificar que la orden existe
                PreparedStatement check = con.prepareStatement(
                    "SELECT id FROM ordenes WHERE id = ?"
                );
                check.setInt(1, idOrden);
                if (!check.executeQuery().next()) {
                    JOptionPane.showMessageDialog(this, "No existe la orden #" + idOrden + ".");
                    return;
                }

                // Insertar pago
                PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO pagos (fecha, monto, metodo, id_orden) VALUES (CURDATE(), ?, ?, ?)"
                );
                ps.setDouble(1, monto);
                ps.setString(2, metodo);
                ps.setInt(3, idOrden);
                ps.executeUpdate();

                // También agregar en memoria para SistemaTaller
                OrdenServicio orden = modelo.SistemaTaller.buscarOrdenPorId(idOrden);
                Pago p = new Pago("Hoy", monto, metodo, orden);
                modelo.SistemaTaller.pagos.add(p);

                Seguridad.registrarAuditoria("PAGO_REGISTRADO",
                    "Orden #" + idOrden + " | $" + monto + " | " + metodo);
                JOptionPane.showMessageDialog(this, "Pago registrado correctamente.");

                // Limpiar campos
                txtOrden.setText("Nº Orden");
                txtOrden.setForeground(MenuRefaccionaria.TEXT_MUTED);
                txtMonto.setText("Monto");
                txtMonto.setForeground(MenuRefaccionaria.TEXT_MUTED);
                cargarPagos();
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "El número de orden y el monto deben ser números válidos.");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }
}
