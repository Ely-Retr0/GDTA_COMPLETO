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
 * GDTA — Panel Usuarios (solo ADMIN)
 * CRUD de usuarios + cambio de contraseña + log de auditoría.
 */
public class PanelUsuarios extends JPanel {

    private MenuRefaccionaria menu;
    private DefaultTableModel modeloUsuarios;
    private DefaultTableModel modeloAuditoria;
    private JTable            tabla;
    private CardLayout        card;
    private JPanel            contenedor;

    public PanelUsuarios(MenuRefaccionaria menu) {
        this.menu = menu;
        setLayout(new BorderLayout(20, 0));
        setBackground(MenuRefaccionaria.BG_DARK);
        setBorder(new EmptyBorder(25, 30, 25, 30));

        add(crearSidebar(), BorderLayout.WEST);

        card      = new CardLayout();
        contenedor = new JPanel(card);
        contenedor.setOpaque(false);
        contenedor.add(crearPanelLista(),     "lista");
        contenedor.add(crearFormUsuario(),    "agregar");
        contenedor.add(crearPanelAuditoria(), "auditoria");
        card.show(contenedor, "lista");
        add(contenedor, BorderLayout.CENTER);
    }

    // ─── SIDEBAR ───
    private JPanel crearSidebar() {
        JPanel p = roundCard();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setPreferredSize(new Dimension(220, 0));
        p.setBorder(new EmptyBorder(20, 15, 20, 15));

        p.add(secLbl("Gestión de Usuarios"));
        p.add(Box.createVerticalStrut(8));
        p.add(menuBtn("Ver usuarios",         () -> { cargarUsuarios(); card.show(contenedor, "lista"); }));
        p.add(Box.createVerticalStrut(6));
        p.add(menuBtn("Agregar usuario",       () -> card.show(contenedor, "agregar")));
        p.add(Box.createVerticalStrut(6));
        p.add(menuBtn("Cambiar contraseña",    this::cambiarPasswordSeleccionado));
        p.add(Box.createVerticalStrut(6));
        p.add(menuBtn("Activar/Desactivar",    this::toggleActivoSeleccionado));
        p.add(Box.createVerticalStrut(20));
        p.add(secLbl("Seguridad"));
        p.add(Box.createVerticalStrut(8));
        p.add(menuBtn("Log de Auditoría",      () -> { cargarAuditoria(); card.show(contenedor, "auditoria"); }));
        p.add(Box.createVerticalGlue());
        return p;
    }

    // ─── LISTA USUARIOS ───
    private JPanel crearPanelLista() {
        JPanel panel = new JPanel(new BorderLayout(0, 15));
        panel.setOpaque(false);

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel lbl = new JLabel("Usuarios del Sistema");
        lbl.setFont(new Font("SansSerif", Font.BOLD, 20));
        lbl.setForeground(MenuRefaccionaria.TEXT_WHITE);
        JButton btnRefresh = MenuRefaccionaria.btnPrimario("↻ Actualizar");
        btnRefresh.addActionListener(e -> cargarUsuarios());
        header.add(lbl, BorderLayout.WEST);
        header.add(btnRefresh, BorderLayout.EAST);
        panel.add(header, BorderLayout.NORTH);

        modeloUsuarios = new DefaultTableModel(
            new String[]{"ID", "Usuario", "Nombre Completo", "Rol", "Activo", "Último acceso"}, 0
        ) { @Override public boolean isCellEditable(int r, int c) { return false; } };

        tabla = new JTable(modeloUsuarios);
        tabla.setBackground(MenuRefaccionaria.BG_CARD);
        tabla.setForeground(MenuRefaccionaria.TEXT_WHITE);
        tabla.setFont(new Font("SansSerif", Font.PLAIN, 13));
        tabla.setRowHeight(32);
        tabla.getTableHeader().setBackground(MenuRefaccionaria.BG_CARD2);
        tabla.getTableHeader().setForeground(MenuRefaccionaria.TEXT_MUTED);
        tabla.setSelectionBackground(MenuRefaccionaria.ACCENT_BLUE);
        tabla.setGridColor(new Color(40, 48, 80));

        tabla.setDefaultRenderer(Object.class, (tbl, value, sel, foc, row, col) -> {
            JLabel cell = new JLabel(value == null ? "" : value.toString());
            cell.setOpaque(true);
            cell.setBorder(new EmptyBorder(0, 8, 0, 8));
            cell.setFont(new Font("SansSerif", Font.PLAIN, 13));
            String activo = "";
            try { activo = tbl.getValueAt(row, 4).toString(); } catch (Exception ignored) {}

            if (sel) {
                cell.setBackground(MenuRefaccionaria.ACCENT_BLUE);
                cell.setForeground(Color.WHITE);
            } else if (activo.equals("Inactivo")) {
                cell.setBackground(MenuRefaccionaria.BG_CARD);
                cell.setForeground(new Color(120, 60, 60));
            } else {
                cell.setBackground(MenuRefaccionaria.BG_CARD);
                cell.setForeground(MenuRefaccionaria.TEXT_WHITE);
            }
            return cell;
        });

        JScrollPane sc = new JScrollPane(tabla);
        sc.getViewport().setBackground(MenuRefaccionaria.BG_CARD);
        sc.setBorder(null);
        panel.add(sc, BorderLayout.CENTER);

        cargarUsuarios();
        return panel;
    }

    private void cargarUsuarios() {
        if (modeloUsuarios == null) return;
        modeloUsuarios.setRowCount(0);
        try (Connection con = ConexionMySQL.conectar()) {
            if (con == null) return;
            ResultSet rs = con.createStatement().executeQuery(
                "SELECT id, username, nombre, rol, activo, ultimo_acceso FROM usuarios ORDER BY rol, nombre"
            );
            while (rs.next()) {
                modeloUsuarios.addRow(new Object[]{
                    rs.getInt("id"),
                    rs.getString("username"),
                    rs.getString("nombre"),
                    rs.getString("rol"),
                    rs.getInt("activo") == 1 ? "Activo" : "Inactivo",
                    rs.getString("ultimo_acceso") != null ? rs.getString("ultimo_acceso") : "—"
                });
            }
        } catch (Exception ex) {
            System.err.println("Error cargando usuarios: " + ex.getMessage());
        }
    }

    // ─── FORM NUEVO USUARIO ───
    private JPanel crearFormUsuario() {
        JPanel panel = new JPanel(new BorderLayout(0, 20));
        panel.setOpaque(false);

        JLabel titulo = new JLabel("Agregar Usuario");
        titulo.setFont(new Font("SansSerif", Font.BOLD, 20));
        titulo.setForeground(MenuRefaccionaria.TEXT_WHITE);
        panel.add(titulo, BorderLayout.NORTH);

        JPanel form = roundCard();
        form.setLayout(new GridBagLayout());
        form.setBorder(new EmptyBorder(25, 25, 25, 25));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.weightx = 1.0;

        JTextField    txtNombre   = MenuRefaccionaria.textField("Nombre completo");
        JTextField    txtUsername = MenuRefaccionaria.textField("Nombre de usuario");
        JPasswordField txtPass1   = new JPasswordField();
        JPasswordField txtPass2   = new JPasswordField();
        stylePass(txtPass1); stylePass(txtPass2);

        String[] roles = {"MECANICO", "CAJERO", "ADMIN"};
        JComboBox<String> cbRol = new JComboBox<>(roles);
        cbRol.setBackground(MenuRefaccionaria.BG_CARD2);
        cbRol.setForeground(MenuRefaccionaria.TEXT_WHITE);
        cbRol.setFont(new Font("SansSerif", Font.PLAIN, 13));

        gbc.gridy = 0; form.add(txtNombre,   gbc);
        gbc.gridy = 1; form.add(txtUsername, gbc);
        gbc.gridy = 2; form.add(lbl("Contraseña:"), gbc);
        gbc.gridy = 3; form.add(txtPass1,    gbc);
        gbc.gridy = 4; form.add(lbl("Confirmar contraseña:"), gbc);
        gbc.gridy = 5; form.add(txtPass2,    gbc);
        gbc.gridy = 6; form.add(lbl("Rol:"), gbc);
        gbc.gridy = 7; form.add(cbRol,       gbc);

        JButton btnGuardar  = MenuRefaccionaria.btnPrimario("Crear usuario");
        JButton btnCancelar = MenuRefaccionaria.btnPeligro("Cancelar");
        btnCancelar.addActionListener(e -> card.show(contenedor, "lista"));

        btnGuardar.addActionListener(e -> {
            String nombre   = txtNombre.getText().trim();
            String username = txtUsername.getText().trim().toLowerCase();
            String pass1    = new String(txtPass1.getPassword());
            String pass2    = new String(txtPass2.getPassword());

            if (nombre.isEmpty() || username.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Nombre y usuario son obligatorios."); return;
            }
            if (pass1.length() < 6) {
                JOptionPane.showMessageDialog(this, "La contraseña debe tener al menos 6 caracteres."); return;
            }
            if (!pass1.equals(pass2)) {
                JOptionPane.showMessageDialog(this, "Las contraseñas no coinciden."); return;
            }
            Seguridad.Rol rol = Seguridad.Rol.valueOf((String) cbRol.getSelectedItem());
            if (Seguridad.crearUsuario(username, pass1, nombre, rol)) {
                JOptionPane.showMessageDialog(this, "Usuario creado correctamente.");
                cargarUsuarios();
                card.show(contenedor, "lista");
            } else {
                JOptionPane.showMessageDialog(this, "Error al crear usuario. ¿El nombre de usuario ya existe?");
            }
        });

        gbc.gridy = 8; gbc.gridwidth = 1; gbc.gridx = 0; gbc.weightx = 0.5;
        form.add(btnCancelar, gbc);
        gbc.gridx = 1; form.add(btnGuardar, gbc);

        panel.add(form, BorderLayout.CENTER);
        return panel;
    }

    // ─── CAMBIAR CONTRASEÑA ───
    private void cambiarPasswordSeleccionado() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) { JOptionPane.showMessageDialog(this, "Selecciona un usuario."); return; }
        String username = modeloUsuarios.getValueAt(fila, 1).toString();

        JPasswordField p1 = new JPasswordField(); stylePass(p1);
        JPasswordField p2 = new JPasswordField(); stylePass(p2);

        JPanel dlg = new JPanel(new GridLayout(4, 1, 0, 8));
        dlg.add(new JLabel("Nueva contraseña para: " + username));
        dlg.add(p1);
        dlg.add(new JLabel("Confirmar:"));
        dlg.add(p2);

        int ok = JOptionPane.showConfirmDialog(this, dlg, "Cambiar contraseña", JOptionPane.OK_CANCEL_OPTION);
        if (ok != JOptionPane.OK_OPTION) return;

        String pass1 = new String(p1.getPassword());
        String pass2 = new String(p2.getPassword());
        if (pass1.length() < 6) { JOptionPane.showMessageDialog(this, "Mínimo 6 caracteres."); return; }
        if (!pass1.equals(pass2)) { JOptionPane.showMessageDialog(this, "Las contraseñas no coinciden."); return; }

        if (Seguridad.cambiarPassword(username, pass1)) {
            JOptionPane.showMessageDialog(this, "Contraseña actualizada.");
        } else {
            JOptionPane.showMessageDialog(this, "Error al actualizar contraseña.");
        }
    }

    // ─── ACTIVAR / DESACTIVAR ───
    private void toggleActivoSeleccionado() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) { JOptionPane.showMessageDialog(this, "Selecciona un usuario."); return; }
        int    id      = (int) modeloUsuarios.getValueAt(fila, 0);
        String activo  = modeloUsuarios.getValueAt(fila, 4).toString();
        String nombre  = modeloUsuarios.getValueAt(fila, 1).toString();
        int nuevoEstado = activo.equals("Activo") ? 0 : 1;
        String accion   = nuevoEstado == 1 ? "activar" : "desactivar";

        int ok = JOptionPane.showConfirmDialog(this,
            "¿" + accion.substring(0,1).toUpperCase()+accion.substring(1) + " al usuario " + nombre + "?",
            "Confirmar", JOptionPane.YES_NO_OPTION);
        if (ok != JOptionPane.YES_OPTION) return;

        try (Connection con = ConexionMySQL.conectar()) {
            PreparedStatement ps = con.prepareStatement("UPDATE usuarios SET activo=? WHERE id=?");
            ps.setInt(1, nuevoEstado); ps.setInt(2, id);
            ps.executeUpdate();
            Seguridad.registrarAuditoria("USUARIO_TOGGLE", nombre + " → " + (nuevoEstado == 1 ? "ACTIVO" : "INACTIVO"));
            JOptionPane.showMessageDialog(this, "Usuario " + (nuevoEstado == 1 ? "activado." : "desactivado."));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
        cargarUsuarios();
    }

    // ─── LOG AUDITORÍA ───
    private JPanel crearPanelAuditoria() {
        JPanel panel = new JPanel(new BorderLayout(0, 15));
        panel.setOpaque(false);

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel lbl = new JLabel("Log de Auditoría");
        lbl.setFont(new Font("SansSerif", Font.BOLD, 20));
        lbl.setForeground(MenuRefaccionaria.TEXT_WHITE);
        JButton btnRefresh = MenuRefaccionaria.btnPrimario("↻ Actualizar");
        btnRefresh.addActionListener(e -> cargarAuditoria());
        header.add(lbl, BorderLayout.WEST);
        header.add(btnRefresh, BorderLayout.EAST);
        panel.add(header, BorderLayout.NORTH);

        modeloAuditoria = new DefaultTableModel(
            new String[]{"Fecha/Hora", "Usuario", "Acción", "Detalle", "IP"}, 0
        ) { @Override public boolean isCellEditable(int r, int c) { return false; } };

        JTable tblAud = new JTable(modeloAuditoria);
        tblAud.setBackground(MenuRefaccionaria.BG_CARD);
        tblAud.setForeground(MenuRefaccionaria.TEXT_WHITE);
        tblAud.setFont(new Font("SansSerif", Font.PLAIN, 12));
        tblAud.setRowHeight(28);
        tblAud.getTableHeader().setBackground(MenuRefaccionaria.BG_CARD2);
        tblAud.getTableHeader().setForeground(MenuRefaccionaria.TEXT_MUTED);
        tblAud.setSelectionBackground(MenuRefaccionaria.ACCENT_BLUE);
        tblAud.setGridColor(new Color(40, 48, 80));

        JScrollPane sc = new JScrollPane(tblAud);
        sc.getViewport().setBackground(MenuRefaccionaria.BG_CARD);
        sc.setBorder(null);
        panel.add(sc, BorderLayout.CENTER);

        cargarAuditoria();
        return panel;
    }

    private void cargarAuditoria() {
        if (modeloAuditoria == null) return;
        modeloAuditoria.setRowCount(0);
        try (Connection con = ConexionMySQL.conectar()) {
            if (con == null) return;
            ResultSet rs = con.createStatement().executeQuery(
                "SELECT fecha_hora, usuario, accion, detalle, ip FROM auditoria ORDER BY id DESC LIMIT 200"
            );
            while (rs.next()) {
                modeloAuditoria.addRow(new Object[]{
                    rs.getString("fecha_hora"),
                    rs.getString("usuario"),
                    rs.getString("accion"),
                    rs.getString("detalle"),
                    rs.getString("ip")
                });
            }
        } catch (Exception ex) {
            System.err.println("Error cargando auditoría: " + ex.getMessage());
        }
    }

    // ─── Helpers ───
    private void stylePass(JPasswordField p) {
        p.setBackground(MenuRefaccionaria.BG_CARD2);
        p.setForeground(MenuRefaccionaria.TEXT_WHITE);
        p.setFont(new Font("SansSerif", Font.PLAIN, 13));
        p.setCaretColor(Color.WHITE);
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(58, 107, 210), 1),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)));
    }

    private JLabel lbl(String t) {
        JLabel l = new JLabel(t);
        l.setForeground(MenuRefaccionaria.TEXT_MUTED);
        l.setFont(new Font("SansSerif", Font.BOLD, 12)); return l;
    }

    private JLabel secLbl(String t) {
        JLabel l = new JLabel(t);
        l.setFont(new Font("SansSerif", Font.BOLD, 13));
        l.setForeground(MenuRefaccionaria.TEXT_MUTED);
        l.setAlignmentX(Component.LEFT_ALIGNMENT); return l;
    }

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
