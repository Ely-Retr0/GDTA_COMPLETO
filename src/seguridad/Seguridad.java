package seguridad;

import modelo.ConexionMySQL;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.net.InetAddress;

/**
 * GDTA — Gestor de Seguridad
 * Maneja: login, RBAC, bcrypt, log de auditoría.
 */
public class Seguridad {

    // ─── Usuario activo en sesión ───
    private static int    idUsuario;
    private static String usuarioActivo = "";
    private static Rol    rolActivo     = null;

    public enum Rol { ADMIN, MECANICO, CAJERO }

    // ─── LOGIN ───
    public static boolean login(String username, String password) {
        try (Connection con = ConexionMySQL.conectar()) {
            if (con == null) return false;
            PreparedStatement ps = con.prepareStatement(
                "SELECT id, password_hash, rol FROM usuarios WHERE username = ? AND activo = 1"
            );
            ps.setString(1, username.trim().toLowerCase());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String hash = rs.getString("password_hash");
                if (BCrypt.checkpw(password, hash)) {
                    idUsuario    = rs.getInt("id");
                    usuarioActivo = username.trim().toLowerCase();
                    rolActivo    = Rol.valueOf(rs.getString("rol"));
                    // Actualizar último acceso
                    PreparedStatement upd = con.prepareStatement(
                        "UPDATE usuarios SET ultimo_acceso = NOW() WHERE id = ?"
                    );
                    upd.setInt(1, idUsuario);
                    upd.executeUpdate();
                    registrarAuditoria("LOGIN", "Inicio de sesión exitoso");
                    return true;
                }
            }
        } catch (Exception e) {
            System.err.println("Error en login: " + e.getMessage());
        }
        return false;
    }

    public static void logout() {
        registrarAuditoria("LOGOUT", "Cierre de sesión");
        usuarioActivo = "";
        rolActivo     = null;
        idUsuario     = 0;
    }

    // ─── RBAC — Permisos por rol ───
    public static boolean puedeGestionarUsuarios() {
        return rolActivo == Rol.ADMIN;
    }

    public static boolean puedeVerInventario() {
        return rolActivo == Rol.ADMIN || rolActivo == Rol.CAJERO;
    }

    public static boolean puedeEditarInventario() {
        return rolActivo == Rol.ADMIN;
    }

    public static boolean puedeVerOrdenes() {
        return rolActivo != null; // todos
    }

    public static boolean puedeCrearOrdenes() {
        return rolActivo == Rol.ADMIN || rolActivo == Rol.MECANICO;
    }

    public static boolean puedeEditarOrdenes() {
        return rolActivo == Rol.ADMIN || rolActivo == Rol.MECANICO;
    }

    public static boolean puedeRealizarVentas() {
        return rolActivo == Rol.ADMIN || rolActivo == Rol.CAJERO;
    }

    public static boolean puedeVerReportes() {
        return rolActivo == Rol.ADMIN;
    }

    // ─── BCRYPT ───
    public static String hashPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt(12));
    }

    public static boolean verificarPassword(String password, String hash) {
        try {
            return BCrypt.checkpw(password, hash);
        } catch (Exception e) {
            return false;
        }
    }

    // ─── AUDITORÍA ───
    public static void registrarAuditoria(String accion, String detalle) {
        if (usuarioActivo.isEmpty()) return;
        try (Connection con = ConexionMySQL.conectar()) {
            if (con == null) return;
            String ip = "127.0.0.1";
            try { ip = InetAddress.getLocalHost().getHostAddress(); } catch (Exception ignored) {}
            PreparedStatement ps = con.prepareStatement(
                "INSERT INTO auditoria (usuario, accion, detalle, ip) VALUES (?, ?, ?, ?)"
            );
            ps.setString(1, usuarioActivo);
            ps.setString(2, accion);
            ps.setString(3, detalle);
            ps.setString(4, ip);
            ps.executeUpdate();
        } catch (Exception e) {
            System.err.println("Error auditoría: " + e.getMessage());
        }
    }

    // ─── GETTERS ───
    public static String getUsuarioActivo()  { return usuarioActivo; }
    public static Rol    getRolActivo()       { return rolActivo; }
    public static int    getIdUsuario()       { return idUsuario; }

    public static String getRolTexto() {
        if (rolActivo == null) return "Sin sesión";
        return switch (rolActivo) {
            case ADMIN    -> "Administrador";
            case MECANICO -> "Mecánico";
            case CAJERO   -> "Cajero";
        };
    }

    // ─── CREAR USUARIO (solo admin) ───
    public static boolean crearUsuario(String username, String password, String nombre, Rol rol) {
        if (!puedeGestionarUsuarios()) return false;
        try (Connection con = ConexionMySQL.conectar()) {
            PreparedStatement ps = con.prepareStatement(
                "INSERT INTO usuarios (username, password_hash, nombre, rol) VALUES (?, ?, ?, ?)"
            );
            ps.setString(1, username.trim().toLowerCase());
            ps.setString(2, hashPassword(password));
            ps.setString(3, nombre.trim().toUpperCase());
            ps.setString(4, rol.name());
            ps.executeUpdate();
            registrarAuditoria("CREAR_USUARIO", "Usuario creado: " + username + " Rol: " + rol);
            return true;
        } catch (Exception e) {
            System.err.println("Error creando usuario: " + e.getMessage());
            return false;
        }
    }

    // ─── CAMBIAR CONTRASEÑA ───
    public static boolean cambiarPassword(String username, String nuevaPassword) {
        try (Connection con = ConexionMySQL.conectar()) {
            PreparedStatement ps = con.prepareStatement(
                "UPDATE usuarios SET password_hash = ? WHERE username = ?"
            );
            ps.setString(1, hashPassword(nuevaPassword));
            ps.setString(2, username);
            ps.executeUpdate();
            registrarAuditoria("CAMBIO_PASSWORD", "Cambio de contraseña: " + username);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
