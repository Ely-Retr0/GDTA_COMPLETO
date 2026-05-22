package vista.panels;

import package_sistemaTR.MenuRefaccionaria;
import util.NetworkChecker;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

/**
 * GDTA — Asistente de Diagnóstico con IA
 * - Modo online: llama a Claude API
 * - Modo offline: respuestas locales si hay firewall o sin internet
 */
public class PanelAsistenteIA extends JPanel {

    private MenuRefaccionaria menu;
    private JTextArea         txtChat;
    private JTextField        txtInput;
    private JButton           btnEnviar;
    private JTextField        txtApiKey;
    private JLabel            lblConexion;

    public PanelAsistenteIA(MenuRefaccionaria menu) {
        this.menu = menu;
        setLayout(new BorderLayout(20, 0));
        setBackground(MenuRefaccionaria.BG_DARK);
        setBorder(new EmptyBorder(25, 30, 25, 30));
        add(crearSidebar(), BorderLayout.WEST);
        add(crearPanelChat(), BorderLayout.CENTER);
        verificarConexionAsync();
    }

    // ─── SIDEBAR ───
    private JPanel crearSidebar() {
        JPanel p = roundCard();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setPreferredSize(new Dimension(240, 0));
        p.setBorder(new EmptyBorder(20, 15, 20, 15));

        JLabel titulo = new JLabel("🤖  Asistente IA");
        titulo.setFont(new Font("SansSerif", Font.BOLD, 15));
        titulo.setForeground(MenuRefaccionaria.TEXT_WHITE);
        titulo.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(titulo);
        p.add(Box.createVerticalStrut(8));

        // Estado de conexión
        lblConexion = new JLabel("● Verificando conexión...");
        lblConexion.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lblConexion.setForeground(MenuRefaccionaria.TEXT_MUTED);
        lblConexion.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(lblConexion);
        p.add(Box.createVerticalStrut(14));

        JLabel lblKey = new JLabel("API Key (Anthropic):");
        lblKey.setFont(new Font("SansSerif", Font.BOLD, 12));
        lblKey.setForeground(MenuRefaccionaria.TEXT_MUTED);
        lblKey.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(lblKey);
        p.add(Box.createVerticalStrut(5));

        txtApiKey = new JTextField(cargarApiKey());
        txtApiKey.setBackground(MenuRefaccionaria.BG_CARD2);
        txtApiKey.setForeground(MenuRefaccionaria.TEXT_WHITE);
        txtApiKey.setFont(new Font("SansSerif", Font.PLAIN, 11));
        txtApiKey.setCaretColor(Color.WHITE);
        txtApiKey.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        txtApiKey.setAlignmentX(Component.LEFT_ALIGNMENT);
        txtApiKey.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(58, 107, 210), 1),
            BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        p.add(txtApiKey);
        p.add(Box.createVerticalStrut(8));

        JButton btnGuardar = menuBtn("Guardar key", () -> {
            guardarApiKey(txtApiKey.getText().trim());
            verificarConexionAsync();
        });
        p.add(btnGuardar);
        p.add(Box.createVerticalStrut(6));

        JButton btnLimpiar = menuBtn("Limpiar chat", () -> {
            txtChat.setText("");
            agregarMensaje("Sistema", "Chat reiniciado.");
        });
        p.add(btnLimpiar);
        p.add(Box.createVerticalStrut(20));

        // Sugerencias rápidas
        JLabel lblSug = new JLabel("Preguntas rápidas:");
        lblSug.setFont(new Font("SansSerif", Font.BOLD, 12));
        lblSug.setForeground(MenuRefaccionaria.TEXT_MUTED);
        lblSug.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(lblSug);
        p.add(Box.createVerticalStrut(6));

        String[] sugs = {
            "Truena al frenar en frío",
            "No arranca en frío",
            "Pierde aceite por abajo",
            "Vibra al manejar",
            "No agarra velocidad",
            "Falla en ralentí",
            "Recalentamiento del motor"
        };
        for (String sug : sugs) {
            JButton b = new JButton(sug);
            b.setFont(new Font("SansSerif", Font.PLAIN, 11));
            b.setForeground(MenuRefaccionaria.TEXT_MUTED);
            b.setBackground(MenuRefaccionaria.BG_CARD2);
            b.setBorder(new EmptyBorder(5, 8, 5, 8));
            b.setFocusPainted(false);
            b.setCursor(new Cursor(Cursor.HAND_CURSOR));
            b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
            b.setAlignmentX(Component.LEFT_ALIGNMENT);
            b.addActionListener(e -> { txtInput.setText(sug); txtInput.requestFocus(); });
            b.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseEntered(java.awt.event.MouseEvent e) { b.setForeground(Color.WHITE); }
                public void mouseExited(java.awt.event.MouseEvent e)  { b.setForeground(MenuRefaccionaria.TEXT_MUTED); }
            });
            p.add(b);
            p.add(Box.createVerticalStrut(3));
        }

        p.add(Box.createVerticalStrut(12));
        JLabel nota = new JLabel("<html><small>Sin API key funciona en<br>modo offline con diagnósticos<br>predefinidos.</small></html>");
        nota.setForeground(new Color(100, 110, 140));
        nota.setFont(new Font("SansSerif", Font.PLAIN, 10));
        nota.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(nota);
        p.add(Box.createVerticalGlue());
        return p;
    }

    // ─── PANEL CHAT ───
    private JPanel crearPanelChat() {
        JPanel panel = new JPanel(new BorderLayout(0, 15));
        panel.setOpaque(false);

        JLabel titulo = new JLabel("🔧  Asistente de Diagnóstico");
        titulo.setFont(new Font("SansSerif", Font.BOLD, 20));
        titulo.setForeground(MenuRefaccionaria.TEXT_WHITE);
        panel.add(titulo, BorderLayout.NORTH);

        txtChat = new JTextArea();
        txtChat.setEditable(false);
        txtChat.setBackground(MenuRefaccionaria.BG_CARD);
        txtChat.setForeground(MenuRefaccionaria.TEXT_WHITE);
        txtChat.setFont(new Font("SansSerif", Font.PLAIN, 13));
        txtChat.setLineWrap(true);
        txtChat.setWrapStyleWord(true);
        txtChat.setBorder(new EmptyBorder(15, 15, 15, 15));

        JScrollPane sc = new JScrollPane(txtChat);
        sc.setOpaque(false);
        sc.getViewport().setBackground(MenuRefaccionaria.BG_CARD);
        sc.setBorder(BorderFactory.createLineBorder(new Color(40, 48, 80), 1));
        panel.add(sc, BorderLayout.CENTER);

        JPanel inputPanel = new JPanel(new BorderLayout(10, 0));
        inputPanel.setOpaque(false);

        txtInput = new JTextField();
        txtInput.setBackground(MenuRefaccionaria.BG_CARD2);
        txtInput.setForeground(MenuRefaccionaria.TEXT_WHITE);
        txtInput.setFont(new Font("SansSerif", Font.PLAIN, 13));
        txtInput.setCaretColor(Color.WHITE);
        txtInput.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(58, 107, 210), 1),
            BorderFactory.createEmptyBorder(12, 14, 12, 14)));
        txtInput.addActionListener(e -> enviarMensaje());

        btnEnviar = MenuRefaccionaria.btnPrimario("Enviar ▶");
        btnEnviar.addActionListener(e -> enviarMensaje());

        inputPanel.add(txtInput,  BorderLayout.CENTER);
        inputPanel.add(btnEnviar, BorderLayout.EAST);
        panel.add(inputPanel, BorderLayout.SOUTH);

        agregarMensaje("Asistente",
            "Hola, soy tu asistente de diagnóstico mecánico.\n" +
            "Descríbeme los síntomas del vehículo y te ayudaré a identificar " +
            "causas probables, refacciones necesarias y tiempo estimado.\n\n" +
            "💡 Si estás en una red sin acceso a internet, usaré diagnósticos predefinidos.");
        return panel;
    }

    // ─── ENVIAR MENSAJE ───
    private void enviarMensaje() {
        String texto = txtInput.getText().trim();
        if (texto.isEmpty()) return;

        agregarMensaje("Tú", texto);
        txtInput.setText("");
        btnEnviar.setEnabled(false);
        btnEnviar.setText("...");

        new Thread(() -> {
            String apiKey = txtApiKey.getText().trim();
            String estado = NetworkChecker.getEstado();

            String respuesta;
            if (!apiKey.isEmpty() && estado.equals("OK")) {
                // Modo online — Claude API
                try {
                    respuesta = llamarAPI(texto, apiKey);
                } catch (Exception ex) {
                    respuesta = respuestaOffline(texto) +
                        "\n\n⚠ (No se pudo conectar a la IA: " + ex.getMessage() + ")";
                }
            } else {
                // Modo offline
                String razon = switch (estado) {
                    case "SIN_INTERNET" -> "sin conexión a internet";
                    case "API_BLOQUEADA" -> "la API está bloqueada por el firewall de esta red";
                    default -> "no hay API key configurada";
                };
                respuesta = respuestaOffline(texto) +
                    "\n\n🔒 Modo offline (" + razon + ").\n" +
                    "Cuando tengas acceso a internet y API key, obtendrás diagnósticos más precisos.";
            }

            final String resp = respuesta;
            SwingUtilities.invokeLater(() -> {
                agregarMensaje("Asistente", resp);
                btnEnviar.setEnabled(true);
                btnEnviar.setText("Enviar ▶");
            });
        }).start();
    }

    // ─── MODO OFFLINE — diagnósticos predefinidos ───
    private String respuestaOffline(String input) {
        String q = input.toLowerCase();

        if (q.contains("frena") || q.contains("freno") || q.contains("truena") && q.contains("fren")) {
            return "🔧 DIAGNÓSTICO OFFLINE — Problemas de frenos\n\n" +
                "CAUSAS PROBABLES:\n" +
                "1. Pastillas de freno desgastadas\n" +
                "2. Disco rayado o con deformación\n" +
                "3. Líquido de frenos bajo o contaminado\n\n" +
                "REFACCIONES SUGERIDAS:\n" +
                "• Pastillas de freno (juego completo)\n" +
                "• Disco de freno (si está rayado)\n" +
                "• Líquido DOT4\n\n" +
                "TIEMPO ESTIMADO: 1-2 horas";
        }
        if (q.contains("aceite") || q.contains("gotea") || q.contains("mancha")) {
            return "🔧 DIAGNÓSTICO OFFLINE — Fuga de aceite\n\n" +
                "CAUSAS PROBABLES:\n" +
                "1. Empaque del carter dañado\n" +
                "2. Sello de válvulas desgastado\n" +
                "3. Tapón del carter flojo o sin empaque\n\n" +
                "REFACCIONES SUGERIDAS:\n" +
                "• Kit de empaques motor\n" +
                "• Empaque carter\n" +
                "• Tapón carter con empaque nuevo\n\n" +
                "TIEMPO ESTIMADO: 30min-3 horas según la fuga";
        }
        if (q.contains("arranca") || q.contains("enciende") || q.contains("batería") || q.contains("bateria")) {
            return "🔧 DIAGNÓSTICO OFFLINE — Problemas de arranque\n\n" +
                "CAUSAS PROBABLES:\n" +
                "1. Batería descargada o sulfatada\n" +
                "2. Motor de arranque defectuoso\n" +
                "3. Bujías desgastadas\n" +
                "4. Sensor de posición del cigüeñal\n\n" +
                "REFACCIONES SUGERIDAS:\n" +
                "• Batería (si tiene más de 3 años)\n" +
                "• Bujías (juego completo)\n" +
                "• Motor de arranque\n\n" +
                "TIEMPO ESTIMADO: 30min-2 horas";
        }
        if (q.contains("vibra") || q.contains("tiembla") || q.contains("bambolea")) {
            return "🔧 DIAGNÓSTICO OFFLINE — Vibración\n\n" +
                "CAUSAS PROBABLES:\n" +
                "1. Balanceo de llantas necesario\n" +
                "2. Amortiguadores desgastados\n" +
                "3. Rótulas o terminales de dirección\n" +
                "4. Disco de freno deformado\n\n" +
                "REFACCIONES SUGERIDAS:\n" +
                "• Balanceo y alineación (servicio)\n" +
                "• Amortiguadores\n" +
                "• Rótulas\n\n" +
                "TIEMPO ESTIMADO: 1-3 horas";
        }
        if (q.contains("recalenta") || q.contains("temperatura") || q.contains("agua") || q.contains("radiador")) {
            return "🔧 DIAGNÓSTICO OFFLINE — Recalentamiento\n\n" +
                "CAUSAS PROBABLES:\n" +
                "1. Nivel de refrigerante bajo\n" +
                "2. Termostato dañado\n" +
                "3. Radiador obstruido o con fugas\n" +
                "4. Bomba de agua defectuosa\n\n" +
                "REFACCIONES SUGERIDAS:\n" +
                "• Refrigerante anticongelante\n" +
                "• Termostato\n" +
                "• Bomba de agua\n\n" +
                "⚠ URGENTE: No conducir con temperatura alta.\n" +
                "TIEMPO ESTIMADO: 1-4 horas";
        }
        if (q.contains("velocidad") || q.contains("acelera") || q.contains("fuerza") || q.contains("potencia")) {
            return "🔧 DIAGNÓSTICO OFFLINE — Pérdida de potencia\n\n" +
                "CAUSAS PROBABLES:\n" +
                "1. Filtro de aire saturado\n" +
                "2. Bujías desgastadas\n" +
                "3. Filtro de gasolina tapado\n" +
                "4. Inyectores sucios\n\n" +
                "REFACCIONES SUGERIDAS:\n" +
                "• Filtro de aire\n" +
                "• Bujías\n" +
                "• Filtro de gasolina\n" +
                "• Limpieza de inyectores\n\n" +
                "TIEMPO ESTIMADO: 1-2 horas";
        }

        return "🔧 DIAGNÓSTICO OFFLINE\n\n" +
            "No encontré un patrón específico para: \"" + input + "\"\n\n" +
            "Síntomas comunes que puedo diagnosticar offline:\n" +
            "• Problemas de frenos\n• Fugas de aceite\n• No arranca\n" +
            "• Vibraciones\n• Recalentamiento\n• Pérdida de potencia\n\n" +
            "Conéctate a internet y configura tu API key para diagnósticos personalizados.";
    }

    // ─── LLAMADA A CLAUDE API ───
    private String llamarAPI(String userMsg, String apiKey) throws Exception {
        String systemPrompt =
            "Eres un experto mecánico automotriz con 20 años de experiencia en autos y motos. " +
            "El usuario es mecánico del taller GDTA. Ante síntomas responde con: " +
            "1) CAUSAS PROBABLES (las 2-3 más comunes), " +
            "2) REFACCIONES NECESARIAS (nombres específicos), " +
            "3) TIEMPO ESTIMADO de reparación, " +
            "4) ADVERTENCIA si es urgente. " +
            "Responde en español, claro y profesional.";

        String payload = "{" +
            "\"model\":\"claude-sonnet-4-20250514\"," +
            "\"max_tokens\":1024," +
            "\"system\":\"" + escapeJson(systemPrompt) + "\"," +
            "\"messages\":[{\"role\":\"user\",\"content\":\"" + escapeJson(userMsg) + "\"}]" +
            "}";

        URL url = new URI("https://api.anthropic.com/v1/messages").toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type",     "application/json");
        conn.setRequestProperty("x-api-key",         apiKey);
        conn.setRequestProperty("anthropic-version", "2023-06-01");
        conn.setDoOutput(true);
        conn.setConnectTimeout(12000);
        conn.setReadTimeout(30000);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(payload.getBytes(StandardCharsets.UTF_8));
        }

        int code = conn.getResponseCode();
        InputStream is = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
        }

        if (code >= 400) throw new RuntimeException("HTTP " + code + ": " + sb);

        String response = sb.toString();
        int idx = response.indexOf("\"text\":\"");
        if (idx == -1) throw new RuntimeException("Respuesta inesperada");
        int start = idx + 8;
        int end = response.indexOf("\"", start);
        while (end != -1 && response.charAt(end - 1) == '\\') end = response.indexOf("\"", end + 1);
        String texto = response.substring(start, end == -1 ? response.length() : end);
        return texto.replace("\\n", "\n").replace("\\\"", "\"");
    }

    // ─── VERIFICAR CONEXIÓN (async) ───
    private void verificarConexionAsync() {
        new Thread(() -> {
            String estado = NetworkChecker.getEstado();
            SwingUtilities.invokeLater(() -> {
                switch (estado) {
                    case "OK" -> {
                        lblConexion.setText("● Online — IA disponible");
                        lblConexion.setForeground(new Color(80, 200, 120));
                    }
                    case "API_BLOQUEADA" -> {
                        lblConexion.setText("● API bloqueada (firewall)");
                        lblConexion.setForeground(new Color(255, 180, 50));
                    }
                    case "SIN_INTERNET" -> {
                        lblConexion.setText("● Sin internet — Modo offline");
                        lblConexion.setForeground(new Color(196, 70, 70));
                    }
                }
            });
        }).start();
    }

    private void agregarMensaje(String remitente, String mensaje) {
        String pref = switch (remitente) {
            case "Tú"        -> "🧑  Tú:\n";
            case "Asistente" -> "🤖  Asistente:\n";
            case "Error"     -> "❌  Error:\n";
            default          -> "ℹ  Sistema:\n";
        };
        txtChat.append("\n" + pref + mensaje + "\n");
        txtChat.append("\n──────────────────────────────────\n");
        txtChat.setCaretPosition(txtChat.getDocument().getLength());
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    private void guardarApiKey(String key) {
        try (FileOutputStream fos = new FileOutputStream("api_key.txt")) {
            fos.write(key.getBytes(StandardCharsets.UTF_8));
            JOptionPane.showMessageDialog(this, "API key guardada.");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "No se pudo guardar: " + ex.getMessage());
        }
    }

    private String cargarApiKey() {
        try (BufferedReader br = new BufferedReader(new FileReader("api_key.txt"))) {
            return br.readLine().trim();
        } catch (Exception e) { return ""; }
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
        b.setFont(new Font("SansSerif", Font.BOLD, 12));
        b.setForeground(Color.WHITE);
        b.setBackground(MenuRefaccionaria.ACCENT_BLUE);
        b.setBorder(new EmptyBorder(8, 12, 8, 12));
        b.setFocusPainted(false);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.addActionListener(e -> r.run());
        return b;
    }
}
