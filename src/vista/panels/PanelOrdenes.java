package vista.panels;

import modelo.ConexionMySQL;
import package_sistemaTR.MenuRefaccionaria;
import seguridad.Seguridad;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.sql.*;

/**
 * GDTA — Panel Órdenes de Trabajo
 * Funcionalidades: listar, buscar, ver detalle, editar estado, eliminar.
 */
public class PanelOrdenes extends JPanel {

    private MenuRefaccionaria menu;
    private DefaultTableModel modeloOrdenes;
    private JTable            tabla;
    private CardLayout        card;
    private JPanel            contenedor;

    public PanelOrdenes(MenuRefaccionaria menu) {
        this.menu = menu;
        setLayout(new BorderLayout(20, 0));
        setBackground(MenuRefaccionaria.BG_DARK);
        setBorder(new EmptyBorder(25, 30, 25, 30));

        add(crearSidebar(), BorderLayout.WEST);

        card      = new CardLayout();
        contenedor = new JPanel(card);
        contenedor.setOpaque(false);
        contenedor.add(crearPanelLista(),   "lista");
        contenedor.add(crearPanelBuscar(),  "buscar");
        contenedor.add(crearPanelDetalle(), "detalle");
        card.show(contenedor, "lista");
        add(contenedor, BorderLayout.CENTER);
    }

    // ─── SIDEBAR ───
    private JPanel crearSidebar() {
        JPanel p = roundCard();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setPreferredSize(new Dimension(220, 0));
        p.setBorder(new EmptyBorder(20, 15, 20, 15));

        p.add(secLbl("Órdenes de Trabajo"));
        p.add(Box.createVerticalStrut(8));
        p.add(menuBtn("Ver todas",          () -> { cargarOrdenes(null); card.show(contenedor, "lista"); }));
        p.add(Box.createVerticalStrut(6));
        p.add(menuBtn("Buscar orden",       () -> card.show(contenedor, "buscar")));
        p.add(Box.createVerticalStrut(6));
        p.add(menuBtn("Ver detalle",        this::verDetalleSeleccionado));
        p.add(Box.createVerticalStrut(6));

        if (Seguridad.puedeEditarOrdenes()) {
            p.add(menuBtn("Cambiar estado", this::cambiarEstadoSeleccionado));
            p.add(Box.createVerticalStrut(6));
            p.add(menuBtn("Eliminar orden", this::eliminarSeleccionada));
        }

        p.add(Box.createVerticalGlue());
        return p;
    }

    // ─── LISTA ÓRDENES ───
    private JPanel crearPanelLista() {
        JPanel panel = new JPanel(new BorderLayout(0, 15));
        panel.setOpaque(false);

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel lbl = new JLabel("Órdenes de Trabajo");
        lbl.setFont(new Font("SansSerif", Font.BOLD, 20));
        lbl.setForeground(MenuRefaccionaria.TEXT_WHITE);
        JButton btnRefresh = MenuRefaccionaria.btnPrimario("↻ Actualizar");
        btnRefresh.addActionListener(e -> cargarOrdenes(null));
        header.add(lbl, BorderLayout.WEST);
        header.add(btnRefresh, BorderLayout.EAST);
        panel.add(header, BorderLayout.NORTH);

        modeloOrdenes = new DefaultTableModel(
            new String[]{"ID", "Cliente", "Vehículo", "Diagnóstico", "Estado", "Ingreso", "Entrega"}, 0
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

        // Color por estado
        tabla.setDefaultRenderer(Object.class, (tbl, value, sel, foc, row, col) -> {
            JLabel cell = new JLabel(value == null ? "" : value.toString());
            cell.setOpaque(true);
            cell.setBorder(new EmptyBorder(0, 8, 0, 8));
            cell.setFont(new Font("SansSerif", Font.PLAIN, 13));

            if (sel) {
                cell.setBackground(MenuRefaccionaria.ACCENT_BLUE);
                cell.setForeground(Color.WHITE);
            } else {
                String estado = "";
                try { estado = tbl.getValueAt(row, 4).toString(); } catch (Exception ignored) {}
                switch (estado) {
                    case "LISTO"                -> { cell.setBackground(new Color(20, 60, 30)); cell.setForeground(new Color(80, 220, 120)); }
                    case "EN_PROCESO"           -> { cell.setBackground(MenuRefaccionaria.BG_CARD); cell.setForeground(MenuRefaccionaria.TEXT_WHITE); }
                    case "ESPERANDO_REFACCION"  -> { cell.setBackground(new Color(60, 50, 15)); cell.setForeground(new Color(255, 200, 60)); }
                    case "ENTREGADO"            -> { cell.setBackground(new Color(25, 30, 50)); cell.setForeground(MenuRefaccionaria.TEXT_MUTED); }
                    default                     -> { cell.setBackground(MenuRefaccionaria.BG_CARD); cell.setForeground(MenuRefaccionaria.TEXT_WHITE); }
                }
            }
            return cell;
        });

        JScrollPane sc = new JScrollPane(tabla);
        sc.getViewport().setBackground(MenuRefaccionaria.BG_CARD);
        sc.setBorder(null);
        panel.add(sc, BorderLayout.CENTER);

        // Leyenda estados
        JPanel leyenda = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 4));
        leyenda.setOpaque(false);
        leyenda.add(lbl("● En proceso",          MenuRefaccionaria.TEXT_WHITE));
        leyenda.add(lbl("● Esperando refacción",  new Color(255, 200, 60)));
        leyenda.add(lbl("● Listo",               new Color(80, 220, 120)));
        leyenda.add(lbl("● Entregado",           MenuRefaccionaria.TEXT_MUTED));
        panel.add(leyenda, BorderLayout.SOUTH);

        cargarOrdenes(null);
        return panel;
    }

    // ─── CARGAR ÓRDENES ───
    private void cargarOrdenes(String filtro) {
        if (modeloOrdenes == null) return;
        modeloOrdenes.setRowCount(0);
        try (Connection con = ConexionMySQL.conectar()) {
            if (con == null) return;
            String sql = "SELECT o.id, c.nombre AS cliente, " +
                         "CONCAT(v.marca,' ',v.modelo,' (',v.placas,')') AS vehiculo, " +
                         "o.diagnostico, o.estado, o.fecha_ingreso, o.fecha_entrega " +
                         "FROM ordenes o " +
                         "JOIN clientes c ON c.id = o.id_cliente " +
                         "JOIN vehiculos v ON v.id = o.id_vehiculo ";
            if (filtro != null && !filtro.isBlank()) {
                sql += "WHERE c.nombre LIKE ? OR o.diagnostico LIKE ? OR o.estado = ? ";
            }
            sql += "ORDER BY o.id DESC";

            PreparedStatement ps = con.prepareStatement(sql);
            if (filtro != null && !filtro.isBlank()) {
                String like = "%" + filtro + "%";
                ps.setString(1, like); ps.setString(2, like);
                ps.setString(3, filtro.toUpperCase().replace(" ", "_"));
            }
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                modeloOrdenes.addRow(new Object[]{
                    rs.getInt("id"),
                    rs.getString("cliente"),
                    rs.getString("vehiculo"),
                    rs.getString("diagnostico"),
                    rs.getString("estado"),
                    rs.getString("fecha_ingreso"),
                    rs.getString("fecha_entrega") == null ? "—" : rs.getString("fecha_entrega")
                });
            }
        } catch (Exception ex) {
            System.err.println("Error cargando órdenes: " + ex.getMessage());
        }
    }

    // ─── BUSCAR ───
    private JPanel crearPanelBuscar() {
        JPanel panel = new JPanel(new BorderLayout(0, 20));
        panel.setOpaque(false);

        JLabel titulo = new JLabel("Buscar Orden");
        titulo.setFont(new Font("SansSerif", Font.BOLD, 20));
        titulo.setForeground(MenuRefaccionaria.TEXT_WHITE);
        panel.add(titulo, BorderLayout.NORTH);

        JPanel top = new JPanel(new BorderLayout(10, 0));
        top.setOpaque(false);
        JTextField txtBuscar = MenuRefaccionaria.textField("Cliente, diagnóstico o estado...");
        JButton btnBuscar  = MenuRefaccionaria.btnPrimario("Buscar");
        JButton btnVolver  = MenuRefaccionaria.btnPeligro("← Volver");
        btnVolver.addActionListener(e -> card.show(contenedor, "lista"));

        JPanel botonesTop = new JPanel(new BorderLayout(8, 0));
        botonesTop.setOpaque(false);
        botonesTop.add(txtBuscar, BorderLayout.CENTER);
        botonesTop.add(btnBuscar,  BorderLayout.EAST);

        Runnable buscar = () -> {
            cargarOrdenes(txtBuscar.getText().trim());
            card.show(contenedor, "lista");
        };
        btnBuscar.addActionListener(e -> buscar.run());
        txtBuscar.addActionListener(e -> buscar.run());

        top.add(botonesTop, BorderLayout.CENTER);
        top.add(btnVolver,  BorderLayout.EAST);

        panel.add(top, BorderLayout.CENTER);
        return panel;
    }

    // ─── DETALLE DE ORDEN ───
    private JPanel crearPanelDetalle() {
        // Se regenera dinámicamente al seleccionar
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        return p;
    }

    private void verDetalleSeleccionado() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) { JOptionPane.showMessageDialog(this, "Selecciona una orden primero."); return; }
        int id = (int) modeloOrdenes.getValueAt(fila, 0);
        mostrarDetalle(id);
    }

    private void mostrarDetalle(int idOrden) {
        JPanel detalle = new JPanel(new BorderLayout(0, 15));
        detalle.setOpaque(false);
        detalle.setBorder(new EmptyBorder(0, 0, 0, 0));

        JLabel titulo = new JLabel("Detalle de Orden #" + idOrden);
        titulo.setFont(new Font("SansSerif", Font.BOLD, 20));
        titulo.setForeground(MenuRefaccionaria.TEXT_WHITE);
        detalle.add(titulo, BorderLayout.NORTH);

        JPanel info = roundCard();
        info.setLayout(new GridBagLayout());
        info.setBorder(new EmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 8, 6, 8);
        gbc.weightx = 1.0;

        try (Connection con = ConexionMySQL.conectar()) {
            if (con == null) return;
            PreparedStatement ps = con.prepareStatement(
                "SELECT o.*, c.nombre AS cli_nombre, c.telefono, " +
                "v.marca, v.modelo, v.anio, v.placas, v.color " +
                "FROM ordenes o JOIN clientes c ON c.id=o.id_cliente " +
                "JOIN vehiculos v ON v.id=o.id_vehiculo WHERE o.id=?"
            );
            ps.setInt(1, idOrden);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int row = 0;
                String[][] campos = {
                    {"Cliente",     rs.getString("cli_nombre")},
                    {"Teléfono",    rs.getString("telefono")},
                    {"Vehículo",    rs.getString("marca")+" "+rs.getString("modelo")+" "+rs.getInt("anio")},
                    {"Placas",      rs.getString("placas")},
                    {"Color",       rs.getString("color")},
                    {"Diagnóstico", rs.getString("diagnostico")},
                    {"Estado",      rs.getString("estado")},
                    {"Observ.",     rs.getString("observaciones") != null ? rs.getString("observaciones") : "—"},
                    {"Ingreso",     rs.getString("fecha_ingreso")},
                    {"Entrega",     rs.getString("fecha_entrega") != null ? rs.getString("fecha_entrega") : "—"}
                };
                for (String[] campo : campos) {
                    gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.35;
                    JLabel lk = new JLabel(campo[0]);
                    lk.setForeground(MenuRefaccionaria.TEXT_MUTED);
                    lk.setFont(new Font("SansSerif", Font.BOLD, 13));
                    info.add(lk, gbc);
                    gbc.gridx = 1; gbc.weightx = 0.65;
                    JLabel lv = new JLabel(campo[1]);
                    lv.setForeground(MenuRefaccionaria.TEXT_WHITE);
                    lv.setFont(new Font("SansSerif", Font.PLAIN, 13));
                    info.add(lv, gbc);
                    row++;
                }
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            return;
        }

        JButton btnVolver = MenuRefaccionaria.btnPeligro("← Volver a lista");
        btnVolver.addActionListener(e -> card.show(contenedor, "lista"));

        detalle.add(info, BorderLayout.CENTER);
        detalle.add(btnVolver, BorderLayout.SOUTH);

        contenedor.add(detalle, "detalle");
        card.show(contenedor, "detalle");
    }

    // ─── CAMBIAR ESTADO ───
    private void cambiarEstadoSeleccionado() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) { JOptionPane.showMessageDialog(this, "Selecciona una orden."); return; }
        int id = (int) modeloOrdenes.getValueAt(fila, 0);

        String[] estados = {"EN_PROCESO", "ESPERANDO_REFACCION", "LISTO", "ENTREGADO"};
        String actual    = modeloOrdenes.getValueAt(fila, 4).toString();

        JComboBox<String> cb = new JComboBox<>(estados);
        cb.setSelectedItem(actual);

        JTextField txtObs = new JTextField();
        JTextField txtEntrega = new JTextField(java.time.LocalDate.now().toString());

        JPanel dlgPanel = new JPanel(new GridLayout(4, 1, 0, 8));
        dlgPanel.add(new JLabel("Nuevo estado:"));
        dlgPanel.add(cb);
        dlgPanel.add(new JLabel("Fecha entrega (YYYY-MM-DD):"));
        dlgPanel.add(txtEntrega);

        int ok = JOptionPane.showConfirmDialog(this, dlgPanel,
            "Cambiar estado — Orden #" + id, JOptionPane.OK_CANCEL_OPTION);
        if (ok != JOptionPane.OK_OPTION) return;

        try (Connection con = ConexionMySQL.conectar()) {
            PreparedStatement ps = con.prepareStatement(
                "UPDATE ordenes SET estado=?, fecha_entrega=? WHERE id=?"
            );
            ps.setString(1, (String) cb.getSelectedItem());
            String fec = txtEntrega.getText().trim();
            ps.setString(2, fec.isEmpty() ? null : fec);
            ps.setInt(3, id);
            ps.executeUpdate();
            Seguridad.registrarAuditoria("ORDEN_ESTADO",
                "ID: " + id + " → " + cb.getSelectedItem());
            JOptionPane.showMessageDialog(this, "Estado actualizado.");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage()); return;
        }
        cargarOrdenes(null);
    }

    // ─── ELIMINAR ───
    private void eliminarSeleccionada() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) { JOptionPane.showMessageDialog(this, "Selecciona una orden."); return; }
        int id = (int) modeloOrdenes.getValueAt(fila, 0);
        int ok = JOptionPane.showConfirmDialog(this,
            "¿Eliminar la Orden #" + id + "?\nSe eliminarán también sus detalles y pagos.",
            "Eliminar orden", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ok != JOptionPane.YES_OPTION) return;
        try (Connection con = ConexionMySQL.conectar()) {
            PreparedStatement ps = con.prepareStatement("DELETE FROM ordenes WHERE id=?");
            ps.setInt(1, id);
            ps.executeUpdate();
            Seguridad.registrarAuditoria("ORDEN_ELIMINAR", "ID: " + id);
            JOptionPane.showMessageDialog(this, "Orden eliminada.");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage()); return;
        }
        cargarOrdenes(null);
    }

    // ─── Helpers UI ───
    private JPanel roundCard() {
        return new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(MenuRefaccionaria.BG_CARD);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 18, 18));
                g2.dispose();
            }
        };
    }

    private JLabel lbl(String t, Color c) {
        JLabel l = new JLabel(t); l.setForeground(c);
        l.setFont(new Font("SansSerif", Font.PLAIN, 12)); return l;
    }

    private JLabel secLbl(String t) {
        JLabel l = new JLabel(t);
        l.setFont(new Font("SansSerif", Font.BOLD, 13));
        l.setForeground(MenuRefaccionaria.TEXT_MUTED);
        l.setAlignmentX(Component.LEFT_ALIGNMENT); return l;
    }

    private JButton menuBtn(String t, Runnable r) {
        JButton b = new JButton(t);
        b.setFont(new Font("SansSerif", Font.BOLD, 13));
        b.setForeground(Color.WHITE);
        b.setBackground(MenuRefaccionaria.ACCENT_BLUE);
        b.setBorder(new EmptyBorder(10, 15, 10, 15));
        b.setFocusPainted(false);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.addActionListener(e -> r.run());
        b.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) { b.setBackground(MenuRefaccionaria.ACCENT_HOVER); }
            public void mouseExited(java.awt.event.MouseEvent e)  { b.setBackground(MenuRefaccionaria.ACCENT_BLUE);  }
        });
        return b;
    }
}
