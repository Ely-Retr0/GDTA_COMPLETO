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
 * GDTA — Panel Inventario (corregido)
 * Fix: carga desde MySQL, no datos demo.
 * Solo ADMIN puede editar/eliminar.
 */
public class PanelInventario extends JPanel {

    private MenuRefaccionaria  menu;
    private DefaultTableModel  modeloTabla;
    private JTable             tabla;
    private JScrollPane        scroll;
    private CardLayout         card;
    private JPanel             contenedor;

    public PanelInventario(MenuRefaccionaria menu) {
        this.menu = menu;
        setLayout(new BorderLayout(20, 0));
        setBackground(MenuRefaccionaria.BG_DARK);
        setBorder(new EmptyBorder(25, 30, 25, 30));

        add(crearSidebar(), BorderLayout.WEST);

        card      = new CardLayout();
        contenedor = new JPanel(card);
        contenedor.setOpaque(false);
        contenedor.add(crearPanelLista(),     "lista");
        contenedor.add(crearFormProducto(false), "agregar");
        contenedor.add(crearFormProducto(true),  "editar");
        card.show(contenedor, "lista");
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

        p.add(secLbl("Inventario"));
        p.add(Box.createVerticalStrut(8));
        p.add(menuBtn("Ver productos",  () -> { cargarDesdeMySQL(); card.show(contenedor, "lista"); }));
        p.add(Box.createVerticalStrut(6));

        if (Seguridad.puedeEditarInventario()) {
            p.add(menuBtn("Agregar producto", () -> card.show(contenedor, "agregar")));
            p.add(Box.createVerticalStrut(6));
            p.add(menuBtn("Editar seleccionado", this::editarSeleccionado));
            p.add(Box.createVerticalStrut(6));
            p.add(menuBtn("Eliminar seleccionado", this::eliminarSeleccionado));
        }

        p.add(Box.createVerticalGlue());
        return p;
    }

    // ─── LISTA ───
    private JPanel crearPanelLista() {
        JPanel panel = new JPanel(new BorderLayout(0, 15));
        panel.setOpaque(false);

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel lbl = new JLabel("Inventario de Refacciones");
        lbl.setFont(new Font("SansSerif", Font.BOLD, 20));
        lbl.setForeground(MenuRefaccionaria.TEXT_WHITE);

        JButton btnActualizar = MenuRefaccionaria.btnPrimario("↻ Actualizar");
        btnActualizar.addActionListener(e -> cargarDesdeMySQL());
        header.add(lbl, BorderLayout.WEST);
        header.add(btnActualizar, BorderLayout.EAST);
        panel.add(header, BorderLayout.NORTH);

        // Tabla
        modeloTabla = new DefaultTableModel(
            new String[]{"ID", "Producto", "Categoría", "Marca", "Precio", "Stock", "Mín."}, 0
        ) { @Override public boolean isCellEditable(int r, int c) { return false; } };

        tabla = new JTable(modeloTabla);
        tabla.setBackground(MenuRefaccionaria.BG_CARD);
        tabla.setForeground(MenuRefaccionaria.TEXT_WHITE);
        tabla.setFont(new Font("SansSerif", Font.PLAIN, 13));
        tabla.setRowHeight(32);
        tabla.getTableHeader().setBackground(MenuRefaccionaria.BG_CARD2);
        tabla.getTableHeader().setForeground(MenuRefaccionaria.TEXT_MUTED);
        tabla.setSelectionBackground(MenuRefaccionaria.ACCENT_BLUE);
        tabla.setGridColor(new Color(40, 48, 80));

        // Renderer: resalta filas con stock bajo
        tabla.setDefaultRenderer(Object.class, (tbl, value, sel, foc, row, col) -> {
            JLabel cell = new JLabel(value == null ? "" : value.toString());
            cell.setOpaque(true);
            cell.setBorder(new EmptyBorder(0, 8, 0, 8));
            cell.setFont(new Font("SansSerif", Font.PLAIN, 13));

            int stock = 0, min = 0;
            try {
                stock = Integer.parseInt(tbl.getValueAt(row, 5).toString());
                min   = Integer.parseInt(tbl.getValueAt(row, 6).toString());
            } catch (Exception ignored) {}

            if (sel) {
                cell.setBackground(MenuRefaccionaria.ACCENT_BLUE);
                cell.setForeground(Color.WHITE);
            } else if (stock <= min && stock > 0) {
                cell.setBackground(new Color(80, 60, 20));
                cell.setForeground(new Color(255, 200, 60));
            } else if (stock == 0) {
                cell.setBackground(new Color(60, 20, 20));
                cell.setForeground(new Color(220, 80, 80));
            } else {
                cell.setBackground(MenuRefaccionaria.BG_CARD);
                cell.setForeground(MenuRefaccionaria.TEXT_WHITE);
            }
            return cell;
        });

        scroll = new JScrollPane(tabla);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(MenuRefaccionaria.BG_CARD);
        panel.add(scroll, BorderLayout.CENTER);

        // Leyenda
        JPanel leyenda = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 4));
        leyenda.setOpaque(false);
        leyenda.add(lbl("● Stock normal",     new Color(80, 200, 120)));
        leyenda.add(lbl("● Stock bajo",       new Color(255, 200, 60)));
        leyenda.add(lbl("● Sin stock",        new Color(220, 80, 80)));
        panel.add(leyenda, BorderLayout.SOUTH);

        cargarDesdeMySQL();
        return panel;
    }

    // ─── CARGA DESDE MYSQL (fix del bug) ───
    private void cargarDesdeMySQL() {
        if (modeloTabla == null) return;
        modeloTabla.setRowCount(0);
        try (Connection con = ConexionMySQL.conectar()) {
            if (con == null) return;
            ResultSet rs = con.createStatement().executeQuery(
                "SELECT * FROM inventario ORDER BY nombre"
            );
            while (rs.next()) {
                modeloTabla.addRow(new Object[]{
                    rs.getInt("id"),
                    rs.getString("nombre"),
                    rs.getString("categoria"),
                    rs.getString("marca"),
                    String.format("$%.2f", rs.getDouble("precio")),
                    rs.getInt("cantidad"),
                    rs.getInt("cant_min")
                });
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error cargando inventario: " + ex.getMessage());
        }
    }

    // ─── FORM PRODUCTO ───
    private JPanel crearFormProducto(boolean editar) {
        JPanel panel = new JPanel(new BorderLayout(0, 20));
        panel.setOpaque(false);

        JLabel titulo = new JLabel(editar ? "Editar Producto" : "Agregar Producto");
        titulo.setFont(new Font("SansSerif", Font.BOLD, 20));
        titulo.setForeground(MenuRefaccionaria.TEXT_WHITE);
        panel.add(titulo, BorderLayout.NORTH);

        JPanel form = cardPanel();
        form.setLayout(new GridBagLayout());
        form.setBorder(new EmptyBorder(25, 25, 25, 25));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.weightx = 1.0;

        JTextField txtNombre    = MenuRefaccionaria.textField("Nombre del producto");
        JTextField txtMarca     = MenuRefaccionaria.textField("Marca");
        JTextField txtCategoria = MenuRefaccionaria.textField("Categoría (ej. Filtros, Aceites)");
        JTextField txtPrecio    = MenuRefaccionaria.textField("Precio (ej. 250.00)");
        JTextField txtCantidad  = MenuRefaccionaria.textField("Cantidad en stock");
        JTextField txtMin       = MenuRefaccionaria.textField("Stock mínimo");

        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2; form.add(txtNombre,    gbc);
        gbc.gridwidth = 1;
        gbc.gridx = 0; gbc.gridy = 1; form.add(txtMarca,     gbc);
        gbc.gridx = 1;                form.add(txtCategoria,  gbc);
        gbc.gridx = 0; gbc.gridy = 2; form.add(txtPrecio,    gbc);
        gbc.gridx = 1;                form.add(txtCantidad,   gbc);
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2; form.add(txtMin, gbc);

        JButton btnGuardar  = MenuRefaccionaria.btnPrimario(editar ? "Guardar cambios" : "Agregar");
        JButton btnCancelar = MenuRefaccionaria.btnPeligro("Cancelar");
        btnCancelar.addActionListener(e -> card.show(contenedor, "lista"));

        btnGuardar.addActionListener(e -> {
            String nombre = txtNombre.getText().trim();
            String precio = txtPrecio.getText().trim();

            if (nombre.isEmpty() || nombre.equals("Nombre del producto")) {
                JOptionPane.showMessageDialog(this, "Ingresa el nombre del producto."); return;
            }
            try {
                double p = Double.parseDouble(precio.replace(",", "."));
                int    c = Integer.parseInt(txtCantidad.getText().trim());
                int    m = Integer.parseInt(txtMin.getText().trim());

                if (!editar) {
                    guardarProducto(nombre, txtMarca.getText().trim(),
                                    txtCategoria.getText().trim(), p, c, m);
                } else {
                    actualizarProducto(nombre, txtMarca.getText().trim(),
                                       txtCategoria.getText().trim(), p, c, m);
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Precio, cantidad y mínimo deben ser números.");
            }
        });

        gbc.gridy = 4; gbc.gridwidth = 1;
        gbc.gridx = 0; form.add(btnCancelar, gbc);
        gbc.gridx = 1; form.add(btnGuardar,  gbc);

        panel.add(form, BorderLayout.CENTER);
        return panel;
    }

    private void guardarProducto(String nombre, String marca, String cat,
                                  double precio, int cantidad, int min) {
        try (Connection con = ConexionMySQL.conectar()) {
            PreparedStatement ps = con.prepareStatement(
                "INSERT INTO inventario (nombre, marca, categoria, precio, cantidad, cant_min) " +
                "VALUES (?, ?, ?, ?, ?, ?)"
            );
            ps.setString(1, nombre.toUpperCase());
            ps.setString(2, marca.toUpperCase());
            ps.setString(3, cat.toUpperCase());
            ps.setDouble(4, precio);
            ps.setInt(5, cantidad);
            ps.setInt(6, min);
            ps.executeUpdate();
            Seguridad.registrarAuditoria("INVENTARIO_AGREGAR", "Producto: " + nombre);
            JOptionPane.showMessageDialog(this, "Producto agregado.");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            return;
        }
        cargarDesdeMySQL();
        card.show(contenedor, "lista");
    }

    private void actualizarProducto(String nombre, String marca, String cat,
                                     double precio, int cantidad, int min) {
        int fila = tabla.getSelectedRow();
        if (fila < 0) { JOptionPane.showMessageDialog(this, "Selecciona un producto."); return; }
        int id = (int) modeloTabla.getValueAt(fila, 0);
        try (Connection con = ConexionMySQL.conectar()) {
            PreparedStatement ps = con.prepareStatement(
                "UPDATE inventario SET nombre=?, marca=?, categoria=?, precio=?, cantidad=?, cant_min=? WHERE id=?"
            );
            ps.setString(1, nombre.toUpperCase());
            ps.setString(2, marca.toUpperCase());
            ps.setString(3, cat.toUpperCase());
            ps.setDouble(4, precio);
            ps.setInt(5, cantidad);
            ps.setInt(6, min);
            ps.setInt(7, id);
            ps.executeUpdate();
            Seguridad.registrarAuditoria("INVENTARIO_EDITAR", "ID: " + id);
            JOptionPane.showMessageDialog(this, "Producto actualizado.");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            return;
        }
        cargarDesdeMySQL();
        card.show(contenedor, "lista");
    }

    private void editarSeleccionado() {
        if (tabla.getSelectedRow() < 0) {
            JOptionPane.showMessageDialog(this, "Selecciona un producto primero."); return;
        }
        card.show(contenedor, "editar");
    }

    private void eliminarSeleccionado() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) { JOptionPane.showMessageDialog(this, "Selecciona un producto."); return; }
        int id  = (int) modeloTabla.getValueAt(fila, 0);
        String nom = modeloTabla.getValueAt(fila, 1).toString();
        int ok = JOptionPane.showConfirmDialog(this,
            "¿Eliminar \"" + nom + "\" del inventario?", "Confirmar", JOptionPane.YES_NO_OPTION);
        if (ok != JOptionPane.YES_OPTION) return;
        try (Connection con = ConexionMySQL.conectar()) {
            PreparedStatement ps = con.prepareStatement("DELETE FROM inventario WHERE id = ?");
            ps.setInt(1, id);
            ps.executeUpdate();
            Seguridad.registrarAuditoria("INVENTARIO_ELIMINAR", "ID: " + id + " " + nom);
            JOptionPane.showMessageDialog(this, "Producto eliminado.");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage()); return;
        }
        cargarDesdeMySQL();
    }

    // ─── Helpers UI ───
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
