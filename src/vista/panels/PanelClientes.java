package vista.panels;

import modelo.*;
import modelo.ConexionMySQL;
import package_sistemaTR.MenuRefaccionaria;
import seguridad.Seguridad;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.sql.*;

public class PanelClientes extends JPanel {

    private MenuRefaccionaria menu;
    private DefaultTableModel modeloClientes;
    private JPanel panelDerecho;
    private CardLayout cardDerecho;
    private JPanel contenedor;
    private JPanel emptyPanel;
    private JScrollPane scroll;
    private JTable tabla;

    // Combos del form orden — declarados como campos para poder recargarlos
    private JComboBox<String[]> cbClienteOrden  = new JComboBox<>();
    private JComboBox<String[]> cbVehiculoOrden = new JComboBox<>();

    public PanelClientes(MenuRefaccionaria menu) {
        this.menu = menu;
        setLayout(new BorderLayout(20, 0));
        setBackground(MenuRefaccionaria.BG_DARK);
        setBorder(new EmptyBorder(25, 30, 25, 30));

        add(crearPanelIzquierdo(), BorderLayout.WEST);

        cardDerecho  = new CardLayout();
        panelDerecho = new JPanel(cardDerecho);
        panelDerecho.setOpaque(false);

        panelDerecho.add(crearPanelListaClientes(), "lista");
        panelDerecho.add(crearFormCliente(false),   "agregar");
        panelDerecho.add(crearFormCliente(true),    "editar");
        panelDerecho.add(crearFormOrden(),          "orden");
        panelDerecho.add(crearPanelBuscar(),        "buscar");

        cardDerecho.show(panelDerecho, "lista");
        add(panelDerecho, BorderLayout.CENTER);
    }

    // ─── PANEL IZQUIERDO ───
    private JPanel crearPanelIzquierdo() {
        JPanel panel = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(MenuRefaccionaria.BG_CARD);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 18, 18));
                g2.dispose();
            }
        };
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setPreferredSize(new Dimension(220, 0));
        panel.setBorder(new EmptyBorder(20, 15, 20, 15));

        panel.add(seccion("Clientes"));
        panel.add(Box.createVerticalStrut(8));
        panel.add(btnMenu("Agregar Cliente",  () -> cardDerecho.show(panelDerecho, "agregar")));
        panel.add(Box.createVerticalStrut(6));
        panel.add(btnMenu("Editar Cliente",   () -> editarClienteSeleccionado()));
        panel.add(Box.createVerticalStrut(6));
        panel.add(btnMenu("Eliminar Cliente", () -> eliminarClienteSeleccionado()));
        panel.add(Box.createVerticalStrut(6));
        panel.add(btnMenu("Buscar Cliente",   () -> cardDerecho.show(panelDerecho, "buscar")));
        panel.add(Box.createVerticalStrut(20));

        panel.add(seccion("Órdenes de Trabajo"));
        panel.add(Box.createVerticalStrut(8));
        // FIX: recargar combos cada vez que se abre el form de orden
        panel.add(btnMenu("Nueva Orden", () -> {
            cargarClientesEnCombo(cbClienteOrden);
            cbVehiculoOrden.removeAllItems();
            // Cargar vehículos del primer cliente si hay alguno
            if (cbClienteOrden.getItemCount() > 0) {
                cbClienteOrden.setSelectedIndex(0);
                Object sel = cbClienteOrden.getSelectedItem();
                if (sel instanceof String[] arr) {
                    cargarVehiculosEnCombo(cbVehiculoOrden, Integer.parseInt(arr[0]));
                }
            }
            cardDerecho.show(panelDerecho, "orden");
        }));
        panel.add(Box.createVerticalStrut(6));
        panel.add(btnMenu("Ver Órdenes", () -> cardDerecho.show(panelDerecho, "lista")));
        panel.add(Box.createVerticalGlue());

        return panel;
    }

    // ─── LISTA CLIENTES ───
    private JPanel crearPanelListaClientes() {
        JPanel panel = new JPanel(new BorderLayout(0, 15));
        panel.setOpaque(false);

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel lbl = new JLabel("Lista de Clientes");
        lbl.setFont(new Font("SansSerif", Font.BOLD, 20));
        lbl.setForeground(MenuRefaccionaria.TEXT_WHITE);
        JButton btnRefresh = MenuRefaccionaria.btnPrimario("↻ Actualizar");
        btnRefresh.addActionListener(e -> refrescarTabla());
        header.add(lbl, BorderLayout.WEST);
        header.add(btnRefresh, BorderLayout.EAST);
        panel.add(header, BorderLayout.NORTH);

        modeloClientes = new DefaultTableModel(
                new String[]{"ID", "Nombre", "Teléfono", "Vehículos"}, 0
        ) { @Override public boolean isCellEditable(int r, int c) { return false; } };

        tabla = new JTable(modeloClientes);
        tabla.setBackground(MenuRefaccionaria.BG_CARD);
        tabla.setForeground(MenuRefaccionaria.TEXT_WHITE);
        tabla.setFont(new Font("SansSerif", Font.PLAIN, 13));
        tabla.setRowHeight(32);
        tabla.getTableHeader().setBackground(MenuRefaccionaria.BG_CARD2);
        tabla.getTableHeader().setForeground(MenuRefaccionaria.TEXT_MUTED);
        tabla.setSelectionBackground(MenuRefaccionaria.ACCENT_BLUE);
        tabla.setGridColor(new Color(40, 48, 80));

        scroll = new JScrollPane(tabla);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(MenuRefaccionaria.BG_CARD);

        emptyPanel = new JPanel();
        emptyPanel.setBackground(MenuRefaccionaria.BG_CARD);
        emptyPanel.setLayout(new BoxLayout(emptyPanel, BoxLayout.Y_AXIS));
        JLabel lblVacio = new JLabel("Sin clientes registrados");
        lblVacio.setFont(new Font("SansSerif", Font.BOLD, 16));
        lblVacio.setForeground(MenuRefaccionaria.TEXT_WHITE);
        lblVacio.setAlignmentX(Component.CENTER_ALIGNMENT);
        emptyPanel.add(Box.createVerticalGlue());
        emptyPanel.add(lblVacio);
        emptyPanel.add(Box.createVerticalGlue());

        contenedor = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(MenuRefaccionaria.BG_CARD);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 18, 18));
                g2.dispose();
            }
        };
        contenedor.setOpaque(false);
        panel.add(contenedor, BorderLayout.CENTER);

        refrescarTabla();
        return panel;
    }

    // ─── FORM CLIENTE ───
    private JPanel crearFormCliente(boolean editar) {
        JPanel panel = new JPanel(new BorderLayout(0, 20));
        panel.setOpaque(false);

        JLabel titulo = new JLabel(editar ? "Editar Cliente" : "Agregar Cliente");
        titulo.setFont(new Font("SansSerif", Font.BOLD, 20));
        titulo.setForeground(MenuRefaccionaria.TEXT_WHITE);
        panel.add(titulo, BorderLayout.NORTH);

        JPanel form = card();
        form.setLayout(new GridBagLayout());
        form.setBorder(new EmptyBorder(30, 30, 30, 30));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.weightx = 1.0;

        JTextField txtNombre   = MenuRefaccionaria.textField("Nombre del cliente");
        JTextField txtTelefono = MenuRefaccionaria.textField("Teléfono (10 dígitos)");
        JTextField txtPlacas   = MenuRefaccionaria.textField("Placas del vehículo");
        JTextField txtMarca    = MenuRefaccionaria.textField("Marca");
        JTextField txtModelo   = MenuRefaccionaria.textField("Modelo");
        JTextField txtAnio     = MenuRefaccionaria.textField("Año (ej. 2020)");
        JTextField txtColor    = MenuRefaccionaria.textField("Color");

        for (JTextField tf : new JTextField[]{txtTelefono, txtAnio}) {
            tf.addKeyListener(new java.awt.event.KeyAdapter() {
                public void keyTyped(java.awt.event.KeyEvent e) {
                    if (!Character.isDigit(e.getKeyChar())) e.consume();
                }
            });
        }

        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2; form.add(txtNombre,   gbc);
        gbc.gridy = 1;                                    form.add(txtTelefono, gbc);
        gbc.gridy = 2; form.add(label("Datos del Vehículo (opcional)"), gbc);
        gbc.gridwidth = 1;
        gbc.gridx = 0; gbc.gridy = 3; form.add(txtPlacas, gbc);
        gbc.gridx = 1;                form.add(txtMarca,  gbc);
        gbc.gridx = 0; gbc.gridy = 4; form.add(txtModelo, gbc);
        gbc.gridx = 1;                form.add(txtAnio,   gbc);
        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 2; form.add(txtColor, gbc);

        JButton btnGuardar  = MenuRefaccionaria.btnPrimario(editar ? "Guardar cambios" : "Agregar Cliente");
        JButton btnCancelar = MenuRefaccionaria.btnPeligro("Cancelar");

        btnCancelar.addActionListener(e -> cardDerecho.show(panelDerecho, "lista"));
        btnGuardar.addActionListener(e -> {
            String nombre = txtNombre.getText().trim();
            String tel    = txtTelefono.getText().trim();
            if (nombre.isEmpty() || nombre.equals("Nombre del cliente")) {
                JOptionPane.showMessageDialog(this, "Ingresa el nombre del cliente."); return;
            }
            if (tel.isEmpty() || tel.length() < 10) {
                JOptionPane.showMessageDialog(this, "El teléfono debe tener al menos 10 dígitos."); return;
            }
            if (!editar) {
                guardarNuevoCliente(nombre, tel, txtPlacas, txtMarca, txtModelo, txtAnio, txtColor);
            } else {
                editarClienteEnBD(nombre, tel);
            }
        });

        gbc.gridwidth = 1; gbc.gridy = 6;
        gbc.gridx = 0; form.add(btnCancelar, gbc);
        gbc.gridx = 1; form.add(btnGuardar,  gbc);

        panel.add(form, BorderLayout.CENTER);
        return panel;
    }

    private void guardarNuevoCliente(String nombre, String tel,
                                     JTextField txtPlacas, JTextField txtMarca,
                                     JTextField txtModelo,  JTextField txtAnio, JTextField txtColor) {
        try (Connection con = ConexionMySQL.conectar()) {
            if (con == null) { JOptionPane.showMessageDialog(this, "Sin conexión a BD."); return; }

            PreparedStatement check = con.prepareStatement("SELECT id FROM clientes WHERE telefono = ?");
            check.setString(1, tel);
            if (check.executeQuery().next()) {
                JOptionPane.showMessageDialog(this, "Ya existe un cliente con ese teléfono."); return;
            }

            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO clientes (nombre, telefono) VALUES (?, ?)",
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setString(1, nombre.toUpperCase());
            ps.setString(2, tel);
            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            int idCliente = keys.next() ? keys.getInt(1) : -1;

            String placas = txtPlacas.getText().trim();
            if (!placas.isEmpty() && !placas.equals("Placas del vehículo") && idCliente != -1) {
                try {
                    PreparedStatement psVeh = con.prepareStatement(
                            "INSERT INTO vehiculos (placas, marca, modelo, anio, color, id_cliente) VALUES (?,?,?,?,?,?)"
                    );
                    psVeh.setString(1, placas.toUpperCase());
                    psVeh.setString(2, txtMarca.getText().trim().toUpperCase());
                    psVeh.setString(3, txtModelo.getText().trim().toUpperCase());
                    int anio = txtAnio.getText().trim().isEmpty() ? 2000
                            : Integer.parseInt(txtAnio.getText().trim());
                    psVeh.setInt(4, anio);
                    String color = txtColor.getText().trim();
                    psVeh.setString(5, color.isEmpty() || color.equals("Color") ? "N/A" : color.toUpperCase());
                    psVeh.setInt(6, idCliente);
                    psVeh.executeUpdate();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this,
                            "Cliente guardado, pero el vehículo no se pudo agregar: " + ex.getMessage());
                }
            }

            Seguridad.registrarAuditoria("CLIENTE_NUEVO", "Cliente: " + nombre);
            JOptionPane.showMessageDialog(this, "Cliente agregado correctamente.");
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            return;
        }
        refrescarTabla();
        cardDerecho.show(panelDerecho, "lista");
    }

    private void editarClienteEnBD(String nombre, String tel) {
        int fila = tabla.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Selecciona un cliente de la lista primero."); return;
        }
        int id = (int) modeloClientes.getValueAt(fila, 0);
        try (Connection con = ConexionMySQL.conectar()) {
            PreparedStatement ps = con.prepareStatement(
                    "UPDATE clientes SET nombre = ?, telefono = ? WHERE id = ?");
            ps.setString(1, nombre.toUpperCase());
            ps.setString(2, tel);
            ps.setInt(3, id);
            ps.executeUpdate();
            Seguridad.registrarAuditoria("CLIENTE_EDITAR", "ID: " + id);
            JOptionPane.showMessageDialog(this, "Cliente actualizado.");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage()); return;
        }
        refrescarTabla();
        cardDerecho.show(panelDerecho, "lista");
    }

    // ─── FORM ORDEN (FIX PRINCIPAL) ───
    private JPanel crearFormOrden() {
        JPanel panel = new JPanel(new BorderLayout(0, 20));
        panel.setOpaque(false);

        JLabel titulo = new JLabel("Generar Orden de Trabajo");
        titulo.setFont(new Font("SansSerif", Font.BOLD, 20));
        titulo.setForeground(MenuRefaccionaria.TEXT_WHITE);
        panel.add(titulo, BorderLayout.NORTH);

        JPanel form = card();
        form.setLayout(new GridBagLayout());
        form.setBorder(new EmptyBorder(25, 25, 25, 25));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.weightx = 1.0;

        // Usar los combos declarados como campos de la clase
        cbClienteOrden.setBackground(MenuRefaccionaria.BG_CARD2);
        cbClienteOrden.setForeground(MenuRefaccionaria.TEXT_WHITE);
        cbClienteOrden.setFont(new Font("SansSerif", Font.PLAIN, 13));
        cbClienteOrden.setRenderer((list, value, idx, sel, foc) -> {
            JLabel l = new JLabel(value == null ? "" : value[1] + " — " + value[2]);
            l.setBackground(sel ? MenuRefaccionaria.ACCENT_BLUE : MenuRefaccionaria.BG_CARD2);
            l.setForeground(MenuRefaccionaria.TEXT_WHITE);
            l.setOpaque(true);
            l.setBorder(new EmptyBorder(4, 8, 4, 8));
            return l;
        });

        cbVehiculoOrden.setBackground(MenuRefaccionaria.BG_CARD2);
        cbVehiculoOrden.setForeground(MenuRefaccionaria.TEXT_WHITE);
        cbVehiculoOrden.setFont(new Font("SansSerif", Font.PLAIN, 13));
        cbVehiculoOrden.setRenderer((list, value, idx, sel, foc) -> {
            JLabel l = new JLabel(value == null ? "Sin vehículo" : value[1]);
            l.setBackground(sel ? MenuRefaccionaria.ACCENT_BLUE : MenuRefaccionaria.BG_CARD2);
            l.setForeground(MenuRefaccionaria.TEXT_WHITE);
            l.setOpaque(true);
            l.setBorder(new EmptyBorder(4, 8, 4, 8));
            return l;
        });

        // Al cambiar cliente → recargar vehículos
        cbClienteOrden.addActionListener(e -> {
            cbVehiculoOrden.removeAllItems();
            Object sel = cbClienteOrden.getSelectedItem();
            if (sel instanceof String[] arr) {
                cargarVehiculosEnCombo(cbVehiculoOrden, Integer.parseInt(arr[0]));
            }
        });

        JTextField txtDiag    = MenuRefaccionaria.textField("Diagnóstico / Servicio");
        JTextField txtObs     = MenuRefaccionaria.textField("Observaciones");
        JTextField txtLlego   = MenuRefaccionaria.textField("Fecha llegó (DD/MM/YYYY)");
        JTextField txtEntrega = MenuRefaccionaria.textField("Fecha entrega (DD/MM/YYYY)");

        String[] estados = {"EN_PROCESO", "ESPERANDO_REFACCION", "LISTO", "ENTREGADO"};
        JComboBox<String> cbEstado = new JComboBox<>(estados);
        cbEstado.setBackground(MenuRefaccionaria.BG_CARD2);
        cbEstado.setForeground(MenuRefaccionaria.TEXT_WHITE);

        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2; form.add(cbClienteOrden,  gbc);
        gbc.gridy = 1;                                    form.add(cbVehiculoOrden, gbc);
        gbc.gridy = 2;                                    form.add(txtDiag,         gbc);
        gbc.gridy = 3;                                    form.add(txtObs,          gbc);
        gbc.gridwidth = 1;
        gbc.gridx = 0; gbc.gridy = 4; form.add(txtLlego,   gbc);
        gbc.gridx = 1;                form.add(txtEntrega,  gbc);
        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 2; form.add(cbEstado, gbc);

        JButton btnGuardar  = MenuRefaccionaria.btnPrimario("Guardar Orden");
        JButton btnCancelar = MenuRefaccionaria.btnPeligro("Cancelar");
        btnCancelar.addActionListener(e -> cardDerecho.show(panelDerecho, "lista"));

        btnGuardar.addActionListener(e -> {
            Object selCli = cbClienteOrden.getSelectedItem();
            if (!(selCli instanceof String[])) {
                JOptionPane.showMessageDialog(this, "Selecciona un cliente."); return;
            }

            String diag    = txtDiag.getText().trim();
            String obs     = txtObs.getText().trim();
            String llegada = txtLlego.getText().trim();
            String entrega = txtEntrega.getText().trim();
            String estado  = (String) cbEstado.getSelectedItem();

            if (diag.isEmpty() || diag.equals("Diagnóstico / Servicio")) {
                JOptionPane.showMessageDialog(this, "Ingresa el diagnóstico."); return;
            }

            int idCli = Integer.parseInt(((String[]) selCli)[0]);

            // FIX: si no hay vehículo en el combo, crear uno genérico
            int idVeh = -1;
            Object selVeh = cbVehiculoOrden.getSelectedItem();
            if (selVeh instanceof String[] arrVeh) {
                idVeh = Integer.parseInt(arrVeh[0]);
            }

            try (Connection con = ConexionMySQL.conectar()) {
                if (con == null) { JOptionPane.showMessageDialog(this, "Sin conexión a BD."); return; }

                // Si el cliente no tiene vehículo, insertar uno genérico automáticamente
                if (idVeh == -1) {
                    PreparedStatement insVeh = con.prepareStatement(
                            "INSERT INTO vehiculos (placas, marca, modelo, anio, color, id_cliente) " +
                                    "VALUES (?, 'SIN MARCA', 'SIN MODELO', 2000, 'N/A', ?)",
                            Statement.RETURN_GENERATED_KEYS
                    );
                    // Placas únicas basadas en timestamp
                    insVeh.setString(1, "GEN-" + System.currentTimeMillis() % 100000);
                    insVeh.setInt(2, idCli);
                    insVeh.executeUpdate();
                    ResultSet kv = insVeh.getGeneratedKeys();
                    idVeh = kv.next() ? kv.getInt(1) : 1;
                }

                PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO ordenes (diagnostico, estado, observaciones, " +
                                "fecha_ingreso, fecha_entrega, id_cliente, id_vehiculo) " +
                                "VALUES (?, ?, ?, ?, ?, ?, ?)"
                );
                ps.setString(1, diag.toUpperCase());
                ps.setString(2, estado);
                ps.setString(3, obs.isEmpty() || obs.equals("Observaciones") ? null : obs.toUpperCase());
                ps.setDate(4, java.sql.Date.valueOf(
                        llegada.isEmpty() || llegada.equals("Fecha llegó (DD/MM/YYYY)")
                                ? java.time.LocalDate.now() : parsearFecha(llegada)));
                String entradaEntrega = entrega.isEmpty()
                        || entrega.equals("Fecha entrega (DD/MM/YYYY)") ? null : entrega;
                if (entradaEntrega != null) {
                    ps.setDate(5, java.sql.Date.valueOf(parsearFecha(entradaEntrega)));
                } else {
                    ps.setNull(5, java.sql.Types.DATE);
                }
                ps.setInt(6, idCli);
                ps.setInt(7, idVeh);
                ps.executeUpdate();

                Seguridad.registrarAuditoria("ORDEN_NUEVA", "Cliente ID: " + idCli + " | " + diag);
                JOptionPane.showMessageDialog(this, "✅ Orden creada correctamente.");

                // Limpiar campos
                txtDiag.setText("Diagnóstico / Servicio");
                txtDiag.setForeground(MenuRefaccionaria.TEXT_MUTED);
                txtObs.setText("Observaciones");
                txtObs.setForeground(MenuRefaccionaria.TEXT_MUTED);
                txtLlego.setText("Fecha llegó (DD/MM/YYYY)");
                txtLlego.setForeground(MenuRefaccionaria.TEXT_MUTED);
                txtEntrega.setText("Fecha entrega (DD/MM/YYYY)");
                txtEntrega.setForeground(MenuRefaccionaria.TEXT_MUTED);

                cardDerecho.show(panelDerecho, "lista");

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error al guardar orden: " + ex.getMessage());
            }
        });

        gbc.gridwidth = 1; gbc.gridy = 6;
        gbc.gridx = 0; form.add(btnCancelar, gbc);
        gbc.gridx = 1; form.add(btnGuardar,  gbc);

        panel.add(form, BorderLayout.CENTER);
        return panel;
    }

    // ─── PANEL BUSCAR ───
    private JPanel crearPanelBuscar() {
        JPanel panel = new JPanel(new BorderLayout(0, 20));
        panel.setOpaque(false);

        JLabel titulo = new JLabel("Buscar Cliente");
        titulo.setFont(new Font("SansSerif", Font.BOLD, 20));
        titulo.setForeground(MenuRefaccionaria.TEXT_WHITE);
        panel.add(titulo, BorderLayout.NORTH);

        JPanel top = new JPanel(new BorderLayout(10, 0));
        top.setOpaque(false);
        JTextField txtBuscar = MenuRefaccionaria.textField("Nombre o teléfono...");
        JButton btnBuscar = MenuRefaccionaria.btnPrimario("Buscar");
        top.add(txtBuscar, BorderLayout.CENTER);
        top.add(btnBuscar, BorderLayout.EAST);

        DefaultTableModel modeloBusq = new DefaultTableModel(
                new String[]{"ID", "Nombre", "Teléfono", "Vehículos"}, 0
        ) { @Override public boolean isCellEditable(int r, int c) { return false; } };

        JTable tblBusq = new JTable(modeloBusq);
        tblBusq.setBackground(MenuRefaccionaria.BG_CARD);
        tblBusq.setForeground(MenuRefaccionaria.TEXT_WHITE);
        tblBusq.setRowHeight(30);
        tblBusq.getTableHeader().setBackground(MenuRefaccionaria.BG_CARD2);
        tblBusq.getTableHeader().setForeground(MenuRefaccionaria.TEXT_MUTED);
        tblBusq.setSelectionBackground(MenuRefaccionaria.ACCENT_BLUE);

        JScrollPane sc = new JScrollPane(tblBusq);
        sc.getViewport().setBackground(MenuRefaccionaria.BG_CARD);

        JPanel centro = new JPanel(new BorderLayout(0, 10));
        centro.setOpaque(false);
        centro.add(top, BorderLayout.NORTH);
        centro.add(sc,  BorderLayout.CENTER);
        panel.add(centro, BorderLayout.CENTER);

        Runnable buscar = () -> {
            String q = txtBuscar.getText().trim();
            if (q.isEmpty() || q.equals("Nombre o teléfono...")) return;
            modeloBusq.setRowCount(0);
            try (Connection con = ConexionMySQL.conectar()) {
                PreparedStatement ps = con.prepareStatement(
                        "SELECT c.id, c.nombre, c.telefono, COUNT(v.id) AS vehs " +
                                "FROM clientes c LEFT JOIN vehiculos v ON v.id_cliente = c.id " +
                                "WHERE c.nombre LIKE ? OR c.telefono LIKE ? " +
                                "GROUP BY c.id ORDER BY c.nombre"
                );
                String like = "%" + q + "%";
                ps.setString(1, like); ps.setString(2, like);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    modeloBusq.addRow(new Object[]{
                            rs.getInt("id"), rs.getString("nombre"),
                            rs.getString("telefono"), rs.getInt("vehs")
                    });
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(panel, "Error: " + ex.getMessage());
            }
        };

        btnBuscar.addActionListener(e -> buscar.run());
        txtBuscar.addActionListener(e -> buscar.run());

        JButton btnVolver = MenuRefaccionaria.btnPeligro("← Volver");
        btnVolver.addActionListener(e -> cardDerecho.show(panelDerecho, "lista"));
        panel.add(btnVolver, BorderLayout.SOUTH);

        return panel;
    }

    // ─── REFRESCAR TABLA ───
    private void refrescarTabla() {
        if (modeloClientes == null || contenedor == null) return;
        modeloClientes.setRowCount(0);
        SistemaTaller.clientes.clear();

        try (Connection con = ConexionMySQL.conectar()) {
            if (con == null) return;
            String sql = "SELECT c.id, c.nombre, c.telefono, COUNT(v.id) AS vehs " +
                    "FROM clientes c LEFT JOIN vehiculos v ON v.id_cliente = c.id " +
                    "GROUP BY c.id ORDER BY c.nombre";
            ResultSet rs = con.createStatement().executeQuery(sql);
            while (rs.next()) {
                Cliente c = new Cliente(rs.getInt("id"), rs.getString("nombre"), rs.getString("telefono"));
                SistemaTaller.clientes.add(c);
                modeloClientes.addRow(new Object[]{
                        c.getId(), c.getNombre(), c.getTelefono(), rs.getInt("vehs")
                });
            }
        } catch (Exception ex) {
            System.err.println("Error cargando clientes: " + ex.getMessage());
        }

        contenedor.removeAll();
        contenedor.add(SistemaTaller.clientes.isEmpty() ? emptyPanel : scroll, BorderLayout.CENTER);
        contenedor.revalidate();
        contenedor.repaint();
    }

    // ─── ELIMINAR ───
    private void eliminarClienteSeleccionado() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) { JOptionPane.showMessageDialog(this, "Selecciona un cliente de la tabla."); return; }
        int id     = (int) modeloClientes.getValueAt(fila, 0);
        String nom = modeloClientes.getValueAt(fila, 1).toString();
        int ok = JOptionPane.showConfirmDialog(this,
                "¿Eliminar a " + nom + "?\nSe eliminarán también sus vehículos y órdenes.",
                "Eliminar cliente", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ok != JOptionPane.YES_OPTION) return;
        try (Connection con = ConexionMySQL.conectar()) {
            PreparedStatement ps = con.prepareStatement("DELETE FROM clientes WHERE id = ?");
            ps.setInt(1, id);
            ps.executeUpdate();
            Seguridad.registrarAuditoria("CLIENTE_ELIMINAR", "ID: " + id + " Nombre: " + nom);
            JOptionPane.showMessageDialog(this, "Cliente eliminado.");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
        refrescarTabla();
    }

    // ─── EDITAR ───
    private void editarClienteSeleccionado() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) { JOptionPane.showMessageDialog(this, "Selecciona un cliente de la tabla primero."); return; }
        cardDerecho.show(panelDerecho, "editar");
    }

    // ─── HELPERS BD ───
    private void cargarClientesEnCombo(JComboBox<String[]> cb) {
        cb.removeAllItems();
        try (Connection con = ConexionMySQL.conectar()) {
            if (con == null) return;
            ResultSet rs = con.createStatement()
                    .executeQuery("SELECT id, nombre, telefono FROM clientes ORDER BY nombre");
            while (rs.next()) {
                cb.addItem(new String[]{
                        String.valueOf(rs.getInt("id")),
                        rs.getString("nombre"),
                        rs.getString("telefono")
                });
            }
        } catch (Exception ex) {
            System.err.println("Error cargando clientes combo: " + ex.getMessage());
        }
    }

    private void cargarVehiculosEnCombo(JComboBox<String[]> cb, int idCliente) {
        cb.removeAllItems();
        try (Connection con = ConexionMySQL.conectar()) {
            if (con == null) return;
            PreparedStatement ps = con.prepareStatement(
                    "SELECT id, marca, modelo, anio, placas FROM vehiculos WHERE id_cliente = ?");
            ps.setInt(1, idCliente);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                cb.addItem(new String[]{
                        String.valueOf(rs.getInt("id")),
                        rs.getString("marca") + " " + rs.getString("modelo") +
                                " " + rs.getInt("anio") + " (" + rs.getString("placas") + ")"
                });
            }
        } catch (Exception ex) {
            System.err.println("Error cargando vehículos combo: " + ex.getMessage());
        }
    }

    // ─── UI helpers ───
    private JPanel card() {
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
        l.setFont(new Font("SansSerif", Font.BOLD, 13));
        return l;
    }

    private JLabel seccion(String texto) {
        JLabel lbl = new JLabel(texto);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 13));
        lbl.setForeground(MenuRefaccionaria.TEXT_MUTED);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
    }

    private JButton btnMenu(String texto, Runnable accion) {
        JButton btn = new JButton(texto);
        btn.setFont(new Font("SansSerif", Font.BOLD, 13));
        btn.setForeground(Color.WHITE);
        btn.setBackground(MenuRefaccionaria.ACCENT_BLUE);
        btn.setBorder(new EmptyBorder(10, 15, 10, 15));
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.addActionListener(e -> accion.run());
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) { btn.setBackground(MenuRefaccionaria.ACCENT_HOVER); }
            public void mouseExited(java.awt.event.MouseEvent e)  { btn.setBackground(MenuRefaccionaria.ACCENT_BLUE); }
        });
        return btn;
    }

    // ─── CONVERTIR FECHA DD/MM/YYYY o DD-MM-YYYY → LocalDate para MySQL ───
    private java.time.LocalDate parsearFecha(String fecha) {
        if (fecha == null || fecha.isBlank()) return java.time.LocalDate.now();
        try {
            if (fecha.matches("\\d{2}[/\\-]\\d{2}[/\\-]\\d{4}")) {
                String[] p = fecha.split("[/\\-]");
                return java.time.LocalDate.of(
                        Integer.parseInt(p[2]),
                        Integer.parseInt(p[1]),
                        Integer.parseInt(p[0])
                );
            }
            if (fecha.matches("\\d{4}-\\d{2}-\\d{2}")) {
                return java.time.LocalDate.parse(fecha);
            }
            return java.time.LocalDate.now();
        } catch (Exception e) {
            return java.time.LocalDate.now();
        }
    }
}