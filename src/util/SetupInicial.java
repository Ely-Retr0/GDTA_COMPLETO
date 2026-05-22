package util;

import modelo.ConexionMySQL;
import seguridad.Seguridad;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;

public class SetupInicial extends JDialog {

    private boolean completado = false;
    private JTextField     txtHost, txtPuerto, txtBD, txtUsuario;
    private JTextField     txtAdminUser, txtAdminNombre;
    private JPasswordField txtPassword, txtAdminPass, txtAdminPass2;
    private JLabel         lblEstado;

    public SetupInicial() {
        setTitle("GDTA — Configuración Inicial");
        setModal(true);
        setSize(500, 610);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setResizable(false);

        Color BG    = new Color(18, 22, 38);
        Color CARD  = new Color(30, 36, 60);
        Color BLUE  = new Color(58, 107, 210);
        Color FG    = new Color(235, 238, 250);
        Color MUTED = new Color(140, 150, 180);
        Font  F13   = new Font("SansSerif", Font.PLAIN, 13);
        Font  FBOLD = new Font("SansSerif", Font.BOLD, 13);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG);
        root.setBorder(new EmptyBorder(28, 32, 28, 32));
        setContentPane(root);

        JLabel titulo = new JLabel("⚙  Configuración Inicial");
        titulo.setFont(new Font("SansSerif", Font.BOLD, 20));
        titulo.setForeground(FG);
        titulo.setBorder(new EmptyBorder(0, 0, 18, 0));
        root.add(titulo, BorderLayout.NORTH);

        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));

        // — BD —
        form.add(seccion("🗄  Conexión a MySQL / MariaDB", MUTED, FBOLD));
        form.add(Box.createVerticalStrut(8));

        txtHost     = campo("localhost", BG, FG, F13, BLUE);
        txtPuerto   = campo("3306",      BG, FG, F13, BLUE);
        txtBD       = campo("bdgdta",    BG, FG, F13, BLUE);
        txtUsuario  = campo("root",      BG, FG, F13, BLUE);
        txtPassword = passField(BG, FG, F13, BLUE);

        form.add(fila("Host",     txtHost,     MUTED, F13));  form.add(gap());
        form.add(fila("Puerto",   txtPuerto,   MUTED, F13));  form.add(gap());
        form.add(fila("BD",       txtBD,       MUTED, F13));  form.add(gap());
        form.add(fila("Usuario",  txtUsuario,  MUTED, F13));  form.add(gap());
        form.add(fila("Password", txtPassword, MUTED, F13));  form.add(Box.createVerticalStrut(10));

        JButton btnProbar = btn("Probar conexión", CARD, FG, FBOLD);
        btnProbar.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnProbar.addActionListener(e -> probarConexion());
        form.add(btnProbar);
        form.add(Box.createVerticalStrut(20));

        // — Admin —
        form.add(seccion("👤  Crear usuario Administrador", MUTED, FBOLD));
        form.add(Box.createVerticalStrut(8));

        txtAdminNombre = campo("Nombre completo", BG, FG, F13, BLUE);
        txtAdminUser   = campo("admin",           BG, FG, F13, BLUE);
        txtAdminPass   = passField(BG, FG, F13, BLUE);
        txtAdminPass2  = passField(BG, FG, F13, BLUE);

        form.add(fila("Nombre",     txtAdminNombre, MUTED, F13)); form.add(gap());
        form.add(fila("Usuario",    txtAdminUser,   MUTED, F13)); form.add(gap());
        form.add(fila("Contraseña", txtAdminPass,   MUTED, F13)); form.add(gap());
        form.add(fila("Confirmar",  txtAdminPass2,  MUTED, F13));

        JScrollPane scroll = new JScrollPane(form);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);
        root.add(scroll, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout(0, 10));
        footer.setOpaque(false);
        footer.setBorder(new EmptyBorder(14, 0, 0, 0));

        lblEstado = new JLabel(" ");
        lblEstado.setFont(F13);
        lblEstado.setForeground(MUTED);
        footer.add(lblEstado, BorderLayout.NORTH);

        JButton btnFin = btn("Finalizar configuración", BLUE, FG, new Font("SansSerif", Font.BOLD, 14));
        btnFin.addActionListener(e -> finalizar());
        footer.add(btnFin, BorderLayout.CENTER);
        root.add(footer, BorderLayout.SOUTH);
    }

    private void probarConexion() {
        ConexionMySQL.guardarConfig(buildURL(),
            txtUsuario.getText().trim(),
            new String(txtPassword.getPassword()));
        if (ConexionMySQL.probarConexion()) {
            lblEstado.setText("✅ Conexión exitosa");
            lblEstado.setForeground(new Color(80, 200, 120));
        } else {
            lblEstado.setText("❌ Sin conexión. Revisa host/usuario/password.");
            lblEstado.setForeground(new Color(196, 70, 70));
        }
    }

    private void finalizar() {
        String nombre   = txtAdminNombre.getText().trim();
        String username = txtAdminUser.getText().trim().toLowerCase();
        String pass1    = new String(txtAdminPass.getPassword());
        String pass2    = new String(txtAdminPass2.getPassword());

        if (nombre.isEmpty() || nombre.equals("Nombre completo")) { error("Ingresa el nombre del administrador."); return; }
        if (username.isEmpty()) { error("Ingresa el nombre de usuario."); return; }
        if (pass1.length() < 6) { error("La contraseña debe tener al menos 6 caracteres."); return; }
        if (!pass1.equals(pass2)) { error("Las contraseñas no coinciden. Verifica ambos campos."); return; }

        ConexionMySQL.guardarConfig(buildURL(),
            txtUsuario.getText().trim(),
            new String(txtPassword.getPassword()));

        try (Connection con = ConexionMySQL.conectar()) {
            if (con == null) { error("Sin conexión a BD. Usa 'Probar conexión' primero."); return; }
            String hash = Seguridad.hashPassword(pass1);
            PreparedStatement upd = con.prepareStatement(
                "UPDATE usuarios SET password_hash=?, nombre=?, username=? WHERE rol='ADMIN' LIMIT 1");
            upd.setString(1, hash);
            upd.setString(2, nombre.toUpperCase());
            upd.setString(3, username);
            int rows = upd.executeUpdate();
            if (rows == 0) {
                PreparedStatement ins = con.prepareStatement(
                    "INSERT INTO usuarios (username, password_hash, nombre, rol, activo) VALUES (?,?,?,'ADMIN',1)");
                ins.setString(1, username);
                ins.setString(2, hash);
                ins.setString(3, nombre.toUpperCase());
                ins.executeUpdate();
            }
        } catch (Exception e) { error("Error en BD: " + e.getMessage()); return; }

        completado = true;
        JOptionPane.showMessageDialog(this, "✅ Configuración completada.\nInicia sesión con: " + username);
        dispose();
    }

    private String buildURL() {
        return "jdbc:mysql://" + txtHost.getText().trim() + ":" + txtPuerto.getText().trim() +
               "/" + txtBD.getText().trim() +
               "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=America/Mexico_City";
    }

    private void error(String msg) { lblEstado.setText(msg); lblEstado.setForeground(new Color(196, 70, 70)); }

    private Component gap() { return Box.createVerticalStrut(6); }

    private JPanel fila(String labelTxt, JComponent campo, Color lc, Font f) {
        JPanel p = new JPanel(new BorderLayout(10, 0));
        p.setOpaque(false);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel lbl = new JLabel(labelTxt);
        lbl.setForeground(lc); lbl.setFont(f);
        lbl.setPreferredSize(new Dimension(90, 30));
        p.add(lbl, BorderLayout.WEST);
        p.add(campo, BorderLayout.CENTER);
        return p;
    }

    private JLabel seccion(String t, Color c, Font f) {
        JLabel l = new JLabel(t); l.setFont(f); l.setForeground(c);
        l.setAlignmentX(Component.LEFT_ALIGNMENT); return l;
    }

    private JTextField campo(String ph, Color bg, Color fg, Font f, Color border) {
        JTextField t = new JTextField(ph);
        t.setBackground(bg); t.setForeground(fg); t.setFont(f); t.setCaretColor(fg);
        t.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        t.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(border, 1),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)));
        t.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent e) {
                if (t.getText().equals(ph)) { t.setText(""); t.setForeground(fg); }
            }
        });
        return t;
    }

    private JPasswordField passField(Color bg, Color fg, Font f, Color border) {
        JPasswordField p = new JPasswordField();
        p.setBackground(bg); p.setForeground(fg); p.setFont(f); p.setCaretColor(fg);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(border, 1),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)));
        return p;
    }

    private JButton btn(String t, Color bg, Color fg, Font f) {
        JButton b = new JButton(t);
        b.setBackground(bg); b.setForeground(fg); b.setFont(f);
        b.setFocusPainted(false);
        b.setBorder(new EmptyBorder(10, 20, 10, 20));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        return b;
    }

    public boolean isCompletado() { return completado; }
}
