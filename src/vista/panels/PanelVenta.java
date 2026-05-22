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
import java.time.LocalDate;

/**
 * GDTA — Panel Ventas (corregido)
 * Fix: ventas se guardan en BD, stock se descuenta, mayúsculas.
 */
public class PanelVenta extends JPanel {

    private MenuRefaccionaria menu;
    private DefaultTableModel modeloCarrito;
    private DefaultTableModel modeloHistorial;
    private JLabel            lblTotal;
    private double            totalActual = 0.0;
    private CardLayout        card;
    private JPanel            contenedor;

    public PanelVenta(MenuRefaccionaria menu) {
        this.menu = menu;
        setLayout(new BorderLayout(20, 0));
        setBackground(MenuRefaccionaria.BG_DARK);
        setBorder(new EmptyBorder(25, 30, 25, 30));

        add(crearSidebar(), BorderLayout.WEST);

        card      = new CardLayout();
        contenedor = new JPanel(card);
        contenedor.setOpaque(false);
        contenedor.add(crearPanelVenta(),     "venta");
        contenedor.add(crearPanelHistorial(), "historial");
        card.show(contenedor, "venta");
        add(contenedor, BorderLayout.CENTER);
    }

    // ─── SIDEBAR ───
    private JPanel crearSidebar() {
        JPanel p = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(MenuRefaccionaria.BG_CARD);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 18, 18));
                g2.dispose();
            }
        };
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setPreferredSize(new Dimension(220, 0));
        p.setBorder(new EmptyBorder(20, 15, 20, 15));

        p.add(secLbl("Ventas"));
        p.add(Box.createVerticalStrut(8));
        p.add(menuBtn("Nueva Venta",      () -> card.show(contenedor, "venta")));
        p.add(Box.createVerticalStrut(6));
        p.add(menuBtn("Historial Ventas", () -> { cargarHistorial(); card.show(contenedor, "historial"); }));
        p.add(Box.createVerticalGlue());
        return p;
    }

    // ─── PANEL NUEVA VENTA ───
    private JPanel crearPanelVenta() {
        JPanel panel = new JPanel(new BorderLayout(0, 15));
        panel.setOpaque(false);

        JLabel titulo = new JLabel("Nueva Venta");
        titulo.setFont(new Font("SansSerif", Font.BOLD, 20));
        titulo.setForeground(MenuRefaccionaria.TEXT_WHITE);
        panel.add(titulo, BorderLayout.NORTH);

        JPanel body = new JPanel(new GridLayout(1, 2, 20, 0));
        body.setOpaque(false);

        // ── Izquierda: buscar y agregar productos ──
        JPanel izq = cardPanel();
        izq.setLayout(new BorderLayout(0, 10));
        izq.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel lblCat = label("Buscar producto");
        JTextField txtBuscar = MenuRefaccionaria.textField("Nombre o categoría...");
        JButton btnBuscar = MenuRefaccionaria.btnPrimario("Buscar");

        DefaultTableModel modeloProd = new DefaultTableModel(
            new String[]{"ID", "Producto", "Precio", "Stock"}, 0
        ) { @Override public boolean isCellEditable(int r, int c) { return false; } };

        JTable tblProd = new JTable(modeloProd);
        estilizarTabla(tblProd);

        JPanel buscarTop = new JPanel(new BorderLayout(8, 0));
        buscarTop.setOpaque(false);
        buscarTop.add(txtBuscar, BorderLayout.CENTER);
        buscarTop.add(btnBuscar, BorderLayout.EAST);

        JScrollPane scProd = new JScrollPane(tblProd);
        scProd.getViewport().setBackground(MenuRefaccionaria.BG_CARD);
        scProd.setBorder(null);

        JButton btnAgregar = MenuRefaccionaria.btnPrimario("+ Agregar al carrito");
        btnAgregar.addActionListener(e -> agregarAlCarrito(tblProd, modeloProd));

        izq.add(lblCat,    BorderLayout.NORTH);
        izq.add(buscarTop, BorderLayout.PAGE_START);

        JPanel izqCentro = new JPanel(new BorderLayout(0, 8));
        izqCentro.setOpaque(false);
        izqCentro.add(scProd,     BorderLayout.CENTER);
        izqCentro.add(btnAgregar, BorderLayout.SOUTH);
        izq.add(izqCentro, BorderLayout.CENTER);

        Runnable buscar = () -> {
            String q = txtBuscar.getText().trim();
            if (q.isEmpty() || q.equals("Nombre o categoría...")) {
                cargarTodosProductos(modeloProd); return;
            }
            modeloProd.setRowCount(0);
            try (Connection con = ConexionMySQL.conectar()) {
                PreparedStatement ps = con.prepareStatement(
                    "SELECT id, nombre, precio, cantidad FROM inventario " +
                    "WHERE nombre LIKE ? OR categoria LIKE ? ORDER BY nombre"
                );
                ps.setString(1, "%" + q + "%"); ps.setString(2, "%" + q + "%");
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    modeloProd.addRow(new Object[]{
                        rs.getInt("id"), rs.getString("nombre"),
                        String.format("$%.2f", rs.getDouble("precio")),
                        rs.getInt("cantidad")
                    });
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            }
        };

        btnBuscar.addActionListener(e -> buscar.run());
        txtBuscar.addActionListener(e -> buscar.run());
        cargarTodosProductos(modeloProd);

        // ── Derecha: carrito y cobro ──
        JPanel der = cardPanel();
        der.setLayout(new BorderLayout(0, 10));
        der.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel lblCarrito = label("Carrito");

        modeloCarrito = new DefaultTableModel(
            new String[]{"ID", "Producto", "Precio Unit.", "Cant.", "Subtotal"}, 0
        ) { @Override public boolean isCellEditable(int r, int c) { return false; } };

        JTable tblCarrito = new JTable(modeloCarrito);
        estilizarTabla(tblCarrito);
        JScrollPane scCarrito = new JScrollPane(tblCarrito);
        scCarrito.getViewport().setBackground(MenuRefaccionaria.BG_CARD2);
        scCarrito.setBorder(null);

        JButton btnQuitar = MenuRefaccionaria.btnPeligro("Quitar seleccionado");
        btnQuitar.addActionListener(e -> {
            int r = tblCarrito.getSelectedRow();
            if (r >= 0) { modeloCarrito.removeRow(r); recalcularTotal(); }
        });

        // Cliente y método de pago
        JTextField txtCliente = MenuRefaccionaria.textField("Nombre del cliente (opcional)");
        String[] metodos = {"Efectivo", "Tarjeta", "Transferencia"};
        JComboBox<String> cbPago = new JComboBox<>(metodos);
        cbPago.setBackground(MenuRefaccionaria.BG_CARD2);
        cbPago.setForeground(MenuRefaccionaria.TEXT_WHITE);
        cbPago.setFont(new Font("SansSerif", Font.PLAIN, 13));

        lblTotal = new JLabel("TOTAL: $0.00");
        lblTotal.setFont(new Font("SansSerif", Font.BOLD, 18));
        lblTotal.setForeground(MenuRefaccionaria.ACCENT_GOLD);

        JButton btnCobrar = MenuRefaccionaria.btnPrimario("💳 Cobrar");
        btnCobrar.setFont(new Font("SansSerif", Font.BOLD, 15));
        btnCobrar.addActionListener(e -> procesarCobro(txtCliente, cbPago));

        JPanel bottomDer = new JPanel(new GridLayout(5, 1, 0, 8));
        bottomDer.setOpaque(false);
        bottomDer.add(txtCliente);
        bottomDer.add(cbPago);
        bottomDer.add(btnQuitar);
        bottomDer.add(lblTotal);
        bottomDer.add(btnCobrar);

        der.add(lblCarrito,  BorderLayout.NORTH);
        der.add(scCarrito,   BorderLayout.CENTER);
        der.add(bottomDer,   BorderLayout.SOUTH);

        body.add(izq);
        body.add(der);
        panel.add(body, BorderLayout.CENTER);
        return panel;
    }

    // ─── PROCESAR COBRO (bug fix: guarda en BD y descuenta stock) ───
    private void procesarCobro(JTextField txtCliente, JComboBox<String> cbPago) {
        if (modeloCarrito.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "El carrito está vacío."); return;
        }
        if (totalActual <= 0) {
            JOptionPane.showMessageDialog(this, "El total debe ser mayor a $0."); return;
        }

        String cliente = txtCliente.getText().trim();
        if (cliente.isEmpty() || cliente.equals("Nombre del cliente (opcional)")) cliente = "CLIENTE GENERAL";
        cliente = cliente.toUpperCase();

        String metodo = (String) cbPago.getSelectedItem();

        int confirm = JOptionPane.showConfirmDialog(this,
            "Cobrar $" + String.format("%.2f", totalActual) + " a " + cliente + "\nMétodo: " + metodo,
            "Confirmar cobro", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        try (Connection con = ConexionMySQL.conectar()) {
            if (con == null) { JOptionPane.showMessageDialog(this, "Sin conexión a BD."); return; }

            // 1. Registrar venta
            PreparedStatement psVenta = con.prepareStatement(
                "INSERT INTO ventas (fecha, nom_cli, met_pag, cantidad, total) VALUES (?, ?, ?, ?, ?)"
            );
            psVenta.setString(1, LocalDate.now().toString());
            psVenta.setString(2, cliente);
            psVenta.setString(3, metodo);
            psVenta.setInt(4, modeloCarrito.getRowCount());
            psVenta.setDouble(5, totalActual);
            psVenta.executeUpdate();

            // 2. Descontar stock por cada item
            for (int i = 0; i < modeloCarrito.getRowCount(); i++) {
                int idProd   = Integer.parseInt(modeloCarrito.getValueAt(i, 0).toString());
                int cantidad = Integer.parseInt(modeloCarrito.getValueAt(i, 3).toString());

                PreparedStatement psStock = con.prepareStatement(
                    "UPDATE inventario SET cantidad = GREATEST(0, cantidad - ?) WHERE id = ?"
                );
                psStock.setInt(1, cantidad);
                psStock.setInt(2, idProd);
                psStock.executeUpdate();
            }

            Seguridad.registrarAuditoria("VENTA_REALIZADA",
                "Cliente: " + cliente + " | Total: $" + totalActual + " | Método: " + metodo);

            JOptionPane.showMessageDialog(this,
                "✅ Venta registrada correctamente.\nTotal cobrado: $" +
                String.format("%.2f", totalActual));

            // Limpiar carrito
            modeloCarrito.setRowCount(0);
            totalActual = 0.0;
            lblTotal.setText("TOTAL: $0.00");
            txtCliente.setText("Nombre del cliente (opcional)");
            txtCliente.setForeground(MenuRefaccionaria.TEXT_MUTED);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error al registrar venta: " + ex.getMessage());
        }
    }

    // ─── HISTORIAL ───
    private JPanel crearPanelHistorial() {
        JPanel panel = new JPanel(new BorderLayout(0, 15));
        panel.setOpaque(false);

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel lbl = new JLabel("Historial de Ventas");
        lbl.setFont(new Font("SansSerif", Font.BOLD, 20));
        lbl.setForeground(MenuRefaccionaria.TEXT_WHITE);
        JButton btnRefresh = MenuRefaccionaria.btnPrimario("↻ Actualizar");
        btnRefresh.addActionListener(e -> cargarHistorial());
        header.add(lbl, BorderLayout.WEST);
        header.add(btnRefresh, BorderLayout.EAST);
        panel.add(header, BorderLayout.NORTH);

        modeloHistorial = new DefaultTableModel(
            new String[]{"ID", "Fecha", "Cliente", "Método Pago", "Artículos", "Total"}, 0
        ) { @Override public boolean isCellEditable(int r, int c) { return false; } };

        JTable tbl = new JTable(modeloHistorial);
        estilizarTabla(tbl);
        JScrollPane sc = new JScrollPane(tbl);
        sc.getViewport().setBackground(MenuRefaccionaria.BG_CARD);
        sc.setBorder(null);
        panel.add(sc, BorderLayout.CENTER);

        cargarHistorial();
        return panel;
    }

    private void cargarHistorial() {
        if (modeloHistorial == null) return;
        modeloHistorial.setRowCount(0);
        try (Connection con = ConexionMySQL.conectar()) {
            if (con == null) return;
            ResultSet rs = con.createStatement().executeQuery(
                "SELECT * FROM ventas ORDER BY fecha DESC, id DESC"
            );
            while (rs.next()) {
                modeloHistorial.addRow(new Object[]{
                    rs.getInt("id"),
                    rs.getString("fecha"),
                    rs.getString("nom_cli"),
                    rs.getString("met_pag"),
                    rs.getInt("cantidad"),
                    String.format("$%.2f", rs.getDouble("total"))
                });
            }
        } catch (Exception ex) {
            System.err.println("Error historial: " + ex.getMessage());
        }
    }

    // ─── Helpers ───
    private void cargarTodosProductos(DefaultTableModel modelo) {
        modelo.setRowCount(0);
        try (Connection con = ConexionMySQL.conectar()) {
            if (con == null) return;
            ResultSet rs = con.createStatement().executeQuery(
                "SELECT id, nombre, precio, cantidad FROM inventario ORDER BY nombre"
            );
            while (rs.next()) {
                modelo.addRow(new Object[]{
                    rs.getInt("id"), rs.getString("nombre"),
                    String.format("$%.2f", rs.getDouble("precio")),
                    rs.getInt("cantidad")
                });
            }
        } catch (Exception ex) {
            System.err.println("Error cargando productos: " + ex.getMessage());
        }
    }

    private void agregarAlCarrito(JTable tblProd, DefaultTableModel modeloProd) {
        int fila = tblProd.getSelectedRow();
        if (fila < 0) { JOptionPane.showMessageDialog(this, "Selecciona un producto."); return; }

        int stock = Integer.parseInt(modeloProd.getValueAt(fila, 3).toString());
        if (stock <= 0) { JOptionPane.showMessageDialog(this, "Producto sin stock."); return; }

        String cantStr = JOptionPane.showInputDialog(this, "¿Cuántas unidades?", "1");
        if (cantStr == null || cantStr.trim().isEmpty()) return;
        int cant;
        try { cant = Integer.parseInt(cantStr.trim()); }
        catch (NumberFormatException e) { JOptionPane.showMessageDialog(this, "Ingresa un número válido."); return; }
        if (cant <= 0 || cant > stock) {
            JOptionPane.showMessageDialog(this, "Cantidad inválida (max: " + stock + ")."); return;
        }

        int    id      = Integer.parseInt(modeloProd.getValueAt(fila, 0).toString());
        String nombre  = modeloProd.getValueAt(fila, 1).toString();
        String precioS = modeloProd.getValueAt(fila, 2).toString().replace("$", "");
        double precio  = Double.parseDouble(precioS);
        double sub     = precio * cant;

        modeloCarrito.addRow(new Object[]{
            id, nombre, String.format("$%.2f", precio), cant, String.format("$%.2f", sub)
        });
        recalcularTotal();
    }

    private void recalcularTotal() {
        totalActual = 0.0;
        for (int i = 0; i < modeloCarrito.getRowCount(); i++) {
            String sub = modeloCarrito.getValueAt(i, 4).toString().replace("$", "");
            try { totalActual += Double.parseDouble(sub); } catch (Exception ignored) {}
        }
        if (lblTotal != null) lblTotal.setText("TOTAL: $" + String.format("%.2f", totalActual));
    }

    private void estilizarTabla(JTable t) {
        t.setBackground(MenuRefaccionaria.BG_CARD);
        t.setForeground(MenuRefaccionaria.TEXT_WHITE);
        t.setFont(new Font("SansSerif", Font.PLAIN, 13));
        t.setRowHeight(30);
        t.getTableHeader().setBackground(MenuRefaccionaria.BG_CARD2);
        t.getTableHeader().setForeground(MenuRefaccionaria.TEXT_MUTED);
        t.setSelectionBackground(MenuRefaccionaria.ACCENT_BLUE);
        t.setGridColor(new Color(40, 48, 80));
    }

    private JPanel cardPanel() {
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

    private JLabel label(String t) {
        JLabel l = new JLabel(t);
        l.setForeground(MenuRefaccionaria.TEXT_MUTED);
        l.setFont(new Font("SansSerif", Font.BOLD, 13)); return l;
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
