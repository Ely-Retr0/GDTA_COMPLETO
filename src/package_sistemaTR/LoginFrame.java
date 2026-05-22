package package_sistemaTR;

import seguridad.Seguridad;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;

/**
 * GDTA — Pantalla de Login
 */
public class LoginFrame extends JFrame {

    private JTextField    txtUsuario;
    private JPasswordField txtPassword;
    private JLabel        lblError;
    private int           intentosFallidos = 0;

    public LoginFrame() {
        setUndecorated(true);
        setSize(420, 520);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setBackground(new Color(0, 0, 0, 0));

        JPanel root = new JPanel(new GridBagLayout());
        root.setBackground(new Color(18, 22, 38));
        setContentPane(root);

        JPanel card = new JPanel(new BorderLayout(0, 18)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(30, 36, 60));
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 20, 20));
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setPreferredSize(new Dimension(360, 420));
        card.setBorder(new EmptyBorder(40, 40, 40, 40));

        // Logo / título
        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));

        java.net.URL urlLogo = getClass().getResource("/iconos/logo.png");
        if (urlLogo != null) {
            Image img = new ImageIcon(urlLogo).getImage().getScaledInstance(64, 64, Image.SCALE_SMOOTH);
            JLabel logo = new JLabel(new ImageIcon(img));
            logo.setAlignmentX(Component.CENTER_ALIGNMENT);
            top.add(logo);
            top.add(Box.createVerticalStrut(12));
        }

        JLabel titulo = new JLabel("GDTA ERP");
        titulo.setFont(new Font("SansSerif", Font.BOLD, 26));
        titulo.setForeground(new Color(235, 238, 250));
        titulo.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel sub = new JLabel("Sistema de Gestión de Taller");
        sub.setFont(new Font("SansSerif", Font.PLAIN, 13));
        sub.setForeground(new Color(140, 150, 180));
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);

        top.add(titulo);
        top.add(Box.createVerticalStrut(4));
        top.add(sub);
        card.add(top, BorderLayout.NORTH);

        // Formulario
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL;
        g.weightx = 1.0;
        g.insets = new Insets(6, 0, 6, 0);

        Color bg  = new Color(38, 45, 72);
        Color fg  = new Color(235, 238, 250);
        Font  f13 = new Font("SansSerif", Font.PLAIN, 13);

        txtUsuario = new JTextField();
        txtUsuario.setBackground(bg); txtUsuario.setForeground(fg);
        txtUsuario.setFont(f13); txtUsuario.setCaretColor(fg);
        txtUsuario.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(58, 107, 210), 1),
            BorderFactory.createEmptyBorder(10, 12, 10, 12)));

        txtPassword = new JPasswordField();
        txtPassword.setBackground(bg); txtPassword.setForeground(fg);
        txtPassword.setFont(f13); txtPassword.setCaretColor(fg);
        txtPassword.setBorder(txtUsuario.getBorder());

        JLabel lblU = new JLabel("Usuario");
        lblU.setForeground(new Color(140, 150, 180)); lblU.setFont(f13);
        JLabel lblP = new JLabel("Contraseña");
        lblP.setForeground(new Color(140, 150, 180)); lblP.setFont(f13);

        g.gridy = 0; form.add(lblU,        g);
        g.gridy = 1; form.add(txtUsuario,  g);
        g.gridy = 2; form.add(lblP,        g);
        g.gridy = 3; form.add(txtPassword, g);

        lblError = new JLabel(" ");
        lblError.setForeground(new Color(196, 70, 70));
        lblError.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lblError.setHorizontalAlignment(SwingConstants.CENTER);
        g.gridy = 4; form.add(lblError, g);

        JButton btnLogin = new JButton("Iniciar sesión");
        btnLogin.setBackground(new Color(58, 107, 210));
        btnLogin.setForeground(fg);
        btnLogin.setFont(new Font("SansSerif", Font.BOLD, 14));
        btnLogin.setBorder(new EmptyBorder(12, 0, 12, 0));
        btnLogin.setFocusPainted(false);
        btnLogin.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnLogin.addActionListener(e -> intentarLogin());
        g.gridy = 5; form.add(btnLogin, g);

        card.add(form, BorderLayout.CENTER);

        // Versión
        JLabel ver = new JLabel("v2.0  •  GDTA ERP");
        ver.setFont(new Font("SansSerif", Font.PLAIN, 11));
        ver.setForeground(new Color(80, 90, 120));
        ver.setHorizontalAlignment(SwingConstants.CENTER);
        card.add(ver, BorderLayout.SOUTH);

        root.add(card);

        // Enter para login
        txtPassword.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) intentarLogin();
            }
        });

        // Drag de ventana sin barra
        MouseAdapter drag = new MouseAdapter() {
            Point start;
            public void mousePressed(MouseEvent e)  { start = e.getPoint(); }
            public void mouseDragged(MouseEvent e)  {
                Point p = LoginFrame.this.getLocation();
                setLocation(p.x + e.getX() - start.x, p.y + e.getY() - start.y);
            }
        };
        root.addMouseListener(drag);
        root.addMouseMotionListener(drag);
    }

    private void intentarLogin() {
        if (intentosFallidos >= 5) {
            lblError.setText("Demasiados intentos. Reinicia el sistema.");
            return;
        }
        String user = txtUsuario.getText().trim();
        String pass = new String(txtPassword.getPassword());

        if (user.isEmpty() || pass.isEmpty()) {
            lblError.setText("Ingresa usuario y contraseña.");
            return;
        }

        if (Seguridad.login(user, pass)) {
            dispose();
            SwingUtilities.invokeLater(() -> new MenuRefaccionaria().setVisible(true));
        } else {
            intentosFallidos++;
            int restantes = 5 - intentosFallidos;
            lblError.setText("Credenciales incorrectas. Intentos restantes: " + restantes);
            txtPassword.setText("");
        }
    }
}
