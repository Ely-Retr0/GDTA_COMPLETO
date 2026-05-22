package vista.panels;

import package_sistemaTR.MenuRefaccionaria;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

public class Dashboard extends JPanel {

    private MenuRefaccionaria menu;

    public Dashboard(MenuRefaccionaria menu) {
        this.menu = menu;
        setBackground(MenuRefaccionaria.BG_DARK);
        setLayout(new BorderLayout());

        JPanel contenido = new JPanel();
        contenido.setOpaque(false);
        contenido.setLayout(new BoxLayout(contenido, BoxLayout.Y_AXIS));
        contenido.setBorder(new EmptyBorder(30, 40, 30, 40));

        // ─── BANNER ───
        JPanel banner = crearBanner();
        banner.setMaximumSize(new Dimension(Integer.MAX_VALUE, 220));
        banner.setAlignmentX(Component.LEFT_ALIGNMENT);
        contenido.add(banner);
        contenido.add(Box.createVerticalStrut(30));

        // ─── LABEL ACCESO RÁPIDO ───
        JLabel lblAcceso = new JLabel("Acceso Rapido");
        lblAcceso.setFont(new Font("SansSerif", Font.BOLD, 18));
        lblAcceso.setForeground(MenuRefaccionaria.TEXT_WHITE);
        lblAcceso.setAlignmentX(Component.LEFT_ALIGNMENT);
        contenido.add(lblAcceso);
        contenido.add(Box.createVerticalStrut(15));

        // ─── CARDS ───
        JPanel cards = new JPanel(new GridLayout(1, 3, 20, 0));
        cards.setOpaque(false);
        cards.setMaximumSize(new Dimension(Integer.MAX_VALUE, 180));
        cards.setAlignmentX(Component.LEFT_ALIGNMENT);

        cards.add(crearCard("Nueva Venta",   "btventa",    "venta"));
        cards.add(crearCard("Nuevo Cliente", "btcliente",  "clientes"));
        cards.add(crearCard("Nueva Orden",   "btorden",    "clientes"));

        contenido.add(cards);

        JScrollPane scroll = new JScrollPane(contenido);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(MenuRefaccionaria.BG_DARK);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        add(scroll, BorderLayout.CENTER);
    }

    // ─── BANNER bienvenida + gráfica ───
    private JPanel crearBanner() {
        JPanel banner = new JPanel(new GridLayout(1, 2, 0, 0)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(MenuRefaccionaria.BG_CARD);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 20, 20));
                g2.dispose();
            }
        };
        banner.setOpaque(false);

        // Texto bienvenida
        JPanel texto = new JPanel();
        texto.setOpaque(false);
        texto.setLayout(new BoxLayout(texto, BoxLayout.Y_AXIS));
        texto.setBorder(new EmptyBorder(50, 40, 40, 20));

        JLabel l1 = new JLabel("Bienvenido a tu");
        l1.setFont(new Font("SansSerif", Font.BOLD, 26));
        l1.setForeground(MenuRefaccionaria.TEXT_WHITE);
        l1.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel l2 = new JLabel("espacio de trabajo!");
        l2.setFont(new Font("SansSerif", Font.BOLD, 26));
        l2.setForeground(MenuRefaccionaria.TEXT_WHITE);
        l2.setAlignmentX(Component.LEFT_ALIGNMENT);

        texto.add(l1);
        texto.add(Box.createVerticalStrut(8));
        texto.add(l2);
        banner.add(texto);

        // Gráfica
        banner.add(new GraficaActividad());
        return banner;
    }

    // ─── CARD acceso rápido ───
    // El parámetro "tipo" ahora es el nombre del archivo PNG (sin extensión)
    private JPanel crearCard(String titulo, String tipo, String seccion) {
        JPanel card = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 16, 16));
                g2.dispose();
            }
        };
        card.setBackground(MenuRefaccionaria.BG_CARD);
        card.setOpaque(false);
        card.setCursor(new Cursor(Cursor.HAND_CURSOR));
        card.setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel centro = new JPanel();
        centro.setOpaque(false);
        centro.setLayout(new BoxLayout(centro, BoxLayout.Y_AXIS));

        // ─── Ícono PNG ───
        IconoPanel icono = new IconoPanel(tipo);
        icono.setPreferredSize(new Dimension(70, 70));
        icono.setMaximumSize(new Dimension(70, 70));
        icono.setOpaque(false);
        icono.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblTitulo = new JLabel(titulo, SwingConstants.CENTER);
        lblTitulo.setFont(new Font("SansSerif", Font.BOLD, 15));
        lblTitulo.setForeground(MenuRefaccionaria.TEXT_WHITE);
        lblTitulo.setAlignmentX(Component.CENTER_ALIGNMENT);

        centro.add(Box.createVerticalGlue());
        centro.add(icono);
        centro.add(Box.createVerticalStrut(14));
        centro.add(lblTitulo);
        centro.add(Box.createVerticalGlue());

        card.add(centro, BorderLayout.CENTER);

        // Hover y clic
        card.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                menu.mostrar(seccion);
            }
            public void mouseEntered(java.awt.event.MouseEvent e) {
                card.setBackground(MenuRefaccionaria.ACCENT_HOVER);
                card.repaint();
            }
            public void mouseExited(java.awt.event.MouseEvent e) {
                card.setBackground(MenuRefaccionaria.BG_CARD);
                card.repaint();
            }
        });

        return card;
    }

    // ─── Ícono cargado desde src/iconos/<nombre>.png ───
    class IconoPanel extends JPanel {
        private ImageIcon icono;

        IconoPanel(String nombre) {
            setOpaque(false);

            // Busca el PNG en el classpath: src/iconos/<nombre>.png
            java.net.URL url = getClass().getResource("/iconos/" + nombre + ".png");

            if (url != null) {
                Image img = new ImageIcon(url)
                        .getImage()
                        .getScaledInstance(60, 60, Image.SCALE_SMOOTH);
                icono = new ImageIcon(img);
            } else {
                // Fallback: avisa en consola si no encuentra el archivo
                System.err.println("[Dashboard] Ícono no encontrado: /iconos/" + nombre + ".png");
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (icono != null) {
                int x = (getWidth()  - icono.getIconWidth())  / 2;
                int y = (getHeight() - icono.getIconHeight()) / 2;
                icono.paintIcon(this, g, x, y);
            }
        }
    }

    // ─── Gráfica de barras ───
    class GraficaActividad extends JPanel {
        private int[]    datos  = {15, 2, 8, 26, 36, 40};
        private String[] meses  = {"Ene","Feb","Mar","Abr","May","Jun"};

        GraficaActividad() { setOpaque(false); }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int pad = 45, w = getWidth() - pad * 2, h = getHeight() - pad * 2;
            int max = 45, barW = Math.max(1, w / datos.length - 12);

            // Título
            g2.setColor(MenuRefaccionaria.TEXT_WHITE);
            g2.setFont(new Font("SansSerif", Font.BOLD, 13));
            g2.drawString("Grafico de Actividad", pad + w / 2 - 65, 18);

            // Líneas guía y etiquetas Y
            g2.setFont(new Font("SansSerif", Font.PLAIN, 10));
            for (int i = 0; i <= 4; i++) {
                int val = i * 10;
                int y   = pad + h - (h * val / max);
                g2.setColor(MenuRefaccionaria.TEXT_MUTED);
                g2.drawString(String.valueOf(val), pad - 30, y + 4);
                g2.setColor(new Color(50, 60, 95));
                g2.drawLine(pad, y, pad + w, y);
            }

            // Barras
            for (int i = 0; i < datos.length; i++) {
                int barH = h * datos[i] / max;
                int x    = pad + i * (w / datos.length) + 6;
                int y    = pad + h - barH;
                g2.setColor(new Color(100, 150, 230));
                g2.fillRoundRect(x, y, barW, barH, 4, 4);
                g2.setColor(MenuRefaccionaria.TEXT_MUTED);
                g2.drawString(meses[i], x, pad + h + 14);
            }

            // Etiqueta eje X
            g2.setColor(MenuRefaccionaria.TEXT_MUTED);
            g2.drawString("Mes", pad + w, pad + h + 14);
            g2.dispose();
        }
    }
}