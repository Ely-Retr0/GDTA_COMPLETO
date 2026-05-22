package package_sistemaTR;

import seguridad.Seguridad;
import vista.panels.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;

/**
 * GDTA — Menú principal con RBAC
 */
public class MenuRefaccionaria extends JFrame {

    private static final long serialVersionUID = 1L;

    // 🎨 Paleta
    public static final Color BG_DARK      = new Color(18, 22, 38);
    public static final Color BG_CARD      = new Color(30, 36, 60);
    public static final Color BG_CARD2     = new Color(38, 45, 72);
    public static final Color ACCENT_BLUE  = new Color(58, 107, 210);
    public static final Color ACCENT_HOVER = new Color(72, 125, 235);
    public static final Color ACCENT_RED   = new Color(196, 70, 70);
    public static final Color ACCENT_GOLD  = new Color(196, 155, 50);
    public static final Color ACCENT_GREEN = new Color(60, 180, 100);
    public static final Color TEXT_WHITE   = new Color(235, 238, 250);
    public static final Color TEXT_MUTED   = new Color(140, 150, 180);
    public static final Color NAV_BG       = new Color(14, 18, 32);
    public static final Color NAV_ACTIVE   = new Color(58, 107, 210);

    private CardLayout cardLayout;
    private JPanel     contentPanel;
    private JButton[]  navBtns;

    public MenuRefaccionaria() {
        setUndecorated(true);

        java.net.URL urlIcono = getClass().getResource("/iconos/logo.png");
        if (urlIcono != null) setIconImage(new ImageIcon(urlIcono).getImage());

        setTitle("GDTA ERP");
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        getContentPane().setBackground(BG_DARK);

        add(crearNavbar(), BorderLayout.NORTH);

        cardLayout   = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        contentPanel.setBackground(BG_DARK);

        contentPanel.add(new Dashboard(this),        "inicio");
        contentPanel.add(new PanelClientes(this),    "clientes");
        contentPanel.add(new PanelInventario(this),  "inventario");
        contentPanel.add(new PanelVenta(this),       "venta");
        contentPanel.add(new PanelOrdenes(this),     "ordenes");
        contentPanel.add(new PanelAsistenteIA(this), "ia");

        // Solo admin ve usuarios y reportes
        if (Seguridad.puedeGestionarUsuarios()) {
            contentPanel.add(new PanelUsuarios(this), "usuarios");
        }

        add(contentPanel, BorderLayout.CENTER);
        mostrar("inicio");
    }

    public void mostrar(String seccion) {
        cardLayout.show(contentPanel, seccion);
        actualizarNavActivo(seccion);
    }

    private JPanel crearNavbar() {
        JPanel nav = new JPanel(new BorderLayout());
        nav.setBackground(NAV_BG);
        nav.setPreferredSize(new Dimension(0, 50));
        nav.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(40, 48, 80)));

        // Logo
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
        left.setOpaque(false);

        java.net.URL urlLogo = getClass().getResource("/iconos/logo.png");
        if (urlLogo != null) {
            Image img = new ImageIcon(urlLogo).getImage().getScaledInstance(28, 28, Image.SCALE_SMOOTH);
            JLabel logo = new JLabel(new ImageIcon(img));
            left.add(logo);
        }
        JLabel nombre = new JLabel("GDTA ERP");
        nombre.setFont(new Font("SansSerif", Font.BOLD, 16));
        nombre.setForeground(TEXT_WHITE);
        left.add(nombre);
        nav.add(left, BorderLayout.WEST);

        // Botones nav según rol
        JPanel center = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        center.setOpaque(false);

        java.util.List<String[]> tabs = new java.util.ArrayList<>();
        tabs.add(new String[]{"🏠  Inicio",      "inicio"});
        tabs.add(new String[]{"👥  Clientes",    "clientes"});

        if (Seguridad.puedeVerInventario())
            tabs.add(new String[]{"📦  Inventario", "inventario"});
        if (Seguridad.puedeRealizarVentas())
            tabs.add(new String[]{"🛒  Ventas",     "venta"});
        if (Seguridad.puedeVerOrdenes())
            tabs.add(new String[]{"🔧  Órdenes",    "ordenes"});

        tabs.add(new String[]{"🤖  Asistente IA", "ia"});

        if (Seguridad.puedeGestionarUsuarios())
            tabs.add(new String[]{"🔑  Usuarios",   "usuarios"});

        navBtns = new JButton[tabs.size()];
        for (int i = 0; i < tabs.size(); i++) {
            final String seccion = tabs.get(i)[1];
            navBtns[i] = navBtn(tabs.get(i)[0], seccion);
            center.add(navBtns[i]);
        }
        nav.add(center, BorderLayout.CENTER);

        // Usuario activo + logout
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        right.setOpaque(false);

        JLabel lblUser = new JLabel(Seguridad.getUsuarioActivo() + "  [" + Seguridad.getRolTexto() + "]");
        lblUser.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lblUser.setForeground(TEXT_MUTED);
        right.add(lblUser);

        JButton btnLogout = new JButton("Cerrar sesión");
        btnLogout.setBackground(ACCENT_RED);
        btnLogout.setForeground(Color.WHITE);
        btnLogout.setFont(new Font("SansSerif", Font.BOLD, 12));
        btnLogout.setBorder(new EmptyBorder(6, 14, 6, 14));
        btnLogout.setFocusPainted(false);
        btnLogout.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnLogout.addActionListener(e -> {
            int ok = JOptionPane.showConfirmDialog(this, "¿Cerrar sesión?", "Logout",
                JOptionPane.YES_NO_OPTION);
            if (ok == JOptionPane.YES_OPTION) {
                Seguridad.logout();
                dispose();
                SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
            }
        });
        right.add(btnLogout);
        nav.add(right, BorderLayout.EAST);

        return nav;
    }

    private JButton navBtn(String texto, String seccion) {
        JButton btn = new JButton(texto);
        btn.setFont(new Font("SansSerif", Font.PLAIN, 13));
        btn.setForeground(TEXT_MUTED);
        btn.setBackground(NAV_BG);
        btn.setBorder(new EmptyBorder(14, 16, 14, 16));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> mostrar(seccion));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                if (!btn.getBackground().equals(NAV_ACTIVE))
                    btn.setForeground(TEXT_WHITE);
            }
            public void mouseExited(MouseEvent e) {
                if (!btn.getBackground().equals(NAV_ACTIVE))
                    btn.setForeground(TEXT_MUTED);
            }
        });
        return btn;
    }

    private void actualizarNavActivo(String seccion) {
        if (navBtns == null) return;
        for (JButton btn : navBtns) {
            boolean activo = btn.getActionListeners().length > 0 &&
                btn.getText().toLowerCase().contains(seccion.toLowerCase().replace("_", " "));
            btn.setBackground(activo ? NAV_ACTIVE : NAV_BG);
            btn.setForeground(activo ? TEXT_WHITE  : TEXT_MUTED);
        }
    }

    // ─── Factory helpers reutilizados por paneles ───
    public static JButton btnPrimario(String texto) {
        JButton b = new JButton(texto);
        b.setBackground(ACCENT_BLUE); b.setForeground(TEXT_WHITE);
        b.setFont(new Font("SansSerif", Font.BOLD, 13));
        b.setBorder(new EmptyBorder(10, 20, 10, 20));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    public static JButton btnPeligro(String texto) {
        JButton b = btnPrimario(texto);
        b.setBackground(ACCENT_RED);
        return b;
    }

    public static JTextField textField(String placeholder) {
        JTextField t = new JTextField(placeholder);
        t.setBackground(BG_CARD2); t.setForeground(TEXT_MUTED);
        t.setFont(new Font("SansSerif", Font.PLAIN, 13));
        t.setCaretColor(TEXT_WHITE);
        t.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(50, 60, 95), 1),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        t.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                if (t.getText().equals(placeholder)) { t.setText(""); t.setForeground(TEXT_WHITE); }
            }
            public void focusLost(FocusEvent e) {
                if (t.getText().isEmpty()) { t.setText(placeholder); t.setForeground(TEXT_MUTED); }
            }
        });
        return t;
    }

    private static class FocusAdapter extends java.awt.event.FocusAdapter {}
}
