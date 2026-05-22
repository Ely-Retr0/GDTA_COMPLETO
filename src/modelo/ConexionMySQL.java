package modelo;

import java.io.*;
import java.sql.*;
import java.util.Properties;

/**
 * GDTA — ConexionMySQL
 * Lee credenciales desde config.properties (nunca hardcodeadas).
 */
public class ConexionMySQL {

    private static String URL;
    private static String USUARIO;
    private static String PASSWORD;

    static {
        cargarConfig();
    }

    private static void cargarConfig() {
        // Busca config.properties junto al JAR primero
        File f = new File("config.properties");
        if (!f.exists()) {
            // Fallback: dentro del classpath (desarrollo en Eclipse)
            try (InputStream is = ConexionMySQL.class
                    .getResourceAsStream("/config.properties")) {
                if (is != null) {
                    Properties p = new Properties();
                    p.load(is);
                    URL      = p.getProperty("db.url",      "jdbc:mysql://localhost:3306/bdgdta?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=America/Mexico_City");
                    USUARIO  = p.getProperty("db.user",     "root");
                    PASSWORD = p.getProperty("db.password", "");
                    return;
                }
            } catch (Exception ignored) {}
            // Defaults si no existe nada (primera ejecución)
            URL      = "jdbc:mysql://localhost:3306/bdgdta?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=America/Mexico_City";
            USUARIO  = "root";
            PASSWORD = "";
            return;
        }
        try (FileInputStream fis = new FileInputStream(f)) {
            Properties p = new Properties();
            p.load(fis);
            URL      = p.getProperty("db.url");
            USUARIO  = p.getProperty("db.user");
            PASSWORD = p.getProperty("db.password");
        } catch (Exception e) {
            System.err.println("Error leyendo config.properties: " + e.getMessage());
        }
    }

    public static Connection conectar() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection con = DriverManager.getConnection(URL, USUARIO, PASSWORD);
            return con;
        } catch (Exception e) {
            System.err.println("❌ Error de conexión: " + e.getMessage());
            return null;
        }
    }

    /** Guarda nuevas credenciales en config.properties */
    public static void guardarConfig(String url, String user, String pass) {
        try (FileOutputStream fos = new FileOutputStream("config.properties")) {
            Properties p = new Properties();
            p.setProperty("db.url",      url);
            p.setProperty("db.user",     user);
            p.setProperty("db.password", pass);
            p.store(fos, "GDTA ERP — Configuracion de base de datos");
            URL      = url;
            USUARIO  = user;
            PASSWORD = pass;
        } catch (Exception e) {
            System.err.println("Error guardando config: " + e.getMessage());
        }
    }

    public static boolean probarConexion() {
        try (Connection con = conectar()) {
            return con != null && !con.isClosed();
        } catch (Exception e) {
            return false;
        }
    }
}
