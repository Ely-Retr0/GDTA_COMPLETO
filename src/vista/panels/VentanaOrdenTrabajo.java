package vista.panels;

import modelo.Cliente;
import modelo.ConexionMySQL;
import modelo.OrdenServicio;
import package_sistemaTR.MenuRefaccionaria;
import seguridad.Seguridad;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.sql.*;

// ============================================================
// CLASE: VentanaOrdenTrabajo (corregida)
// Fix: combo de clientes carga desde MySQL (no depende de
//      lista en memoria que podría estar vacía),
//      usa modelo.SistemaTaller con ruta absoluta,
//      guarda en BD, mayúsculas.
// ============================================================
public class VentanaOrdenTrabajo extends JPanel {

    private MenuRefaccionaria menu;
    private DefaultTableModel modeloOrdenes;
    private JTable            tabla;
    private JComboBox<String[]> cbClientes;

    public VentanaOrdenTrabajo(MenuRefaccionaria menu) {
        this.menu = menu;
        setLayout(new BorderLayout(20, 0));
        setBackground(MenuRefaccionaria.BG_DARK);
        setBorder(new EmptyBorder(25, 30, 25, 30));

        add(crearPanelLista(),  BorderLayout.CENTER);
        add(crearFormOrden(),   BorderLayout.EAST);
    }

    // ─── LISTA DE ÓRDENES ───
    private JPanel crearPanelLista() {
        JPanel panel = new JPanel(new BorderLayout(0, 15));
        panel.setOpaque(false);

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel titulo = new JLabel("Órdenes de Trabajo");
        titulo.setFont(new Font("SansSerif", Font.BOLD, 20));
        titulo.setForeground(MenuRefaccionaria.TEXT_WHITE);
        JButton btnRefresh = MenuRefaccionaria.btnPrimario("↻ Actualizar");
        btnRefresh.addActionListener(e -> cargarOrdenes());
        header.add(titulo, BorderLayout.WEST);
        header.add(btnRefresh, BorderLayout.EAST);
        panel.add(header, BorderLayout.NORTH);

        modeloOrdenes = new DefaultTableModel(
            new String[]{"ID", "Diagnóstico", "Estado", "Ingreso", "Entrega", "Cliente"}, 0
        ) { @Override public boolean isCellEditable(int r, int c) { return false; } };

        tabla = new JTable(modeloOrdenes);
        tabla.setBackground(MenuRefaccionaria.BG_CARD);
        tabla.setForeground(MenuRefaccionaria.TEXT_WHITE);
        tabla.setFont(new Font("SansSerif", Font.PLAIN, 13));
        tabla.setRowHeight(32);
        tabla.getTableHeader().setBackground(MenuRefaccionaria.BG_CARD2);
        tabla.getTableHeader().setForeground(MenuRefaccionaria.TEXT_MUTED);
        tabla.setSelectionBackground(MenuRefaccionaria.ACCENT_BLUE);
        tabla.setGridColor(new Color(40, 48, 80));

        JScrollPane sc = new JScrollPane(tabla);
        sc.getViewport().setBackground(MenuRefaccionaria.BG_CARD);
        sc.setBorder(null);
        panel.add(sc, BorderLayout.CENTER);

        cargarOrdenes();
        return panel;
    }

    // ─── FORMULARIO NUEVA ORDEN ───
    private JPanel crearFormOrden() {
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
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setPreferredSize(new Dimension(280, 0));
        form.setBorder(new EmptyBorder(20, 18, 20, 18));

        JLabel titulo = new JLabel("Nueva Orden");
        titulo.setFont(new Font("SansSerif", Font.BOLD, 16));
        titulo.setForeground(MenuRefaccionaria.TEXT_WHITE);
        titulo.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(titulo);
        form.add(Box.createVerticalStrut(14));

        // Combo clientes — carga SIEMPRE desde BD
        cbClientes = new JComboBox<>();
        cbClientes.setBackground(MenuRefaccionaria.BG_CARD2);
        cbClientes.setForeground(MenuRefaccionaria.TEXT_WHITE);
        cbClientes.setFont(new Font("SansSerif", Font.PLAIN, 13));
        cbClientes.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        cbClientes.setAlignmentX(Component.LEFT_ALIGNMENT);
        cbClientes.setRenderer((list, value, idx, sel, foc) -> {
            JLabel l = new JLabel(value == null ? "" : value[1]);
            l.setBackground(sel ? MenuRefaccionaria.ACCENT_BLUE : MenuRefaccionaria.BG_CARD2);
            l.setForeground(MenuRefaccionaria.TEXT_WHITE);
            l.setOpaque(true);
            l.setBorder(new EmptyBorder(4, 8, 4, 8));
            return l;
        });

        JTextField txtDiag    = campo("Diagnóstico");
        JTextField txtObs     = campo("Observaciones (opcional)");
        JTextField txtFechaE  = campo("Fecha entrega (YYYY-MM-DD)");

        String[] estados = {"EN_PROCESO", "ESPERANDO_REFACCION", "LISTO", "ENTREGADO"};
        JComboBox<String> cbEstado = new JComboBox<>(estados);
        cbEstado.setBackground(MenuRefaccionaria.BG_CARD2);
        cbEstado.setForeground(MenuRefaccionaria.TEXT_WHITE);
        cbEstado.setFont(new Font("SansSerif", Font.PLAIN, 13));
        cbEstado.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        cbEstado.setAlignmentX(Component.LEFT_ALIGNMENT);

        form.add(lbl("Cliente:"));          form.add(Box.createVerticalStrut(4));
        form.add(cbClientes);               form.add(Box.createVerticalStrut(10));
        form.add(lbl("Diagnóstico:"));      form.add(Box.createVerticalStrut(4));
        form.add(txtDiag);                  form.add(Box.createVerticalStrut(10));
        form.add(lbl("Observaciones:"));    form.add(Box.createVerticalStrut(4));
        form.add(txtObs);                   form.add(Box.createVerticalStrut(10));
        form.add(lbl("Fecha entrega:"));    form.add(Box.createVerticalStrut(4));
        form.add(txtFechaE);                form.add(Box.createVerticalStrut(10));
        form.add(lbl("Estado:"));           form.add(Box.createVerticalStrut(4));
        form.add(cbEstado);                 form.add(Box.createVerticalStrut(16));

        JButton btnGuardar = MenuRefaccionaria.btnPrimario("Guardar Orden");
        btnGuardar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        btnGuardar.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnGuardar.addActionListener(e ->
            guardar(txtDiag, txtObs, txtFechaE, cbEstado));
        form.add(btnGuardar);

        // Cargar clientes al mostrar el panel
        form.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentShown(java.awt.event.ComponentEvent e) {
                cargarClientesEnCombo();
            }
        });
        cargarClientesEnCombo();

        return form;
    }

    // ─── CARGAR ÓRDENES DESDE BD ───
    private void cargarOrdenes() {
        modeloOrdenes.setRowCount(0);
        modelo.SistemaTaller.ordenes.clear();

        try (Connection con = ConexionMySQL.conectar()) {
            if (con == null) return;
            ResultSet rs = con.createStatement().executeQuery(
                "SELECT o.id, o.diagnostico, o.estado, o.fecha_ingreso, " +
                "o.fecha_entrega, c.nombre AS cliente " +
                "FROM ordenes o JOIN clientes c ON c.id = o.id_cliente " +
                "ORDER BY o.id DESC"
            );
            while (rs.next()) {
                // Guardar en memoria (ruta absoluta para evitar shadowing)
                OrdenServicio o = new OrdenServicio();
                o.setId(rs.getInt("id"));
                o.setDiagnostico(rs.getString("diagnostico"));
                o.setEstado(rs.getString("estado"));
                modelo.SistemaTaller.ordenes.add(o);

                modeloOrdenes.addRow(new Object[]{
                    rs.getInt("id"),
                    rs.getString("diagnostico"),
                    rs.getString("estado"),
                    rs.getString("fecha_ingreso"),
                    rs.getString("fecha_entrega") != null ? rs.getString("fecha_entrega") : "—",
                    rs.getString("cliente")
                });
            }
        } catch (Exception ex) {
            System.err.println("Error cargando órdenes: " + ex.getMessage());
        }
    }

    // ─── CARGAR CLIENTES EN COMBO (siempre desde BD) ───
    private void cargarClientesEnCombo() {
        cbClientes.removeAllItems();
        try (Connection con = ConexionMySQL.conectar()) {
            if (con == null) return;
            ResultSet rs = con.createStatement().executeQuery(
                "SELECT id, nombre, telefono FROM clientes ORDER BY nombre"
            );
            while (rs.next()) {
                cbClientes.addItem(new String[]{
                    String.valueOf(rs.getInt("id")),
                    rs.getString("nombre") + " — " + rs.getString("telefono")
                });
            }
        } catch (Exception ex) {
            System.err.println("Error cargando clientes en combo: " + ex.getMessage());
        }
    }

    // ─── GUARDAR ORDEN EN BD ───
    private void guardar(JTextField txtDiag, JTextField txtObs,
                          JTextField txtFechaE, JComboBox<String> cbEstado) {
        Object selCli = cbClientes.getSelectedItem();
        if (!(selCli instanceof String[])) {
            JOptionPane.showMessageDialog(this, "Selecciona un cliente."); return;
        }
        String diag = txtDiag.getText().trim();
        if (diag.isEmpty() || diag.equals("Diagnóstico")) {
            JOptionPane.showMessageDialog(this, "Ingresa el diagnóstico."); return;
        }

        int    idCliente = Integer.parseInt(((String[]) selCli)[0]);
        String estado    = (String) cbEstado.getSelectedItem();
        String obs       = txtObs.getText().trim();
        String fechaE    = txtFechaE.getText().trim();

        try (Connection con = ConexionMySQL.conectar()) {
            if (con == null) { JOptionPane.showMessageDialog(this, "Sin conexión a BD."); return; }

            // Buscar el primer vehículo del cliente (si tiene)
            PreparedStatement psVeh = con.prepareStatement(
                "SELECT id FROM vehiculos WHERE id_cliente = ? LIMIT 1"
            );
            psVeh.setInt(1, idCliente);
            ResultSet rsVeh = psVeh.executeQuery();
            Integer idVehiculo = rsVeh.next() ? rsVeh.getInt("id") : null;

            if (idVehiculo == null) {
                int ok = JOptionPane.showConfirmDialog(this,
                    "Este cliente no tiene vehículo registrado.\n¿Crear la orden sin vehículo?",
                    "Sin vehículo", JOptionPane.YES_NO_OPTION);
                if (ok != JOptionPane.YES_OPTION) return;

                // Insertar vehículo genérico para satisfacer FK
                PreparedStatement insVeh = con.prepareStatement(
                    "INSERT INTO vehiculos (placas, marca, modelo, anio, color, id_cliente) " +
                    "VALUES ('SIN-PLACAS','SIN MARCA','SIN MODELO',2000,'N/A',?)",
                    Statement.RETURN_GENERATED_KEYS
                );
                insVeh.setInt(1, idCliente);
                insVeh.executeUpdate();
                ResultSet keys = insVeh.getGeneratedKeys();
                idVehiculo = keys.next() ? keys.getInt(1) : 1;
            }

            PreparedStatement ps = con.prepareStatement(
                "INSERT INTO ordenes (diagnostico, estado, observaciones, " +
                "fecha_ingreso, fecha_entrega, id_cliente, id_vehiculo) " +
                "VALUES (?, ?, ?, CURDATE(), ?, ?, ?)"
            );
            ps.setString(1, diag.toUpperCase());
            ps.setString(2, estado);
            ps.setString(3, obs.isEmpty() ? null : obs.toUpperCase());
            ps.setString(4, fechaE.isEmpty() ? null : fechaE);
            ps.setInt(5, idCliente);
            ps.setInt(6, idVehiculo);
            ps.executeUpdate();

            Seguridad.registrarAuditoria("ORDEN_NUEVA", "Cliente ID: " + idCliente);
            JOptionPane.showMessageDialog(this, "Orden creada correctamente.");

            // Limpiar campos
            txtDiag.setText("Diagnóstico");
            txtDiag.setForeground(MenuRefaccionaria.TEXT_MUTED);
            txtObs.setText("Observaciones (opcional)");
            txtObs.setForeground(MenuRefaccionaria.TEXT_MUTED);
            txtFechaE.setText("Fecha entrega (YYYY-MM-DD)");
            txtFechaE.setForeground(MenuRefaccionaria.TEXT_MUTED);
            cargarOrdenes();

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }

    // ─── Helpers UI ───
    private JTextField campo(String ph) {
        JTextField t = MenuRefaccionaria.textField(ph);
        t.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        t.setAlignmentX(Component.LEFT_ALIGNMENT);
        return t;
    }

    private JLabel lbl(String t) {
        JLabel l = new JLabel(t);
        l.setForeground(MenuRefaccionaria.TEXT_MUTED);
        l.setFont(new Font("SansSerif", Font.BOLD, 12));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }
}
