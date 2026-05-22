package package_sistemaTR;

import modelo.ConexionMySQL;
import util.SetupInicial;

import javax.swing.*;

/**
 * GDTA ERP — Punto de entrada
 */
public class MenuPrincipal {

    public static void main(String[] args) {
        // Look & Feel del sistema
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> {
            // Si no hay config o no conecta → mostrar setup
            if (!ConexionMySQL.probarConexion()) {
                SetupInicial setup = new SetupInicial();
                setup.setVisible(true);
                if (!setup.isCompletado()) {
                    System.exit(0);
                }
            }
            // Mostrar login
            new LoginFrame().setVisible(true);
        });
    }
}
